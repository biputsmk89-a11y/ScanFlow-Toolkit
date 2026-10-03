package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.engine.CompressionEngine
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class CompressionEngineImpl : CompressionEngine {

    companion object {
        private const val TAG = "CompressionEngine"
    }

    override suspend fun compressPdf(
        inputFile: File,
        config: CompressionConfig,
        outputFile: File,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val originalSize = inputFile.length()

        try {
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile).use { document ->
                val totalPages = document.numberOfPages

                // 1. Strip Metadata if requested
                if (config.stripMetadata) {
                    val info = document.documentInformation
                    info.cosObject.removeItem(COSName.TITLE)
                    info.cosObject.removeItem(COSName.AUTHOR)
                    info.cosObject.removeItem(COSName.SUBJECT)
                    info.cosObject.removeItem(COSName.KEYWORDS)
                    info.title = null
                    info.author = null
                    info.subject = null
                    info.keywords = null
                    info.creator = null
                    info.producer = null
                    info.creationDate = null
                    info.modificationDate = null
                    try {
                        document.documentCatalog.metadata = null
                    } catch (e: Exception) {
                        SafeLogger.w(TAG, "Metadata stripping notice: ${e.message}")
                    }
                }

                // 2. Optimize Embedded Images
                if (config.compressImages) {
                    val quality = config.level.imageQuality
                    val maxDpi = config.level.maxImageDpi

                    for (i in 0 until totalPages) {
                        val page = document.getPage(i)
                        val resources = page.resources ?: continue
                        compressImagesInResources(document, resources, quality, maxDpi)
                        onProgress?.invoke(i + 1, totalPages)
                    }
                }

                document.save(outputFile)
            }

            // Verify output
            if (!outputFile.exists() || outputFile.length() == 0L) {
                return@withContext OperationResult.failure(
                    OperationType.COMPRESS_PDF,
                    ErrorCode.COMPRESSION_FAILED,
                    "Compressed output file was not created or is empty."
                )
            }

            // If compressed output is somehow larger than original, copy original
            val finalOutputFile = if (outputFile.length() > originalSize) {
                inputFile.copyTo(outputFile, overwrite = true)
            } else {
                outputFile
            }

            val newSize = finalOutputFile.length()
            val duration = System.currentTimeMillis() - startTime
            val reductionPercentage = if (originalSize > 0) {
                (((originalSize - newSize).toDouble() / originalSize.toDouble()) * 100.0)
                    .coerceAtLeast(0.0)
                    .roundToInt()
            } else 0

            val metadata = mapOf(
                "originalSize" to "$originalSize",
                "newSize" to "$newSize",
                "reduction" to "$reductionPercentage%",
                "duration" to "${duration}ms"
            )

            SafeLogger.logOperation(
                "SF-017",
                OperationType.COMPRESS_PDF,
                com.scanflow.app.core.result.OperationStatus.SUCCESS,
                duration
            )

            OperationResult.success(
                operationType = OperationType.COMPRESS_PDF,
                outputPath = finalOutputFile.absolutePath,
                outputSize = newSize,
                durationMs = duration,
                metadata = metadata
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.COMPRESSION_FAILED, e)
            OperationResult.failure(
                OperationType.COMPRESS_PDF,
                ErrorCode.COMPRESSION_FAILED,
                e.message ?: "Failed to compress PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    private fun compressImagesInResources(
        document: PDDocument,
        resources: PDResources,
        quality: Int,
        maxDpi: Int
    ) {
        val xObjectNames = resources.xObjectNames ?: return
        for (name in xObjectNames) {
            try {
                val xObject = resources.getXObject(name)
                if (xObject is PDImageXObject) {
                    val origWidth = xObject.width
                    val origHeight = xObject.height

                    // Downsample if image resolution is very high (> 1600px)
                    val maxDimension = (maxDpi * 8.5).toInt().coerceIn(800, 2000)
                    val scale = if (origWidth > maxDimension || origHeight > maxDimension) {
                        maxOf(origWidth.toFloat() / maxDimension, origHeight.toFloat() / maxDimension)
                    } else 1.0f

                    val targetW = (origWidth / scale).toInt().coerceAtLeast(1)
                    val targetH = (origHeight / scale).toInt().coerceAtLeast(1)

                    val originalBitmap = try { xObject.image } catch (e: Throwable) { null } ?: continue
                    try {
                        val scaledBitmap = if (scale > 1.0f) {
                            try {
                                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
                            } catch (e: Throwable) {
                                originalBitmap
                            }
                        } else {
                            originalBitmap
                        }

                        try {
                            val compressedXObject = JPEGFactory.createFromImage(document, scaledBitmap, quality / 100f)
                            resources.put(name, compressedXObject)
                        } finally {
                            if (scaledBitmap !== originalBitmap) {
                                scaledBitmap.recycle()
                            }
                        }
                    } finally {
                        originalBitmap.recycle()
                    }
                }
            } catch (e: Throwable) {
                // If specific embedded image fails to re-encode, preserve existing
                SafeLogger.w(TAG, "Skipping image optimization: ${e.message}")
            }
        }
    }
}

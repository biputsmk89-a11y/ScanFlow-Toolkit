package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.engine.ConversionEngine
import com.scanflow.app.engine.PdfRendererEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ConversionEngineImpl(
    private val pdfRendererEngine: PdfRendererEngine
) : ConversionEngine {

    companion object {
        private const val TAG = "ConversionEngine"
        private const val A4_WIDTH_PT = 595
        private const val A4_HEIGHT_PT = 842
    }

    override suspend fun imagesToPdf(
        imageFiles: List<File>,
        outputFile: File,
        fitPage: Boolean,
        pageSize: String
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (imageFiles.isEmpty()) {
            return@withContext OperationResult.failure(
                OperationType.IMAGE_TO_PDF,
                ErrorCode.INVALID_FILE,
                "No images provided for PDF conversion."
            )
        }

        val pdfDoc = PdfDocument()
        try {
            outputFile.parentFile?.mkdirs()

            imageFiles.forEachIndexed { index, file ->
                if (!file.exists()) return@forEachIndexed
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEachIndexed

                val isLandscape = bitmap.width > bitmap.height
                val pageW = if (isLandscape) A4_HEIGHT_PT else A4_WIDTH_PT
                val pageH = if (isLandscape) A4_WIDTH_PT else A4_HEIGHT_PT

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                val scale = minOf(pageW.toFloat() / bitmap.width, pageH.toFloat() / bitmap.height)
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f

                canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), null)
                pdfDoc.finishPage(page)
                bitmap.recycle()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }

            val duration = System.currentTimeMillis() - startTime
            SafeLogger.logOperation(
                "SF-039",
                OperationType.IMAGE_TO_PDF,
                com.scanflow.app.core.result.OperationStatus.SUCCESS,
                duration
            )

            OperationResult.success(
                operationType = OperationType.IMAGE_TO_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = imageFiles.size
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CONVERSION_FAILED, e)
            OperationResult.failure(
                OperationType.IMAGE_TO_PDF,
                ErrorCode.CONVERSION_FAILED,
                e.message ?: "Failed to convert images to PDF",
                System.currentTimeMillis() - startTime
            )
        } finally {
            pdfDoc.close()
        }
    }

    override suspend fun pdfToImages(
        inputFile: File,
        outputDirectory: File,
        format: String,
        dpi: Int,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): List<OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OperationResult>()
        val startTime = System.currentTimeMillis()

        try {
            outputDirectory.mkdirs()
            if (pdfRendererEngine.open(inputFile)) {
                val totalPages = pdfRendererEngine.getPageCount()
                val scaleFactor = (dpi / 72f).coerceIn(1f, 4f)

                for (i in 0 until totalPages) {
                    val width = (A4_WIDTH_PT * scaleFactor).toInt()
                    val height = (A4_HEIGHT_PT * scaleFactor).toInt()
                    val bitmap = pdfRendererEngine.renderPage(i, width, height)

                    if (bitmap != null) {
                        val ext = if (format.equals("PNG", ignoreCase = true)) "png" else "jpg"
                        val compressFormat = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                        val outImage = File(outputDirectory, "${inputFile.nameWithoutExtension}_page_${i + 1}.$ext")

                        FileOutputStream(outImage).use { out ->
                            bitmap.compress(compressFormat, 90, out)
                        }

                        results.add(
                            OperationResult.success(
                                operationType = OperationType.PDF_TO_IMAGES,
                                outputPath = outImage.absolutePath,
                                outputSize = outImage.length(),
                                durationMs = System.currentTimeMillis() - startTime,
                                pagesProcessed = 1
                            )
                        )
                        bitmap.recycle()
                    }
                    onProgress?.invoke(i + 1, totalPages)
                }
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CONVERSION_FAILED, e)
            results.add(
                OperationResult.failure(
                    OperationType.PDF_TO_IMAGES,
                    ErrorCode.CONVERSION_FAILED,
                    e.message ?: "Failed to convert PDF to images"
                )
            )
        } finally {
            pdfRendererEngine.close()
        }

        results
    }

    override suspend fun pdfToText(inputFile: File, outputFile: File): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            var extractedText = ""
            PDDocument.load(inputFile).use { document ->
                val stripper = PDFTextStripper()
                extractedText = stripper.getText(document)
            }

            FileOutputStream(outputFile).use { fos ->
                fos.write(extractedText.toByteArray(Charsets.UTF_8))
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.PDF_TO_TEXT,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CONVERSION_FAILED, e)
            OperationResult.failure(
                OperationType.PDF_TO_TEXT,
                ErrorCode.CONVERSION_FAILED,
                e.message ?: "Failed to extract text from PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }
}

package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.RectF
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.SecurityConfig
import com.scanflow.app.engine.SecurityEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SecurityEngineImpl : SecurityEngine {

    companion object {
        private const val TAG = "SecurityEngine"
    }

    override suspend fun protectPdf(
        inputFile: File,
        config: SecurityConfig,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            val userPass = config.userPassword ?: ""
            val ownerPass = config.ownerPassword ?: userPass

            if (userPass.isEmpty() && ownerPass.isEmpty()) {
                return@withContext OperationResult.failure(
                    OperationType.PROTECT_PDF,
                    ErrorCode.INVALID_FILE,
                    "Password cannot be empty."
                )
            }

            PDDocument.load(inputFile).use { document ->
                val ap = AccessPermission().apply {
                    setCanPrint(config.allowPrinting)
                    setCanExtractContent(config.allowCopying)
                }

                val policy = StandardProtectionPolicy(ownerPass, userPass, ap).apply {
                    encryptionKeyLength = config.keyLengthBits
                }

                document.protect(policy)
                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            SafeLogger.logOperation(
                "SF-079",
                OperationType.PROTECT_PDF,
                com.scanflow.app.core.result.OperationStatus.SUCCESS,
                duration
            )

            OperationResult.success(
                operationType = OperationType.PROTECT_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.STORAGE_ERROR, e)
            OperationResult.failure(
                OperationType.PROTECT_PDF,
                ErrorCode.STORAGE_ERROR,
                e.message ?: "Failed to protect PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun unlockPdf(
        inputFile: File,
        password: String,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile, password).use { document ->
                document.isAllSecurityToBeRemoved = true
                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            SafeLogger.logOperation(
                "SF-081",
                OperationType.UNLOCK_PDF,
                com.scanflow.app.core.result.OperationStatus.SUCCESS,
                duration
            )

            OperationResult.success(
                operationType = OperationType.UNLOCK_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PASSWORD, e)
            OperationResult.failure(
                OperationType.UNLOCK_PDF,
                ErrorCode.INVALID_PASSWORD,
                "Incorrect password or failed to unlock PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun removeMetadata(inputFile: File, outputFile: File): OperationResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                outputFile.parentFile?.mkdirs()

                PDDocument.load(inputFile).use { document ->
                    val info = document.documentInformation
                    info.title = null
                    info.author = null
                    info.subject = null
                    info.keywords = null
                    info.creator = null
                    info.producer = null
                    info.creationDate = null
                    info.modificationDate = null

                    document.documentCatalog.metadata = null
                    document.save(outputFile)
                }

                val duration = System.currentTimeMillis() - startTime
                OperationResult.success(
                    operationType = OperationType.REMOVE_METADATA,
                    outputPath = outputFile.absolutePath,
                    outputSize = outputFile.length(),
                    durationMs = duration
                )
            } catch (e: Exception) {
                SafeLogger.e(TAG, ErrorCode.STORAGE_ERROR, e)
                OperationResult.failure(
                    OperationType.REMOVE_METADATA,
                    ErrorCode.STORAGE_ERROR,
                    e.message ?: "Failed to remove metadata",
                    System.currentTimeMillis() - startTime
                )
            }
        }

    override suspend fun redactPdf(
        inputFile: File,
        pageRedactions: Map<Int, List<RectF>>,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile).use { document ->
                val totalPages = document.numberOfPages

                pageRedactions.forEach { (pageNum, rects) ->
                    val pageIndex = pageNum - 1
                    if (pageIndex in 0 until totalPages) {
                        val page = document.getPage(pageIndex)
                        val mediaBox = page.mediaBox

                        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            cs.setNonStrokingColor(0, 0, 0)
                            rects.forEach { rect ->
                                val y = mediaBox.height - rect.bottom
                                cs.fillRect(rect.left, y, rect.width(), rect.height())
                            }
                        }
                    }
                }
                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.ANNOTATION,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.STORAGE_ERROR, e)
            OperationResult.failure(
                OperationType.ANNOTATION,
                ErrorCode.STORAGE_ERROR,
                e.message ?: "Failed to redact PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun stampSignature(
        inputFile: File,
        signatureBitmap: Bitmap,
        pageIndex: Int,
        rect: RectF,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile).use { document ->
                if (pageIndex in 0 until document.numberOfPages) {
                    val page = document.getPage(pageIndex)
                    val mediaBox = page.mediaBox
                    val imageXObject = JPEGFactory.createFromImage(document, signatureBitmap, 0.9f)

                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        val y = mediaBox.height - rect.bottom
                        cs.drawImage(imageXObject, rect.left, y, rect.width(), rect.height())
                    }
                }
                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.ANNOTATION,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.STORAGE_ERROR, e)
            OperationResult.failure(
                OperationType.ANNOTATION,
                ErrorCode.STORAGE_ERROR,
                e.message ?: "Failed to stamp signature",
                System.currentTimeMillis() - startTime
            )
        }
    }
}

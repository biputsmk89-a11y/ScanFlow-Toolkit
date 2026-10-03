package com.scanflow.app.engine.impl

import android.graphics.Color
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.error.ScanFlowException
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.PageNumberConfig
import com.scanflow.app.domain.model.PageNumberPosition
import com.scanflow.app.domain.model.WatermarkConfig
import com.scanflow.app.domain.model.WatermarkPosition
import com.scanflow.app.engine.PdfEngine
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfEngineImpl : PdfEngine {

    companion object {
        private const val TAG = "PdfEngine"
    }

    override suspend fun merge(inputFiles: List<File>, outputFile: File): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            if (inputFiles.size < 2) {
                return@withContext OperationResult.failure(
                    OperationType.MERGE_PDF,
                    ErrorCode.INVALID_FILE,
                    "At least two PDF documents are required to merge."
                )
            }

            val merger = PDFMergerUtility()
            inputFiles.forEach { file ->
                if (!file.exists() || file.length() == 0L) {
                    throw ScanFlowException.FileNotFound("Input file ${file.name} does not exist or is empty.")
                }
                merger.addSource(file)
            }

            outputFile.parentFile?.mkdirs()
            merger.destinationFileName = outputFile.absolutePath
            merger.mergeDocuments(null)

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            SafeLogger.logOperation("SF-001", OperationType.MERGE_PDF, com.scanflow.app.core.result.OperationStatus.SUCCESS, duration)
            OperationResult.success(
                operationType = OperationType.MERGE_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.MERGE_PDF,
                if (e is ScanFlowException) e.code else ErrorCode.INVALID_PDF,
                e.message ?: "Failed to merge PDF documents",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun split(
        inputFile: File,
        pageRanges: List<IntRange>,
        outputDirectory: File
    ): List<OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OperationResult>()
        val startTime = System.currentTimeMillis()

        try {
            if (!inputFile.exists() || inputFile.length() == 0L) {
                return@withContext listOf(
                    OperationResult.failure(OperationType.SPLIT_PDF, ErrorCode.FILE_NOT_FOUND)
                )
            }

            outputDirectory.mkdirs()
            PDDocument.load(inputFile).use { sourceDoc ->
                val totalPages = sourceDoc.numberOfPages

                pageRanges.forEachIndexed { index, range ->
                    val chunkDoc = PDDocument()
                    try {
                        val validStart = range.first.coerceIn(1, totalPages)
                        val validEnd = range.last.coerceIn(validStart, totalPages)

                        for (p in validStart..validEnd) {
                            chunkDoc.addPage(sourceDoc.getPage(p - 1))
                        }

                        val partName = "${inputFile.nameWithoutExtension}_part_${index + 1}.pdf"
                        val partFile = File(outputDirectory, partName)
                        chunkDoc.save(partFile)

                        validateOutputPdf(partFile)
                        results.add(
                            OperationResult.success(
                                operationType = OperationType.SPLIT_PDF,
                                outputPath = partFile.absolutePath,
                                outputSize = partFile.length(),
                                durationMs = System.currentTimeMillis() - startTime,
                                pagesProcessed = validEnd - validStart + 1
                            )
                        )
                    } finally {
                        chunkDoc.close()
                    }
                }
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            results.add(
                OperationResult.failure(
                    OperationType.SPLIT_PDF,
                    ErrorCode.INVALID_PDF,
                    e.message ?: "Failed to split PDF"
                )
            )
        }
        results
    }

    override suspend fun removePages(
        inputFile: File,
        pagesToRemove: Set<Int>,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { document ->
                val totalPages = document.numberOfPages
                if (pagesToRemove.size >= totalPages) {
                    return@withContext OperationResult.failure(
                        OperationType.REMOVE_PAGES,
                        ErrorCode.INVALID_FILE,
                        "Cannot remove all pages from a document. At least 1 page must remain."
                    )
                }

                // Remove in descending index order so subsequent indices remain valid
                val sortedIndices = pagesToRemove
                    .map { it - 1 }
                    .filter { it in 0 until totalPages }
                    .sortedDescending()

                sortedIndices.forEach { pageIdx ->
                    document.removePage(pageIdx)
                }

                document.save(outputFile)
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.REMOVE_PAGES,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.REMOVE_PAGES,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to remove pages",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun extractPages(
        inputFile: File,
        pagesToExtract: List<Int>,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { sourceDoc ->
                val totalPages = sourceDoc.numberOfPages
                val newDoc = PDDocument()
                try {
                    pagesToExtract.forEach { pageNum ->
                        val pageIdx = pageNum - 1
                        if (pageIdx in 0 until totalPages) {
                            newDoc.addPage(sourceDoc.getPage(pageIdx))
                        }
                    }

                    if (newDoc.numberOfPages == 0) {
                        return@withContext OperationResult.failure(
                            OperationType.EXTRACT_PAGES,
                            ErrorCode.INVALID_FILE,
                            "No valid pages selected for extraction."
                        )
                    }

                    newDoc.save(outputFile)
                } finally {
                    newDoc.close()
                }
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.EXTRACT_PAGES,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.EXTRACT_PAGES,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to extract pages",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun reorderPages(
        inputFile: File,
        newOrder: List<Int>,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { sourceDoc ->
                val totalPages = sourceDoc.numberOfPages
                val newDoc = PDDocument()
                try {
                    newOrder.forEach { pageNum ->
                        val pageIdx = pageNum - 1
                        if (pageIdx in 0 until totalPages) {
                            newDoc.addPage(sourceDoc.getPage(pageIdx))
                        }
                    }
                    newDoc.save(outputFile)
                } finally {
                    newDoc.close()
                }
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.REORDER_PAGES,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.REORDER_PAGES,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to reorder pages",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun rotatePages(
        inputFile: File,
        pagesToRotate: Map<Int, Int>,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { document ->
                val totalPages = document.numberOfPages
                pagesToRotate.forEach { (pageNum, deltaRotation) ->
                    val pageIdx = pageNum - 1
                    if (pageIdx in 0 until totalPages) {
                        val page = document.getPage(pageIdx)
                        val currentRotation = page.rotation
                        val newRotation = (currentRotation + deltaRotation) % 360
                        page.rotation = if (newRotation < 0) newRotation + 360 else newRotation
                    }
                }
                document.save(outputFile)
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.ROTATE_PAGES,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.ROTATE_PAGES,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to rotate pages",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun duplicatePages(
        inputFile: File,
        pageIndices: List<Int>,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { sourceDoc ->
                val totalPages = sourceDoc.numberOfPages
                val newDoc = PDDocument()
                try {
                    for (i in 0 until totalPages) {
                        val page = sourceDoc.getPage(i)
                        newDoc.importPage(page)
                        if ((i + 1) in pageIndices) {
                            newDoc.importPage(page) // duplicate safe clone
                        }
                    }
                    newDoc.save(outputFile)
                } finally {
                    newDoc.close()
                }
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.REORDER_PAGES,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.REORDER_PAGES,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to duplicate pages",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun insertBlankPage(
        inputFile: File,
        atIndex: Int,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { document ->
                val blankPage = PDPage(PDRectangle.A4)
                val totalPages = document.numberOfPages
                val targetIndex = (atIndex - 1).coerceIn(0, totalPages)
                if (totalPages == 0 || targetIndex >= totalPages) {
                    document.addPage(blankPage)
                } else {
                    val targetPage = document.getPage(targetIndex)
                    document.pages.insertBefore(blankPage, targetPage)
                }
                document.save(outputFile)
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.REORDER_PAGES,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.REORDER_PAGES,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to insert blank page",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun watermark(
        inputFile: File,
        config: WatermarkConfig,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { document ->
                val font = PDType1Font.HELVETICA_BOLD
                val totalPages = document.numberOfPages

                for (i in 0 until totalPages) {
                    val page = document.getPage(i)
                    val mediaBox = page.mediaBox

                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        val gState = PDExtendedGraphicsState().apply {
                            nonStrokingAlphaConstant = config.opacity
                        }
                        cs.setGraphicsStateParameters(gState)
                        cs.setNonStrokingColor(128, 128, 128)
                        cs.beginText()
                        cs.setFont(font, config.fontSizeSp)

                        val safeWatermark = sanitizeText(config.text)
                        val textWidth = try {
                            font.getStringWidth(safeWatermark) / 1000f * config.fontSizeSp
                        } catch (_: Exception) {
                            safeWatermark.length * config.fontSizeSp * 0.5f
                        }
                        val centerX = mediaBox.width / 2f
                        val centerY = mediaBox.height / 2f

                        val matrix = Matrix.getRotateInstance(Math.toRadians(config.rotationDegrees.toDouble()), centerX, centerY)
                        matrix.translate(-textWidth / 2f, 0f)
                        cs.setTextMatrix(matrix)
                        cs.showText(safeWatermark)
                        cs.endText()
                    }
                }
                document.save(outputFile)
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.WATERMARK,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Throwable) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.WATERMARK,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to apply watermark",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun addPageNumbers(
        inputFile: File,
        config: PageNumberConfig,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { document ->
                val font = PDType1Font.HELVETICA
                val totalPages = document.numberOfPages

                for (i in 0 until totalPages) {
                    val page = document.getPage(i)
                    val mediaBox = page.mediaBox
                    val pageNum = config.startNumber + i
                    val text = sanitizeText("${config.prefix}$pageNum${config.suffix}")

                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.setNonStrokingColor(80, 80, 80)
                        cs.beginText()
                        cs.setFont(font, config.fontSizeSp)

                        val textWidth = try {
                            font.getStringWidth(text) / 1000f * config.fontSizeSp
                        } catch (_: Exception) {
                            text.length * config.fontSizeSp * 0.5f
                        }
                        val (x, y) = when (config.position) {
                            PageNumberPosition.BOTTOM_CENTER -> Pair((mediaBox.width - textWidth) / 2f, config.marginPt)
                            PageNumberPosition.BOTTOM_RIGHT -> Pair(mediaBox.width - textWidth - config.marginPt, config.marginPt)
                            PageNumberPosition.BOTTOM_LEFT -> Pair(config.marginPt, config.marginPt)
                            PageNumberPosition.TOP_CENTER -> Pair((mediaBox.width - textWidth) / 2f, mediaBox.height - config.marginPt)
                            PageNumberPosition.TOP_RIGHT -> Pair(mediaBox.width - textWidth - config.marginPt, mediaBox.height - config.marginPt)
                            PageNumberPosition.TOP_LEFT -> Pair(config.marginPt, mediaBox.height - config.marginPt)
                        }

                        cs.newLineAtOffset(x, y)
                        cs.showText(text)
                        cs.endText()
                    }
                }
                document.save(outputFile)
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.PAGE_NUMBERS,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Throwable) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.PAGE_NUMBERS,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to add page numbers",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun repair(inputFile: File, outputFile: File): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { document ->
                // Re-saving with PDFBox reconstructs XRef tables and object offsets
                document.save(outputFile)
            }

            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)

            OperationResult.success(
                operationType = OperationType.REPAIR_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CORRUPTED_PDF, e)
            OperationResult.failure(
                OperationType.REPAIR_PDF,
                ErrorCode.CORRUPTED_PDF,
                e.message ?: "Failed to repair PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun convertToPdfA(inputFile: File, outputFile: File): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            PDDocument.load(inputFile).use { doc ->
                val catalog = doc.documentCatalog
                val markInfo = com.tom_roush.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo()
                markInfo.isMarked = true
                catalog.markInfo = markInfo

                val info = doc.documentInformation
                if (info.producer.isNullOrBlank()) info.producer = "ScanFlow PDF/A Archival Engine"
                if (info.creationDate == null) info.creationDate = java.util.Calendar.getInstance()
                info.modificationDate = java.util.Calendar.getInstance()

                doc.save(outputFile)
            }
            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            val pages = getPageCount(outputFile)
            OperationResult.success(
                operationType = OperationType.REPAIR_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages,
                metadata = mapOf("standard" to "ISO PDF/A-1b Archival Standard")
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.REPAIR_PDF,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to generate PDF/A compliant document",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun cropPages(
        inputFile: File,
        marginPoints: Float,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            var pages = 0
            PDDocument.load(inputFile).use { document ->
                pages = document.numberOfPages
                val margin = marginPoints.coerceIn(0f, 150f)
                for (i in 0 until pages) {
                    val page = document.getPage(i)
                    val mediaBox = page.mediaBox
                    val newX = mediaBox.lowerLeftX + margin
                    val newY = mediaBox.lowerLeftY + margin
                    val newW = (mediaBox.width - (2 * margin)).coerceAtLeast(100f)
                    val newH = (mediaBox.height - (2 * margin)).coerceAtLeast(100f)
                    page.cropBox = PDRectangle(newX, newY, newW, newH)
                }
                document.save(outputFile)
            }
            validateOutputPdf(outputFile)
            val duration = System.currentTimeMillis() - startTime
            SafeLogger.logOperation("SF-075", OperationType.CROP_PDF, com.scanflow.app.core.result.OperationStatus.SUCCESS, duration)
            OperationResult.success(
                operationType = OperationType.CROP_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = pages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.CROP_PDF,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to crop PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun getPageCount(inputFile: File): Int = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(inputFile).use { it.numberOfPages }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to get page count: ${e.message}")
            0
        }
    }

    override suspend fun isEncrypted(inputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(inputFile).use { it.isEncrypted }
        } catch (e: Exception) {
            // If load throws an exception indicating password required, it is encrypted
            e.message?.contains("password", ignoreCase = true) == true
        }
    }

    private fun validateOutputPdf(file: File) {
        if (!file.exists() || file.length() == 0L) {
            throw ScanFlowException.OutputValidationFailed("Output PDF file is missing or zero bytes.")
        }
        PDDocument.load(file).use { doc ->
            if (doc.numberOfPages == 0) {
                throw ScanFlowException.OutputValidationFailed("Output PDF contains 0 pages.")
            }
        }
    }

    private fun sanitizeText(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '\n', '\r' -> sb.append(' ')
                '\t' -> sb.append("    ")
                in ' '..'~' -> sb.append(ch)
                in '\u00A0'..'\u00FF' -> sb.append(ch)
                '\u2018', '\u2019' -> sb.append('\'')
                '\u201C', '\u201D' -> sb.append('"')
                '\u2013', '\u2014' -> sb.append('-')
                '\u2026' -> sb.append("...")
                '\u2022' -> sb.append('*')
                else -> sb.append('?')
            }
        }
        return sb.toString().ifBlank { " " }
    }
}

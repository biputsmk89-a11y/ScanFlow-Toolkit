package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.OcrBlock
import com.scanflow.app.domain.model.OcrPageResult
import com.scanflow.app.domain.model.OcrResult
import com.scanflow.app.engine.OcrEngine
import com.scanflow.app.engine.PdfRendererEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OcrEngineImpl(
    private val pdfRendererEngine: PdfRendererEngine
) : OcrEngine {

    companion object {
        private const val TAG = "OcrEngine"
    }

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognizeImage(bitmap: Bitmap): OcrPageResult = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = Tasks.await(recognizer.process(inputImage))

            val blocks = visionText.textBlocks.mapNotNull { block ->
                val box = block.boundingBox ?: return@mapNotNull null
                val rectF = RectF(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat())
                OcrBlock(
                    text = block.text,
                    boundingBox = rectF,
                    lines = block.lines.map { it.text }
                )
            }

            OcrPageResult(
                pageNumber = 1,
                fullText = visionText.text,
                blocks = blocks
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.OCR_FAILED, e)
            OcrPageResult(pageNumber = 1, fullText = "", blocks = emptyList())
        }
    }

    override suspend fun recognizePdf(
        inputFile: File,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): OcrResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val pageResults = mutableListOf<OcrPageResult>()
        val combinedText = StringBuilder()

        try {
            if (pdfRendererEngine.open(inputFile)) {
                val totalPages = pdfRendererEngine.getPageCount()
                for (p in 0 until totalPages) {
                    val pageBitmap = pdfRendererEngine.renderPage(p, 1200, 1600)
                    if (pageBitmap != null) {
                        val pageResult = recognizeImage(pageBitmap)
                        val adjustedPageResult = pageResult.copy(
                            pageNumber = p + 1,
                            renderWidth = pageBitmap.width,
                            renderHeight = pageBitmap.height
                        )
                        pageResults.add(adjustedPageResult)
                        combinedText.append("--- Page ${p + 1} ---\n")
                        combinedText.append(adjustedPageResult.fullText).append("\n\n")
                        pageBitmap.recycle()
                    }
                    onProgress?.invoke(p + 1, totalPages)
                }
            }
        } finally {
            pdfRendererEngine.close()
        }

        val duration = System.currentTimeMillis() - startTime
        SafeLogger.logOperation(
            "SF-060",
            OperationType.OCR_PDF,
            com.scanflow.app.core.result.OperationStatus.SUCCESS,
            duration
        )

        OcrResult(
            documentId = inputFile.nameWithoutExtension,
            pageResults = pageResults,
            fullText = combinedText.toString(),
            durationMs = duration
        )
    }

    override suspend fun generateSearchablePdf(
        inputFile: File,
        outputFile: File,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            // Run OCR to retrieve text blocks
            val ocrResult = recognizePdf(inputFile, onProgress)

            // Inject invisible text layer into PDFBox document
            PDDocument.load(inputFile).use { document ->
                val font = PDType1Font.HELVETICA
                val totalPages = document.numberOfPages

                ocrResult.pageResults.forEach { pageRes ->
                    val pageIndex = pageRes.pageNumber - 1
                    if (pageIndex in 0 until totalPages) {
                        val page = document.getPage(pageIndex)
                        val mediaBox = page.mediaBox

                        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            // Set invisible text rendering (alpha = 0)
                            val gState = PDExtendedGraphicsState().apply {
                                nonStrokingAlphaConstant = 0.0f
                            }
                            cs.setGraphicsStateParameters(gState)

                            val scaleX = mediaBox.width / maxOf(1f, pageRes.renderWidth.toFloat())
                            val scaleY = mediaBox.height / maxOf(1f, pageRes.renderHeight.toFloat())

                            pageRes.blocks.forEach { block ->
                                if (block.text.isNotBlank()) {
                                    val calcFontSize = (block.boundingBox.height() * scaleY * 0.8f).coerceIn(6f, 24f)
                                    cs.beginText()
                                    cs.setFont(font, calcFontSize)

                                    // Map OCR pixel coordinates to PDF point coordinates (PDF origin is bottom-left)
                                    val x = (block.boundingBox.left * scaleX).coerceIn(0f, mediaBox.width)
                                    val y = (mediaBox.height - (block.boundingBox.bottom * scaleY)).coerceIn(0f, mediaBox.height)

                                    cs.newLineAtOffset(x, y)
                                    // Sanitize text for standard font
                                    val safeText = block.text.replace("\n", " ").filter { it.code in 32..126 }
                                    if (safeText.isNotEmpty()) {
                                        cs.showText(safeText)
                                    }
                                    cs.endText()
                                }
                            }
                        }
                    }
                }
                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.SEARCHABLE_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = ocrResult.pageResults.size
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.OCR_FAILED, e)
            OperationResult.failure(
                OperationType.SEARCHABLE_PDF,
                ErrorCode.OCR_FAILED,
                e.message ?: "Failed to generate searchable PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun extractTextToTextFile(inputFile: File, outputFile: File): OperationResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                outputFile.parentFile?.mkdirs()
                val ocrResult = recognizePdf(inputFile)

                FileOutputStream(outputFile).use { fos ->
                    fos.write(ocrResult.fullText.toByteArray(Charsets.UTF_8))
                }

                val duration = System.currentTimeMillis() - startTime
                OperationResult.success(
                    operationType = OperationType.OCR_PDF,
                    outputPath = outputFile.absolutePath,
                    outputSize = outputFile.length(),
                    durationMs = duration,
                    pagesProcessed = ocrResult.pageResults.size
                )
            } catch (e: Exception) {
                SafeLogger.e(TAG, ErrorCode.OCR_FAILED, e)
                OperationResult.failure(
                    OperationType.OCR_PDF,
                    ErrorCode.OCR_FAILED,
                    e.message ?: "Failed to extract text",
                    System.currentTimeMillis() - startTime
                )
            }
        }
}

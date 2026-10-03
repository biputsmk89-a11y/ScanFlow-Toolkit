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
import com.scanflow.app.domain.model.OcrLine
import com.scanflow.app.domain.model.OcrPageResult
import com.scanflow.app.domain.model.OcrResult
import com.scanflow.app.engine.OcrEngine
import com.scanflow.app.engine.PdfRendererEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OcrEngineImpl(
    private val pdfRendererEngine: PdfRendererEngine
) : OcrEngine {

    companion object {
        private const val TAG = "OcrEngine"
        private const val RENDER_TARGET_WIDTH = 1500
        private const val RENDER_TARGET_HEIGHT = 2000
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

                val ocrLines = block.lines.mapNotNull { line ->
                    val lineBox = line.boundingBox ?: return@mapNotNull null
                    OcrLine(
                        text = line.text,
                        boundingBox = RectF(
                            lineBox.left.toFloat(),
                            lineBox.top.toFloat(),
                            lineBox.right.toFloat(),
                            lineBox.bottom.toFloat()
                        )
                    )
                }

                OcrBlock(
                    text = block.text,
                    boundingBox = rectF,
                    lines = ocrLines
                )
            }

            OcrPageResult(
                pageNumber = 1,
                fullText = visionText.text,
                blocks = blocks,
                renderWidth = bitmap.width,
                renderHeight = bitmap.height
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.OCR_FAILED, e)
            OcrPageResult(
                pageNumber = 1,
                fullText = "",
                blocks = emptyList(),
                renderWidth = bitmap.width,
                renderHeight = bitmap.height
            )
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
                    val pageBitmap = pdfRendererEngine.renderPage(p, RENDER_TARGET_WIDTH, RENDER_TARGET_HEIGHT)
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

            // Run OCR to retrieve high-resolution text blocks and lines
            val ocrResult = recognizePdf(inputFile, onProgress)

            // Inject 100% invisible text layer into PDFBox document (ISO 32000-1 Mode 3 standard)
            PDDocument.load(inputFile).use { document ->
                val font = try {
                    PDType1Font.HELVETICA
                } catch (e: Throwable) {
                    SafeLogger.w(TAG, "Standard14 font not available in current environment: ${e.message}")
                    null
                }
                val totalPages = document.numberOfPages

                ocrResult.pageResults.forEach { pageRes ->
                    val pageIndex = pageRes.pageNumber - 1
                    if (pageIndex in 0 until totalPages) {
                        val page = document.getPage(pageIndex)
                        val viewBox = page.cropBox ?: page.mediaBox

                        PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            // 1. Text rendering mode 3: Neither fill nor stroke text (Standard Invisible Text)
                            cs.setRenderingMode(RenderingMode.NEITHER)

                            // 2. Extra safety: Set non-stroking alpha to 0.0f
                            val gState = PDExtendedGraphicsState().apply {
                                nonStrokingAlphaConstant = 0.0f
                                strokingAlphaConstant = 0.0f
                            }
                            cs.setGraphicsStateParameters(gState)

                            val scaleX = viewBox.width / maxOf(1f, pageRes.renderWidth.toFloat())
                            val scaleY = viewBox.height / maxOf(1f, pageRes.renderHeight.toFloat())

                            if (font != null) {
                                pageRes.blocks.forEach { block ->
                                // Render line-by-line for exact typography metrics and positioning
                                val linesToRender = if (block.lines.isNotEmpty()) {
                                    block.lines
                                } else {
                                    block.text.split("\n").filter { it.isNotBlank() }.map { lineText ->
                                        OcrLine(text = lineText, boundingBox = block.boundingBox)
                                    }
                                }

                                linesToRender.forEach { line ->
                                    val cleanText = sanitizeForType1Font(font, line.text)
                                    if (cleanText.isNotBlank()) {
                                        val lineBox = line.boundingBox
                                        val lineWidthPt = lineBox.width() * scaleX
                                        val lineHeightPt = lineBox.height() * scaleY

                                        // Measure text width at 1pt font size using the font metrics
                                        val textWidthAt1Pt = try {
                                            font.getStringWidth(cleanText) / 1000f
                                        } catch (e: Exception) {
                                            cleanText.length * 0.5f
                                        }

                                        // Calculate optimal font size fitting both line height and width
                                        val fontFromWidth = if (textWidthAt1Pt > 0) (lineWidthPt / textWidthAt1Pt) else lineHeightPt * 0.8f
                                        val fontFromHeight = lineHeightPt * 0.85f
                                        val calcFontSize = minOf(fontFromWidth, fontFromHeight).coerceIn(4f, 48f)

                                        // Position baseline accurately:
                                        // PDF coordinates have origin at bottom-left; ML Kit origin is at top-left
                                        val x = (viewBox.lowerLeftX + (lineBox.left * scaleX))
                                            .coerceIn(viewBox.lowerLeftX, viewBox.upperRightX)
                                        val y = (viewBox.lowerLeftY + (viewBox.height - (lineBox.bottom * scaleY)) + (calcFontSize * 0.15f))
                                            .coerceIn(viewBox.lowerLeftY, viewBox.upperRightY)

                                        try {
                                            cs.beginText()
                                            cs.setFont(font, calcFontSize)
                                            cs.newLineAtOffset(x, y)
                                            cs.showText(cleanText)
                                            cs.endText()
                                        } catch (e: Exception) {
                                            SafeLogger.w(TAG, "Failed to render text line '$cleanText': ${e.message}")
                                        }
                                    }
                                }
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

    /**
     * Sanitizes strings to ensure full compatibility with PDType1Font (WinAnsiEncoding).
     * Converts smart quotes, dashes, and filters unsupported glyphs to prevent encoding exceptions.
     */
    private fun sanitizeForType1Font(font: PDType1Font, text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            val mapped = when (char) {
                '\n', '\r', '\t' -> ' '
                '“', '”' -> '"'
                '‘', '’' -> '\''
                '—', '–' -> '-'
                '…' -> '.'
                '•' -> '*'
                else -> char
            }
            if (mapped.code in 32..126) {
                sb.append(mapped)
            } else {
                try {
                    font.encode(mapped.toString())
                    sb.append(mapped)
                } catch (_: Exception) {
                    sb.append(' ')
                }
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}

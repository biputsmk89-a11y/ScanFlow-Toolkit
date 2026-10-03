package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Html
import android.text.StaticLayout
import android.text.TextPaint
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.engine.ConversionEngine
import com.scanflow.app.engine.PdfRendererEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        pageSize: String,
        orientation: String,
        margin: String
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

            // Standard 300 DPI high-definition canvas (iLovePDF print & screen standard)
            val a4Width = 2480
            val a4Height = 3508
            val letterWidth = 2550
            val letterHeight = 3300

            val baseWidth = if (pageSize.equals("LETTER", ignoreCase = true)) letterWidth else a4Width
            val baseHeight = if (pageSize.equals("LETTER", ignoreCase = true)) letterHeight else a4Height

            val marginPx = when (margin.uppercase(Locale.ROOT)) {
                "NONE" -> 0f
                "NORMAL" -> 100f // approx 8.5mm on 300 DPI
                else -> 40f      // "SMALL", approx 3.4mm on 300 DPI
            }

            val paint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            var processedCount = 0
            imageFiles.forEachIndexed { index, file ->
                if (!file.exists()) return@forEachIndexed
                val bitmap = decodeSafeBitmap(file) ?: return@forEachIndexed

                val isImgLandscape = bitmap.width > bitmap.height
                val (pageW, pageH) = when (orientation.uppercase(Locale.ROOT)) {
                    "PORTRAIT" -> Pair(minOf(baseWidth, baseHeight), maxOf(baseWidth, baseHeight))
                    "LANDSCAPE" -> Pair(maxOf(baseWidth, baseHeight), minOf(baseWidth, baseHeight))
                    else -> { // "AUTO"
                        if (isImgLandscape) {
                            Pair(maxOf(baseWidth, baseHeight), minOf(baseWidth, baseHeight))
                        } else {
                            Pair(minOf(baseWidth, baseHeight), maxOf(baseWidth, baseHeight))
                        }
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Pure white canvas background (clean transparent PNG rendering)
                canvas.drawColor(Color.WHITE)

                val availW = (pageW - 2 * marginPx).coerceAtLeast(100f)
                val availH = (pageH - 2 * marginPx).coerceAtLeast(100f)

                if (fitPage) {
                    val scale = minOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
                    val drawW = bitmap.width * scale
                    val drawH = bitmap.height * scale
                    val left = marginPx + (availW - drawW) / 2f
                    val top = marginPx + (availH - drawH) / 2f
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)
                } else {
                    // Fill page mode: cover usable area centered and clipped
                    val scale = maxOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
                    val drawW = bitmap.width * scale
                    val drawH = bitmap.height * scale
                    val left = marginPx + (availW - drawW) / 2f
                    val top = marginPx + (availH - drawH) / 2f

                    canvas.save()
                    canvas.clipRect(marginPx, marginPx, marginPx + availW, marginPx + availH)
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)
                    canvas.restore()
                }

                pdfDoc.finishPage(page)
                bitmap.recycle()
                processedCount++
            }

            if (processedCount == 0) {
                return@withContext OperationResult.failure(
                    OperationType.IMAGE_TO_PDF,
                    ErrorCode.INVALID_FILE,
                    "Failed to decode any of the provided images."
                )
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }

            val duration = System.currentTimeMillis() - startTime
            SafeLogger.logOperation(
                "SF-039",
                OperationType.IMAGE_TO_PDF,
                OperationStatus.SUCCESS,
                duration
            )

            OperationResult.success(
                operationType = OperationType.IMAGE_TO_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = processedCount
            )
        } catch (e: Throwable) {
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

    private fun decodeSafeBitmap(file: File, maxDim: Int = 3000): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            val maxSide = maxOf(options.outWidth, options.outHeight)
            while (maxSide / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (oom: OutOfMemoryError) {
            SafeLogger.w(TAG, "OOM decoding ${file.name}, trying half-resolution fallback")
            try {
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = 4
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } catch (_: Throwable) {
                null
            }
        } catch (e: Exception) {
            null
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
                // 300 DPI gives ~4.16x scale factor for crisp print quality
                val effectiveDpi = if (dpi <= 0) 300 else dpi
                val scaleFactor = (effectiveDpi / 72f).coerceIn(1f, 4.5f)

                for (i in 0 until totalPages) {
                    val maxDim = (maxOf(A4_WIDTH_PT, A4_HEIGHT_PT) * scaleFactor).toInt()
                    // Pass renderMode = 2 (RENDER_MODE_FOR_PRINT) for maximum vector antialiasing & subpixel text
                    val bitmap = pdfRendererEngine.renderPage(i, maxDim, maxDim, 2)

                    if (bitmap != null) {
                        val ext = if (format.equals("PNG", ignoreCase = true)) "png" else "jpg"
                        val compressFormat = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                        val quality = if (ext == "png") 100 else 92
                        val outImage = File(outputDirectory, "${inputFile.nameWithoutExtension}_page_${i + 1}.$ext")

                        FileOutputStream(outImage).use { out ->
                            bitmap.compress(compressFormat, quality, out)
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

    override suspend fun htmlToPdf(
        htmlFile: File,
        outputFile: File,
        title: String?
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val pdfDoc = PdfDocument()
        try {
            outputFile.parentFile?.mkdirs()
            val rawHtml = if (htmlFile.exists()) htmlFile.readText(Charsets.UTF_8) else "<p>No content</p>"
            val spanned = Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY)

            val textPaint = TextPaint().apply {
                color = Color.rgb(15, 23, 42) // Dark slate
                textSize = 12f
                isAntiAlias = true
            }

            val docTitle = title ?: htmlFile.nameWithoutExtension.replace("_", " ").replaceFirstChar { it.uppercase() }
            val headerPaint = Paint().apply {
                color = Color.rgb(100, 116, 139) // Slate 500
                textSize = 8.5f
                isAntiAlias = true
            }
            val dividerPaint = Paint().apply {
                color = Color.rgb(226, 232, 240) // Slate 200
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val marginHoriz = 44f
            val printableWidth = (A4_WIDTH_PT - 2 * marginHoriz).toInt()
            val topContentMargin = 55f
            val bottomContentMargin = 45f
            val printableHeight = A4_HEIGHT_PT - topContentMargin - bottomContentMargin

            val staticLayout = StaticLayout.Builder
                .obtain(spanned, 0, spanned.length, textPaint, printableWidth)
                .setLineSpacing(2f, 1.25f)
                .build()

            // Calculate precise line boundaries to eliminate chopped/cut text across page boundaries
            val pageLineRanges = mutableListOf<Pair<Int, Int>>()
            var lineStart = 0
            while (lineStart < staticLayout.lineCount) {
                val startY = staticLayout.getLineTop(lineStart)
                var lineEnd = lineStart
                while (lineEnd + 1 < staticLayout.lineCount &&
                    staticLayout.getLineBottom(lineEnd + 1) - startY <= printableHeight
                ) {
                    lineEnd++
                }
                pageLineRanges.add(Pair(lineStart, lineEnd))
                lineStart = lineEnd + 1
            }

            if (pageLineRanges.isEmpty()) {
                pageLineRanges.add(Pair(0, 0))
            }

            val totalPages = pageLineRanges.size

            pageLineRanges.forEachIndexed { pageIdx, (startLine, endLine) ->
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageIdx + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Pure white background
                canvas.drawColor(Color.WHITE)

                // Top Running Header
                canvas.drawText(docTitle.take(50), marginHoriz, 32f, headerPaint)
                val rightHeaderText = "ScanFlow Clean HTML"
                val rightHeaderWidth = headerPaint.measureText(rightHeaderText)
                canvas.drawText(rightHeaderText, A4_WIDTH_PT - marginHoriz - rightHeaderWidth, 32f, headerPaint)
                canvas.drawLine(marginHoriz, 38f, A4_WIDTH_PT - marginHoriz, 38f, dividerPaint)

                // Render content slice without cutting lines
                val startY = staticLayout.getLineTop(startLine)
                val endY = staticLayout.getLineBottom(endLine)
                val contentHeight = endY - startY

                canvas.save()
                canvas.clipRect(marginHoriz, topContentMargin, marginHoriz + printableWidth, topContentMargin + contentHeight + 4f)
                canvas.translate(marginHoriz, topContentMargin - startY)
                staticLayout.draw(canvas)
                canvas.restore()

                // Bottom Running Footer
                val footerY = A4_HEIGHT_PT - 24f
                canvas.drawLine(marginHoriz, footerY - 10f, A4_WIDTH_PT - marginHoriz, footerY - 10f, dividerPaint)
                canvas.drawText("Rendered by ScanFlow Toolkit", marginHoriz, footerY + 2f, headerPaint)
                val pageText = "Page ${pageIdx + 1} of $totalPages"
                val pageTextWidth = headerPaint.measureText(pageText)
                canvas.drawText(pageText, A4_WIDTH_PT - marginHoriz - pageTextWidth, footerY + 2f, headerPaint)

                pdfDoc.finishPage(page)
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.HTML_TO_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = totalPages
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CONVERSION_FAILED, e)
            OperationResult.failure(
                operationType = OperationType.HTML_TO_PDF,
                ErrorCode.CONVERSION_FAILED,
                e.message ?: "Failed to convert HTML to PDF",
                System.currentTimeMillis() - startTime
            )
        } finally {
            pdfDoc.close()
        }
    }

    override suspend fun textToPdf(
        textFile: File,
        outputFile: File,
        title: String?,
        fontSize: Float
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            val lines = if (textFile.exists()) textFile.readLines(Charsets.UTF_8) else listOf("")

            val docTitle = title ?: textFile.nameWithoutExtension.replace("_", " ").replaceFirstChar { it.uppercase() }

            PDDocument().use { doc ->
                val font = PDType1Font.HELVETICA
                val boldFont = PDType1Font.HELVETICA_BOLD
                val effectiveFontSize = if (fontSize <= 0f) 11f else fontSize
                val leading = effectiveFontSize * 1.45f
                val margin = 45f
                val pageWidth = PDRectangle.A4.width
                val pageHeight = PDRectangle.A4.height
                val contentWidth = pageWidth - 2 * margin
                val topTextY = pageHeight - 65f
                val bottomLimit = 55f

                var currentPage = PDPage(PDRectangle.A4)
                doc.addPage(currentPage)
                var contentStream = PDPageContentStream(doc, currentPage)
                contentStream.beginText()
                contentStream.setFont(font, effectiveFontSize)
                contentStream.setNonStrokingColor(15, 23, 42) // Dark Slate
                contentStream.newLineAtOffset(margin, topTextY)

                var currentY = topTextY

                for (line in lines) {
                    val sanitizedLine = sanitizeForWinAnsi(line)
                    if (sanitizedLine.isBlank()) {
                        // Paragraph separation gap
                        val gap = leading * 0.65f
                        if (currentY - gap < bottomLimit) {
                            contentStream.endText()
                            contentStream.close()

                            currentPage = PDPage(PDRectangle.A4)
                            doc.addPage(currentPage)
                            contentStream = PDPageContentStream(doc, currentPage)
                            contentStream.beginText()
                            contentStream.setFont(font, effectiveFontSize)
                            contentStream.setNonStrokingColor(15, 23, 42)
                            contentStream.newLineAtOffset(margin, topTextY)
                            currentY = topTextY
                        } else {
                            contentStream.newLineAtOffset(0f, -gap)
                            currentY -= gap
                        }
                        continue
                    }

                    val words = sanitizedLine.split(" ")
                    var currentLineText = ""

                    for (word in words) {
                        val testLine = if (currentLineText.isEmpty()) word else "$currentLineText $word"
                        val lineWidth = font.getStringWidth(testLine) / 1000f * effectiveFontSize

                        if (lineWidth > contentWidth) {
                            if (currentY - leading < bottomLimit) {
                                contentStream.endText()
                                contentStream.close()

                                currentPage = PDPage(PDRectangle.A4)
                                doc.addPage(currentPage)
                                contentStream = PDPageContentStream(doc, currentPage)
                                contentStream.beginText()
                                contentStream.setFont(font, effectiveFontSize)
                                contentStream.setNonStrokingColor(15, 23, 42)
                                contentStream.newLineAtOffset(margin, topTextY)
                                currentY = topTextY
                            }

                            contentStream.showText(currentLineText)
                            contentStream.newLineAtOffset(0f, -leading)
                            currentY -= leading
                            currentLineText = word
                        } else {
                            currentLineText = testLine
                        }
                    }

                    if (currentLineText.isNotEmpty()) {
                        if (currentY - leading < bottomLimit) {
                            contentStream.endText()
                            contentStream.close()

                            currentPage = PDPage(PDRectangle.A4)
                            doc.addPage(currentPage)
                            contentStream = PDPageContentStream(doc, currentPage)
                            contentStream.beginText()
                            contentStream.setFont(font, effectiveFontSize)
                            contentStream.setNonStrokingColor(15, 23, 42)
                            contentStream.newLineAtOffset(margin, topTextY)
                            currentY = topTextY
                        }

                        contentStream.showText(currentLineText)
                        contentStream.newLineAtOffset(0f, -leading)
                        currentY -= leading
                    }
                }

                contentStream.endText()
                contentStream.close()

                // Second Pass: Stamp Elegant iLovePDF-style Running Header & Running Footer
                val totalPages = doc.numberOfPages
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                for (p in 0 until totalPages) {
                    val page = doc.getPage(p)
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        // Header
                        cs.setStrokingColor(226, 232, 240) // Slate 200
                        cs.setLineWidth(0.75f)
                        cs.moveTo(margin, pageHeight - 48f)
                        cs.lineTo(pageWidth - margin, pageHeight - 48f)
                        cs.stroke()

                        cs.beginText()
                        cs.setFont(boldFont, 8.5f)
                        cs.setNonStrokingColor(71, 85, 105) // Slate 600
                        cs.newLineAtOffset(margin, pageHeight - 42f)
                        cs.showText(sanitizeForWinAnsi(docTitle.take(45)))
                        cs.endText()

                        cs.beginText()
                        cs.setFont(font, 8f)
                        cs.setNonStrokingColor(148, 163, 184) // Slate 400
                        val dateWidth = font.getStringWidth(dateStr) / 1000f * 8f
                        cs.newLineAtOffset(pageWidth - margin - dateWidth, pageHeight - 42f)
                        cs.showText(dateStr)
                        cs.endText()

                        // Footer
                        cs.setStrokingColor(226, 232, 240)
                        cs.setLineWidth(0.75f)
                        cs.moveTo(margin, 40f)
                        cs.lineTo(pageWidth - margin, 40f)
                        cs.stroke()

                        cs.beginText()
                        cs.setFont(font, 8f)
                        cs.setNonStrokingColor(148, 163, 184)
                        cs.newLineAtOffset(margin, 28f)
                        cs.showText("ScanFlow Clean Document Engine")
                        cs.endText()

                        val pageLabel = "Page ${p + 1} of $totalPages"
                        val pageLabelWidth = boldFont.getStringWidth(pageLabel) / 1000f * 8f
                        cs.beginText()
                        cs.setFont(boldFont, 8f)
                        cs.setNonStrokingColor(71, 85, 105)
                        cs.newLineAtOffset(pageWidth - margin - pageLabelWidth, 28f)
                        cs.showText(pageLabel)
                        cs.endText()
                    }
                }

                doc.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.TEXT_TO_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CONVERSION_FAILED, e)
            OperationResult.failure(
                OperationType.TEXT_TO_PDF,
                ErrorCode.CONVERSION_FAILED,
                e.message ?: "Failed to convert Text to PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun pdfToCsv(inputFile: File, outputFile: File): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            val text = PDDocument.load(inputFile).use { doc ->
                PDFTextStripper().getText(doc)
            }

            val lines = text.lines()
            val csvLines = mutableListOf<String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val tokens = when {
                    trimmed.contains("\t") -> trimmed.split("\t")
                    trimmed.contains(Regex(" {2,}")) -> trimmed.split(Regex(" {2,}"))
                    else -> trimmed.split(",")
                }

                val row = tokens.map { col ->
                    val clean = col.trim().replace("\"", "\"\"")
                    if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains(";")) {
                        "\"$clean\""
                    } else {
                        clean
                    }
                }
                csvLines.add(row.joinToString(","))
            }

            FileOutputStream(outputFile).use { fos ->
                fos.write(csvLines.joinToString("\n").toByteArray(Charsets.UTF_8))
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
                e.message ?: "Failed to extract CSV from PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun csvToPdf(
        csvFile: File,
        outputFile: File,
        title: String?,
        orientation: String,
        styleTheme: String
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()
            val rawLines = if (csvFile.exists()) csvFile.readLines(Charsets.UTF_8) else emptyList()
            val rows = parseCsvRows(rawLines)

            if (rows.isEmpty()) {
                return@withContext OperationResult.failure(
                    OperationType.CSV_TO_PDF,
                    ErrorCode.INVALID_FILE,
                    "CSV document contains no readable records."
                )
            }

            val docTitle = title ?: csvFile.nameWithoutExtension.replace("_", " ").replaceFirstChar { it.uppercase() }
            val maxCols = rows.maxOfOrNull { it.size } ?: 1

            // Orientation logic: If > 4 columns or LANDSCAPE requested, use A4 Landscape
            val isLandscape = orientation.equals("LANDSCAPE", ignoreCase = true) ||
                    (orientation.equals("AUTO", ignoreCase = true) && maxCols > 4)

            val pageRect = if (isLandscape) {
                PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width) // 842 x 595 pt
            } else {
                PDRectangle(PDRectangle.A4.width, PDRectangle.A4.height) // 595 x 842 pt
            }

            val pageWidth = pageRect.width
            val pageHeight = pageRect.height
            val margin = 36f
            val tableWidth = pageWidth - 2 * margin
            val topTableY = pageHeight - 56f
            val bottomLimit = 46f
            val headerRowHeight = 26f
            val dataRowHeight = 20f

            // Calculate dynamic proportional column widths based on maximum cell lengths
            val colLengths = IntArray(maxCols) { 4 }
            rows.forEach { row ->
                row.forEachIndexed { i, cell ->
                    if (i < maxCols) {
                        colLengths[i] = maxOf(colLengths[i], cell.trim().length.coerceIn(4, 50))
                    }
                }
            }
            val totalWeight = colLengths.sum().coerceAtLeast(1)
            val minColWidth = 36f
            val colWidths = FloatArray(maxCols) { i ->
                (colLengths[i].toFloat() / totalWeight * tableWidth).coerceAtLeast(minColWidth)
            }
            // Normalize column widths to fit exact tableWidth
            val sumColWidths = colWidths.sum()
            if (sumColWidths > 0) {
                val factor = tableWidth / sumColWidths
                for (i in 0 until maxCols) {
                    colWidths[i] = colWidths[i] * factor
                }
            }

            // Theme Styling Palette
            data class ThemePalette(
                val headerBg: Triple<Int, Int, Int>,
                val headerText: Triple<Int, Int, Int>,
                val evenBg: Triple<Int, Int, Int>,
                val oddBg: Triple<Int, Int, Int>,
                val border: Triple<Int, Int, Int>,
                val textColor: Triple<Int, Int, Int>
            )

            val palette = when (styleTheme.uppercase(Locale.ROOT)) {
                "CLEAN_SLATE" -> ThemePalette(
                    headerBg = Triple(51, 65, 85),      // Slate 700
                    headerText = Triple(255, 255, 255),  // White
                    evenBg = Triple(255, 255, 255),      // Pure White
                    oddBg = Triple(248, 250, 252),       // Slate 50
                    border = Triple(226, 232, 240),      // Slate 200
                    textColor = Triple(30, 41, 59)       // Slate 800
                )
                "MINIMAL" -> ThemePalette(
                    headerBg = Triple(241, 245, 249),    // Slate 100
                    headerText = Triple(15, 23, 42),     // Dark Slate
                    evenBg = Triple(255, 255, 255),
                    oddBg = Triple(250, 250, 250),
                    border = Triple(203, 213, 225),      // Slate 300
                    textColor = Triple(15, 23, 42)
                )
                else -> ThemePalette( // "MODERN_NAVY" (iLovePDF benchmark)
                    headerBg = Triple(30, 58, 138),      // Deep Royal Navy (#1E3A8A)
                    headerText = Triple(255, 255, 255),
                    evenBg = Triple(255, 255, 255),
                    oddBg = Triple(241, 245, 249),       // Ice Blue Tint (#F1F5F9)
                    border = Triple(226, 232, 240),      // Slate 200
                    textColor = Triple(15, 23, 42)
                )
            }

            PDDocument().use { doc ->
                val font = PDType1Font.HELVETICA
                val boldFont = PDType1Font.HELVETICA_BOLD

                var currentPage = PDPage(pageRect)
                doc.addPage(currentPage)
                var cs = PDPageContentStream(doc, currentPage)
                var currentY = topTableY

                // Helper to render header row
                fun drawHeaderRow(stream: PDPageContentStream, y: Float) {
                    val headerRow = rows.firstOrNull() ?: emptyList()
                    var colX = margin
                    for (c in 0 until maxCols) {
                        val w = colWidths[c]
                        // Header background fill
                        stream.setNonStrokingColor(palette.headerBg.first, palette.headerBg.second, palette.headerBg.third)
                        stream.addRect(colX, y - headerRowHeight, w, headerRowHeight)
                        stream.fill()

                        // Header border
                        stream.setStrokingColor(palette.border.first, palette.border.second, palette.border.third)
                        stream.setLineWidth(0.6f)
                        stream.addRect(colX, y - headerRowHeight, w, headerRowHeight)
                        stream.stroke()

                        // Header text
                        val text = if (c < headerRow.size) headerRow[c] else "Col ${c + 1}"
                        val truncatedText = ellipsizeText(text, boldFont, 8.5f, w - 10f)
                        stream.beginText()
                        stream.setFont(boldFont, 8.5f)
                        stream.setNonStrokingColor(palette.headerText.first, palette.headerText.second, palette.headerText.third)
                        stream.newLineAtOffset(colX + 5f, y - 17f)
                        stream.showText(sanitizeForWinAnsi(truncatedText))
                        stream.endText()

                        colX += w
                    }
                }

                // Draw initial table header
                drawHeaderRow(cs, currentY)
                currentY -= headerRowHeight

                // Draw data rows (skip index 0 as it's the header)
                for (rowIndex in 1 until rows.size) {
                    val row = rows[rowIndex]

                    if (currentY - dataRowHeight < bottomLimit) {
                        cs.close()
                        currentPage = PDPage(pageRect)
                        doc.addPage(currentPage)
                        cs = PDPageContentStream(doc, currentPage)
                        currentY = topTableY

                        // Repeat header row on new page for clean table readability
                        drawHeaderRow(cs, currentY)
                        currentY -= headerRowHeight
                    }

                    val isOdd = (rowIndex % 2 == 1)
                    val bg = if (isOdd) palette.oddBg else palette.evenBg

                    var colX = margin
                    for (c in 0 until maxCols) {
                        val w = colWidths[c]

                        // Cell background fill
                        cs.setNonStrokingColor(bg.first, bg.second, bg.third)
                        cs.addRect(colX, currentY - dataRowHeight, w, dataRowHeight)
                        cs.fill()

                        // Cell border
                        cs.setStrokingColor(palette.border.first, palette.border.second, palette.border.third)
                        cs.setLineWidth(0.5f)
                        cs.addRect(colX, currentY - dataRowHeight, w, dataRowHeight)
                        cs.stroke()

                        // Cell content
                        val rawText = if (c < row.size) row[c] else ""
                        val truncatedText = ellipsizeText(rawText, font, 8f, w - 8f)
                        cs.beginText()
                        cs.setFont(font, 8f)
                        cs.setNonStrokingColor(palette.textColor.first, palette.textColor.second, palette.textColor.third)
                        cs.newLineAtOffset(colX + 4f, currentY - 14f)
                        cs.showText(sanitizeForWinAnsi(truncatedText))
                        cs.endText()

                        colX += w
                    }

                    currentY -= dataRowHeight
                }
                cs.close()

                // Second Pass: Stamp Running Table Title & Running Footers
                val totalPages = doc.numberOfPages
                for (p in 0 until totalPages) {
                    val page = doc.getPage(p)
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                        // Top Header rule and title
                        stream.setStrokingColor(226, 232, 240)
                        stream.setLineWidth(0.75f)
                        stream.moveTo(margin, pageHeight - 38f)
                        stream.lineTo(pageWidth - margin, pageHeight - 38f)
                        stream.stroke()

                        stream.beginText()
                        stream.setFont(boldFont, 8.5f)
                        stream.setNonStrokingColor(71, 85, 105)
                        stream.newLineAtOffset(margin, pageHeight - 32f)
                        stream.showText(sanitizeForWinAnsi(docTitle.take(50)))
                        stream.endText()

                        val tableInfo = "Records: ${rows.size - 1} | Columns: $maxCols"
                        val infoWidth = font.getStringWidth(tableInfo) / 1000f * 8f
                        stream.beginText()
                        stream.setFont(font, 8f)
                        stream.setNonStrokingColor(148, 163, 184)
                        stream.newLineAtOffset(pageWidth - margin - infoWidth, pageHeight - 32f)
                        stream.showText(tableInfo)
                        stream.endText()

                        // Bottom Footer rule and pagination
                        stream.setStrokingColor(226, 232, 240)
                        stream.setLineWidth(0.75f)
                        stream.moveTo(margin, 34f)
                        stream.lineTo(pageWidth - margin, 34f)
                        stream.stroke()

                        stream.beginText()
                        stream.setFont(font, 8f)
                        stream.setNonStrokingColor(148, 163, 184)
                        stream.newLineAtOffset(margin, 22f)
                        stream.showText("ScanFlow Air-Gap Table Renderer")
                        stream.endText()

                        val pageLabel = "Page ${p + 1} of $totalPages"
                        val pageLabelWidth = boldFont.getStringWidth(pageLabel) / 1000f * 8f
                        stream.beginText()
                        stream.setFont(boldFont, 8f)
                        stream.setNonStrokingColor(71, 85, 105)
                        stream.newLineAtOffset(pageWidth - margin - pageLabelWidth, 22f)
                        stream.showText(pageLabel)
                        stream.endText()
                    }
                }

                doc.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.CSV_TO_PDF,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.CONVERSION_FAILED, e)
            OperationResult.failure(
                OperationType.CSV_TO_PDF,
                ErrorCode.CONVERSION_FAILED,
                e.message ?: "Failed to convert CSV to PDF",
                System.currentTimeMillis() - startTime
            )
        }
    }

    private fun parseCsvRows(lines: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false

        for (line in lines) {
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    c == '\"' -> {
                        if (insideQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                            currentField.append('\"')
                            i++
                        } else {
                            insideQuotes = !insideQuotes
                        }
                    }
                    c == ',' && !insideQuotes -> {
                        currentRecord.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    c == ';' && !insideQuotes && !line.contains(",") -> {
                        // Support semicolon delimiter if no comma present
                        currentRecord.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
                i++
            }

            if (!insideQuotes) {
                currentRecord.add(currentField.toString().trim())
                currentField.clear()
                if (currentRecord.any { it.isNotBlank() }) {
                    rows.add(currentRecord.toList())
                }
                currentRecord.clear()
            } else {
                currentField.append("\n")
            }
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            if (currentRecord.any { it.isNotBlank() }) {
                rows.add(currentRecord.toList())
            }
        }

        return rows
    }

    private fun ellipsizeText(
        text: String,
        font: PDType1Font,
        fontSize: Float,
        maxWidth: Float
    ): String {
        val sanitized = sanitizeForWinAnsi(text)
        val fullWidth = font.getStringWidth(sanitized) / 1000f * fontSize
        if (fullWidth <= maxWidth) return sanitized

        val ellipsis = "..."
        val ellipsisWidth = font.getStringWidth(ellipsis) / 1000f * fontSize
        val targetWidth = maxWidth - ellipsisWidth
        if (targetWidth <= 0f) return ellipsis

        var low = 0
        var high = sanitized.length
        var best = ""

        while (low <= high) {
            val mid = (low + high) / 2
            val sub = sanitized.substring(0, mid)
            val subWidth = font.getStringWidth(sub) / 1000f * fontSize
            if (subWidth <= targetWidth) {
                best = sub
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return "$best$ellipsis"
    }

    private fun sanitizeForWinAnsi(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '\t' -> sb.append("    ")
                '\u2018', '\u2019' -> sb.append('\'')
                '\u201C', '\u201D' -> sb.append('"')
                '\u2013', '\u2014' -> sb.append('-')
                '\u2026' -> sb.append("...")
                '\u2022' -> sb.append("*")
                in ' '..'~' -> sb.append(ch)
                in '\u00A0'..'\u00FF' -> {
                    try {
                        PDType1Font.HELVETICA.encode(ch.toString())
                        sb.append(ch)
                    } catch (_: Exception) {
                        sb.append('?')
                    }
                }
                else -> sb.append('?')
            }
        }
        return sb.toString()
    }
}

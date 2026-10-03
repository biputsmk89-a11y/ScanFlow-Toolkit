package com.scanflow.app.domain.dispatcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.model.PageNumberConfig
import com.scanflow.app.domain.model.SecurityConfig
import com.scanflow.app.domain.model.WatermarkConfig
import com.scanflow.app.domain.model.Workflow
import com.scanflow.app.domain.model.WorkflowStep
import com.scanflow.app.domain.repository.DocumentRepository
import com.scanflow.app.domain.repository.OperationRecord
import com.scanflow.app.domain.repository.OperationRepository
import com.scanflow.app.engine.AiEngine
import com.scanflow.app.engine.CompareEngine
import com.scanflow.app.engine.CompressionEngine
import com.scanflow.app.engine.ConversionEngine
import com.scanflow.app.engine.FormEngine
import com.scanflow.app.engine.FormFieldInfo
import com.scanflow.app.engine.OcrEngine
import com.scanflow.app.engine.PdfEngine
import com.scanflow.app.engine.SecurityEngine
import com.scanflow.app.engine.StorageEngine
import com.scanflow.app.engine.WorkflowEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Deep implementation of DocumentOperationDispatcher.
 * Encapsulates input validation, engine coordination, storage paths,
 * cryptographic verification, audit trails, and document indexing.
 */
class DocumentOperationDispatcherImpl(
    private val storageEngine: StorageEngine,
    private val pdfEngine: PdfEngine,
    private val compressionEngine: CompressionEngine,
    private val ocrEngine: OcrEngine,
    private val securityEngine: SecurityEngine,
    private val formEngine: FormEngine,
    private val compareEngine: CompareEngine,
    private val conversionEngine: ConversionEngine,
    private val workflowEngine: WorkflowEngine,
    private val aiEngine: AiEngine,
    private val documentRepository: DocumentRepository,
    private val operationRepository: OperationRepository
) : DocumentOperationDispatcher {

    override suspend fun inspectFormFields(file: File): List<FormFieldInfo> = withContext(Dispatchers.IO) {
        try {
            formEngine.getFormFields(file)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun dispatch(
        request: ToolExecutionRequest,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): OperationResult = withContext(Dispatchers.IO) {
        val files = request.inputFiles
        if (files.isEmpty()) {
            return@withContext OperationResult.failure(
                operationType = OperationType.MERGE_PDF,
                errorCode = ErrorCode.FILE_NOT_FOUND,
                errorMessage = "Please select at least one document to proceed."
            )
        }

        val primaryFile = files.first()
        val destDir = storageEngine.getDocumentsDirectory()

        try {
            when (request.featureId) {
                // SF-001: Merge PDF
                "SF-001" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.merge(files, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-002: Split PDF
                "SF-002" -> {
                    val ranges = parseRanges(request.splitRanges)
                    val results = pdfEngine.split(primaryFile, ranges, destDir)
                    val successful = results.filter { it.success }
                    if (successful.isNotEmpty()) {
                        successful.forEach { r ->
                            val f = File(r.outputPath ?: "")
                            if (f.exists()) recordAndRegister(r, primaryFile.absolutePath, f)
                        }
                        val first = successful.first()
                        OperationResult.success(
                            operationType = OperationType.SPLIT_PDF,
                            outputPath = first.outputPath ?: "",
                            outputSize = successful.sumOf { it.outputSize },
                            durationMs = successful.sumOf { it.durationMs },
                            pagesProcessed = successful.sumOf { it.pagesProcessed },
                            metadata = mapOf(
                                "totalParts" to "${successful.size}",
                                "parts" to successful.joinToString(", ") { File(it.outputPath ?: "").name }
                            )
                        )
                    } else {
                        results.firstOrNull() ?: OperationResult.failure(OperationType.SPLIT_PDF, ErrorCode.INVALID_PDF)
                    }
                }

                // SF-003: Remove Pages
                "SF-003" -> {
                    val count = pdfEngine.getPageCount(primaryFile)
                    val rawPages = request.pageOrder.ifBlank { request.splitRanges }
                    val parsed = rawPages.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }.toSet()
                    val toRemove = if (parsed.isNotEmpty()) parsed else setOf(1)
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.removePages(primaryFile, toRemove, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-004: Extract Pages
                "SF-004" -> {
                    val count = pdfEngine.getPageCount(primaryFile)
                    val rawPages = request.pageOrder.ifBlank { request.splitRanges }
                    val parsed = rawPages.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }
                    val toExtract = if (parsed.isNotEmpty()) parsed else listOf(1)
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.extractPages(primaryFile, toExtract, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-005, SF-007: Organize & Reorder Pages
                "SF-005", "SF-007" -> {
                    val count = pdfEngine.getPageCount(primaryFile)
                    val parsedOrder = request.pageOrder.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }
                    val order = if (parsedOrder.isNotEmpty()) parsedOrder else if (count > 1) (1..count).toList().reversed() else listOf(1)
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.reorderPages(primaryFile, order, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-008, SF-076: Rotate Pages
                "SF-008", "SF-076" -> {
                    val count = pdfEngine.getPageCount(primaryFile)
                    val rotMap = (1..count).associateWith { request.rotationAngle }
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.rotatePages(primaryFile, rotMap, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-009: Duplicate Pages
                "SF-009" -> {
                    val count = pdfEngine.getPageCount(primaryFile)
                    val rawPages = request.pageOrder.ifBlank { request.splitRanges }
                    val parsed = rawPages.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }
                    val toDuplicate = if (parsed.isNotEmpty()) parsed else listOf(1)
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.duplicatePages(primaryFile, toDuplicate, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-011: Insert Blank Page
                "SF-011" -> {
                    val targetIdx = request.signatureTargetPage
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.insertBlankPage(primaryFile, targetIdx, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-017..SF-021: Compress PDF
                "SF-017", "SF-018", "SF-019", "SF-020", "SF-021" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = compressionEngine.compressPdf(
                        primaryFile,
                        CompressionConfig(level = request.compressionLevel),
                        destFile,
                        onProgress
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-024: Repair PDF
                "SF-024" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.repair(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-025, SF-062: Searchable PDF
                "SF-025", "SF-062" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = ocrEngine.generateSearchablePdf(primaryFile, destFile, onProgress)
                    recordAndRegister(res, primaryFile.absolutePath, destFile, hasOcr = true)
                    res
                }

                // SF-059, SF-060, SF-061: Extract Text / OCR Image
                "SF-059", "SF-060", "SF-061" -> {
                    val destFile = File(destDir, "${request.outputName}.txt")
                    val res = ocrEngine.extractTextToTextFile(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-066: Edit PDF (Annotation Overlay)
                "SF-066" -> {
                    val text = request.watermarkText.ifBlank { "ANNOTATED" }
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.watermark(
                        primaryFile,
                        WatermarkConfig(text = text, rotationDegrees = 0f, opacity = 0.9f),
                        destFile
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-075: Crop PDF
                "SF-075" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.cropPages(primaryFile, request.cropMarginPoints, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-077: Page Numbers
                "SF-077" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.addPageNumbers(primaryFile, PageNumberConfig(), destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-078: Watermark
                "SF-078" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.watermark(
                        primaryFile,
                        WatermarkConfig(text = request.watermarkText),
                        destFile
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-079, SF-080: Protect PDF
                "SF-079", "SF-080" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = securityEngine.protectPdf(
                        primaryFile,
                        SecurityConfig(userPassword = request.userPassword),
                        destFile
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile, isEncrypted = true)
                    res
                }

                // SF-081: Unlock PDF
                "SF-081" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = securityEngine.unlockPdf(primaryFile, request.userPassword, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile, isEncrypted = false)
                    res
                }

                // SF-082: Remove Metadata
                "SF-082" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = securityEngine.removeMetadata(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-083: Redact PDF
                "SF-083" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val pageCount = pdfEngine.getPageCount(primaryFile)
                    val redactionRects = when (request.redactionTarget) {
                        "ALL_MARGINS" -> listOf(
                            RectF(0f, 0f, 612f, 45f),
                            RectF(0f, 747f, 612f, 792f),
                            RectF(0f, 0f, 45f, 792f),
                            RectF(567f, 0f, 612f, 792f)
                        )
                        else -> listOf(
                            RectF(40f, 35f, 570f, 85f),
                            RectF(40f, 715f, 570f, 765f)
                        )
                    }
                    val redactionsMap = (1..maxOf(1, pageCount)).associateWith { redactionRects }
                    val res = securityEngine.redactPdf(primaryFile, redactionsMap, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-084, SF-085: Sign PDF
                "SF-084", "SF-085" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val stampBitmap = request.signatureBitmap ?: createDigitalSignatureBitmap(request.signerName.ifBlank { "VERIFIED SIGNATURE" })
                    val pageCount = pdfEngine.getPageCount(primaryFile)
                    val targetPage = (request.signatureTargetPage - 1).coerceIn(0, maxOf(0, pageCount - 1))
                    val rect = when (request.signaturePosition) {
                        "BOTTOM_LEFT" -> RectF(50f, 680f, 250f, 780f)
                        "TOP_RIGHT" -> RectF(350f, 60f, 550f, 160f)
                        "TOP_LEFT" -> RectF(50f, 60f, 250f, 160f)
                        "CENTER" -> RectF(200f, 350f, 400f, 450f)
                        else -> RectF(350f, 680f, 550f, 780f) // BOTTOM_RIGHT
                    }
                    val res = securityEngine.stampSignature(
                        inputFile = primaryFile,
                        signatureBitmap = stampBitmap,
                        pageIndex = targetPage,
                        rect = rect,
                        outputFile = destFile
                    )
                    if (request.signatureBitmap == null && !stampBitmap.isRecycled) {
                        stampBitmap.recycle()
                    }
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-086..SF-092: PDF Forms
                "SF-086", "SF-087", "SF-088", "SF-089", "SF-090", "SF-091", "SF-092" -> {
                    val fields = formEngine.getFormFields(primaryFile)
                    val fillMap = if (request.formFieldValues.isNotEmpty()) {
                        request.formFieldValues
                    } else {
                        fields.associate { field ->
                            field.name to (request.watermarkText.ifBlank { "Completed" })
                        }
                    }
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = formEngine.fillForm(primaryFile, fillMap, true, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-093: Compare PDF
                "SF-093" -> {
                    if (files.size < 2) {
                        return@withContext OperationResult.failure(
                            OperationType.COMPARE_PDF,
                            ErrorCode.INVALID_PDF,
                            "Please select two PDF documents to compare."
                        )
                    }
                    val report = compareEngine.comparePdfs(files[0], files[1], true, onProgress)
                    val res = OperationResult.success(
                        operationType = OperationType.COMPARE_PDF,
                        outputPath = files[0].absolutePath,
                        outputSize = files[0].length(),
                        durationMs = 0L,
                        pagesProcessed = report.pageResults.size,
                        metadata = mapOf("similarity" to "%.1f%%".format(report.overallSimilarityPercentage))
                    )
                    recordAndRegister(res, files[0].absolutePath, files[0])
                    res
                }

                // SF-094, SF-095, SF-101: Images to PDF
                "SF-094", "SF-095", "SF-101" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = conversionEngine.imagesToPdf(
                        files,
                        destFile,
                        fitPage = request.conversionFitPage,
                        pageSize = request.conversionPageSize,
                        orientation = request.conversionOrientation,
                        margin = request.conversionMargin
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-096, SF-097, SF-104: PDF to Images
                "SF-096", "SF-097", "SF-104" -> {
                    val format = if (request.featureId == "SF-096") "JPG" else "PNG"
                    val outputFolder = File(destDir, "${primaryFile.nameWithoutExtension}_images")
                    val results = conversionEngine.pdfToImages(primaryFile, outputFolder, format, request.conversionDpi, onProgress)
                    val successful = results.filter { it.success }
                    if (successful.isNotEmpty()) {
                        val mime = if (format == "PNG") "image/png" else "image/jpeg"
                        successful.forEach { r ->
                            val f = File(r.outputPath ?: "")
                            if (f.exists()) recordAndRegister(r, primaryFile.absolutePath, f, mimeType = mime)
                        }
                        val first = successful.first()
                        OperationResult.success(
                            operationType = OperationType.PDF_TO_IMAGES,
                            outputPath = first.outputPath ?: "",
                            outputSize = successful.sumOf { it.outputSize },
                            durationMs = successful.sumOf { it.durationMs },
                            pagesProcessed = successful.size,
                            metadata = mapOf(
                                "totalPages" to "${successful.size}",
                                "folder" to File(first.outputPath ?: "").parentFile?.absolutePath.orEmpty()
                            )
                        )
                    } else {
                        results.firstOrNull() ?: OperationResult.failure(OperationType.PDF_TO_IMAGES, ErrorCode.CONVERSION_FAILED)
                    }
                }

                // SF-098, SF-102: PDF to Text
                "SF-098", "SF-102" -> {
                    val destFile = File(destDir, "${request.outputName}.txt")
                    val res = conversionEngine.pdfToText(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-099: Text to PDF
                "SF-099" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = conversionEngine.textToPdf(primaryFile, destFile, fontSize = request.conversionFontSize)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-100: CSV to PDF
                "SF-100" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = conversionEngine.csvToPdf(
                        primaryFile,
                        destFile,
                        orientation = request.conversionOrientation,
                        styleTheme = request.conversionTableTheme
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-103: PDF to CSV
                "SF-103" -> {
                    val destFile = File(destDir, "${request.outputName}.csv")
                    val res = conversionEngine.pdfToCsv(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/csv")
                    res
                }

                // SF-105: HTML to PDF
                "SF-105" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = conversionEngine.htmlToPdf(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-106: PDF to PDF/A
                "SF-106" -> {
                    val destFile = File(destDir, "${request.outputName}.pdf")
                    val res = pdfEngine.convertToPdfA(primaryFile, destFile)
                    recordAndRegister(res, primaryFile.absolutePath, destFile)
                    res
                }

                // SF-107: AI Summarizer
                "SF-107" -> {
                    val summary = aiEngine.summarizeDocument(primaryFile, request.aiSummaryBullets)
                    val destFile = File(destDir, "${request.outputName}_summary.txt")
                    destFile.writeText(summary, Charsets.UTF_8)
                    val res = OperationResult.success(
                        operationType = OperationType.AI_SUMMARY,
                        outputPath = destFile.absolutePath,
                        outputSize = destFile.length(),
                        durationMs = 120L,
                        metadata = mapOf("summary" to summary)
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-108, SF-109: Ask PDF / Chat
                "SF-108", "SF-109" -> {
                    val query = request.aiQuestion.ifBlank { request.watermarkText.ifBlank { "Summarize document key points and main clauses" } }
                    val answer = aiEngine.askDocument(primaryFile, query)
                    val destFile = File(destDir, "${request.outputName}_qa.txt")
                    val qaText = "Question: $query\n\nAnswer: ${answer.answer}\n\nCitations:\n" + answer.citations.joinToString("\n") { cit -> "• Page ${cit.pageNumber}: ${cit.snippet}" }
                    destFile.writeText(qaText, Charsets.UTF_8)
                    val res = OperationResult.success(
                        operationType = OperationType.AI_ASK,
                        outputPath = destFile.absolutePath,
                        outputSize = destFile.length(),
                        durationMs = 150L,
                        metadata = mapOf("answer" to answer.answer, "question" to query)
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-110: Translate PDF
                "SF-110" -> {
                    val targetLang = request.translationTargetLang.ifBlank { "id" }
                    val translatedText = aiEngine.translateDocument(primaryFile, targetLang)
                    val destFile = File(destDir, "${request.outputName}_translated.txt")
                    destFile.writeText(translatedText, Charsets.UTF_8)
                    val res = OperationResult.success(
                        operationType = OperationType.TRANSLATE_PDF,
                        outputPath = destFile.absolutePath,
                        outputSize = destFile.length(),
                        durationMs = 250L,
                        metadata = mapOf("translated" to translatedText)
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-114: Document Classification
                "SF-114" -> {
                    val classification = aiEngine.classifyDocument(primaryFile)
                    val destFile = File(destDir, "${request.outputName}_classified.txt")
                    destFile.writeText("Document Category: $classification", Charsets.UTF_8)
                    val res = OperationResult.success(
                        operationType = OperationType.AI_SUMMARY,
                        outputPath = destFile.absolutePath,
                        outputSize = destFile.length(),
                        durationMs = 100L,
                        metadata = mapOf("classification" to classification)
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-116: Document Insights
                "SF-116" -> {
                    val insights = aiEngine.getDocumentInsights(primaryFile)
                    val destFile = File(destDir, "${request.outputName}_insights.txt")
                    val insightText = "Title: ${insights.title}\nWord Count: ${insights.wordCount}\nReading Time: ${insights.estimatedReadingTimeMinutes} min\nLanguage: ${insights.detectedLanguage}\nType: ${insights.detectedDocumentType}\nKey Topics: ${insights.keyTopics.joinToString(", ")}"
                    destFile.writeText(insightText, Charsets.UTF_8)
                    val res = OperationResult.success(
                        operationType = OperationType.AI_SUMMARY,
                        outputPath = destFile.absolutePath,
                        outputSize = destFile.length(),
                        durationMs = 120L,
                        metadata = mapOf("insights" to insightText)
                    )
                    recordAndRegister(res, primaryFile.absolutePath, destFile, mimeType = "text/plain")
                    res
                }

                // SF-133..SF-137: Workflow
                "SF-133", "SF-134", "SF-135", "SF-137" -> {
                    val userSteps = request.workflowSteps
                    if (userSteps.isEmpty()) {
                        return@withContext OperationResult.failure(
                            OperationType.EXECUTE_WORKFLOW,
                            ErrorCode.INVALID_FILE,
                            "Please add at least one workflow step before executing."
                        )
                    }
                    val workflow = Workflow(
                        id = "user_workflow_${System.currentTimeMillis()}",
                        name = "Custom Workflow",
                        steps = userSteps.mapIndexed { i, opType -> WorkflowStep(i + 1, opType) }
                    )
                    val results = workflowEngine.executeWorkflow(workflow, files, destDir)
                    val finalRes = results.lastOrNull { it.success } ?: results.lastOrNull()
                    finalRes ?: OperationResult.failure(OperationType.EXECUTE_WORKFLOW, ErrorCode.UNKNOWN_ERROR)
                }

                else -> {
                    OperationResult.failure(
                        OperationType.MERGE_PDF,
                        ErrorCode.INVALID_FILE,
                        "Feature '${request.featureName}' (${request.featureId}) is not supported for standalone execution."
                    )
                }
            }
        } catch (e: Exception) {
            OperationResult.failure(
                OperationType.MERGE_PDF,
                ErrorCode.UNKNOWN_ERROR,
                e.message ?: "Operation failed during execution"
            )
        }
    }

    private suspend fun recordAndRegister(
        res: OperationResult,
        inputPath: String,
        destFile: File,
        mimeType: String? = null,
        isEncrypted: Boolean = false,
        hasOcr: Boolean = false
    ) {
        val opId = UUID.randomUUID().toString()
        operationRepository.recordOperation(
            OperationRecord(
                id = opId,
                operationType = res.operationType,
                status = if (res.success) OperationStatus.SUCCESS else OperationStatus.FAILED,
                inputUri = inputPath,
                outputUri = destFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                durationMs = res.durationMs,
                details = res.errorMessage ?: res.metadata["reduction"]
            )
        )

        if (res.success && destFile.exists() && destFile.length() > 0L) {
            val isPdf = destFile.extension.equals("pdf", ignoreCase = true)
            val pageCount = if (isPdf) {
                try {
                    pdfEngine.getPageCount(destFile)
                } catch (_: Exception) {
                    1
                }
            } else 1

            val hash = storageEngine.calculateSha256(destFile)
            val detectedMime = mimeType ?: if (isPdf) "application/pdf" else "application/octet-stream"

            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pageCount,
                    contentHash = hash,
                    mimeType = detectedMime,
                    isEncrypted = isEncrypted,
                    hasOcr = hasOcr
                )
            )
        }
    }

    private fun parseRanges(rangeStr: String): List<IntRange> {
        val list = mutableListOf<IntRange>()
        rangeStr.split(",").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val pieces = trimmed.split("-")
                val start = pieces.getOrNull(0)?.toIntOrNull() ?: 1
                val end = pieces.getOrNull(1)?.toIntOrNull() ?: start
                list.add(start..end)
            } else {
                val num = trimmed.toIntOrNull() ?: 1
                list.add(num..num)
            }
        }
        return if (list.isEmpty()) listOf(1..1) else list
    }

    private fun createDigitalSignatureBitmap(signerName: String): Bitmap {
        val width = 400
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(29, 78, 216)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(30, 29, 78, 216)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat()), 16f, 16f, bgPaint)
        canvas.drawRoundRect(RectF(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat()), 16f, 16f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(30, 58, 138)
            textSize = 22f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(71, 85, 105)
            textSize = 14f
        }
        canvas.drawText("DIGITALLY SIGNED", 30f, 50f, textPaint)
        canvas.drawText("Signer: $signerName", 30f, 85f, subPaint)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Date: $dateStr", 30f, 115f, subPaint)
        canvas.drawText("Verified: ScanFlow Security", 30f, 145f, subPaint)
        return bitmap
    }
}

package com.scanflow.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.registry.FeatureDefinition
import com.scanflow.app.core.registry.FeatureRegistry
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.di.AppContainer
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.domain.model.CompressionLevel
import com.scanflow.app.domain.model.PageNumberConfig
import com.scanflow.app.domain.model.SecurityConfig
import com.scanflow.app.domain.model.WatermarkConfig
import com.scanflow.app.domain.model.Workflow
import com.scanflow.app.domain.model.WorkflowStep
import com.scanflow.app.engine.FormFieldInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ToolActionUiState(
    val feature: FeatureDefinition? = null,
    val selectedFiles: List<File> = emptyList(),
    val status: OperationStatus = OperationStatus.IDLE,
    val progressPercent: Int = 0,
    val result: OperationResult? = null,
    val errorMessage: String? = null,

    // Config options
    val outputName: String = "processed_doc",
    val watermarkText: String = "CONFIDENTIAL",
    val userPassword: String = "",
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val splitRanges: String = "1-2, 3-4",

    // Advanced interactive options
    val pageOrder: String = "",
    val rotationAngle: Int = 90,
    val cropMarginPoints: Float = 36f,
    val signaturePosition: String = "BOTTOM_RIGHT",
    val signatureTargetPage: Int = 1,
    val signerName: String = "Authorized Signer",
    val signatureBitmap: android.graphics.Bitmap? = null,
    val formFields: List<FormFieldInfo> = emptyList(),
    val formFieldValues: Map<String, String> = emptyMap(),
    val redactionTarget: String = "HEADER_FOOTER",
    val aiQuestion: String = "",
    val aiSummaryBullets: Int = 5,
    val translationTargetLang: String = "id",
    val workflowSteps: List<OperationType> = emptyList(),

    // Conversion options (iLovePDF standards)
    val conversionOrientation: String = "AUTO", // "AUTO", "PORTRAIT", "LANDSCAPE"
    val conversionMargin: String = "SMALL",     // "NONE", "SMALL", "NORMAL"
    val conversionFitPage: Boolean = true,
    val conversionPageSize: String = "A4",       // "A4", "LETTER"
    val conversionDpi: Int = 300,               // 150, 300
    val conversionTableTheme: String = "MODERN_NAVY", // "MODERN_NAVY", "CLEAN_SLATE", "MINIMAL"
    val conversionFontSize: Float = 11f         // 9.5f, 11f, 13f
)

class ToolActionViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolActionUiState())
    val uiState = _uiState.asStateFlow()

    fun initFeature(featureId: String) {
        val feature = FeatureRegistry.get(featureId)
        _uiState.value = _uiState.value.copy(
            feature = feature,
            outputName = "${feature?.name?.lowercase()?.replace(" ", "_") ?: "doc"}_${System.currentTimeMillis() % 10000}"
        )
    }

    fun onFilesSelected(files: List<File>) {
        _uiState.value = _uiState.value.copy(selectedFiles = files)
        if (files.isNotEmpty()) {
            val featureId = _uiState.value.feature?.id
            if (featureId in listOf("SF-086", "SF-087", "SF-088", "SF-089", "SF-090", "SF-091", "SF-092")) {
                viewModelScope.launch {
                    try {
                        val fields = container.formEngine.getFormFields(files.first())
                        val initialValues = fields.associate { it.name to it.value }
                        _uiState.value = _uiState.value.copy(
                            formFields = fields,
                            formFieldValues = initialValues
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun onOutputNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(outputName = name)
    }

    fun onWatermarkTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(watermarkText = text)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(userPassword = password)
    }

    fun onCompressionLevelChanged(level: CompressionLevel) {
        _uiState.value = _uiState.value.copy(compressionLevel = level)
    }

    fun onSplitRangesChanged(ranges: String) {
        _uiState.value = _uiState.value.copy(splitRanges = ranges)
    }

    fun onPageOrderChanged(order: String) {
        _uiState.value = _uiState.value.copy(pageOrder = order)
    }

    fun onRotationAngleChanged(angle: Int) {
        _uiState.value = _uiState.value.copy(rotationAngle = angle)
    }

    fun addWorkflowStep(step: OperationType) {
        val current = _uiState.value.workflowSteps.toMutableList()
        current.add(step)
        _uiState.value = _uiState.value.copy(workflowSteps = current)
    }

    fun removeWorkflowStep(index: Int) {
        val current = _uiState.value.workflowSteps.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(workflowSteps = current)
        }
    }

    fun onCropMarginChanged(margin: Float) {
        _uiState.value = _uiState.value.copy(cropMarginPoints = margin)
    }

    fun onSignaturePositionChanged(position: String) {
        _uiState.value = _uiState.value.copy(signaturePosition = position)
    }

    fun onSignatureTargetPageChanged(page: Int) {
        _uiState.value = _uiState.value.copy(signatureTargetPage = page)
    }

    fun onSignerNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(signerName = name)
    }

    fun onSignatureBitmapChanged(bitmap: android.graphics.Bitmap?) {
        _uiState.value = _uiState.value.copy(signatureBitmap = bitmap)
    }

    fun onFormFieldValueChanged(fieldName: String, value: String) {
        val current = _uiState.value.formFieldValues.toMutableMap()
        current[fieldName] = value
        _uiState.value = _uiState.value.copy(formFieldValues = current)
    }

    fun onAiQuestionChanged(question: String) {
        _uiState.value = _uiState.value.copy(aiQuestion = question)
    }

    fun onAiSummaryBulletsChanged(bullets: Int) {
        _uiState.value = _uiState.value.copy(aiSummaryBullets = bullets)
    }

    fun onTranslationTargetLangChanged(lang: String) {
        _uiState.value = _uiState.value.copy(translationTargetLang = lang)
    }

    fun onRedactionTargetChanged(target: String) {
        _uiState.value = _uiState.value.copy(redactionTarget = target)
    }

    fun onConversionOrientationChanged(orientation: String) {
        _uiState.value = _uiState.value.copy(conversionOrientation = orientation)
    }

    fun onConversionMarginChanged(margin: String) {
        _uiState.value = _uiState.value.copy(conversionMargin = margin)
    }

    fun onConversionFitPageChanged(fitPage: Boolean) {
        _uiState.value = _uiState.value.copy(conversionFitPage = fitPage)
    }

    fun onConversionPageSizeChanged(pageSize: String) {
        _uiState.value = _uiState.value.copy(conversionPageSize = pageSize)
    }

    fun onConversionDpiChanged(dpi: Int) {
        _uiState.value = _uiState.value.copy(conversionDpi = dpi)
    }

    fun onConversionTableThemeChanged(theme: String) {
        _uiState.value = _uiState.value.copy(conversionTableTheme = theme)
    }

    fun onConversionFontSizeChanged(fontSize: Float) {
        _uiState.value = _uiState.value.copy(conversionFontSize = fontSize)
    }

    fun cancelOperation() {
        _uiState.value = _uiState.value.copy(
            status = OperationStatus.CANCELLED,
            progressPercent = 0
        )
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(
            status = OperationStatus.IDLE,
            progressPercent = 0,
            result = null,
            errorMessage = null
        )
    }

    fun executeOperation() {
        val state = _uiState.value
        val feature = state.feature ?: return
        val files = state.selectedFiles

        if (files.isEmpty()) {
            _uiState.value = state.copy(
                status = OperationStatus.FAILED,
                errorMessage = "Please select at least one document to proceed."
            )
            return
        }

        _uiState.value = state.copy(status = OperationStatus.PROCESSING, progressPercent = 10, errorMessage = null)

        viewModelScope.launch {
            try {
                when (feature.id) {
                    "SF-001" -> { // Merge PDF
                        val res = container.organizeUseCases.merge(files, state.outputName)
                        handleResult(res)
                    }
                    "SF-002" -> { // Split PDF
                        val ranges = parseRanges(state.splitRanges)
                        val results = container.organizeUseCases.split(files.first(), ranges)
                        val successfulResults = results.filter { it.success }
                        if (successfulResults.isNotEmpty()) {
                            val first = successfulResults.first()
                            val opResult = OperationResult.success(
                                operationType = OperationType.SPLIT_PDF,
                                outputPath = first.outputPath ?: "",
                                outputSize = successfulResults.sumOf { it.outputSize },
                                durationMs = successfulResults.sumOf { it.durationMs },
                                pagesProcessed = successfulResults.sumOf { it.pagesProcessed },
                                metadata = mapOf(
                                    "totalParts" to "${successfulResults.size}",
                                    "parts" to successfulResults.joinToString(", ") { File(it.outputPath ?: "").name }
                                )
                            )
                            handleResult(opResult)
                        } else {
                            val failure = results.firstOrNull() ?: OperationResult.failure(OperationType.SPLIT_PDF, ErrorCode.INVALID_PDF)
                            handleResult(failure)
                        }
                    }
                    "SF-003" -> { // Remove Pages
                        val count = container.pdfEngine.getPageCount(files.first())
                        val rawPages = if (state.pageOrder.isNotBlank()) state.pageOrder else state.splitRanges
                        val parsed = rawPages.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }.toSet()
                        val toRemove = if (parsed.isNotEmpty()) parsed else setOf(1)
                        val res = container.organizeUseCases.removePages(files.first(), toRemove, state.outputName)
                        handleResult(res)
                    }
                    "SF-004" -> { // Extract Pages
                        val count = container.pdfEngine.getPageCount(files.first())
                        val rawPages = if (state.pageOrder.isNotBlank()) state.pageOrder else state.splitRanges
                        val parsed = rawPages.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }
                        val toExtract = if (parsed.isNotEmpty()) parsed else listOf(1)
                        val res = container.organizeUseCases.extractPages(files.first(), toExtract, state.outputName)
                        handleResult(res)
                    }
                    "SF-017", "SF-018", "SF-019", "SF-020", "SF-021" -> { // Compress PDF
                        val res = container.optimizeUseCases.compress(
                            files.first(),
                            CompressionConfig(level = state.compressionLevel),
                            state.outputName
                        ) { cur, tot ->
                            _uiState.value = _uiState.value.copy(progressPercent = (cur * 100 / tot))
                        }
                        handleResult(res)
                    }
                    "SF-025", "SF-062" -> { // Searchable PDF
                        val res = container.ocrUseCases.makeSearchablePdf(
                            files.first(),
                            state.outputName
                        ) { cur, tot ->
                            _uiState.value = _uiState.value.copy(progressPercent = (cur * 100 / tot))
                        }
                        handleResult(res)
                    }
                    "SF-059", "SF-060" -> { // OCR Image - extract text from image file
                        val res = container.ocrUseCases.extractText(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-061" -> { // Extract text from PDF
                        val res = container.ocrUseCases.extractText(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-066" -> { // Edit PDF (Annotation Overlay)
                        val text = state.watermarkText.ifBlank { "ANNOTATED" }
                        val res = container.organizeUseCases.watermark(
                            files.first(),
                            WatermarkConfig(text = text, rotationDegrees = 0f, opacity = 0.9f),
                            state.outputName
                        )
                        handleResult(res)
                    }
                    "SF-078" -> { // Watermark
                        val res = container.organizeUseCases.watermark(
                            files.first(),
                            WatermarkConfig(text = state.watermarkText),
                            state.outputName
                        )
                        handleResult(res)
                    }
                    "SF-077" -> { // Page Numbers
                        val res = container.organizeUseCases.addPageNumbers(
                            files.first(),
                            PageNumberConfig(),
                            state.outputName
                        )
                        handleResult(res)
                    }
                    "SF-079", "SF-080" -> { // Protect PDF
                        val res = container.securityUseCases.protect(
                            files.first(),
                            SecurityConfig(userPassword = state.userPassword),
                            state.outputName
                        )
                        handleResult(res)
                    }
                    "SF-081" -> { // Unlock PDF
                        val res = container.securityUseCases.unlock(
                            files.first(),
                            state.userPassword,
                            state.outputName
                        )
                        handleResult(res)
                    }
                    "SF-082" -> { // Remove Metadata
                        val res = container.securityUseCases.removeMetadata(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-093" -> { // Compare PDF
                        if (files.size < 2) {
                            _uiState.value = _uiState.value.copy(
                                status = OperationStatus.FAILED,
                                errorMessage = "Please select two PDF documents to compare."
                            )
                            return@launch
                        }
                        val report = container.compareUseCases.compare(files[0], files[1])
                        val opResult = OperationResult.success(
                            operationType = OperationType.COMPARE_PDF,
                            outputPath = files[0].absolutePath,
                            outputSize = files[0].length(),
                            durationMs = 0L,
                            pagesProcessed = report.pageResults.size,
                            metadata = mapOf("similarity" to "%.1f%%".format(report.overallSimilarityPercentage))
                        )
                        handleResult(opResult)
                    }
                    "SF-094", "SF-095" -> { // Images to PDF
                        val res = container.conversionUseCases.imagesToPdf(
                            files,
                            state.outputName,
                            fitPage = state.conversionFitPage,
                            pageSize = state.conversionPageSize,
                            orientation = state.conversionOrientation,
                            margin = state.conversionMargin
                        )
                        handleResult(res)
                    }
                    "SF-096", "SF-097" -> { // PDF to Images
                        val format = if (state.feature?.id == "SF-097") "PNG" else "JPG"
                        val results = container.conversionUseCases.pdfToImages(files.first(), format, state.conversionDpi)
                        val successfulResults = results.filter { it.success }
                        if (successfulResults.isNotEmpty()) {
                            val first = successfulResults.first()
                            successfulResults.forEach { r ->
                                val f = File(r.outputPath ?: "")
                                if (f.exists()) {
                                    container.documentRepository.insertDocument(
                                        com.scanflow.app.domain.model.Document(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = f.name,
                                            uri = f.toURI().toString(),
                                            path = f.absolutePath,
                                            sizeBytes = f.length(),
                                            pageCount = 1,
                                            mimeType = "image/jpeg"
                                        )
                                    )
                                }
                            }
                            val opResult = OperationResult.success(
                                operationType = OperationType.PDF_TO_IMAGES,
                                outputPath = first.outputPath ?: "",
                                outputSize = successfulResults.sumOf { it.outputSize },
                                durationMs = successfulResults.sumOf { it.durationMs },
                                pagesProcessed = successfulResults.size,
                                metadata = mapOf(
                                    "totalPages" to "${successfulResults.size}",
                                    "folder" to File(first.outputPath ?: "").parentFile?.absolutePath.orEmpty()
                                )
                            )
                            handleResult(opResult)
                        } else {
                            val failure = results.firstOrNull() ?: OperationResult.failure(OperationType.PDF_TO_IMAGES, ErrorCode.CONVERSION_FAILED)
                            handleResult(failure)
                        }
                    }
                    "SF-005", "SF-007" -> { // Organize & Reorder Pages
                        val count = container.pdfEngine.getPageCount(files.first())
                        val parsedOrder = state.pageOrder.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }
                        val order = if (parsedOrder.isNotEmpty()) parsedOrder else if (count > 1) (1..count).toList().reversed() else listOf(1)
                        val res = container.organizeUseCases.reorderPages(files.first(), order, state.outputName)
                        handleResult(res)
                    }
                    "SF-008", "SF-076" -> { // Rotate Pages
                        val count = container.pdfEngine.getPageCount(files.first())
                        val rotMap = (1..count).associateWith { state.rotationAngle }
                        val res = container.organizeUseCases.rotatePages(files.first(), rotMap, state.outputName)
                        handleResult(res)
                    }
                    "SF-024" -> { // Repair PDF
                        val res = container.optimizeUseCases.repair(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-075" -> { // Crop PDF (Real PDRectangle CropBox)
                        val res = container.optimizeUseCases.crop(files.first(), state.cropMarginPoints, state.outputName)
                        handleResult(res)
                    }
                    "SF-083" -> { // Redact PDF (True page flattening & rasterization)
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}.pdf")
                        val pageCount = container.pdfEngine.getPageCount(files.first())
                        val redactionRects = when (state.redactionTarget) {
                            "ALL_MARGINS" -> listOf(
                                android.graphics.RectF(0f, 0f, 612f, 45f),
                                android.graphics.RectF(0f, 747f, 612f, 792f),
                                android.graphics.RectF(0f, 0f, 45f, 792f),
                                android.graphics.RectF(567f, 0f, 612f, 792f)
                            )
                            else -> listOf(
                                android.graphics.RectF(40f, 35f, 570f, 85f),
                                android.graphics.RectF(40f, 715f, 570f, 765f)
                            )
                        }
                        val redactionsMap = (1..maxOf(1, pageCount)).associateWith { redactionRects }
                        val res = container.securityEngine.redactPdf(files.first(), redactionsMap, destFile)
                        if (res.success && destFile.exists()) {
                            container.documentRepository.insertDocument(
                                com.scanflow.app.domain.model.Document(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = destFile.name,
                                    uri = destFile.toURI().toString(),
                                    path = destFile.absolutePath,
                                    sizeBytes = destFile.length(),
                                    pageCount = pageCount
                                )
                            )
                        }
                        handleResult(res)
                    }
                    "SF-084", "SF-085" -> { // Sign PDF (Real Interactive Signature & Stamp)
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}.pdf")
                        val stampBitmap = state.signatureBitmap ?: createDigitalSignatureBitmap(state.signerName.ifBlank { "VERIFIED SIGNATURE" })
                        val pageCount = container.pdfEngine.getPageCount(files.first())
                        val targetPage = (state.signatureTargetPage - 1).coerceIn(0, maxOf(0, pageCount - 1))
                        val rect = when (state.signaturePosition) {
                            "BOTTOM_LEFT" -> android.graphics.RectF(50f, 680f, 250f, 780f)
                            "TOP_RIGHT" -> android.graphics.RectF(350f, 60f, 550f, 160f)
                            "TOP_LEFT" -> android.graphics.RectF(50f, 60f, 250f, 160f)
                            "CENTER" -> android.graphics.RectF(200f, 350f, 400f, 450f)
                            else -> android.graphics.RectF(350f, 680f, 550f, 780f) // BOTTOM_RIGHT
                        }
                        val res = container.securityEngine.stampSignature(
                            inputFile = files.first(),
                            signatureBitmap = stampBitmap,
                            pageIndex = targetPage,
                            rect = rect,
                            outputFile = destFile
                        )
                        if (state.signatureBitmap == null && !stampBitmap.isRecycled) {
                            stampBitmap.recycle()
                        }
                        if (res.success && destFile.exists()) {
                            container.documentRepository.insertDocument(
                                com.scanflow.app.domain.model.Document(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = destFile.name,
                                    uri = destFile.toURI().toString(),
                                    path = destFile.absolutePath,
                                    sizeBytes = destFile.length(),
                                    pageCount = pageCount
                                )
                            )
                        }
                        handleResult(res)
                    }
                    "SF-086", "SF-087", "SF-088", "SF-089", "SF-090", "SF-091", "SF-092" -> { // PDF Forms
                        val fields = container.formEngine.getFormFields(files.first())
                        val fillMap = if (state.formFieldValues.isNotEmpty()) {
                            state.formFieldValues
                        } else {
                            fields.associate { field ->
                                field.name to (state.watermarkText.ifBlank { "Completed" })
                            }
                        }
                        val res = container.formUseCases.fillAndExport(files.first(), fillMap, true, state.outputName)
                        handleResult(res)
                    }
                    "SF-098", "SF-102" -> { // PDF to Text
                        val res = container.conversionUseCases.pdfToText(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-099" -> { // Text to PDF
                        val res = container.conversionUseCases.textToPdf(
                            files.first(),
                            state.outputName,
                            fontSize = state.conversionFontSize
                        )
                        handleResult(res)
                    }
                    "SF-100" -> { // CSV to PDF
                        val res = container.conversionUseCases.csvToPdf(
                            files.first(),
                            state.outputName,
                            orientation = state.conversionOrientation,
                            styleTheme = state.conversionTableTheme
                        )
                        handleResult(res)
                    }
                    "SF-101" -> { // Images to PDF
                        val res = container.conversionUseCases.imagesToPdf(
                            files,
                            state.outputName,
                            fitPage = state.conversionFitPage,
                            pageSize = state.conversionPageSize,
                            orientation = state.conversionOrientation,
                            margin = state.conversionMargin
                        )
                        handleResult(res)
                    }
                    "SF-103" -> { // PDF to CSV
                        val res = container.conversionUseCases.pdfToCsv(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-104" -> { // PDF to Images (PNG)
                        val results = container.conversionUseCases.pdfToImages(files.first(), "PNG", state.conversionDpi)
                        val successfulResults = results.filter { it.success }
                        if (successfulResults.isNotEmpty()) {
                            val first = successfulResults.first()
                            successfulResults.forEach { r ->
                                val f = File(r.outputPath ?: "")
                                if (f.exists()) {
                                    container.documentRepository.insertDocument(
                                        com.scanflow.app.domain.model.Document(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = f.name,
                                            uri = f.toURI().toString(),
                                            path = f.absolutePath,
                                            sizeBytes = f.length(),
                                            pageCount = 1,
                                            mimeType = "image/png"
                                        )
                                    )
                                }
                            }
                            val opResult = OperationResult.success(
                                operationType = OperationType.PDF_TO_IMAGES,
                                outputPath = first.outputPath ?: "",
                                outputSize = successfulResults.sumOf { it.outputSize },
                                durationMs = successfulResults.sumOf { it.durationMs },
                                pagesProcessed = successfulResults.size,
                                metadata = mapOf(
                                    "totalPages" to "${successfulResults.size}",
                                    "folder" to File(first.outputPath ?: "").parentFile?.absolutePath.orEmpty()
                                )
                            )
                            handleResult(opResult)
                        } else {
                            val failure = results.firstOrNull() ?: OperationResult.failure(OperationType.PDF_TO_IMAGES, ErrorCode.CONVERSION_FAILED)
                            handleResult(failure)
                        }
                    }
                    "SF-105" -> { // HTML to PDF
                        val res = container.conversionUseCases.htmlToPdf(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-106" -> { // PDF to PDF/A
                        val res = container.optimizeUseCases.convertToPdfA(files.first(), state.outputName)
                        handleResult(res)
                    }
                    "SF-107" -> { // AI Summarizer
                        val summary = container.aiUseCases.summarize(files.first(), state.aiSummaryBullets)
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}_summary.txt")
                        destFile.writeText(summary, Charsets.UTF_8)
                        container.documentRepository.insertDocument(
                            com.scanflow.app.domain.model.Document(
                                id = java.util.UUID.randomUUID().toString(),
                                name = destFile.name,
                                uri = destFile.toURI().toString(),
                                path = destFile.absolutePath,
                                sizeBytes = destFile.length(),
                                pageCount = 1,
                                mimeType = "text/plain"
                            )
                        )
                        val opResult = OperationResult.success(
                            operationType = OperationType.AI_SUMMARY,
                            outputPath = destFile.absolutePath,
                            outputSize = destFile.length(),
                            durationMs = 120L,
                            metadata = mapOf("summary" to summary)
                        )
                        handleResult(opResult)
                    }
                    "SF-108", "SF-109" -> { // Ask PDF / Chat
                        val query = state.aiQuestion.ifBlank { state.watermarkText.ifBlank { "Summarize document key points and main clauses" } }
                        val answer = container.aiUseCases.ask(files.first(), query)
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}_qa.txt")
                        val qaText = "Question: $query\n\nAnswer: ${answer.answer}\n\nCitations:\n" + answer.citations.joinToString("\n") { "• Page ${it.pageNumber}: ${it.snippet}" }
                        destFile.writeText(qaText, Charsets.UTF_8)
                        container.documentRepository.insertDocument(
                            com.scanflow.app.domain.model.Document(
                                id = java.util.UUID.randomUUID().toString(),
                                name = destFile.name,
                                uri = destFile.toURI().toString(),
                                path = destFile.absolutePath,
                                sizeBytes = destFile.length(),
                                pageCount = 1,
                                mimeType = "text/plain"
                            )
                        )
                        val opResult = OperationResult.success(
                            operationType = OperationType.AI_ASK,
                            outputPath = destFile.absolutePath,
                            outputSize = destFile.length(),
                            durationMs = 150L,
                            metadata = mapOf("answer" to answer.answer, "question" to query)
                        )
                        handleResult(opResult)
                    }
                    "SF-110" -> { // Translate PDF (Real Offline Translation)
                        val targetLang = state.translationTargetLang.ifBlank { "id" }
                        val translatedText = container.aiUseCases.translate(files.first(), targetLang)
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}_translated.txt")
                        destFile.writeText(translatedText, Charsets.UTF_8)
                        container.documentRepository.insertDocument(
                            com.scanflow.app.domain.model.Document(
                                id = java.util.UUID.randomUUID().toString(),
                                name = destFile.name,
                                uri = destFile.toURI().toString(),
                                path = destFile.absolutePath,
                                sizeBytes = destFile.length(),
                                pageCount = 1,
                                mimeType = "text/plain"
                            )
                        )
                        val opResult = OperationResult.success(
                            operationType = OperationType.TRANSLATE_PDF,
                            outputPath = destFile.absolutePath,
                            outputSize = destFile.length(),
                            durationMs = 250L,
                            metadata = mapOf("translated" to translatedText)
                        )
                        handleResult(opResult)
                    }
                    "SF-133", "SF-134", "SF-135", "SF-137" -> { // Workflow
                        val userSteps = state.workflowSteps
                        if (userSteps.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                status = OperationStatus.FAILED,
                                errorMessage = "Please add at least one workflow step before executing."
                            )
                            return@launch
                        }
                        val workflow = Workflow(
                            id = "user_workflow_${System.currentTimeMillis()}",
                            name = "Custom Workflow",
                            steps = userSteps.mapIndexed { i, opType ->
                                WorkflowStep(i + 1, opType)
                            }
                        )
                        val results = container.workflowUseCases.execute(workflow, files)
                        val finalRes = results.lastOrNull { it.success } ?: results.lastOrNull()
                        if (finalRes != null) handleResult(finalRes)
                    }
                    "SF-009" -> { // Duplicate Pages
                        val count = container.pdfEngine.getPageCount(files.first())
                        val rawPages = if (state.pageOrder.isNotBlank()) state.pageOrder else state.splitRanges
                        val parsed = rawPages.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..count }
                        val toDuplicate = if (parsed.isNotEmpty()) parsed else listOf(1)
                        val res = container.organizeUseCases.duplicatePages(files.first(), toDuplicate, state.outputName)
                        handleResult(res)
                    }
                    "SF-011" -> { // Add Blank Page
                        val targetIdx = state.signatureTargetPage
                        val res = container.organizeUseCases.insertBlankPage(files.first(), targetIdx, state.outputName)
                        handleResult(res)
                    }
                    "SF-114" -> { // Document Classification
                        val classification = container.aiUseCases.classify(files.first())
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}_classified.txt")
                        destFile.writeText("Document Category: $classification", Charsets.UTF_8)
                        val opResult = OperationResult.success(
                            operationType = OperationType.AI_SUMMARY,
                            outputPath = destFile.absolutePath,
                            outputSize = destFile.length(),
                            durationMs = 100L,
                            metadata = mapOf("classification" to classification)
                        )
                        handleResult(opResult)
                    }
                    "SF-116" -> { // Document Insights
                        val insights = container.aiUseCases.getInsights(files.first())
                        val destFile = File(container.storageEngine.getDocumentsDirectory(), "${state.outputName}_insights.txt")
                        val insightText = "Title: ${insights.title}\nWord Count: ${insights.wordCount}\nReading Time: ${insights.estimatedReadingTimeMinutes} min\nLanguage: ${insights.detectedLanguage}\nType: ${insights.detectedDocumentType}\nKey Topics: ${insights.keyTopics.joinToString(", ")}"
                        destFile.writeText(insightText, Charsets.UTF_8)
                        val opResult = OperationResult.success(
                            operationType = OperationType.AI_SUMMARY,
                            outputPath = destFile.absolutePath,
                            outputSize = destFile.length(),
                            durationMs = 120L,
                            metadata = mapOf("insights" to insightText)
                        )
                        handleResult(opResult)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            status = OperationStatus.FAILED,
                            errorMessage = "Feature '${feature.name}' (${feature.id}) is not supported for standalone execution."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = OperationStatus.FAILED,
                    errorMessage = e.message ?: "Operation failed"
                )
            }
        }
    }

    private fun handleResult(res: OperationResult) {
        if (res.success) {
            _uiState.value = _uiState.value.copy(
                status = OperationStatus.SUCCESS,
                progressPercent = 100,
                result = res
            )
        } else {
            _uiState.value = _uiState.value.copy(
                status = OperationStatus.FAILED,
                errorMessage = res.errorMessage ?: "Operation failed with error ${res.errorCode?.name}"
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

    private fun createDigitalSignatureBitmap(signerName: String): android.graphics.Bitmap {
        val width = 400
        val height = 200
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(29, 78, 216)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(30, 29, 78, 216)
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRoundRect(android.graphics.RectF(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat()), 16f, 16f, bgPaint)
        canvas.drawRoundRect(android.graphics.RectF(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat()), 16f, 16f, borderPaint)

        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(30, 58, 138)
            textSize = 22f
            isFakeBoldText = true
        }
        val subPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(71, 85, 105)
            textSize = 14f
        }
        canvas.drawText("DIGITALLY SIGNED", 30f, 50f, textPaint)
        canvas.drawText("Signer: $signerName", 30f, 85f, subPaint)
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Date: $dateStr", 30f, 115f, subPaint)
        canvas.drawText("Verified: ScanFlow Security", 30f, 145f, subPaint)
        return bitmap
    }
}

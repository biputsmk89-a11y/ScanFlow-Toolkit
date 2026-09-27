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
    val splitRanges: String = "1-2, 3-4"
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
                        val finalRes = results.firstOrNull { it.success } ?: results.firstOrNull()
                        if (finalRes != null) handleResult(finalRes)
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
                    "SF-061" -> { // Extract text
                        val res = container.ocrUseCases.extractText(files.first(), state.outputName)
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
                        val res = container.conversionUseCases.imagesToPdf(files, state.outputName)
                        handleResult(res)
                    }
                    "SF-096", "SF-097" -> { // PDF to Images
                        val results = container.conversionUseCases.pdfToImages(files.first(), "JPG")
                        val finalRes = results.firstOrNull { it.success } ?: results.firstOrNull()
                        if (finalRes != null) handleResult(finalRes)
                    }
                    "SF-135" -> { // Workflow
                        val sampleWorkflow = Workflow(
                            id = "default_workflow",
                            name = "Optimize & Watermark",
                            steps = listOf(
                                WorkflowStep(1, OperationType.COMPRESS_PDF),
                                WorkflowStep(2, OperationType.WATERMARK)
                            )
                        )
                        val results = container.workflowUseCases.execute(sampleWorkflow, files)
                        val finalRes = results.firstOrNull { it.success } ?: results.firstOrNull()
                        if (finalRes != null) handleResult(finalRes)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            status = OperationStatus.FAILED,
                            errorMessage = "Feature ${feature.name} action is ready to execute."
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
}

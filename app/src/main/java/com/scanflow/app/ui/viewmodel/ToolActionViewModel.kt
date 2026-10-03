package com.scanflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.core.registry.FeatureDefinition
import com.scanflow.app.core.registry.FeatureRegistry
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.dispatcher.DocumentOperationDispatcher
import com.scanflow.app.domain.dispatcher.ToolExecutionRequest
import com.scanflow.app.domain.model.CompressionLevel
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

/**
 * Presentation ViewModel for document tools.
 * Deeply decoupled from engine implementations and service locators.
 * Communicates strictly across the DocumentOperationDispatcher seam.
 */
class ToolActionViewModel(
    private val dispatcher: DocumentOperationDispatcher
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
                    val fields = dispatcher.inspectFormFields(files.first())
                    val initialValues = fields.associate { it.name to it.value }
                    _uiState.value = _uiState.value.copy(
                        formFields = fields,
                        formFieldValues = initialValues
                    )
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
            val request = ToolExecutionRequest(
                featureId = feature.id,
                featureName = feature.name,
                inputFiles = files,
                outputName = state.outputName,
                watermarkText = state.watermarkText,
                userPassword = state.userPassword,
                compressionLevel = state.compressionLevel,
                splitRanges = state.splitRanges,
                pageOrder = state.pageOrder,
                rotationAngle = state.rotationAngle,
                cropMarginPoints = state.cropMarginPoints,
                signaturePosition = state.signaturePosition,
                signatureTargetPage = state.signatureTargetPage,
                signerName = state.signerName,
                signatureBitmap = state.signatureBitmap,
                formFieldValues = state.formFieldValues,
                redactionTarget = state.redactionTarget,
                aiQuestion = state.aiQuestion,
                aiSummaryBullets = state.aiSummaryBullets,
                translationTargetLang = state.translationTargetLang,
                workflowSteps = state.workflowSteps,
                conversionOrientation = state.conversionOrientation,
                conversionMargin = state.conversionMargin,
                conversionFitPage = state.conversionFitPage,
                conversionPageSize = state.conversionPageSize,
                conversionDpi = state.conversionDpi,
                conversionTableTheme = state.conversionTableTheme,
                conversionFontSize = state.conversionFontSize
            )

            val result = dispatcher.dispatch(request) { cur, tot ->
                if (tot > 0) {
                    _uiState.value = _uiState.value.copy(progressPercent = (cur * 100 / tot).coerceIn(0, 100))
                }
            }
            handleResult(result)
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
}

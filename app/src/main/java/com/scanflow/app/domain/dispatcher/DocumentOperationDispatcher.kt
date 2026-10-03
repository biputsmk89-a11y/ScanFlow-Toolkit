package com.scanflow.app.domain.dispatcher

import android.graphics.Bitmap
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionLevel
import com.scanflow.app.engine.FormFieldInfo
import java.io.File

/**
 * Immutable request contract capturing parameters for executing any tool operation.
 * Keeps the presentation layer decoupled from engine interfaces, storage paths, and domain logic.
 */
data class ToolExecutionRequest(
    val featureId: String,
    val featureName: String = "",
    val inputFiles: List<File>,
    val outputName: String,
    val watermarkText: String = "CONFIDENTIAL",
    val userPassword: String = "",
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val splitRanges: String = "",
    val pageOrder: String = "",
    val rotationAngle: Int = 90,
    val cropMarginPoints: Float = 36f,
    val signaturePosition: String = "BOTTOM_RIGHT",
    val signatureTargetPage: Int = 1,
    val signerName: String = "Authorized Signer",
    val signatureBitmap: Bitmap? = null,
    val formFieldValues: Map<String, String> = emptyMap(),
    val redactionTarget: String = "HEADER_FOOTER",
    val aiQuestion: String = "",
    val aiSummaryBullets: Int = 5,
    val translationTargetLang: String = "id",
    val workflowSteps: List<OperationType> = emptyList(),
    val conversionOrientation: String = "AUTO",
    val conversionMargin: String = "SMALL",
    val conversionFitPage: Boolean = true,
    val conversionPageSize: String = "A4",
    val conversionDpi: Int = 300,
    val conversionTableTheme: String = "MODERN_NAVY",
    val conversionFontSize: Float = 11f
)

/**
 * Deep execution module interface providing high leverage and strict locality
 * for all 31+ document operations in ScanFlow.
 */
interface DocumentOperationDispatcher {
    /**
     * Dispatches and executes a tool operation with progress reporting,
     * handling input validation, engine execution, output verification,
     * audit logging, and document registration behind a single seam.
     */
    suspend fun dispatch(
        request: ToolExecutionRequest,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OperationResult

    /**
     * Inspects form fields present in a PDF document.
     */
    suspend fun inspectFormFields(file: File): List<FormFieldInfo>
}

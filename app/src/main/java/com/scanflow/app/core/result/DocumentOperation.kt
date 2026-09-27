package com.scanflow.app.core.result

import com.scanflow.app.core.error.ErrorCode

data class DocumentOperation(
    val id: String,
    val type: OperationType,
    val displayName: String = type.displayName,
    val category: String = type.category,
    val supportedInputs: List<String> = listOf("application/pdf"),
    val supportedOutputs: List<String> = listOf("application/pdf"),
    val supportsOffline: Boolean = true,
    val requiresNetwork: Boolean = false,
    val estimatedWork: Int = 100,
    val cancellable: Boolean = true
)

data class OperationResult(
    val success: Boolean,
    val operationType: OperationType,
    val outputUri: String? = null,
    val outputPath: String? = null,
    val outputSize: Long = 0L,
    val durationMs: Long = 0L,
    val pagesProcessed: Int = 0,
    val warnings: List<String> = emptyList(),
    val errorCode: ErrorCode? = null,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun failure(
            operationType: OperationType,
            errorCode: ErrorCode,
            errorMessage: String = errorCode.userMessage,
            durationMs: Long = 0L
        ): OperationResult = OperationResult(
            success = false,
            operationType = operationType,
            errorCode = errorCode,
            errorMessage = errorMessage,
            durationMs = durationMs
        )

        fun success(
            operationType: OperationType,
            outputPath: String,
            outputSize: Long,
            durationMs: Long,
            pagesProcessed: Int = 1,
            outputUri: String? = null,
            metadata: Map<String, String> = emptyMap(),
            warnings: List<String> = emptyList()
        ): OperationResult = OperationResult(
            success = true,
            operationType = operationType,
            outputUri = outputUri ?: outputPath,
            outputPath = outputPath,
            outputSize = outputSize,
            durationMs = durationMs,
            pagesProcessed = pagesProcessed,
            metadata = metadata,
            warnings = warnings
        )
    }
}

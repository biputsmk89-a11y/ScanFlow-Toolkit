package com.scanflow.app.engine

import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType

/**
 * Standardized generic outcome contract across all engine seams in ScanFlow.
 * Eliminates ad-hoc result adapters and standardizes execution telemetry,
 * duration tracking, error payloads, and metadata.
 */
data class EngineOutput<out T>(
    val success: Boolean,
    val data: T? = null,
    val durationMs: Long = 0L,
    val errorCode: ErrorCode? = null,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    val isFailure: Boolean get() = !success

    fun getOrNull(): T? = data

    fun getOrDefault(default: @UnsafeVariance T): T = data ?: default

    fun toOperationResult(
        operationType: OperationType,
        outputPath: String = "",
        outputSize: Long = 0L,
        pagesProcessed: Int = 1
    ): OperationResult {
        return if (success) {
            OperationResult.success(
                operationType = operationType,
                outputPath = outputPath,
                outputSize = outputSize,
                durationMs = durationMs,
                pagesProcessed = pagesProcessed,
                metadata = metadata
            )
        } else {
            OperationResult.failure(
                operationType = operationType,
                errorCode = errorCode ?: ErrorCode.UNKNOWN_ERROR,
                errorMessage = errorMessage ?: "Engine operation failed",
                durationMs = durationMs
            )
        }
    }

    companion object {
        fun <T> success(
            data: T,
            durationMs: Long = 0L,
            metadata: Map<String, String> = emptyMap()
        ): EngineOutput<T> = EngineOutput(
            success = true,
            data = data,
            durationMs = durationMs,
            metadata = metadata
        )

        fun <T> failure(
            errorCode: ErrorCode,
            errorMessage: String = errorCode.userMessage,
            durationMs: Long = 0L
        ): EngineOutput<T> = EngineOutput(
            success = false,
            data = null,
            durationMs = durationMs,
            errorCode = errorCode,
            errorMessage = errorMessage
        )
    }
}

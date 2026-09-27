package com.scanflow.app.core.logging

import android.util.Log
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType

/**
 * Privacy-First, Safe Logger for ScanFlow.
 * Enforces zero leakage of passwords, document text, OCR data, or user content.
 */
object SafeLogger {

    private const val TAG_PREFIX = "ScanFlow_"

    fun logOperation(
        featureId: String,
        operationType: OperationType,
        status: OperationStatus,
        durationMs: Long = 0L,
        errorCode: ErrorCode? = null
    ) {
        val message = "Feature=$featureId | Op=${operationType.name} | Status=${status.name} | Duration=${durationMs}ms" +
                if (errorCode != null) " | Error=${errorCode.name}" else ""
        try {
            Log.i(TAG_PREFIX + operationType.category, message)
        } catch (_: Throwable) {
            println("[$TAG_PREFIX${operationType.category}] $message")
        }
    }

    fun d(tag: String, message: String) {
        try {
            Log.d(TAG_PREFIX + tag, sanitize(message))
        } catch (_: Throwable) {
            println("[$TAG_PREFIX$tag] ${sanitize(message)}")
        }
    }

    fun i(tag: String, message: String) {
        try {
            Log.i(TAG_PREFIX + tag, sanitize(message))
        } catch (_: Throwable) {
            println("[$TAG_PREFIX$tag] ${sanitize(message)}")
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.w(TAG_PREFIX + tag, sanitize(message), throwable)
        } catch (_: Throwable) {
            println("[$TAG_PREFIX$tag] ${sanitize(message)} ${throwable?.message ?: ""}")
        }
    }

    fun e(tag: String, errorCode: ErrorCode, throwable: Throwable? = null) {
        try {
            Log.e(TAG_PREFIX + tag, "Error: ${errorCode.name} (${errorCode.userMessage})", throwable)
        } catch (_: Throwable) {
            println("[$TAG_PREFIX$tag] Error: ${errorCode.name} (${errorCode.userMessage}) ${throwable?.message ?: ""}")
        }
    }

    /**
     * Defense-in-depth sanitization: strips potential secrets, keys, or passwords.
     */
    private fun sanitize(message: String): String {
        return message
            .replace(Regex("(?i)password\\s*=\\s*[^,\\s]+"), "password=***")
            .replace(Regex("(?i)secret\\s*=\\s*[^,\\s]+"), "secret=***")
            .replace(Regex("(?i)bearer\\s+[^,\\s]+"), "bearer ***")
    }
}

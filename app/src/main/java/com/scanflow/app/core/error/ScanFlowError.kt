package com.scanflow.app.core.error

/**
 * Standardized exception wrapper for ScanFlow domain and engine operations.
 */
sealed class ScanFlowException(
    val code: ErrorCode,
    override val message: String = code.userMessage,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    class FileNotFound(message: String = ErrorCode.FILE_NOT_FOUND.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.FILE_NOT_FOUND, message, cause)

    class InvalidFile(message: String = ErrorCode.INVALID_FILE.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.INVALID_FILE, message, cause)

    class InvalidPdf(message: String = ErrorCode.INVALID_PDF.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.INVALID_PDF, message, cause)

    class CorruptedPdf(message: String = ErrorCode.CORRUPTED_PDF.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.CORRUPTED_PDF, message, cause)

    class PasswordRequired(message: String = ErrorCode.PASSWORD_REQUIRED.userMessage) :
        ScanFlowException(ErrorCode.PASSWORD_REQUIRED, message)

    class InvalidPassword(message: String = ErrorCode.INVALID_PASSWORD.userMessage) :
        ScanFlowException(ErrorCode.INVALID_PASSWORD, message)

    class UnsupportedFormat(message: String = ErrorCode.UNSUPPORTED_FORMAT.userMessage) :
        ScanFlowException(ErrorCode.UNSUPPORTED_FORMAT, message)

    class StorageError(message: String = ErrorCode.STORAGE_ERROR.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.STORAGE_ERROR, message, cause)

    class PermissionDenied(message: String = ErrorCode.PERMISSION_DENIED.userMessage) :
        ScanFlowException(ErrorCode.PERMISSION_DENIED, message)

    class MemoryError(message: String = ErrorCode.MEMORY_ERROR.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.MEMORY_ERROR, message, cause)

    class OcrFailed(message: String = ErrorCode.OCR_FAILED.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.OCR_FAILED, message, cause)

    class CompressionFailed(message: String = ErrorCode.COMPRESSION_FAILED.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.COMPRESSION_FAILED, message, cause)

    class ConversionFailed(message: String = ErrorCode.CONVERSION_FAILED.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.CONVERSION_FAILED, message, cause)

    class NetworkError(message: String = ErrorCode.NETWORK_ERROR.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.NETWORK_ERROR, message, cause)

    class AiError(message: String = ErrorCode.AI_ERROR.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.AI_ERROR, message, cause)

    class Cancelled(message: String = ErrorCode.PROCESS_CANCELLED.userMessage) :
        ScanFlowException(ErrorCode.PROCESS_CANCELLED, message)

    class OutputValidationFailed(message: String = ErrorCode.OUTPUT_VALIDATION_FAILED.userMessage) :
        ScanFlowException(ErrorCode.OUTPUT_VALIDATION_FAILED, message)

    class Unknown(message: String = ErrorCode.UNKNOWN_ERROR.userMessage, cause: Throwable? = null) :
        ScanFlowException(ErrorCode.UNKNOWN_ERROR, message, cause)
}

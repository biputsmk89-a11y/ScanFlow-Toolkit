package com.scanflow.app.core.error

/**
 * Standardized error taxonomy for ScanFlow operations.
 */
enum class ErrorCode(val userMessage: String) {
    FILE_NOT_FOUND("The requested document could not be found."),
    INVALID_FILE("The selected file is invalid or not recognized."),
    INVALID_PDF("The document is not a valid PDF file."),
    CORRUPTED_PDF("The PDF file appears to be corrupted and cannot be read."),
    PASSWORD_REQUIRED("This document is password protected."),
    INVALID_PASSWORD("The password provided is incorrect."),
    UNSUPPORTED_FORMAT("The format of this document is currently unsupported."),
    STORAGE_ERROR("A storage read or write error occurred."),
    PERMISSION_DENIED("Required storage or camera permission was denied."),
    MEMORY_ERROR("Insufficient memory to complete this operation. Try with a smaller file."),
    OCR_FAILED("Text recognition failed to process this document."),
    COMPRESSION_FAILED("Document compression failed."),
    CONVERSION_FAILED("Document conversion failed."),
    NETWORK_ERROR("Network connection error. Check your connection."),
    AI_ERROR("Document AI processing failed."),
    PROCESS_CANCELLED("The operation was cancelled by the user."),
    OUTPUT_VALIDATION_FAILED("The generated output could not be validated."),
    UNKNOWN_ERROR("An unexpected error occurred. Please try again.")
}

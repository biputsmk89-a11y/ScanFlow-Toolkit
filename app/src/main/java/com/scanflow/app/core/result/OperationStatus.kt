package com.scanflow.app.core.result

enum class OperationStatus {
    IDLE,
    QUEUED,
    PROCESSING,
    CANCELLING,
    SUCCESS,
    FAILED,
    CANCELLED
}

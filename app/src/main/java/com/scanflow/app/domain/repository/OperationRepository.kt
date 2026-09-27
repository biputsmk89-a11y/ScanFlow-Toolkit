package com.scanflow.app.domain.repository

import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import kotlinx.coroutines.flow.Flow

data class OperationRecord(
    val id: String,
    val operationType: OperationType,
    val status: OperationStatus,
    val inputUri: String,
    val outputUri: String?,
    val timestamp: Long,
    val durationMs: Long,
    val details: String?
)

interface OperationRepository {
    fun getRecentOperations(limit: Int = 50): Flow<List<OperationRecord>>
    suspend fun recordOperation(record: OperationRecord)
    suspend fun clearHistory()
}

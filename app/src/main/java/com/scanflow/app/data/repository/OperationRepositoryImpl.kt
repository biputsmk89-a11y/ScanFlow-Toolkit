package com.scanflow.app.data.repository

import com.scanflow.app.data.local.OperationDao
import com.scanflow.app.data.local.OperationEntity
import com.scanflow.app.domain.repository.OperationRecord
import com.scanflow.app.domain.repository.OperationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OperationRepositoryImpl(
    private val operationDao: OperationDao
) : OperationRepository {

    override fun getRecentOperations(limit: Int): Flow<List<OperationRecord>> {
        return operationDao.getRecentOperations(limit).map { list ->
            list.map {
                OperationRecord(
                    id = it.id,
                    operationType = it.operationType,
                    status = it.status,
                    inputUri = it.inputUri,
                    outputUri = it.outputUri,
                    timestamp = it.timestamp,
                    durationMs = it.durationMs,
                    details = it.details
                )
            }
        }
    }

    override suspend fun recordOperation(record: OperationRecord) {
        operationDao.insertOperation(
            OperationEntity(
                id = record.id,
                operationType = record.operationType,
                status = record.status,
                inputUri = record.inputUri,
                outputUri = record.outputUri,
                timestamp = record.timestamp,
                durationMs = record.durationMs,
                details = record.details
            )
        )
    }

    override suspend fun clearHistory() {
        operationDao.clearHistory()
    }
}

package com.scanflow.app.domain.repository

import com.scanflow.app.domain.model.Workflow
import kotlinx.coroutines.flow.Flow

interface WorkflowRepository {
    fun getAllWorkflows(): Flow<List<Workflow>>
    suspend fun getWorkflowById(id: String): Workflow?
    suspend fun saveWorkflow(workflow: Workflow)
    suspend fun deleteWorkflow(id: String): Boolean
}

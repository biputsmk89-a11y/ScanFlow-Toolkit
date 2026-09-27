package com.scanflow.app.engine

import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.domain.model.Workflow
import java.io.File

data class WorkflowStepProgress(
    val currentStepIndex: Int,
    val totalSteps: Int,
    val stepName: String,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false
)

interface WorkflowEngine {
    suspend fun executeWorkflow(
        workflow: Workflow,
        inputFiles: List<File>,
        outputDirectory: File,
        onProgress: ((progress: WorkflowStepProgress) -> Unit)? = null
    ): List<OperationResult>
}

package com.scanflow.app.domain.model

import com.scanflow.app.core.result.OperationType

data class WorkflowStep(
    val stepIndex: Int,
    val operationType: OperationType,
    val configurationJson: String = "{}"
)

data class Workflow(
    val id: String,
    val name: String,
    val description: String = "",
    val steps: List<WorkflowStep>,
    val createdAt: Long = System.currentTimeMillis()
)

package com.scanflow.app.data.repository

import com.scanflow.app.data.local.WorkflowDao
import com.scanflow.app.data.local.WorkflowEntity
import com.scanflow.app.data.local.WorkflowStepEntity
import com.scanflow.app.domain.model.Workflow
import com.scanflow.app.domain.model.WorkflowStep
import com.scanflow.app.domain.repository.WorkflowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkflowRepositoryImpl(
    private val workflowDao: WorkflowDao
) : WorkflowRepository {

    override fun getAllWorkflows(): Flow<List<Workflow>> {
        return workflowDao.getAllWorkflows().map { list ->
            list.map { entity ->
                val steps = workflowDao.getStepsForWorkflow(entity.id).map { step ->
                    WorkflowStep(step.stepIndex, step.operationType, step.configurationJson)
                }
                Workflow(entity.id, entity.name, entity.description, steps, entity.createdAt)
            }
        }
    }

    override suspend fun getWorkflowById(id: String): Workflow? {
        val entity = workflowDao.getWorkflowById(id) ?: return null
        val steps = workflowDao.getStepsForWorkflow(entity.id).map { step ->
            WorkflowStep(step.stepIndex, step.operationType, step.configurationJson)
        }
        return Workflow(entity.id, entity.name, entity.description, steps, entity.createdAt)
    }

    override suspend fun saveWorkflow(workflow: Workflow) {
        workflowDao.insertWorkflow(
            WorkflowEntity(
                id = workflow.id,
                name = workflow.name,
                description = workflow.description,
                createdAt = workflow.createdAt
            )
        )
        workflowDao.deleteSteps(workflow.id)
        val steps = workflow.steps.map {
            WorkflowStepEntity(
                workflowId = workflow.id,
                stepIndex = it.stepIndex,
                operationType = it.operationType,
                configurationJson = it.configurationJson
            )
        }
        workflowDao.insertWorkflowSteps(steps)
    }

    override suspend fun deleteWorkflow(id: String): Boolean {
        workflowDao.deleteSteps(id)
        workflowDao.deleteWorkflow(id)
        return true
    }
}

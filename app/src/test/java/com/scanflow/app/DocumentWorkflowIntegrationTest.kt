package com.scanflow.app

import com.scanflow.app.core.registry.FeatureCategory
import com.scanflow.app.core.registry.FeatureRegistry
import com.scanflow.app.core.registry.FeatureStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.Workflow
import com.scanflow.app.domain.model.WorkflowStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DocumentWorkflowIntegrationTest {

    @Test
    fun testFeatureRegistry_coversAllBaselineOperations() {
        val allFeatures = FeatureRegistry.getAll()
        assertTrue("Feature registry should contain >= 100 registered capabilities", allFeatures.size >= 100)

        // Verify key capabilities are implemented
        val mergeFeature = FeatureRegistry.get("SF-001")
        assertNotNull(mergeFeature)
        assertEquals(FeatureStatus.IMPLEMENTED, mergeFeature?.status)
        assertTrue(mergeFeature?.offline == true)

        val ocrFeature = FeatureRegistry.get("SF-059")
        assertNotNull(ocrFeature)
        assertEquals(FeatureStatus.IMPLEMENTED, ocrFeature?.status)

        val compressFeature = FeatureRegistry.get("SF-017")
        assertNotNull(compressFeature)
        assertEquals(FeatureStatus.IMPLEMENTED, compressFeature?.status)
    }

    @Test
    fun testWorkflow_constructionAndOrdering() {
        val workflowId = UUID.randomUUID().toString()
        val steps = listOf(
            WorkflowStep(stepIndex = 1, operationType = OperationType.OCR_PDF),
            WorkflowStep(stepIndex = 2, operationType = OperationType.COMPRESS_PDF),
            WorkflowStep(stepIndex = 3, operationType = OperationType.WATERMARK)
        )

        val workflow = Workflow(
            id = workflowId,
            name = "OCR + Compress + Watermark Pipeline",
            description = "Automated processing pipeline for incoming scans",
            steps = steps
        )

        assertEquals(3, workflow.steps.size)
        assertEquals(OperationType.OCR_PDF, workflow.steps[0].operationType)
        assertEquals(OperationType.COMPRESS_PDF, workflow.steps[1].operationType)
        assertEquals(OperationType.WATERMARK, workflow.steps[2].operationType)
    }

    @Test
    fun testCategories_mappingConsistency() {
        for (category in FeatureCategory.values()) {
            val features = FeatureRegistry.getByCategory(category)
            assertNotNull("Category ${category.name} query must not return null", features)
        }
    }
}

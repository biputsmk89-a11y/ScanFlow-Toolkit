package com.scanflow.app.engine.impl

import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.domain.model.PageNumberConfig
import com.scanflow.app.domain.model.WatermarkConfig
import com.scanflow.app.domain.model.Workflow
import com.scanflow.app.engine.CompressionEngine
import com.scanflow.app.engine.OcrEngine
import com.scanflow.app.engine.PdfEngine
import com.scanflow.app.engine.StorageEngine
import com.scanflow.app.engine.WorkflowEngine
import com.scanflow.app.engine.WorkflowStepProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WorkflowEngineImpl(
    private val pdfEngine: PdfEngine,
    private val compressionEngine: CompressionEngine,
    private val ocrEngine: OcrEngine,
    private val storageEngine: StorageEngine
) : WorkflowEngine {

    companion object {
        private const val TAG = "WorkflowEngine"
    }

    override suspend fun executeWorkflow(
        workflow: Workflow,
        inputFiles: List<File>,
        outputDirectory: File,
        onProgress: ((progress: WorkflowStepProgress) -> Unit)?
    ): List<OperationResult> = withContext(Dispatchers.IO) {
        val overallResults = mutableListOf<OperationResult>()
        outputDirectory.mkdirs()

        inputFiles.forEach { inputFile ->
            var currentFile = inputFile
            val intermediateFiles = mutableListOf<File>()
            var fileSucceeded = true

            workflow.steps.forEachIndexed { index, step ->
                onProgress?.invoke(
                    WorkflowStepProgress(
                        currentStepIndex = index + 1,
                        totalSteps = workflow.steps.size,
                        stepName = step.operationType.displayName
                    )
                )

                val stepOutputFile = storageEngine.createTempFile("wf_step_${index + 1}", "pdf")
                intermediateFiles.add(stepOutputFile)

                val result = when (step.operationType) {
                    OperationType.COMPRESS_PDF -> {
                        compressionEngine.compressPdf(currentFile, CompressionConfig(), stepOutputFile)
                    }
                    OperationType.WATERMARK -> {
                        pdfEngine.watermark(currentFile, WatermarkConfig(text = "ScanFlow Workflow"), stepOutputFile)
                    }
                    OperationType.PAGE_NUMBERS -> {
                        pdfEngine.addPageNumbers(currentFile, PageNumberConfig(), stepOutputFile)
                    }
                    OperationType.SEARCHABLE_PDF -> {
                        ocrEngine.generateSearchablePdf(currentFile, stepOutputFile)
                    }
                    OperationType.REPAIR_PDF -> {
                        pdfEngine.repair(currentFile, stepOutputFile)
                    }
                    else -> {
                        OperationResult.failure(
                            step.operationType,
                            ErrorCode.UNSUPPORTED_FORMAT,
                            "Operation not supported inside automated chain."
                        )
                    }
                }

                if (result.success && stepOutputFile.exists() && stepOutputFile.length() > 0L) {
                    currentFile = stepOutputFile
                } else {
                    fileSucceeded = false
                    overallResults.add(result)
                    return@forEachIndexed
                }
            }

            if (fileSucceeded) {
                // Move final processed file to target output directory
                val finalName = "${inputFile.nameWithoutExtension}_workflow_out.pdf"
                val finalFile = File(outputDirectory, finalName)
                currentFile.copyTo(finalFile, overwrite = true)

                overallResults.add(
                    OperationResult.success(
                        operationType = OperationType.EXECUTE_WORKFLOW,
                        outputPath = finalFile.absolutePath,
                        outputSize = finalFile.length(),
                        durationMs = 0L,
                        pagesProcessed = 1
                    )
                )
            }

            // Cleanup intermediate files
            intermediateFiles.forEach { tempFile ->
                if (tempFile.exists()) tempFile.delete()
            }
        }

        overallResults
    }
}

package com.scanflow.app.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.scanflow.app.ScanFlowApplication
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.domain.model.CompressionLevel
import java.io.File

class DocumentProcessingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_OPERATION_TYPE = "operation_type"
        const val KEY_INPUT_PATH = "input_path"
        const val KEY_OUTPUT_NAME = "output_name"
        const val KEY_PROGRESS = "progress"
        const val KEY_OUTPUT_PATH = "output_path"
        private const val TAG = "DocProcessingWorker"
    }

    override suspend fun doWork(): Result {
        val opTypeName = inputData.getString(KEY_OPERATION_TYPE) ?: return Result.failure()
        val inputPath = inputData.getString(KEY_INPUT_PATH) ?: return Result.failure()
        val outputName = inputData.getString(KEY_OUTPUT_NAME) ?: "processed_doc"

        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return Result.failure()
        }

        val container = ScanFlowApplication.container

        return try {
            when (opTypeName) {
                OperationType.COMPRESS_PDF.name -> {
                    setProgress(workDataOf(KEY_PROGRESS to 10))
                    val result = container.optimizeUseCases.compress(
                        file = inputFile,
                        config = CompressionConfig(level = CompressionLevel.MEDIUM),
                        outputName = outputName,
                        onProgress = { current, total ->
                            val percent = (current.toFloat() / total.toFloat() * 100).toInt()
                            // setProgress runs asynchronously in WorkManager
                        }
                    )
                    if (result.success && result.outputPath != null) {
                        Result.success(workDataOf(KEY_OUTPUT_PATH to result.outputPath))
                    } else {
                        Result.failure()
                    }
                }
                OperationType.SEARCHABLE_PDF.name -> {
                    setProgress(workDataOf(KEY_PROGRESS to 20))
                    val result = container.ocrUseCases.makeSearchablePdf(
                        file = inputFile,
                        outputName = outputName
                    )
                    if (result.success && result.outputPath != null) {
                        Result.success(workDataOf(KEY_OUTPUT_PATH to result.outputPath))
                    } else {
                        Result.failure()
                    }
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, com.scanflow.app.core.error.ErrorCode.UNKNOWN_ERROR, e)
            Result.failure()
        }
    }
}

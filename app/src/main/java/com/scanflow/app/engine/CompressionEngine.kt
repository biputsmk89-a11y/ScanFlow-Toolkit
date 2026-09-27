package com.scanflow.app.engine

import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.domain.model.CompressionConfig
import java.io.File

interface CompressionEngine {
    suspend fun compressPdf(
        inputFile: File,
        config: CompressionConfig,
        outputFile: File,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OperationResult
}

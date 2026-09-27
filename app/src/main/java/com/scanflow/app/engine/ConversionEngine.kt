package com.scanflow.app.engine

import com.scanflow.app.core.result.OperationResult
import java.io.File

interface ConversionEngine {
    suspend fun imagesToPdf(
        imageFiles: List<File>,
        outputFile: File,
        fitPage: Boolean = true,
        pageSize: String = "A4"
    ): OperationResult

    suspend fun pdfToImages(
        inputFile: File,
        outputDirectory: File,
        format: String = "JPG", // "JPG" or "PNG"
        dpi: Int = 150,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<OperationResult>

    suspend fun pdfToText(
        inputFile: File,
        outputFile: File
    ): OperationResult
}

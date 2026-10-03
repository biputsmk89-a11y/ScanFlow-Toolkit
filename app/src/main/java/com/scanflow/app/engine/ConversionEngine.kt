package com.scanflow.app.engine

import com.scanflow.app.core.result.OperationResult
import java.io.File

interface ConversionEngine {
    suspend fun imagesToPdf(
        imageFiles: List<File>,
        outputFile: File,
        fitPage: Boolean = true,
        pageSize: String = "A4",
        orientation: String = "AUTO", // "AUTO", "PORTRAIT", "LANDSCAPE"
        margin: String = "SMALL"      // "NONE", "SMALL", "NORMAL"
    ): OperationResult

    suspend fun pdfToImages(
        inputFile: File,
        outputDirectory: File,
        format: String = "JPG", // "JPG" or "PNG"
        dpi: Int = 300,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<OperationResult>

    suspend fun pdfToText(
        inputFile: File,
        outputFile: File
    ): OperationResult

    suspend fun htmlToPdf(
        htmlFile: File,
        outputFile: File,
        title: String? = null
    ): OperationResult

    suspend fun textToPdf(
        textFile: File,
        outputFile: File,
        title: String? = null,
        fontSize: Float = 11f
    ): OperationResult

    suspend fun pdfToCsv(
        inputFile: File,
        outputFile: File
    ): OperationResult

    suspend fun csvToPdf(
        csvFile: File,
        outputFile: File,
        title: String? = null,
        orientation: String = "AUTO", // "AUTO", "PORTRAIT", "LANDSCAPE"
        styleTheme: String = "MODERN_NAVY" // "MODERN_NAVY", "CLEAN_SLATE", "MINIMAL"
    ): OperationResult
}


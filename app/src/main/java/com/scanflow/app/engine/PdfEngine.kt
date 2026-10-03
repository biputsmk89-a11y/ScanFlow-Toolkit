package com.scanflow.app.engine

import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.domain.model.PageNumberConfig
import com.scanflow.app.domain.model.WatermarkConfig
import java.io.File

interface PdfEngine {
    suspend fun merge(inputFiles: List<File>, outputFile: File): OperationResult
    suspend fun split(inputFile: File, pageRanges: List<IntRange>, outputDirectory: File): List<OperationResult>
    suspend fun removePages(inputFile: File, pagesToRemove: Set<Int>, outputFile: File): OperationResult
    suspend fun extractPages(inputFile: File, pagesToExtract: List<Int>, outputFile: File): OperationResult
    suspend fun reorderPages(inputFile: File, newOrder: List<Int>, outputFile: File): OperationResult
    suspend fun rotatePages(inputFile: File, pagesToRotate: Map<Int, Int>, outputFile: File): OperationResult
    suspend fun duplicatePages(inputFile: File, pageIndices: List<Int>, outputFile: File): OperationResult
    suspend fun insertBlankPage(inputFile: File, atIndex: Int, outputFile: File): OperationResult
    suspend fun watermark(inputFile: File, config: WatermarkConfig, outputFile: File): OperationResult
    suspend fun addPageNumbers(inputFile: File, config: PageNumberConfig, outputFile: File): OperationResult
    suspend fun repair(inputFile: File, outputFile: File): OperationResult
    suspend fun convertToPdfA(inputFile: File, outputFile: File): OperationResult
    suspend fun cropPages(inputFile: File, marginPoints: Float, outputFile: File): OperationResult
    suspend fun getPageCount(inputFile: File): Int
    suspend fun isEncrypted(inputFile: File): Boolean
}

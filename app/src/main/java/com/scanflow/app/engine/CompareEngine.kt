package com.scanflow.app.engine

import android.graphics.Bitmap
import com.scanflow.app.core.result.OperationResult
import java.io.File

data class PageComparisonResult(
    val pageNumber: Int,
    val differencePercentage: Float,
    val diffBitmap: Bitmap? = null,
    val hasDifferences: Boolean = differencePercentage > 0.01f
)

data class ComparisonReport(
    val fileA: File,
    val fileB: File,
    val pageCountA: Int,
    val pageCountB: Int,
    val pageResults: List<PageComparisonResult>,
    val overallSimilarityPercentage: Float
)

interface CompareEngine {
    suspend fun comparePdfs(
        fileA: File,
        fileB: File,
        generateDiffImages: Boolean = true,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ComparisonReport
}

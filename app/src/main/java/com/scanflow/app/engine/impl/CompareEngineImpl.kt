package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.Color
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.engine.CompareEngine
import com.scanflow.app.engine.ComparisonReport
import com.scanflow.app.engine.PageComparisonResult
import com.scanflow.app.engine.PdfRendererEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max

class CompareEngineImpl(
    private val pdfRendererEngine: PdfRendererEngine
) : CompareEngine {

    companion object {
        private const val TAG = "CompareEngine"
        private const val COMPARE_WIDTH = 800
        private const val COMPARE_HEIGHT = 1100
        private const val COLOR_THRESHOLD = 30
    }

    override suspend fun comparePdfs(
        fileA: File,
        fileB: File,
        generateDiffImages: Boolean,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): ComparisonReport = withContext(Dispatchers.IO) {
        var pageCountA = 0
        var pageCountB = 0

        // Get page counts
        if (pdfRendererEngine.open(fileA)) {
            pageCountA = pdfRendererEngine.getPageCount()
            pdfRendererEngine.close()
        }
        if (pdfRendererEngine.open(fileB)) {
            pageCountB = pdfRendererEngine.getPageCount()
            pdfRendererEngine.close()
        }

        val totalPagesToCompare = max(pageCountA, pageCountB)
        val pageResults = mutableListOf<PageComparisonResult>()
        var totalDiffAccumulator = 0f

        for (pageIndex in 0 until totalPagesToCompare) {
            var bitmapA: Bitmap? = null
            var bitmapB: Bitmap? = null

            if (pageIndex < pageCountA) {
                if (pdfRendererEngine.open(fileA)) {
                    bitmapA = pdfRendererEngine.renderPage(pageIndex, COMPARE_WIDTH, COMPARE_HEIGHT)
                    pdfRendererEngine.close()
                }
            }

            if (pageIndex < pageCountB) {
                if (pdfRendererEngine.open(fileB)) {
                    bitmapB = pdfRendererEngine.renderPage(pageIndex, COMPARE_WIDTH, COMPARE_HEIGHT)
                    pdfRendererEngine.close()
                }
            }

            val pageDiff = compareBitmaps(bitmapA, bitmapB, generateDiffImages)
            pageResults.add(
                PageComparisonResult(
                    pageNumber = pageIndex + 1,
                    differencePercentage = pageDiff.first,
                    diffBitmap = pageDiff.second
                )
            )
            totalDiffAccumulator += pageDiff.first

            bitmapA?.recycle()
            bitmapB?.recycle()

            onProgress?.invoke(pageIndex + 1, totalPagesToCompare)
        }

        val overallSimilarity = if (totalPagesToCompare > 0) {
            (100f - (totalDiffAccumulator / totalPagesToCompare * 100f)).coerceIn(0f, 100f)
        } else 100f

        ComparisonReport(
            fileA = fileA,
            fileB = fileB,
            pageCountA = pageCountA,
            pageCountB = pageCountB,
            pageResults = pageResults,
            overallSimilarityPercentage = overallSimilarity
        )
    }

    private fun compareBitmaps(
        bitmapA: Bitmap?,
        bitmapB: Bitmap?,
        generateDiff: Boolean
    ): Pair<Float, Bitmap?> {
        if (bitmapA == null && bitmapB == null) return Pair(0f, null)
        if (bitmapA == null || bitmapB == null) {
            // One page is entirely missing
            val fullDiffBitmap = if (generateDiff) {
                val bmp = Bitmap.createBitmap(COMPARE_WIDTH, COMPARE_HEIGHT, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.argb(128, 255, 0, 0))
                bmp
            } else null
            return Pair(1.0f, fullDiffBitmap)
        }

        val width = COMPARE_WIDTH
        val height = COMPARE_HEIGHT
        val totalPixels = width * height

        val pixelsA = IntArray(totalPixels)
        val pixelsB = IntArray(totalPixels)
        bitmapA.getPixels(pixelsA, 0, width, 0, 0, width, height)
        bitmapB.getPixels(pixelsB, 0, width, 0, 0, width, height)

        val diffPixels = if (generateDiff) IntArray(totalPixels) else null
        var differentPixelCount = 0

        for (i in 0 until totalPixels) {
            val colorA = pixelsA[i]
            val colorB = pixelsB[i]

            val diffR = abs(Color.red(colorA) - Color.red(colorB))
            val diffG = abs(Color.green(colorA) - Color.green(colorB))
            val diffB = abs(Color.blue(colorA) - Color.blue(colorB))

            if (diffR > COLOR_THRESHOLD || diffG > COLOR_THRESHOLD || diffB > COLOR_THRESHOLD) {
                differentPixelCount++
                if (diffPixels != null) {
                    // Mark difference in high-visibility bright red
                    diffPixels[i] = Color.rgb(239, 68, 68)
                }
            } else {
                if (diffPixels != null) {
                    // Ghost existing page in muted grayscale
                    val gray = (0.299 * Color.red(colorA) + 0.587 * Color.green(colorA) + 0.114 * Color.blue(colorA)).toInt()
                    diffPixels[i] = Color.rgb(gray, gray, gray)
                }
            }
        }

        val diffFraction = differentPixelCount.toFloat() / totalPixels.toFloat()
        val diffBitmap = if (diffPixels != null) {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.setPixels(diffPixels, 0, width, 0, 0, width, height)
            bmp
        } else null

        return Pair(diffFraction, diffBitmap)
    }
}

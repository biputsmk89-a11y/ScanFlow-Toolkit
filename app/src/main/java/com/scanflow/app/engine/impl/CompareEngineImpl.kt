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
        var pfdA: android.os.ParcelFileDescriptor? = null
        var pfdB: android.os.ParcelFileDescriptor? = null
        var rendererA: android.graphics.pdf.PdfRenderer? = null
        var rendererB: android.graphics.pdf.PdfRenderer? = null
        var pageCountA = 0
        var pageCountB = 0

        val pageResults = mutableListOf<PageComparisonResult>()
        var totalDiffAccumulator = 0f

        try {
            if (fileA.exists() && fileA.length() > 0L) {
                pfdA = android.os.ParcelFileDescriptor.open(fileA, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                rendererA = android.graphics.pdf.PdfRenderer(pfdA)
                pageCountA = rendererA.pageCount
            }
            if (fileB.exists() && fileB.length() > 0L) {
                pfdB = android.os.ParcelFileDescriptor.open(fileB, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                rendererB = android.graphics.pdf.PdfRenderer(pfdB)
                pageCountB = rendererB.pageCount
            }

            val totalPagesToCompare = max(pageCountA, pageCountB)

            for (pageIndex in 0 until totalPagesToCompare) {
                var bitmapA: Bitmap? = null
                var bitmapB: Bitmap? = null

                if (pageIndex < pageCountA && rendererA != null) {
                    var page: android.graphics.pdf.PdfRenderer.Page? = null
                    try {
                        page = rendererA.openPage(pageIndex)
                        val bmp = Bitmap.createBitmap(COMPARE_WIDTH, COMPARE_HEIGHT, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmapA = bmp
                    } catch (e: Throwable) {
                        SafeLogger.w(TAG, "Failed to render page $pageIndex of doc A: ${e.message}")
                    } finally {
                        page?.close()
                    }
                }

                if (pageIndex < pageCountB && rendererB != null) {
                    var page: android.graphics.pdf.PdfRenderer.Page? = null
                    try {
                        page = rendererB.openPage(pageIndex)
                        val bmp = Bitmap.createBitmap(COMPARE_WIDTH, COMPARE_HEIGHT, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmapB = bmp
                    } catch (e: Throwable) {
                        SafeLogger.w(TAG, "Failed to render page $pageIndex of doc B: ${e.message}")
                    } finally {
                        page?.close()
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
        } finally {
            try { rendererA?.close() } catch (_: Throwable) {}
            try { pfdA?.close() } catch (_: Throwable) {}
            try { rendererB?.close() } catch (_: Throwable) {}
            try { pfdB?.close() } catch (_: Throwable) {}
        }
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

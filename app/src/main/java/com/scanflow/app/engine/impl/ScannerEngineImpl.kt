package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.engine.ImageProcessingEngine
import com.scanflow.app.engine.ScanFilterType
import com.scanflow.app.engine.ScanSession
import com.scanflow.app.engine.ScannerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class ScannerEngineImpl(
    private val imageProcessingEngine: ImageProcessingEngine
) : ScannerEngine {

    companion object {
        private const val TAG = "ScannerEngine"
        private const val A4_WIDTH_PT = 595
        private const val A4_HEIGHT_PT = 842
    }

    override fun detectDocumentCorners(bitmap: Bitmap): List<PointF>? {
        return try {
            val srcMat = Mat()
            Utils.bitmapToMat(bitmap, srcMat)

            // Resize small for fast contour analysis
            val ratio = bitmap.height.toDouble() / 500.0
            val targetWidth = (bitmap.width / ratio).toInt()
            val smallMat = Mat()
            Imgproc.resize(srcMat, smallMat, Size(targetWidth.toDouble(), 500.0))

            // Grayscale + Gaussian blur + Canny
            val grayMat = Mat()
            Imgproc.cvtColor(smallMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 0.0)

            val edgeMat = Mat()
            Imgproc.Canny(grayMat, edgeMat, 75.0, 200.0)

            // Dilate edges
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edgeMat, edgeMat, kernel)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(edgeMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            // Sort contours by area descending
            contours.sortByDescending { Imgproc.contourArea(it) }

            var docPoints: List<PointF>? = null
            for (contour in contours) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.total() == 4L && Imgproc.contourArea(contour) > 5000.0) {
                    val pts = approx.toArray()
                    val scaledPoints = pts.map { PointF((it.x * ratio).toFloat(), (it.y * ratio).toFloat()) }
                    docPoints = sortCorners(scaledPoints)
                    approx.release()
                    contour2f.release()
                    break
                }
                approx.release()
                contour2f.release()
            }

            // Cleanup Mats
            srcMat.release()
            smallMat.release()
            grayMat.release()
            edgeMat.release()
            kernel.release()
            hierarchy.release()
            contours.forEach { it.release() }

            docPoints
        } catch (e: Throwable) {
            SafeLogger.w(TAG, "Contour detection failed: ${e.message}")
            null
        }
    }

    override suspend fun processScannedPage(
        originalBitmap: Bitmap,
        corners: List<PointF>?,
        filter: ScanFilterType
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Perspective crop
        val cropped = if (corners != null && corners.size == 4) {
            imageProcessingEngine.perspectiveCorrection(originalBitmap, corners)
        } else {
            originalBitmap
        }

        // Step 2: Apply document filter
        when (filter) {
            ScanFilterType.ORIGINAL -> cropped
            ScanFilterType.AUTO_ENHANCE -> {
                // Auto brightness + contrast + sharpen
                val adjusted = imageProcessingEngine.adjustBrightnessContrast(cropped, 10f, 1.2f)
                imageProcessingEngine.sharpen(adjusted)
            }
            ScanFilterType.GRAYSCALE -> imageProcessingEngine.toGrayscale(cropped)
            ScanFilterType.BLACK_AND_WHITE -> imageProcessingEngine.toBlackAndWhite(cropped, 135)
        }
    }

    override suspend fun compileSessionToPdf(
        session: ScanSession,
        outputFile: File,
        includeInvisibleOcrLayer: Boolean
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (session.pages.isEmpty()) {
            return@withContext OperationResult.failure(
                OperationType.SCAN_DOCUMENT,
                ErrorCode.INVALID_FILE,
                "No scanned pages in this session."
            )
        }

        val pdfDoc = PdfDocument()
        try {
            outputFile.parentFile?.mkdirs()

            session.pages.forEachIndexed { index, page ->
                val imageFile = File(page.enhancedImagePath)
                if (!imageFile.exists()) return@forEachIndexed

                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return@forEachIndexed

                // Determine orientation
                val isLandscape = bitmap.width > bitmap.height
                val pageW = if (isLandscape) A4_HEIGHT_PT else A4_WIDTH_PT
                val pageH = if (isLandscape) A4_WIDTH_PT else A4_HEIGHT_PT

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val pdfPage = pdfDoc.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Fit and center bitmap on page
                val scale = minOf(pageW.toFloat() / bitmap.width, pageH.toFloat() / bitmap.height)
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f

                val dstRect = RectF(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(bitmap, null, dstRect, null)

                pdfDoc.finishPage(pdfPage)
                bitmap.recycle()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.SCAN_DOCUMENT,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration,
                pagesProcessed = session.pages.size
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.STORAGE_ERROR, e)
            OperationResult.failure(
                OperationType.SCAN_DOCUMENT,
                ErrorCode.STORAGE_ERROR,
                e.message ?: "Failed to save scanned document",
                System.currentTimeMillis() - startTime
            )
        } finally {
            pdfDoc.close()
        }
    }

    /**
     * Sort 4 points into canonical order:
     * 0: Top-Left, 1: Top-Right, 2: Bottom-Right, 3: Bottom-Left
     */
    private fun sortCorners(points: List<PointF>): List<PointF> {
        if (points.size != 4) return points

        // Sum (x + y): min is top-left, max is bottom-right
        val sortedBySum = points.sortedBy { it.x + it.y }
        val tl = sortedBySum.first()
        val br = sortedBySum.last()

        // Difference (y - x): min is top-right, max is bottom-left
        val remaining = points.filter { it != tl && it != br }
        val tr = remaining.minByOrNull { it.y - it.x } ?: remaining[0]
        val bl = remaining.maxByOrNull { it.y - it.x } ?: remaining[1]

        return listOf(tl, tr, br, bl)
    }
}

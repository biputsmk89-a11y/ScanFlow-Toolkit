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
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class ScannerEngineImpl(
    private val imageProcessingEngine: ImageProcessingEngine,
    private val ocrEngine: com.scanflow.app.engine.OcrEngine? = null
) : ScannerEngine {

    companion object {
        private const val TAG = "ScannerEngine"
        // Standard 300 DPI A4 print resolution (2480 x 3508 pixels)
        private const val A4_WIDTH_300DPI = 2480
        private const val A4_HEIGHT_300DPI = 3508
    }

    override fun detectDocumentCorners(bitmap: Bitmap): List<PointF>? {
        return try {
            val srcMat = Mat()
            Utils.bitmapToMat(bitmap, srcMat)

            // Resize small for fast and noise-free contour analysis (target height ~600px)
            val ratio = bitmap.height.toDouble() / 600.0
            val targetWidth = (bitmap.width / ratio).toInt().coerceAtLeast(1)
            val smallMat = Mat()
            Imgproc.resize(srcMat, smallMat, Size(targetWidth.toDouble(), 600.0))

            // 1. Grayscale + Gaussian blur to suppress noise/texture
            val grayMat = Mat()
            Imgproc.cvtColor(smallMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            val blurMat = Mat()
            Imgproc.GaussianBlur(grayMat, blurMat, Size(5.0, 5.0), 0.0)

            // 2. Adaptive edge detection: compute Otsu threshold dynamically for lighting invariance
            val dummyMat = Mat()
            val otsuVal = Imgproc.threshold(blurMat, dummyMat, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
            dummyMat.release()

            val edgeMat = Mat()
            Imgproc.Canny(blurMat, edgeMat, (0.4 * otsuVal).coerceAtLeast(30.0), (1.0 * otsuVal).coerceAtLeast(100.0))

            // 3. Morphological closing to bridge small breaks caused by pens, clips, or folds
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
            val closedMat = Mat()
            Imgproc.morphologyEx(edgeMat, closedMat, Imgproc.MORPH_CLOSE, kernel)

            // 4. Find all contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(closedMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            val frameArea = smallMat.width().toDouble() * smallMat.height().toDouble()
            // Contours must occupy between 8% and 98% of the camera frame
            val validContours = contours.filter {
                val area = Imgproc.contourArea(it)
                area in (frameArea * 0.08)..(frameArea * 0.98)
            }.sortedByDescending { Imgproc.contourArea(it) }

            var docPoints: List<PointF>? = null

            for (contour in validContours) {
                // Compute convex hull to eliminate indentations from pens, fingers, or notebook spirals
                val hullIndices = MatOfInt()
                Imgproc.convexHull(contour, hullIndices)
                val contourPts = contour.toArray()
                val hullPts = hullIndices.toArray().map { contourPts[it] }.toTypedArray()
                val hullContour = MatOfPoint(*hullPts)
                val hull2f = MatOfPoint2f(*hullPts)
                val peri = Imgproc.arcLength(hull2f, true)

                var foundQuad: MatOfPoint2f? = null

                // Try varying approximation factors (0.015 to 0.05) to find a clean 4-corner quad
                for (factor in listOf(0.02, 0.03, 0.015, 0.04, 0.05)) {
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(hull2f, approx, factor * peri, true)
                    if (approx.total() == 4L && Imgproc.isContourConvex(MatOfPoint(*approx.toArray()))) {
                        foundQuad = approx
                        break
                    } else {
                        approx.release()
                    }
                }

                if (foundQuad != null) {
                    val pts = foundQuad.toArray()
                    val scaledPoints = pts.map { PointF((it.x * ratio).toFloat(), (it.y * ratio).toFloat()) }
                    docPoints = sortCorners(scaledPoints)
                    foundQuad.release()
                } else if (hullPts.size >= 4) {
                    // Quad approximation didn't return exactly 4 points, but convex hull is large.
                    // Extract the 4 extreme corner points from the convex hull
                    val tl = hullPts.minByOrNull { it.x + it.y } ?: hullPts[0]
                    val br = hullPts.maxByOrNull { it.x + it.y } ?: hullPts[hullPts.size - 1]
                    val tr = hullPts.minByOrNull { it.y - it.x } ?: hullPts[0]
                    val bl = hullPts.maxByOrNull { it.y - it.x } ?: hullPts[0]

                    val quadCandidate = listOf(
                        PointF((tl.x * ratio).toFloat(), (tl.y * ratio).toFloat()),
                        PointF((tr.x * ratio).toFloat(), (tr.y * ratio).toFloat()),
                        PointF((br.x * ratio).toFloat(), (br.y * ratio).toFloat()),
                        PointF((bl.x * ratio).toFloat(), (bl.y * ratio).toFloat())
                    )
                    docPoints = sortCorners(quadCandidate)
                }

                hullIndices.release()
                hullContour.release()
                hull2f.release()

                if (docPoints != null && docPoints.size == 4) {
                    break
                }
            }

            // Cleanup Mats
            srcMat.release()
            smallMat.release()
            grayMat.release()
            blurMat.release()
            edgeMat.release()
            closedMat.release()
            kernel.release()
            hierarchy.release()
            contours.forEach { it.release() }

            // If no quad was detected (e.g. document completely covers screen edge-to-edge),
            // provide a smart 2% border inset quad so corners are always valid!
            docPoints ?: run {
                val w = bitmap.width.toFloat()
                val h = bitmap.height.toFloat()
                listOf(
                    PointF(w * 0.02f, h * 0.02f),
                    PointF(w * 0.98f, h * 0.02f),
                    PointF(w * 0.98f, h * 0.98f),
                    PointF(w * 0.02f, h * 0.98f)
                )
            }
        } catch (e: Throwable) {
            SafeLogger.w(TAG, "Contour detection failed: ${e.message}")
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            listOf(
                PointF(w * 0.02f, h * 0.02f),
                PointF(w * 0.98f, h * 0.02f),
                PointF(w * 0.98f, h * 0.98f),
                PointF(w * 0.02f, h * 0.98f)
            )
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
                // Auto brightness + contrast + sharpen for crisp document reading
                val adjusted = imageProcessingEngine.adjustBrightnessContrast(cropped, 12f, 1.25f)
                val sharpened = imageProcessingEngine.sharpen(adjusted)
                if (adjusted !== cropped && adjusted !== sharpened) {
                    adjusted.recycle()
                }
                if (cropped !== originalBitmap && cropped !== sharpened) {
                    cropped.recycle()
                }
                sharpened
            }
            ScanFilterType.GRAYSCALE -> {
                val gray = imageProcessingEngine.toGrayscale(cropped)
                if (cropped !== originalBitmap && cropped !== gray) {
                    cropped.recycle()
                }
                gray
            }
            ScanFilterType.BLACK_AND_WHITE -> {
                val bw = imageProcessingEngine.toBlackAndWhite(cropped, 128)
                if (cropped !== originalBitmap && cropped !== bw) {
                    cropped.recycle()
                }
                bw
            }
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

                // Determine orientation at 300 DPI high-definition canvas
                val isLandscape = bitmap.width > bitmap.height
                val pageW = if (isLandscape) A4_HEIGHT_300DPI else A4_WIDTH_300DPI
                val pageH = if (isLandscape) A4_WIDTH_300DPI else A4_HEIGHT_300DPI

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val pdfPage = pdfDoc.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Fit and center bitmap on page with high-definition anti-aliasing Paint
                val scale = minOf(pageW.toFloat() / bitmap.width, pageH.toFloat() / bitmap.height)
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f

                val dstRect = RectF(left, top, left + drawW, top + drawH)
                val paint = Paint().apply {
                    isFilterBitmap = true
                    isAntiAlias = true
                    isDither = true
                }
                canvas.drawBitmap(bitmap, null, dstRect, paint)

                // Inject invisible OCR text layer if requested and OCR engine is present
                if (includeInvisibleOcrLayer && ocrEngine != null) {
                    try {
                        val ocrRes = ocrEngine.recognizeImage(bitmap)
                        if (ocrRes.blocks.isNotEmpty()) {
                            val textPaint = Paint().apply {
                                color = android.graphics.Color.TRANSPARENT
                                textSize = 28f
                                isAntiAlias = true
                            }
                            ocrRes.blocks.forEach { block ->
                                val lines = if (block.lines.isNotEmpty()) block.lines else block.text.lines()
                                val textLeft = left + block.boundingBox.left * scale
                                val lineSpacing = if (lines.isNotEmpty()) (block.boundingBox.height() * scale / lines.size) else 34f
                                lines.forEachIndexed { lineIdx, lineText ->
                                    if (lineText.isNotBlank()) {
                                        val lineY = top + (block.boundingBox.top * scale) + ((lineIdx + 1) * lineSpacing)
                                        canvas.drawText(lineText, textLeft, lineY, textPaint)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        SafeLogger.w(TAG, "Failed to render invisible OCR text layer: ${e.message}")
                    }
                }

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
        if (remaining.size < 2) return points
        val tr = remaining.minByOrNull { it.y - it.x } ?: remaining[0]
        val bl = remaining.maxByOrNull { it.y - it.x } ?: remaining[1]

        return listOf(tl, tr, br, bl)
    }
}

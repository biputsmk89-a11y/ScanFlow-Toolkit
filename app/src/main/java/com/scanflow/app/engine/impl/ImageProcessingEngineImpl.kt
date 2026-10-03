package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.engine.ImageProcessingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ImageProcessingEngineImpl : ImageProcessingEngine {

    companion object {
        private const val TAG = "ImageProcessingEngine"
    }

    override suspend fun crop(bitmap: Bitmap, rect: RectF): Bitmap = withContext(Dispatchers.Default) {
        val left = minOf(rect.left, rect.right).toInt().coerceIn(0, bitmap.width - 1)
        val top = minOf(rect.top, rect.bottom).toInt().coerceIn(0, bitmap.height - 1)
        val right = maxOf(rect.left, rect.right).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = maxOf(rect.top, rect.bottom).toInt().coerceIn(top + 1, bitmap.height)
        val width = (right - left).coerceIn(1, bitmap.width - left)
        val height = (bottom - top).coerceIn(1, bitmap.height - top)
        Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    override suspend fun rotate(bitmap: Bitmap, degrees: Float): Bitmap = withContext(Dispatchers.Default) {
        if (degrees % 360f == 0f) return@withContext bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override suspend fun resize(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap = withContext(Dispatchers.Default) {
        if (bitmap.width == targetWidth && bitmap.height == targetHeight) return@withContext bitmap
        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    override suspend fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap = withContext(Dispatchers.Default) {
        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f
        val matrix = Matrix().apply { postScale(sx, sy, bitmap.width / 2f, bitmap.height / 2f) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override suspend fun toGrayscale(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        output
    }

    override suspend fun toBlackAndWhite(bitmap: Bitmap, threshold: Int): Bitmap = withContext(Dispatchers.Default) {
        var mat: Mat? = null
        var grayMat: Mat? = null
        var bwMat: Mat? = null
        try {
            // Use OpenCV adaptive Gaussian thresholding for shadow-free crisp document binarization
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            bwMat = Mat()
            Imgproc.adaptiveThreshold(
                grayMat,
                bwMat,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                25,
                10.0
            )
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(bwMat, output)
            output
        } catch (e: Throwable) {
            // Pure Kotlin fallback if OpenCV native is not ready
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                val value = if (lum >= threshold) 255 else 0
                pixels[i] = Color.rgb(value, value, value)
            }
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            output.setPixels(pixels, 0, width, 0, 0, width, height)
            output
        } finally {
            mat?.release()
            grayMat?.release()
            bwMat?.release()
        }
    }

    override suspend fun adjustBrightnessContrast(
        bitmap: Bitmap,
        brightness: Float, // -100 to 100
        contrast: Float   // 0.5 to 2.0
    ): Bitmap = withContext(Dispatchers.Default) {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        output
    }

    override suspend fun sharpen(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            val kernel = Mat(3, 3, CvType.CV_32F).apply {
                put(0, 0, 0.0, -1.0, 0.0)
                put(1, 0, -1.0, 5.0, -1.0)
                put(2, 0, 0.0, -1.0, 0.0)
            }
            val sharpened = Mat()
            Imgproc.filter2D(mat, sharpened, -1, kernel)
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(sharpened, output)
            mat.release()
            kernel.release()
            sharpened.release()
            output
        } catch (e: Throwable) {
            bitmap // Return original if OpenCV unavailable
        }
    }

    override suspend fun deskew(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        var mat: Mat? = null
        var gray: Mat? = null
        var lines: Mat? = null
        try {
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            org.opencv.core.Core.bitwise_not(gray, gray)

            lines = Mat()
            Imgproc.HoughLinesP(gray, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)

            var totalAngle = 0.0
            var count = 0
            for (i in 0 until lines.rows()) {
                val line = lines.get(i, 0)
                val angle = Math.toDegrees(Math.atan2(line[3] - line[1], line[2] - line[0]))
                if (Math.abs(angle) in 1.0..45.0) {
                    totalAngle += angle
                    count++
                }
            }

            if (count > 0) {
                val averageAngle = (totalAngle / count).toFloat()
                rotate(bitmap, -averageAngle)
            } else {
                bitmap
            }
        } catch (e: Throwable) {
            bitmap
        } finally {
            mat?.release()
            gray?.release()
            lines?.release()
        }
    }

    override suspend fun perspectiveCorrection(bitmap: Bitmap, corners: List<PointF>): Bitmap =
        withContext(Dispatchers.Default) {
            if (corners.size != 4) return@withContext bitmap

            var srcPoints: MatOfPoint2f? = null
            var dstPoints: MatOfPoint2f? = null
            var perspectiveTransform: Mat? = null
            var srcMat: Mat? = null
            var dstMat: Mat? = null
            try {
                val tl = corners[0]
                val tr = corners[1]
                val br = corners[2]
                val bl = corners[3]

                // Calculate output width & height based on corner distances
                val widthA = sqrt(((br.x - bl.x) * (br.x - bl.x) + (br.y - bl.y) * (br.y - bl.y)).toDouble())
                val widthB = sqrt(((tr.x - tl.x) * (tr.x - tl.x) + (tr.y - tl.y) * (tr.y - tl.y)).toDouble())
                val maxWidth = max(widthA, widthB).toInt().coerceAtLeast(100)

                val heightA = sqrt(((tr.x - br.x) * (tr.x - br.x) + (tr.y - br.y) * (tr.y - br.y)).toDouble())
                val heightB = sqrt(((tl.x - bl.x) * (tl.x - bl.x) + (tl.y - bl.y) * (tl.y - bl.y)).toDouble())
                val maxHeight = max(heightA, heightB).toInt().coerceAtLeast(100)

                srcPoints = MatOfPoint2f(
                    Point(tl.x.toDouble(), tl.y.toDouble()),
                    Point(tr.x.toDouble(), tr.y.toDouble()),
                    Point(br.x.toDouble(), br.y.toDouble()),
                    Point(bl.x.toDouble(), bl.y.toDouble())
                )

                dstPoints = MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(maxWidth.toDouble(), 0.0),
                    Point(maxWidth.toDouble(), maxHeight.toDouble()),
                    Point(0.0, maxHeight.toDouble())
                )

                perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
                srcMat = Mat()
                Utils.bitmapToMat(bitmap, srcMat)
                dstMat = Mat()
                Imgproc.warpPerspective(
                    srcMat,
                    dstMat,
                    perspectiveTransform,
                    Size(maxWidth.toDouble(), maxHeight.toDouble()),
                    Imgproc.INTER_CUBIC
                )

                val output = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(dstMat, output)
                output
            } catch (e: Throwable) {
                SafeLogger.w(TAG, "Perspective correction failed: ${e.message}")
                bitmap
            } finally {
                srcPoints?.release()
                dstPoints?.release()
                perspectiveTransform?.release()
                srcMat?.release()
                dstMat?.release()
            }
        }

    override suspend fun compressImage(bitmap: Bitmap, outputFile: File, quality: Int): File = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
        }
        outputFile
    }

    override suspend fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                BitmapFactory.decodeFile(file.absolutePath, options)
            } catch (e: Exception) {
                SafeLogger.w(TAG, "Failed to decode sampled bitmap: ${e.message}")
                null
            }
        }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

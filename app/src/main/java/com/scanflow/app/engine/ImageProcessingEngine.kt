package com.scanflow.app.engine

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import java.io.File

interface ImageProcessingEngine {
    suspend fun crop(bitmap: Bitmap, rect: RectF): Bitmap
    suspend fun rotate(bitmap: Bitmap, degrees: Float): Bitmap
    suspend fun resize(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap
    suspend fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap
    suspend fun toGrayscale(bitmap: Bitmap): Bitmap
    suspend fun toBlackAndWhite(bitmap: Bitmap, threshold: Int = 128): Bitmap
    suspend fun adjustBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap
    suspend fun sharpen(bitmap: Bitmap): Bitmap
    suspend fun deskew(bitmap: Bitmap): Bitmap
    suspend fun perspectiveCorrection(bitmap: Bitmap, corners: List<PointF>): Bitmap
    suspend fun compressImage(bitmap: Bitmap, outputFile: File, quality: Int = 80): File
    suspend fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap?
}

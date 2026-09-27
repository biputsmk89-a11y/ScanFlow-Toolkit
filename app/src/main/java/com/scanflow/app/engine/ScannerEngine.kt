package com.scanflow.app.engine

import android.graphics.Bitmap
import android.graphics.PointF
import com.scanflow.app.core.result.OperationResult
import java.io.File

data class ScannedPage(
    val id: String,
    val originalImagePath: String,
    val enhancedImagePath: String,
    val corners: List<PointF> = emptyList(),
    val rotationDegrees: Int = 0,
    val filterType: ScanFilterType = ScanFilterType.AUTO_ENHANCE
)

enum class ScanFilterType {
    ORIGINAL,
    AUTO_ENHANCE,
    GRAYSCALE,
    BLACK_AND_WHITE
}

data class ScanSession(
    val id: String,
    val pages: MutableList<ScannedPage> = mutableListOf()
)

interface ScannerEngine {
    fun detectDocumentCorners(bitmap: Bitmap): List<PointF>?
    suspend fun processScannedPage(
        originalBitmap: Bitmap,
        corners: List<PointF>?,
        filter: ScanFilterType
    ): Bitmap
    suspend fun compileSessionToPdf(
        session: ScanSession,
        outputFile: File,
        includeInvisibleOcrLayer: Boolean = false
    ): OperationResult
}

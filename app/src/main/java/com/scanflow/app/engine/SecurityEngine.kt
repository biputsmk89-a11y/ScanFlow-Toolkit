package com.scanflow.app.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.domain.model.SecurityConfig
import java.io.File

interface SecurityEngine {
    suspend fun protectPdf(inputFile: File, config: SecurityConfig, outputFile: File): OperationResult
    suspend fun unlockPdf(inputFile: File, password: String, outputFile: File): OperationResult
    suspend fun removeMetadata(inputFile: File, outputFile: File): OperationResult
    suspend fun redactPdf(inputFile: File, pageRedactions: Map<Int, List<RectF>>, outputFile: File): OperationResult
    suspend fun stampSignature(
        inputFile: File,
        signatureBitmap: Bitmap,
        pageIndex: Int,
        rect: RectF,
        outputFile: File
    ): OperationResult
}

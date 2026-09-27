package com.scanflow.app.engine

import android.graphics.Bitmap
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.domain.model.OcrPageResult
import com.scanflow.app.domain.model.OcrResult
import java.io.File

interface OcrEngine {
    suspend fun recognizeImage(bitmap: Bitmap): OcrPageResult
    suspend fun recognizePdf(inputFile: File, onProgress: ((current: Int, total: Int) -> Unit)? = null): OcrResult
    suspend fun generateSearchablePdf(inputFile: File, outputFile: File, onProgress: ((current: Int, total: Int) -> Unit)? = null): OperationResult
    suspend fun extractTextToTextFile(inputFile: File, outputFile: File): OperationResult
}

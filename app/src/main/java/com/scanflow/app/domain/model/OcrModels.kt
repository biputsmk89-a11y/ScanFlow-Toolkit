package com.scanflow.app.domain.model

import android.graphics.RectF

data class OcrBlock(
    val text: String,
    val boundingBox: RectF,
    val confidence: Float = 1.0f,
    val lines: List<String> = emptyList()
)

data class OcrPageResult(
    val pageNumber: Int,
    val fullText: String,
    val blocks: List<OcrBlock>,
    val renderWidth: Int = 1200,
    val renderHeight: Int = 1600
)

data class OcrResult(
    val documentId: String,
    val pageResults: List<OcrPageResult>,
    val fullText: String,
    val durationMs: Long
)

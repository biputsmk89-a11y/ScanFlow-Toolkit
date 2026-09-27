package com.scanflow.app.engine

import android.graphics.Bitmap
import java.io.File

interface PdfRendererEngine {
    suspend fun open(file: File): Boolean
    suspend fun getPageCount(): Int
    suspend fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap?
    suspend fun renderThumbnail(file: File, pageIndex: Int = 0, size: Int = 256): Bitmap?
    fun close()
}

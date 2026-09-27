package com.scanflow.app.engine.impl

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.engine.PdfRendererEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererEngineImpl : PdfRendererEngine {

    companion object {
        private const val TAG = "PdfRendererEngine"
    }

    private var currentPfd: ParcelFileDescriptor? = null
    private var currentRenderer: PdfRenderer? = null
    private var currentFile: File? = null
    private val mutex = Mutex()

    override suspend fun open(file: File): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closeInternal()
                if (!file.exists() || file.length() == 0L) {
                    return@withContext false
                }
                currentPfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                currentRenderer = PdfRenderer(currentPfd!!)
                currentFile = file
                true
            } catch (e: Exception) {
                SafeLogger.w(TAG, "Failed to open PDF for rendering: ${e.message}")
                closeInternal()
                false
            }
        }
    }

    override suspend fun getPageCount(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            currentRenderer?.pageCount ?: 0
        }
    }

    override suspend fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val renderer = currentRenderer ?: return@withContext null
                if (pageIndex !in 0 until renderer.pageCount) return@withContext null

                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(pageIndex)
                    val width = if (targetWidth > 0) targetWidth else page.width
                    val height = if (targetHeight > 0) targetHeight else page.height

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } catch (e: Exception) {
                    SafeLogger.w(TAG, "Failed to render page $pageIndex: ${e.message}")
                    null
                } finally {
                    page?.close()
                }
            }
        }

    override suspend fun renderThumbnail(file: File, pageIndex: Int, size: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                if (pageIndex !in 0 until renderer.pageCount) return@withContext null

                page = renderer.openPage(pageIndex)
                val aspectRatio = page.width.toFloat() / page.height.toFloat()
                val (w, h) = if (aspectRatio >= 1f) {
                    Pair(size, (size / aspectRatio).toInt().coerceAtLeast(1))
                } else {
                    Pair((size * aspectRatio).toInt().coerceAtLeast(1), size)
                }

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (e: Exception) {
                SafeLogger.w(TAG, "Failed to render thumbnail for ${file.name}: ${e.message}")
                null
            } finally {
                page?.close()
                renderer?.close()
                pfd?.close()
            }
        }

    override fun close() {
        closeInternal()
    }

    private fun closeInternal() {
        try {
            currentRenderer?.close()
            currentPfd?.close()
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Error closing renderer: ${e.message}")
        } finally {
            currentRenderer = null
            currentPfd = null
            currentFile = null
        }
    }
}

package com.scanflow.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.di.AppContainer
import com.scanflow.app.domain.model.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ViewerUiState(
    val document: Document? = null,
    val totalPages: Int = 0,
    val currentPageIndex: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ViewerViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState = _uiState.asStateFlow()

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Step 1: Resolve the Document — try Room DB first, then fall back to file path
            val doc = withContext(Dispatchers.IO) {
                // Try database lookup first (normal flow from DocumentsScreen)
                val dbDoc = container.documentRepository.getDocumentById(documentId)
                if (dbDoc != null) return@withContext dbDoc

                // If not found in DB, check if documentId is actually a file path
                val fileFromPath = File(documentId)
                if (fileFromPath.exists()) {
                    // Create a transient Document from the file path
                    val ext = fileFromPath.extension.lowercase()
                    val mime = when (ext) {
                        "pdf" -> "application/pdf"
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        "bmp" -> "image/bmp"
                        "txt" -> "text/plain"
                        "csv" -> "text/csv"
                        else -> "application/octet-stream"
                    }
                    Document(
                        id = documentId,
                        name = fileFromPath.name,
                        uri = fileFromPath.toURI().toString(),
                        path = fileFromPath.absolutePath,
                        sizeBytes = fileFromPath.length(),
                        pageCount = if (ext == "pdf") try { container.pdfEngine.getPageCount(fileFromPath) } catch (_: Exception) { 1 } else 1,
                        mimeType = mime
                    )
                } else {
                    null
                }
            }

            if (doc == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Document not found in database."
                )
                return@launch
            }

            val file = File(doc.path)
            if (!file.exists()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "File does not exist: ${file.name}"
                )
                return@launch
            }

            val ext = file.extension.lowercase()

            // Step 2: Render based on file type
            when {
                // PDF files — use PdfRenderer
                ext == "pdf" || doc.mimeType.contains("pdf") -> {
                    val renderResult = withContext(Dispatchers.IO) {
                        if (container.pdfRendererEngine.open(file)) {
                            val count = container.pdfRendererEngine.getPageCount()
                            val firstPage = container.pdfRendererEngine.renderPage(0, 1600, 2260)
                            Pair(count, firstPage)
                        } else {
                            null
                        }
                    }
                    if (renderResult != null) {
                        _uiState.value = ViewerUiState(
                            document = doc,
                            totalPages = renderResult.first,
                            currentPageIndex = 0,
                            currentPageBitmap = renderResult.second,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Cannot open PDF file: ${file.name}"
                        )
                    }
                }

                // Image files — decode directly with BitmapFactory
                doc.mimeType.startsWith("image") || ext in listOf("jpg", "jpeg", "png", "webp", "bmp") -> {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        _uiState.value = ViewerUiState(
                            document = doc,
                            totalPages = 1,
                            currentPageIndex = 0,
                            currentPageBitmap = bitmap,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Cannot decode image file: ${file.name}"
                        )
                    }
                }

                // Text and CSV files — render text content onto a Bitmap
                ext in listOf("txt", "csv") || doc.mimeType.startsWith("text") -> {
                    val textBitmap = withContext(Dispatchers.IO) {
                        try {
                            val text = file.readText(Charsets.UTF_8)
                            renderTextToBitmap(text, file.name)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (textBitmap != null) {
                        _uiState.value = ViewerUiState(
                            document = doc,
                            totalPages = 1,
                            currentPageIndex = 0,
                            currentPageBitmap = textBitmap,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Cannot read text file: ${file.name}"
                        )
                    }
                }

                // Unsupported format fallback
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unsupported file format: .${ext}"
                    )
                }
            }
        }
    }

    /**
     * Renders plain text content onto a Bitmap so it can be displayed
     * in the same viewer canvas used for PDF pages.
     */
    private fun renderTextToBitmap(text: String, fileName: String): Bitmap {
        val width = 1080
        val lineHeight = 42f
        val padding = 60f
        val headerHeight = 100f

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        }

        val headerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 28f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }

        // Word-wrap the text to fit the bitmap width
        val maxTextWidth = width - (padding * 2)
        val wrappedLines = mutableListOf<String>()
        text.lines().forEach { line ->
            if (line.isBlank()) {
                wrappedLines.add("")
            } else {
                var remaining = line
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, maxTextWidth, null)
                    if (count <= 0) break
                    wrappedLines.add(remaining.substring(0, count))
                    remaining = remaining.substring(count).trimStart()
                }
            }
        }

        val contentHeight = headerHeight + (wrappedLines.size * lineHeight) + (padding * 2)
        val height = maxOf(1500, contentHeight.toInt() + 100)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.parseColor("#FAFBFC"))

        // Draw header
        canvas.drawText("📄 $fileName", padding, padding + 30f, headerPaint)

        // Draw separator line
        val sepPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(padding, headerHeight, width - padding, headerHeight, sepPaint)

        // Draw text lines
        var y = headerHeight + lineHeight
        for (line in wrappedLines) {
            canvas.drawText(line, padding, y, paint)
            y += lineHeight
        }

        return bitmap
    }

    fun goToPage(pageIndex: Int) {
        val state = _uiState.value
        val doc = state.document ?: return
        if (pageIndex in 0 until state.totalPages) {
            viewModelScope.launch {
                val file = File(doc.path)
                val bitmap = withContext(Dispatchers.IO) {
                    if (container.pdfRendererEngine.open(file)) {
                        container.pdfRendererEngine.renderPage(pageIndex, 1600, 2260)
                    } else {
                        null
                    }
                }
                if (bitmap != null) {
                    _uiState.value = _uiState.value.copy(
                        currentPageIndex = pageIndex,
                        currentPageBitmap = bitmap
                    )
                }
            }
        }
    }

    fun nextPage() {
        val next = _uiState.value.currentPageIndex + 1
        if (next < _uiState.value.totalPages) goToPage(next)
    }

    fun prevPage() {
        val prev = _uiState.value.currentPageIndex - 1
        if (prev >= 0) goToPage(prev)
    }

    suspend fun getCurrentPageText(): String = withContext(Dispatchers.IO) {
        getPageText(_uiState.value.currentPageIndex)
    }

    suspend fun searchInDocument(query: String): List<Int> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val doc = _uiState.value.document ?: return@withContext emptyList()
        val file = File(doc.path)
        if (!file.exists()) return@withContext emptyList()
        val matches = mutableListOf<Int>()
        try {
            // Step 1: Fast search on digital PDF text stream using PDFBox
            try {
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(file).use { pdfDoc ->
                    val totalPages = pdfDoc.numberOfPages
                    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                    for (p in 0 until totalPages) {
                        stripper.startPage = p + 1
                        stripper.endPage = p + 1
                        val text = stripper.getText(pdfDoc)
                        if (text.contains(query, ignoreCase = true)) {
                            matches.add(p)
                        }
                    }
                }
            } catch (_: Exception) {}

            if (matches.isNotEmpty()) {
                return@withContext matches
            }

            // Step 2: Fallback to ML Kit OCR for scanned documents
            val ocr = container.ocrEngine.recognizePdf(file)
            ocr.pageResults
                .filter { it.fullText.contains(query, ignoreCase = true) }
                .map { it.pageNumber - 1 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toggleBookmark() {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            val newFav = !doc.isFavorite
            container.documentRepository.setFavorite(doc.id, newFav)
            _uiState.value = _uiState.value.copy(
                document = doc.copy(isFavorite = newFav)
            )
        }
    }

    suspend fun getPageText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        val state = _uiState.value
        val doc = state.document ?: return@withContext ""
        val file = File(doc.path)
        if (!file.exists()) return@withContext ""
        try {
            // First attempt to extract digital text using PDFTextStripper
            var extracted = ""
            try {
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(file).use { pdfDoc ->
                    if (pageIndex < pdfDoc.numberOfPages) {
                        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper().apply {
                            startPage = pageIndex + 1
                            endPage = pageIndex + 1
                        }
                        extracted = stripper.getText(pdfDoc).trim()
                    }
                }
            } catch (_: Exception) {}

            if (extracted.isNotBlank()) {
                return@withContext extracted
            }

            // Fallback: If scanned PDF (no digital text stream), use ML Kit OCR on rendered page
            val bitmap = if (pageIndex == state.currentPageIndex && state.currentPageBitmap != null) {
                state.currentPageBitmap
            } else {
                if (container.pdfRendererEngine.open(file)) {
                    container.pdfRendererEngine.renderPage(pageIndex, 1600, 2260)
                } else null
            }
            if (bitmap != null) {
                container.ocrEngine.recognizeImage(bitmap).fullText.trim()
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun saveAnnotationToPdf(pageIndex: Int, strokes: List<List<androidx.compose.ui.geometry.Offset>>, canvasWidth: Float, canvasHeight: Float): Boolean = withContext(Dispatchers.IO) {
        val doc = _uiState.value.document ?: return@withContext false
        val file = File(doc.path)
        if (!file.exists() || strokes.isEmpty()) return@withContext false
        var overlay: Bitmap? = null
        try {
            val w = maxOf(100, canvasWidth.toInt())
            val h = maxOf(100, canvasHeight.toInt())
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            overlay = bmp
            val canvas = android.graphics.Canvas(bmp)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(120, 253, 224, 71)
                strokeWidth = 26f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            strokes.forEach { stroke ->
                if (stroke.size == 1) {
                    val fillPaint = android.graphics.Paint(paint).apply { style = android.graphics.Paint.Style.FILL }
                    canvas.drawCircle(stroke[0].x, stroke[0].y, 13f, fillPaint)
                } else {
                    for (i in 0 until stroke.size - 1) {
                        canvas.drawLine(stroke[i].x, stroke[i].y, stroke[i + 1].x, stroke[i + 1].y, paint)
                    }
                }
            }

            var mediaWidth = 595f
            var mediaHeight = 842f
            try {
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(file).use { pdfDoc ->
                    if (pageIndex in 0 until pdfDoc.numberOfPages) {
                        val box = pdfDoc.getPage(pageIndex).mediaBox
                        mediaWidth = box.width
                        mediaHeight = box.height
                    }
                }
            } catch (_: Exception) {}

            val tempOutput = container.storageEngine.createTempFile("annotated", "pdf")
            val result = container.securityEngine.stampSignature(
                inputFile = file,
                signatureBitmap = bmp,
                pageIndex = pageIndex,
                rect = android.graphics.RectF(0f, 0f, mediaWidth, mediaHeight),
                outputFile = tempOutput
            )
            if (result.success && tempOutput.exists()) {
                tempOutput.copyTo(file, overwrite = true)
                tempOutput.delete()
                goToPage(pageIndex)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            SafeLogger.e("ViewerViewModel", ErrorCode.STORAGE_ERROR, e)
            false
        } finally {
            if (overlay != null && !overlay.isRecycled) {
                overlay.recycle()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        container.pdfRendererEngine.close()
    }
}

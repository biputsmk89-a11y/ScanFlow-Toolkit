package com.scanflow.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.di.AppContainer
import com.scanflow.app.domain.model.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
            val doc = container.documentRepository.getDocumentById(documentId)
            if (doc != null) {
                val file = File(doc.path)
                if (file.exists() && container.pdfRendererEngine.open(file)) {
                    val count = container.pdfRendererEngine.getPageCount()
                    val firstPage = container.pdfRendererEngine.renderPage(0, 1080, 1500)
                    _uiState.value = ViewerUiState(
                        document = doc,
                        totalPages = count,
                        currentPageIndex = 0,
                        currentPageBitmap = firstPage,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Cannot open document file: ${file.name}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Document not found in database."
                )
            }
        }
    }

    fun goToPage(pageIndex: Int) {
        val state = _uiState.value
        val doc = state.document ?: return
        if (pageIndex in 0 until state.totalPages) {
            viewModelScope.launch {
                val file = File(doc.path)
                if (container.pdfRendererEngine.open(file)) {
                    val bitmap = container.pdfRendererEngine.renderPage(pageIndex, 1080, 1500)
                    _uiState.value = state.copy(
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

    override fun onCleared() {
        super.onCleared()
        container.pdfRendererEngine.close()
    }
}

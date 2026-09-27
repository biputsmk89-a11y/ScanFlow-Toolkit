package com.scanflow.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.di.AppContainer
import com.scanflow.app.engine.ScanFilterType
import com.scanflow.app.engine.ScanSession
import com.scanflow.app.engine.ScannedPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ScannerUiState(
    val session: ScanSession = ScanSession(id = UUID.randomUUID().toString()),
    val currentFilter: ScanFilterType = ScanFilterType.AUTO_ENHANCE,
    val isAutoCaptureEnabled: Boolean = false,
    val isProcessingPage: Boolean = false,
    val status: OperationStatus = OperationStatus.IDLE,
    val compilationResult: OperationResult? = null,
    val errorMessage: String? = null
)

class ScannerViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()

    fun setFilter(filter: ScanFilterType) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
    }

    fun toggleAutoCapture() {
        val current = _uiState.value.isAutoCaptureEnabled
        _uiState.value = _uiState.value.copy(isAutoCaptureEnabled = !current)
    }

    fun onBitmapCaptured(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(isProcessingPage = true)
        viewModelScope.launch {
            try {
                // 1. Detect corners
                val corners = container.scannerUseCases.detectCorners(bitmap)

                // 2. Process page with selected filter
                val processedBitmap = container.scannerUseCases.processPage(
                    bitmap,
                    corners,
                    _uiState.value.currentFilter
                )

                // 3. Save to temp files
                val origFile = container.storageEngine.createTempFile("scan_orig", "jpg")
                val enhFile = container.storageEngine.createTempFile("scan_enh", "jpg")

                container.imageProcessingEngine.compressImage(bitmap, origFile, 90)
                container.imageProcessingEngine.compressImage(processedBitmap, enhFile, 90)

                val newPage = ScannedPage(
                    id = UUID.randomUUID().toString(),
                    originalImagePath = origFile.absolutePath,
                    enhancedImagePath = enhFile.absolutePath,
                    corners = corners ?: emptyList(),
                    filterType = _uiState.value.currentFilter
                )

                val currentSession = _uiState.value.session
                currentSession.pages.add(newPage)

                _uiState.value = _uiState.value.copy(
                    session = currentSession.copy(pages = ArrayList(currentSession.pages)),
                    isProcessingPage = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingPage = false,
                    errorMessage = e.message ?: "Failed to process captured page"
                )
            }
        }
    }

    fun deletePage(pageIndex: Int) {
        val currentSession = _uiState.value.session
        if (pageIndex in currentSession.pages.indices) {
            val page = currentSession.pages.removeAt(pageIndex)
            File(page.originalImagePath).delete()
            File(page.enhancedImagePath).delete()
            _uiState.value = _uiState.value.copy(
                session = currentSession.copy(pages = ArrayList(currentSession.pages))
            )
        }
    }

    fun compileDocument(documentName: String) {
        val session = _uiState.value.session
        if (session.pages.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please capture at least one page.")
            return
        }

        _uiState.value = _uiState.value.copy(status = OperationStatus.PROCESSING)
        viewModelScope.launch {
            val result = container.scannerUseCases.compileSession(session, documentName)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    status = OperationStatus.SUCCESS,
                    compilationResult = result
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    status = OperationStatus.FAILED,
                    errorMessage = result.errorMessage ?: "Failed to compile scanned PDF"
                )
            }
        }
    }
}

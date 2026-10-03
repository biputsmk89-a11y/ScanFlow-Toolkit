package com.scanflow.app.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.PointF
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
    val isDocumentDetected: Boolean = false,
    val detectionLabel: String = "ALIGN DOCUMENT IN FRAME",
    val isProcessingPage: Boolean = false,
    val status: OperationStatus = OperationStatus.IDLE,
    val compilationResult: OperationResult? = null,
    val errorMessage: String? = null,

    // Interactive Crop & Review State
    val isReviewingCrop: Boolean = false,
    val pendingOriginalBitmap: Bitmap? = null,
    val pendingCorners: List<PointF> = emptyList(),
    val reviewFilter: ScanFilterType = ScanFilterType.AUTO_ENHANCE
)

class ScannerViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()

    fun setFilter(filter: ScanFilterType) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
    }

    fun setReviewFilter(filter: ScanFilterType) {
        _uiState.value = _uiState.value.copy(reviewFilter = filter)
    }

    fun toggleAutoCapture() {
        val current = _uiState.value.isAutoCaptureEnabled
        _uiState.value = _uiState.value.copy(isAutoCaptureEnabled = !current)
    }

    fun onDocumentDetectionUpdated(detected: Boolean) {
        if (_uiState.value.isDocumentDetected != detected) {
            _uiState.value = _uiState.value.copy(
                isDocumentDetected = detected,
                detectionLabel = if (detected) "DOCUMENT DETECTED • READY" else "ALIGN DOCUMENT IN FRAME"
            )
        }
    }

    fun onBitmapCaptured(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(isProcessingPage = true)
        viewModelScope.launch {
            try {
                // 1. Detect corners with OpenCV
                val detected = container.scannerUseCases.detectCorners(bitmap)
                val corners = if (!detected.isNullOrEmpty() && detected.size == 4) {
                    detected
                } else {
                    val w = bitmap.width.toFloat()
                    val h = bitmap.height.toFloat()
                    listOf(
                        PointF(w * 0.04f, h * 0.04f),
                        PointF(w * 0.96f, h * 0.04f),
                        PointF(w * 0.96f, h * 0.96f),
                        PointF(w * 0.04f, h * 0.96f)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isProcessingPage = false,
                    isReviewingCrop = true,
                    pendingOriginalBitmap = bitmap,
                    pendingCorners = corners,
                    reviewFilter = _uiState.value.currentFilter
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingPage = false,
                    errorMessage = e.message ?: "Failed to process captured image"
                )
            }
        }
    }

    fun updatePendingCorners(corners: List<PointF>) {
        if (corners.size == 4) {
            _uiState.value = _uiState.value.copy(pendingCorners = corners)
        }
    }

    fun resetPendingCornersToFull() {
        val bitmap = _uiState.value.pendingOriginalBitmap ?: return
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val fullCorners = listOf(
            PointF(0f, 0f),
            PointF(w, 0f),
            PointF(w, h),
            PointF(0f, h)
        )
        _uiState.value = _uiState.value.copy(pendingCorners = fullCorners)
    }

    fun reDetectCorners() {
        val bitmap = _uiState.value.pendingOriginalBitmap ?: return
        viewModelScope.launch {
            val detected = container.scannerUseCases.detectCorners(bitmap)
            if (!detected.isNullOrEmpty() && detected.size == 4) {
                _uiState.value = _uiState.value.copy(pendingCorners = detected)
            }
        }
    }

    fun rotatePendingBitmap() {
        val bitmap = _uiState.value.pendingOriginalBitmap ?: return
        viewModelScope.launch {
            val rotated = container.imageProcessingEngine.rotate(bitmap, 90f)
            val detected = container.scannerUseCases.detectCorners(rotated)
            val corners = if (!detected.isNullOrEmpty() && detected.size == 4) {
                detected
            } else {
                val w = rotated.width.toFloat()
                val h = rotated.height.toFloat()
                listOf(
                    PointF(w * 0.04f, h * 0.04f),
                    PointF(w * 0.96f, h * 0.04f),
                    PointF(w * 0.96f, h * 0.96f),
                    PointF(w * 0.04f, h * 0.96f)
                )
            }
            if (bitmap != rotated && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            _uiState.value = _uiState.value.copy(
                pendingOriginalBitmap = rotated,
                pendingCorners = corners
            )
        }
    }

    fun discardPendingPage() {
        val bitmap = _uiState.value.pendingOriginalBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        _uiState.value = _uiState.value.copy(
            isReviewingCrop = false,
            pendingOriginalBitmap = null,
            pendingCorners = emptyList()
        )
    }

    fun confirmPendingPage() {
        val bitmap = _uiState.value.pendingOriginalBitmap ?: return
        val corners = _uiState.value.pendingCorners
        val filter = _uiState.value.reviewFilter

        _uiState.value = _uiState.value.copy(isProcessingPage = true)
        viewModelScope.launch {
            var processedBitmap: Bitmap? = null
            try {
                // 1. Process page with confirmed corners & chosen filter
                processedBitmap = container.scannerUseCases.processPage(
                    bitmap,
                    if (corners.size == 4) corners else null,
                    filter
                )

                // 2. Save high-fidelity uncompressed temp files (95% quality)
                val origFile = container.storageEngine.createTempFile("scan_orig", "jpg")
                val enhFile = container.storageEngine.createTempFile("scan_enh", "jpg")

                container.imageProcessingEngine.compressImage(bitmap, origFile, 95)
                container.imageProcessingEngine.compressImage(processedBitmap, enhFile, 95)

                val newPage = ScannedPage(
                    id = UUID.randomUUID().toString(),
                    originalImagePath = origFile.absolutePath,
                    enhancedImagePath = enhFile.absolutePath,
                    corners = corners,
                    filterType = filter
                )

                val currentSession = _uiState.value.session
                currentSession.pages.add(newPage)

                _uiState.value = _uiState.value.copy(
                    session = currentSession.copy(pages = ArrayList(currentSession.pages)),
                    isProcessingPage = false,
                    isReviewingCrop = false,
                    pendingOriginalBitmap = null,
                    pendingCorners = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingPage = false,
                    errorMessage = e.message ?: "Failed to process cropped page"
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
                if (processedBitmap != null && processedBitmap != bitmap && !processedBitmap.isRecycled) {
                    processedBitmap.recycle()
                }
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

    override fun onCleared() {
        super.onCleared()
        val bitmap = _uiState.value.pendingOriginalBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

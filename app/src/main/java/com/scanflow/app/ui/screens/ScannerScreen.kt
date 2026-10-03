package com.scanflow.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scanflow.app.ScanFlowApplication
import com.scanflow.app.core.theme.MetricMono
import com.scanflow.app.core.theme.MetricMonoSmall
import com.scanflow.app.core.theme.StitchPrimary
import com.scanflow.app.core.theme.StitchPrimaryFixed
import com.scanflow.app.engine.ScanFilterType
import com.scanflow.app.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onDocumentCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var documentName by remember { mutableStateOf("ScanFlow_${System.currentTimeMillis() % 100000}") }

    // Tap-to-focus animation states
    var tapFocusOffset by remember { mutableStateOf<Offset?>(null) }
    val focusScale = remember { Animatable(1.5f) }
    val focusAlpha = remember { Animatable(1f) }

    // Auto-capture timer
    var autoCaptureStableCount by remember { mutableIntStateOf(0) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Auto-capture countdown logic
    LaunchedEffect(uiState.isDocumentDetected, uiState.isAutoCaptureEnabled, uiState.isReviewingCrop) {
        if (uiState.isAutoCaptureEnabled && uiState.isDocumentDetected && !uiState.isReviewingCrop && !uiState.isProcessingPage) {
            delay(1200) // Steady hold for 1.2s
            val capture = imageCapture
            if (capture != null && uiState.isDocumentDetected && !uiState.isReviewingCrop) {
                triggerCapture(context, capture, viewModel)
            }
        }
    }

    // Gallery Picker fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    viewModel.onBitmapCaptured(bitmap)
                }
            }
        }
    }

    LaunchedEffect(uiState.compilationResult) {
        val result = uiState.compilationResult
        if (result != null && result.success && result.outputPath != null) {
            onDocumentCreated(result.outputPath)
        }
    }

    if (!hasCameraPermission) {
        // Permission Request View
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ScanFlow needs camera permission to capture documents with real-time autofocus, edge detection, and high-definition perspective unwarping.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Gallery instead")
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

            // Camera Viewfinder & Controls (Active when not reviewing crop)
            if (!uiState.isReviewingCrop) {
                // High-Resolution CameraX Preview with Tap-to-Focus
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Set 4:3 high-definition resolution selector for maximum clarity
                            val resolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                                .build()

                            val preview = Preview.Builder()
                                .setResolutionSelector(resolutionSelector)
                                .build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .setResolutionSelector(resolutionSelector)
                                .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                .build()

                            imageCapture = capture

                            // Real-time edge detection analyzer
                            val analysisResolution = ResolutionSelector.Builder()
                                .setResolutionStrategy(ResolutionStrategy(android.util.Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
                                .build()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .setResolutionSelector(analysisResolution)
                                .build()

                            val scannerEngine = ScanFlowApplication.container.scannerEngine
                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                try {
                                    val planes = imageProxy.planes
                                    if (planes.isNotEmpty()) {
                                        val buffer = planes[0].buffer
                                        val w = imageProxy.width
                                        val h = imageProxy.height
                                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                        buffer.rewind()
                                        bmp.copyPixelsFromBuffer(buffer)
                                        val corners = scannerEngine.detectDocumentCorners(bmp)
                                        val isDetected = (corners != null && corners.size == 4)
                                        bmp.recycle()
                                        viewModel.onDocumentDetectionUpdated(isDetected)
                                    }
                                } catch (_: Throwable) {
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture,
                                    imageAnalysis
                                )
                                activeCamera = cam
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        // Tap-to-Focus Touch Listener
                        previewView.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                val x = event.x
                                val y = event.y
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(x, y)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                activeCamera?.cameraControl?.startFocusAndMetering(action)

                                tapFocusOffset = Offset(x, y)
                                scope.launch {
                                    focusScale.snapTo(1.5f)
                                    focusAlpha.snapTo(1f)
                                    focusScale.animateTo(1.0f, tween(300))
                                    delay(800)
                                    focusAlpha.animateTo(0f, tween(300))
                                    tapFocusOffset = null
                                }
                            }
                            true
                        }

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Visual Tap-to-Focus Ring
                tapFocusOffset?.let { offset ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((offset.x - 36).toInt(), (offset.y - 36).toInt()) }
                            .size(72.dp)
                            .border(
                                2.dp,
                                StitchPrimary.copy(alpha = focusAlpha.value),
                                RoundedCornerShape(8.dp)
                            )
                    )
                }

                // Viewfinder Reticle Overlay with Active Corner Brackets
                val bracketColor = if (uiState.isDocumentDetected) Color(0xFF22C55E) else StitchPrimary.copy(alpha = 0.85f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, bracketColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        // Top-Left Corner
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.TopStart)
                                .border(4.dp, bracketColor, RoundedCornerShape(topStart = 16.dp))
                        )
                        // Top-Right Corner
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.TopEnd)
                                .border(4.dp, bracketColor, RoundedCornerShape(topEnd = 16.dp))
                        )
                        // Bottom-Left Corner
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomStart)
                                .border(4.dp, bracketColor, RoundedCornerShape(bottomStart = 16.dp))
                        )
                        // Bottom-Right Corner
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .border(4.dp, bracketColor, RoundedCornerShape(bottomEnd = 16.dp))
                        )
                    }

                    // Dynamic Document Status Banner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, bracketColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isDocumentDetected) Color(0xFF22C55E) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.detectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top Toolbar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Flash Button
                        IconButton(
                            onClick = {
                                isFlashOn = !isFlashOn
                                imageCapture?.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) StitchPrimaryFixed else Color.White
                            )
                        }

                        // Auto-Capture Toggle
                        IconButton(
                            onClick = { viewModel.toggleAutoCapture() },
                            modifier = Modifier.background(
                                if (uiState.isAutoCaptureEnabled) StitchPrimary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Auto Capture",
                                tint = if (uiState.isAutoCaptureEnabled) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Save PDF Button
                    if (uiState.session.pages.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = StitchPrimary,
                            modifier = Modifier.clickable { showNameDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save (${uiState.session.pages.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(top = 10.dp, bottom = 28.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Captured Pages Filmstrip
                    if (uiState.session.pages.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.session.pages.forEachIndexed { index, page ->
                                val thumbBitmap = remember(page.enhancedImagePath) {
                                    val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                                    BitmapFactory.decodeFile(page.enhancedImagePath, opts)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(54.dp, 72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.5.dp, StitchPrimary, RoundedCornerShape(8.dp))
                                ) {
                                    if (thumbBitmap != null) {
                                        Image(
                                            bitmap = thumbBitmap.asImageBitmap(),
                                            contentDescription = "Page ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .background(StitchPrimary, RoundedCornerShape(bottomEnd = 6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MetricMonoSmall,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                            .clickable { viewModel.deletePage(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove page",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filter Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ScanFilterType.values().forEach { filter ->
                            val isSelected = uiState.currentFilter == filter
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) StitchPrimary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { viewModel.setFilter(filter) }
                            ) {
                                Text(
                                    text = filter.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shutter Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gallery Button
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Shutter Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .border(4.dp, StitchPrimary, CircleShape)
                                .padding(6.dp)
                                .background(Color.White, CircleShape)
                                .clickable(enabled = !uiState.isProcessingPage) {
                                    val capture = imageCapture ?: return@clickable
                                    triggerCapture(context, capture, viewModel)
                                }
                        ) {
                            if (uiState.isProcessingPage) {
                                CircularProgressIndicator(
                                    color = StitchPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Page Counter
                        Surface(
                            shape = CircleShape,
                            color = StitchPrimary.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StitchPrimary),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${uiState.session.pages.size}",
                                    style = MetricMono,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Interactive Document Crop & Boundary Adjustment Review Screen
                DocumentCropReviewScreen(
                    bitmap = uiState.pendingOriginalBitmap!!,
                    corners = uiState.pendingCorners,
                    selectedFilter = uiState.reviewFilter,
                    isProcessing = uiState.isProcessingPage,
                    onUpdateCorners = { viewModel.updatePendingCorners(it) },
                    onResetToFull = { viewModel.resetPendingCornersToFull() },
                    onAutoDetect = { viewModel.reDetectCorners() },
                    onRotate = { viewModel.rotatePendingBitmap() },
                    onFilterSelected = { viewModel.setReviewFilter(it) },
                    onConfirm = { viewModel.confirmPendingPage() },
                    onDiscard = { viewModel.discardPendingPage() }
                )
            }

            // Save PDF File Name Dialog
            if (showNameDialog) {
                AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    title = {
                        Text("Save Scanned PDF", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                "All ${uiState.session.pages.size} pages will be compiled in high-definition (300 DPI) into a single PDF document.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = documentName,
                                onValueChange = { documentName = it },
                                label = { Text("Document File Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showNameDialog = false
                                viewModel.compileDocument(documentName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Compile & Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNameDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Triggers CameraX image capture with correct rotation handling.
 */
private fun triggerCapture(
    context: android.content.Context,
    imageCapture: ImageCapture,
    viewModel: ScannerViewModel
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotationDegrees = image.imageInfo.rotationDegrees
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                image.close()
                if (bitmap != null) {
                    val orientedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        bitmap.recycle()
                        rotated
                    } else {
                        bitmap
                    }
                    viewModel.onBitmapCaptured(orientedBitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

/**
 * CamScanner-style interactive Document Crop / Boundary Review Screen.
 * Allows user to drag 4 corner handles to adjust the crop quadrilateral,
 * rotate, auto-detect, and select filters before saving!
 */
@Composable
private fun DocumentCropReviewScreen(
    bitmap: Bitmap,
    corners: List<PointF>,
    selectedFilter: ScanFilterType,
    isProcessing: Boolean,
    onUpdateCorners: (List<PointF>) -> Unit,
    onResetToFull: () -> Unit,
    onAutoDetect: () -> Unit,
    onRotate: () -> Unit,
    onFilterSelected: (ScanFilterType) -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    var activeDraggingCornerIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Control Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onDiscard,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Discard",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Adjust Document Boundary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onRotate,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onAutoDetect,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Auto-Detect Quad",
                            tint = StitchPrimaryFixed
                        )
                    }
                }
            }

            // Viewport Canvas with Interactive 4-Corner Mesh
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val viewportWidth = constraints.maxWidth.toFloat()
                val viewportHeight = constraints.maxHeight.toFloat()

                val bmpW = bitmap.width.toFloat()
                val bmpH = bitmap.height.toFloat()

                val scale = min(viewportWidth / bmpW, viewportHeight / bmpH)
                val displayedWidth = bmpW * scale
                val displayedHeight = bmpH * scale
                val offsetX = (viewportWidth - displayedWidth) / 2f
                val offsetY = (viewportHeight - displayedHeight) / 2f

                // Map 4 corner points from bitmap coordinates to screen viewport coordinates
                val currentScreenCorners = remember(corners, scale, offsetX, offsetY) {
                    if (corners.size == 4) {
                        corners.map { pt ->
                            Offset(offsetX + pt.x * scale, offsetY + pt.y * scale)
                        }
                    } else {
                        listOf(
                            Offset(offsetX, offsetY),
                            Offset(offsetX + displayedWidth, offsetY),
                            Offset(offsetX + displayedWidth, offsetY + displayedHeight),
                            Offset(offsetX, offsetY + displayedHeight)
                        )
                    }
                }

                // Render image inside viewport
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Scan Page",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Crop Polygon & Drag Handles Overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentScreenCorners, scale) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    // Find closest corner handle within touch radius (48dp)
                                    val touchThreshold = 48.dp.toPx()
                                    var closestIndex = -1
                                    var closestDist = Float.MAX_VALUE
                                    currentScreenCorners.forEachIndexed { index, cornerOffset ->
                                        val dist = hypot(startOffset.x - cornerOffset.x, startOffset.y - cornerOffset.y)
                                        if (dist < closestDist && dist < touchThreshold) {
                                            closestDist = dist
                                            closestIndex = index
                                        }
                                    }
                                    activeDraggingCornerIndex = closestIndex
                                },
                                onDrag = { change, dragAmount ->
                                    if (activeDraggingCornerIndex in 0..3) {
                                        change.consume()
                                        val targetPt = currentScreenCorners[activeDraggingCornerIndex] + dragAmount
                                        // Convert back to bitmap coordinates
                                        val newBmpX = ((targetPt.x - offsetX) / scale).coerceIn(0f, bmpW)
                                        val newBmpY = ((targetPt.y - offsetY) / scale).coerceIn(0f, bmpH)

                                        val updatedBmpCorners = corners.toMutableList()
                                        if (updatedBmpCorners.size == 4) {
                                            updatedBmpCorners[activeDraggingCornerIndex] = PointF(newBmpX, newBmpY)
                                            onUpdateCorners(updatedBmpCorners)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    activeDraggingCornerIndex = -1
                                },
                                onDragCancel = {
                                    activeDraggingCornerIndex = -1
                                }
                            )
                        }
                ) {
                    if (currentScreenCorners.size == 4) {
                        val path = Path().apply {
                            moveTo(currentScreenCorners[0].x, currentScreenCorners[0].y)
                            lineTo(currentScreenCorners[1].x, currentScreenCorners[1].y)
                            lineTo(currentScreenCorners[2].x, currentScreenCorners[2].y)
                            lineTo(currentScreenCorners[3].x, currentScreenCorners[3].y)
                            close()
                        }

                        // Draw boundary crop polygon
                        drawPath(
                            path = path,
                            color = StitchPrimary,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw corner pins
                        currentScreenCorners.forEachIndexed { index, cornerOffset ->
                            val isBeingDragged = index == activeDraggingCornerIndex
                            val handleRadius = if (isBeingDragged) 18.dp.toPx() else 14.dp.toPx()

                            // Outer shadow/white ring
                            drawCircle(
                                color = Color.White,
                                radius = handleRadius,
                                center = cornerOffset
                            )
                            // Inner primary indicator
                            drawCircle(
                                color = if (isBeingDragged) Color(0xFF22C55E) else StitchPrimary,
                                radius = handleRadius - 3.dp.toPx(),
                                center = cornerOffset
                            )
                        }
                    }
                }
            }

            // Bottom Control Strip (Filter selector + Confirm button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Full Image / Reset Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onResetToFull) {
                        Icon(Icons.Default.CropFree, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Full Photo", color = StitchPrimary, style = MaterialTheme.typography.labelMedium)
                    }

                    Text(
                        text = "Drag 4 pins to align edges",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Document Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ScanFilterType.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) StitchPrimary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { onFilterSelected(filter) }
                        ) {
                            Text(
                                text = filter.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Page Button
                Button(
                    onClick = onConfirm,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unwarping & Enhancing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Done • Keep Page", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

package com.scanflow.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.scanflow.app.ScanFlowApplication
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.core.theme.MetricMono
import com.scanflow.app.core.theme.MetricMonoSmall
import com.scanflow.app.ui.viewmodel.ToolActionUiState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.scanflow.app.domain.model.CompressionLevel
import com.scanflow.app.ui.viewmodel.ToolActionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolActionScreen(
    featureId: String,
    viewModel: ToolActionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val storageEngine = remember { ScanFlowApplication.container.storageEngine }

    LaunchedEffect(featureId) {
        viewModel.initFeature(featureId)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val copiedFiles = mutableListOf<File>()
                val contentResolver = context.contentResolver
                uris.forEachIndexed { i, uri ->
                    var ext = when (uiState.feature?.id) {
                        "SF-099" -> "txt"
                        "SF-100" -> "csv"
                        "SF-101", "SF-094", "SF-095", "SF-059", "SF-060" -> "jpg"
                        "SF-105" -> "html"
                        else -> "pdf"
                    }
                    try {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0 && cursor.moveToFirst()) {
                                val name = cursor.getString(nameIndex)
                                val dot = name.lastIndexOf('.')
                                if (dot >= 0) ext = name.substring(dot + 1)
                            }
                        }
                    } catch (_: Exception) {}
                    val temp = storageEngine.createTempFile("pick_${i}", ext)
                    if (storageEngine.copyUriToTempFile(uri, temp)) {
                        copiedFiles.add(temp)
                    }
                }
                viewModel.onFilesSelected(copiedFiles)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = uiState.feature?.name ?: "Tool Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ScanFlow Offline Station",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AIR-GAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Branching: If PROCESSING, show Stitch Processing Status screen
        // If SUCCESS, show Stitch Success & Output Preview screen
        // Else, show configuration form
        when (uiState.status) {
            OperationStatus.PROCESSING -> {
                ProcessingStatusView(
                    featureName = uiState.feature?.name ?: "Document Processing",
                    firstFileName = uiState.selectedFiles.firstOrNull()?.name ?: "document.pdf",
                    fileSize = uiState.selectedFiles.firstOrNull()?.length() ?: 0L,
                    progressPercent = uiState.progressPercent,
                    onCancel = { viewModel.cancelOperation() }
                )
            }

            OperationStatus.SUCCESS -> {
                SuccessPreviewView(
                    result = uiState.result,
                    onOpenDocument = {
                        uiState.result?.outputPath?.let { path ->
                            onNavigateToViewer(path)
                        }
                    },
                    onShareDocument = {
                        uiState.result?.outputPath?.let { path ->
                            val file = File(path)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Document"))
                        }
                    },
                    onDone = onNavigateBack
                )
            }

            else -> {
                // Configuration View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Privacy Assurance Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zero Cloud Exposure",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% processed on-device. Files never leave your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // File Picker / Selection Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val mime = when (uiState.feature?.id) {
                                    "SF-099" -> "text/plain"
                                    "SF-100" -> "*/*"
                                    "SF-101", "SF-094", "SF-095", "SF-059", "SF-060" -> "image/*"
                                    "SF-105" -> "text/html"
                                    else -> "application/pdf"
                                }
                                filePickerLauncher.launch(mime)
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Upload",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (uiState.selectedFiles.isEmpty()) "Tap to Select Documents" else "${uiState.selectedFiles.size} document(s) chosen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.selectedFiles.isEmpty()) "Supports PDF, TXT, CSV, HTML, JPG, PNG" else uiState.selectedFiles.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Tool Specific Parameter Cards
                    when (uiState.feature?.id) {
                        "SF-017", "SF-018", "SF-019", "SF-020", "SF-021" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Compression Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.compressionLevel == CompressionLevel.LOW,
                                            onClick = { viewModel.onCompressionLevelChanged(CompressionLevel.LOW) },
                                            label = { Text("Low") }
                                        )
                                        FilterChip(
                                            selected = uiState.compressionLevel == CompressionLevel.MEDIUM,
                                            onClick = { viewModel.onCompressionLevelChanged(CompressionLevel.MEDIUM) },
                                            label = { Text("Medium (Recommended)") }
                                        )
                                        FilterChip(
                                            selected = uiState.compressionLevel == CompressionLevel.HIGH,
                                            onClick = { viewModel.onCompressionLevelChanged(CompressionLevel.HIGH) },
                                            label = { Text("High") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-002" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Split Page Ranges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Example: 1-3, 5, 8-10 or 1, 2, 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.splitRanges,
                                        onValueChange = { viewModel.onSplitRangesChanged(it) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-003" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Pages to Remove", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Specify comma-separated page numbers to delete (e.g. 1, 3, 5).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.pageOrder,
                                        onValueChange = { viewModel.onPageOrderChanged(it) },
                                        placeholder = { Text("e.g. 1, 3") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-004" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Pages to Extract", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Specify page numbers to extract into a new standalone PDF (e.g. 1, 2, 4).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.pageOrder,
                                        onValueChange = { viewModel.onPageOrderChanged(it) },
                                        placeholder = { Text("e.g. 1, 2, 4") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-005", "SF-007" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Page Sequence Order", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Specify comma-separated page numbers (e.g. 3, 1, 2) or leave blank to invert sequence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.pageOrder,
                                        onValueChange = { viewModel.onPageOrderChanged(it) },
                                        placeholder = { Text("e.g. 2, 1, 3") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-008", "SF-076" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Rotation Angle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.rotationAngle == 90,
                                            onClick = { viewModel.onRotationAngleChanged(90) },
                                            label = { Text("90° Clockwise") }
                                        )
                                        FilterChip(
                                            selected = uiState.rotationAngle == 180,
                                            onClick = { viewModel.onRotationAngleChanged(180) },
                                            label = { Text("180° Flip") }
                                        )
                                        FilterChip(
                                            selected = uiState.rotationAngle == 270,
                                            onClick = { viewModel.onRotationAngleChanged(270) },
                                            label = { Text("270° Counter") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-075" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Crop Inset Margin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Trims empty margins around content using standard PDF points.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.cropMarginPoints == 18f,
                                            onClick = { viewModel.onCropMarginChanged(18f) },
                                            label = { Text("Light (18 pt)") }
                                        )
                                        FilterChip(
                                            selected = uiState.cropMarginPoints == 36f,
                                            onClick = { viewModel.onCropMarginChanged(36f) },
                                            label = { Text("Medium (36 pt)") }
                                        )
                                        FilterChip(
                                            selected = uiState.cropMarginPoints == 54f,
                                            onClick = { viewModel.onCropMarginChanged(54f) },
                                            label = { Text("Deep (54 pt)") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-077", "SF-078" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Watermark Text Stamp", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.watermarkText,
                                        onValueChange = { viewModel.onWatermarkTextChanged(it) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-079", "SF-080", "SF-081" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (uiState.feature?.id == "SF-081") "Enter Password to Unlock" else "Set Document Password (AES-256)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.userPassword,
                                        onValueChange = { viewModel.onPasswordChanged(it) },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-083" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("True Permanent Redaction Target", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Redacted pages are rasterized & flattened. Text streams and vectors under blackouts are permanently eliminated.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.redactionTarget == "HEADER_FOOTER",
                                            onClick = { viewModel.onRedactionTargetChanged("HEADER_FOOTER") },
                                            label = { Text("Header & Footer") }
                                        )
                                        FilterChip(
                                            selected = uiState.redactionTarget == "ALL_MARGINS",
                                            onClick = { viewModel.onRedactionTargetChanged("ALL_MARGINS") },
                                            label = { Text("All Borders & Bleed") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-084", "SF-085" -> {
                            SignaturePadCard(uiState = uiState, viewModel = viewModel)
                        }

                        "SF-086", "SF-087", "SF-088", "SF-089", "SF-090", "SF-091", "SF-092" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Interactive PDF Form Fields", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (uiState.formFields.isNotEmpty()) {
                                        Text("${uiState.formFields.size} field(s) detected in document:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        uiState.formFields.forEach { field ->
                                            OutlinedTextField(
                                                value = uiState.formFieldValues[field.name] ?: "",
                                                onValueChange = { viewModel.onFormFieldValueChanged(field.name, it) },
                                                label = { Text(field.name) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Text("No AcroForm fields found in this document. Enter text to stamp as annotation:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = uiState.watermarkText,
                                            onValueChange = { viewModel.onWatermarkTextChanged(it) },
                                            label = { Text("Annotation Text") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        "SF-066" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Text Annotation / Stamp", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Enter text or status note to overlay onto the document pages.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.watermarkText,
                                        onValueChange = { viewModel.onWatermarkTextChanged(it) },
                                        placeholder = { Text("e.g. APPROVED, REVIEWED, DRAFT") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "SF-107" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Executive Summary Length", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Select how many key bullet points to extract using on-device NLP:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.aiSummaryBullets == 3,
                                            onClick = { viewModel.onAiSummaryBulletsChanged(3) },
                                            label = { Text("3 Bullets") }
                                        )
                                        FilterChip(
                                            selected = uiState.aiSummaryBullets == 5,
                                            onClick = { viewModel.onAiSummaryBulletsChanged(5) },
                                            label = { Text("5 Bullets") }
                                        )
                                        FilterChip(
                                            selected = uiState.aiSummaryBullets == 8,
                                            onClick = { viewModel.onAiSummaryBulletsChanged(8) },
                                            label = { Text("8 Bullets") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-108", "SF-109" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Ask Document a Question", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Type your question below or tap a quick prompt:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = uiState.aiQuestion,
                                        onValueChange = { viewModel.onAiQuestionChanged(it) },
                                        placeholder = { Text("e.g. What is the total invoice amount?") },
                                        singleLine = false,
                                        maxLines = 3,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Quick Suggestions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        SuggestionChip(
                                            onClick = { viewModel.onAiQuestionChanged("Summarize document key points") },
                                            label = { Text("Key Points") }
                                        )
                                        SuggestionChip(
                                            onClick = { viewModel.onAiQuestionChanged("What are the total amounts and payments?") },
                                            label = { Text("Totals") }
                                        )
                                        SuggestionChip(
                                            onClick = { viewModel.onAiQuestionChanged("Who are the parties and signatories?") },
                                            label = { Text("Parties") }
                                        )
                                        SuggestionChip(
                                            onClick = { viewModel.onAiQuestionChanged("What are the key dates and deadlines?") },
                                            label = { Text("Deadlines") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-110" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Target Translation Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Select language for on-device document translation:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.translationTargetLang == "id",
                                            onClick = { viewModel.onTranslationTargetLangChanged("id") },
                                            label = { Text("Bahasa Indonesia (id)") }
                                        )
                                        FilterChip(
                                            selected = uiState.translationTargetLang == "en",
                                            onClick = { viewModel.onTranslationTargetLangChanged("en") },
                                            label = { Text("English (en)") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-133", "SF-134", "SF-135", "SF-137" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Workflow Pipeline Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add operations in the order they should execute on your document:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Current pipeline steps
                                    if (uiState.workflowSteps.isEmpty()) {
                                        Text(
                                            "No steps added yet. Tap an operation below to add it.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        uiState.workflowSteps.forEachIndexed { index, step ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}. ${step.displayName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removeWorkflowStep(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove step",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Available Operations:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Available operation chips
                                    val availableOps = listOf(
                                        OperationType.COMPRESS_PDF,
                                        OperationType.WATERMARK,
                                        OperationType.PROTECT_PDF,
                                        OperationType.OCR_PDF,
                                        OperationType.ROTATE_PAGES,
                                        OperationType.REPAIR_PDF,
                                        OperationType.PAGE_NUMBERS,
                                        OperationType.REMOVE_METADATA
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        availableOps.forEach { op ->
                                            SuggestionChip(
                                                onClick = { viewModel.addWorkflowStep(op) },
                                                label = { Text("+ ${op.displayName}", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "SF-094", "SF-095", "SF-101" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("PDF Page Layout (iLovePDF Style)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("High-definition 300 DPI canvas with smart orientation and border framing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Page Orientation:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionOrientation == "AUTO",
                                            onClick = { viewModel.onConversionOrientationChanged("AUTO") },
                                            label = { Text("Auto Detect") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionOrientation == "PORTRAIT",
                                            onClick = { viewModel.onConversionOrientationChanged("PORTRAIT") },
                                            label = { Text("Portrait") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionOrientation == "LANDSCAPE",
                                            onClick = { viewModel.onConversionOrientationChanged("LANDSCAPE") },
                                            label = { Text("Landscape") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Page Margins:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionMargin == "NONE",
                                            onClick = { viewModel.onConversionMarginChanged("NONE") },
                                            label = { Text("No Margin") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionMargin == "SMALL",
                                            onClick = { viewModel.onConversionMarginChanged("SMALL") },
                                            label = { Text("Small (Clean)") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionMargin == "NORMAL",
                                            onClick = { viewModel.onConversionMarginChanged("NORMAL") },
                                            label = { Text("Normal") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Image Fitting:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionFitPage,
                                            onClick = { viewModel.onConversionFitPageChanged(true) },
                                            label = { Text("Fit Page (Keep Ratio)") }
                                        )
                                        FilterChip(
                                            selected = !uiState.conversionFitPage,
                                            onClick = { viewModel.onConversionFitPageChanged(false) },
                                            label = { Text("Fill Page") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Paper Format:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionPageSize == "A4",
                                            onClick = { viewModel.onConversionPageSizeChanged("A4") },
                                            label = { Text("A4 Standard") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionPageSize == "LETTER",
                                            onClick = { viewModel.onConversionPageSizeChanged("LETTER") },
                                            label = { Text("US Letter") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-096", "SF-097", "SF-104" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Image Rendering Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Render PDF vector text and graphics into high-resolution images.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Resolution (DPI):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionDpi == 300,
                                            onClick = { viewModel.onConversionDpiChanged(300) },
                                            label = { Text("300 DPI (Ultra HD Print)") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionDpi == 150,
                                            onClick = { viewModel.onConversionDpiChanged(150) },
                                            label = { Text("150 DPI (Standard Screen)") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    val fmt = if (uiState.feature?.id == "SF-096") "High-Quality JPG (92% Quality)" else "Lossless 24-bit PNG"
                                    Text("Format: $fmt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        "SF-100" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Tabular PDF Styling (iLovePDF Style)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Converts CSV data into formatted tables with repeating headers and alternating rows.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Table Style Theme:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionTableTheme == "MODERN_NAVY",
                                            onClick = { viewModel.onConversionTableThemeChanged("MODERN_NAVY") },
                                            label = { Text("Modern Navy (Pro)") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionTableTheme == "CLEAN_SLATE",
                                            onClick = { viewModel.onConversionTableThemeChanged("CLEAN_SLATE") },
                                            label = { Text("Clean Slate") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionTableTheme == "MINIMAL",
                                            onClick = { viewModel.onConversionTableThemeChanged("MINIMAL") },
                                            label = { Text("Minimal") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Page Orientation:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionOrientation == "AUTO",
                                            onClick = { viewModel.onConversionOrientationChanged("AUTO") },
                                            label = { Text("Auto (Landscape if > 4 cols)") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionOrientation == "LANDSCAPE",
                                            onClick = { viewModel.onConversionOrientationChanged("LANDSCAPE") },
                                            label = { Text("Landscape") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionOrientation == "PORTRAIT",
                                            onClick = { viewModel.onConversionOrientationChanged("PORTRAIT") },
                                            label = { Text("Portrait") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-099" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Typography & Document Formatting", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Formats plain text with 45pt margins, running header, and page numbering.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Font Size & Spacing:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = uiState.conversionFontSize == 9.5f,
                                            onClick = { viewModel.onConversionFontSizeChanged(9.5f) },
                                            label = { Text("Compact (9.5 pt)") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionFontSize == 11f,
                                            onClick = { viewModel.onConversionFontSizeChanged(11f) },
                                            label = { Text("Standard (11 pt)") }
                                        )
                                        FilterChip(
                                            selected = uiState.conversionFontSize == 13f,
                                            onClick = { viewModel.onConversionFontSizeChanged(13f) },
                                            label = { Text("Large (13 pt)") }
                                        )
                                    }
                                }
                            }
                        }

                        "SF-105" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Smart Line-Break Pagination", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ScanFlow calculates line height boundaries so text lines are never cut across page breaks. Includes top header rule and page numbers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        "SF-098", "SF-102", "SF-103" -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Structured Text Extraction", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val desc = if (uiState.feature?.id == "SF-103") {
                                        "Extracts tabular data into standard RFC-4180 CSV with quotes escaping for Excel/Sheets."
                                    } else {
                                        "Extracts full clean document text with Unicode character normalization."
                                    }
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Output Filename
                    OutlinedTextField(
                        value = uiState.outputName,
                        onValueChange = { viewModel.onOutputNameChanged(it) },
                        label = { Text("Output Filename") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error Message (if any)
                    if (uiState.status == OperationStatus.FAILED && uiState.errorMessage != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage ?: "Operation failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Execute Button
                    Button(
                        onClick = { viewModel.executeOperation() },
                        enabled = uiState.selectedFiles.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "EXECUTE ${uiState.feature?.name?.uppercase() ?: "ACTION"}",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Processing Status Screen matching Stitch project screen c238a0c6cf914f61ba3df2e781cbde95
 */
@Composable
private fun ProcessingStatusView(
    featureName: String,
    firstFileName: String,
    fileSize: Long,
    progressPercent: Int,
    onCancel: () -> Unit
) {
    val formattedSize = remember(fileSize) {
        val kb = fileSize / 1024f
        if (kb < 1024) "%.1f KB".format(kb) else "%.1f MB".format(kb / 1024f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Privacy Assurance
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.Default.EnhancedEncryption, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Zero Cloud Exposure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("100% processed on-device. Your file never leaves your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // File Header & Telemetry Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(firstFileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(formattedSize, style = MetricMonoSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Native Offline", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Circular Progress & Percentage Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { maxOf(0.08f, progressPercent / 100f) },
                        strokeWidth = 10.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(140.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = featureName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry subtext pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "On-device processing • Real-time engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepItem(title = "Extracting and analyzing pages", isDone = progressPercent >= 30, isActive = progressPercent < 30)
                    StepItem(title = "Processing document transformations", isDone = progressPercent >= 80, isActive = progressPercent in 30..79)
                    StepItem(title = "Validating output checksum & catalog", isDone = progressPercent >= 100, isActive = progressPercent in 80..99)
                }
            }
        }

        // Cancel Button
        Button(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CANCEL PROCESSING", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepItem(
    title: String,
    isDone: Boolean,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDone) MaterialTheme.colorScheme.primary else if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                } else if (isActive) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                } else {
                    Icon(Icons.Default.HourglassEmpty, null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = if (isDone) "Completed" else if (isActive) "In progress" else "Pending",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Success & Output Preview Screen matching Stitch project screen 668ee33e1623414e878350f7b57c0ba4
 */
@Composable
private fun SuccessPreviewView(
    result: com.scanflow.app.core.result.OperationResult?,
    onOpenDocument: () -> Unit,
    onShareDocument: () -> Unit,
    onDone: () -> Unit
) {
    val formattedOutputSize = remember(result?.outputSize) {
        val size = result?.outputSize ?: 0L
        val kb = size / 1024f
        if (kb < 1024) "%.1f KB".format(kb) else "%.1f MB".format(kb / 1024f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Big Green Verified Checkmark
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(52.dp)
            )
        }

        Text(
            text = "Processing Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Your output has been cryptographically validated and saved to device storage.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Metrics Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Output File Size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formattedOutputSize, style = MetricMono, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pages Processed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${result?.pagesProcessed ?: 1}", style = MetricMono, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Execution Duration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${result?.durationMs ?: 0} ms", style = MetricMono, fontWeight = FontWeight.Bold)
                }

                result?.outputPath?.let { path ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Path: $path",
                        style = MetricMonoSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        val currentCtx = androidx.compose.ui.platform.LocalContext.current
        val aiText = result?.metadata?.get("summary") 
            ?: result?.metadata?.get("answer") 
            ?: result?.metadata?.get("translated")

        if (aiText != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (result?.metadata?.containsKey("summary") == true) "AI Summary Result"
                                else if (result?.metadata?.containsKey("answer") == true) "AI Answer & Citations"
                                else "Translation Output",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = currentCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("ScanFlow AI", aiText)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(currentCtx, "Copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = aiText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        result?.metadata?.get("similarity")?.let { sim ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.Compare, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Document Visual Similarity: $sim", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions: Open Document & Share
        Button(
            onClick = onOpenDocument,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("OPEN DOCUMENT", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onShareDocument,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SHARE DOCUMENT", fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onDone) {
            Text("Back to Home", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SignaturePadCard(
    uiState: ToolActionUiState,
    viewModel: ToolActionViewModel
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Interactive Signature Pad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Draw your handwritten signature below with your finger or stylus.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Canvas drawing area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths.add(currentPath)
                                    currentPath = emptyList()
                                    val bmp = renderPathsToBitmap(paths, 400, 180)
                                    viewModel.onSignatureBitmapChanged(bmp)
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { stroke ->
                        for (i in 0 until stroke.size - 1) {
                            drawLine(
                                color = androidx.compose.ui.graphics.Color.Black,
                                start = stroke[i],
                                end = stroke[i + 1],
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    for (i in 0 until currentPath.size - 1) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.Black,
                            start = currentPath[i],
                            end = currentPath[i + 1],
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                if (paths.isEmpty() && currentPath.isEmpty() && uiState.signatureBitmap == null) {
                    Text(
                        text = "Touch & sign here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.LightGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        paths.clear()
                        currentPath = emptyList()
                        viewModel.onSignatureBitmapChanged(null)
                    }
                ) {
                    Text("Clear Pad")
                }
                if (uiState.signatureBitmap != null) {
                    Text(
                        text = "✓ Signature Captured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Stamp Position", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "BOTTOM_RIGHT" to "Bottom Right",
                    "BOTTOM_LEFT" to "Bottom Left",
                    "TOP_RIGHT" to "Top Right",
                    "CENTER" to "Center"
                ).forEach { (pos, label) ->
                    FilterChip(
                        selected = uiState.signaturePosition == pos,
                        onClick = { viewModel.onSignaturePositionChanged(pos) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.signerName,
                    onValueChange = { viewModel.onSignerNameChanged(it) },
                    label = { Text("Signer Name / Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "${uiState.signatureTargetPage}",
                    onValueChange = { str ->
                        str.toIntOrNull()?.let { viewModel.onSignatureTargetPageChanged(it) }
                    },
                    label = { Text("Page #") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

private fun renderPathsToBitmap(paths: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        strokeWidth = 6f
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }
    paths.forEach { stroke ->
        for (i in 0 until stroke.size - 1) {
            canvas.drawLine(stroke[i].x, stroke[i].y, stroke[i + 1].x, stroke[i + 1].y, paint)
        }
    }
    return bitmap
}


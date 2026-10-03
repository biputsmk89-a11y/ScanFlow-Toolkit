package com.scanflow.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.app.R
import com.scanflow.app.ScanFlowApplication
import com.scanflow.app.core.theme.MetricMono
import com.scanflow.app.core.theme.MetricMonoSmall
import com.scanflow.app.domain.model.Document
import com.scanflow.app.ui.components.AppHeader
import com.scanflow.app.ui.components.DocumentItem
import com.scanflow.app.ui.components.EmptyStateView
import com.scanflow.app.ui.viewmodel.DocFilterType
import com.scanflow.app.ui.viewmodel.DocSortOption
import com.scanflow.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI UX Pro Max Modern Bento Card Home Screen.
 * Implements:
 * - Asymmetric Modular Bento Grid (2x1 Hero Card, 2x2 Feature Bento, 2x1 Metrics Strip)
 * - Spring physics tactile micro-interactions (bentoClickable press feedback)
 * - Material 3 high-contrast tonal surface hierarchy and live breathing status indicators
 * - Integrated Quick Filters, Voice Search, and Inline Document Management
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit,
    onNavigateToAiChat: (String) -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDocuments: () -> Unit = {},
    onShareDocument: (Document) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val displayedDocs by viewModel.displayedDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    val currentFilter by viewModel.filterType.collectAsState()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onSearchQueryChanged(spoken)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val storageEngine = ScanFlowApplication.container.storageEngine
                val contentResolver = context.contentResolver
                uris.forEach { uri ->
                    var name = "imported_${System.currentTimeMillis()}"
                    var ext = "pdf"
                    try {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0 && cursor.moveToFirst()) {
                                val fullName = cursor.getString(nameIndex)
                                name = fullName
                                val dot = fullName.lastIndexOf('.')
                                if (dot >= 0) ext = fullName.substring(dot + 1)
                            }
                        }
                    } catch (_: Exception) {}
                    val destFile = File(storageEngine.getDocumentsDirectory(), name)
                    if (storageEngine.copyUriToTempFile(uri, destFile)) {
                        val mime = when (ext.lowercase()) {
                            "pdf" -> "application/pdf"
                            "png" -> "image/png"
                            "jpg", "jpeg" -> "image/jpeg"
                            "txt" -> "text/plain"
                            "csv" -> "text/csv"
                            "html", "htm" -> "text/html"
                            else -> "application/octet-stream"
                        }
                        val pageCount = if (mime == "application/pdf") {
                            try { ScanFlowApplication.container.pdfEngine.getPageCount(destFile) } catch (_: Exception) { 1 }
                        } else 1
                        viewModel.importFile(destFile, mime, pageCount)
                    }
                }
            }
        }
    }

    var showFilterDialog by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var documentToDelete by remember { mutableStateOf<Document?>(null) }

    val totalDocCount = displayedDocs.size
    val pdfCount = remember(displayedDocs) {
        displayedDocs.count { it.mimeType.contains("pdf", ignoreCase = true) || it.name.endsWith(".pdf", ignoreCase = true) }
    }
    val imgCount = remember(displayedDocs) {
        displayedDocs.count { it.mimeType.contains("image", ignoreCase = true) || it.name.endsWith(".jpg", ignoreCase = true) || it.name.endsWith(".png", ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        AppHeader(
            title = "ScanFlow",
            subtitle = "Intelligence Toolkit",
            onProfileClick = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search & Filter Bar (Apple-style Modern Pill with micro-interaction)
            item {
                ModernBentoSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onVoiceSearchClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search documents, tools, or smart OCR...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (_: Exception) {}
                    },
                    onFilterClick = { showFilterDialog = true },
                    isFilterActive = currentFilter != DocFilterType.ALL || currentSort != DocSortOption.DATE_DESC
                )
            }

            // 2. Hero Bento Card: AI Camera Scanner & Auto Digitize (2x1 Span)
            item {
                BentoHeroScannerCard(
                    onStartScan = onNavigateToScanner,
                    onImagesToPdf = { onNavigateToTool("SF-101") }
                )
            }

            // 3. Asymmetric Bento Feature Grid (Core Tools)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: AI Assistant (Spotlight) + Smart Compress
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BentoFeatureTile(
                            title = "AI Doc Chat",
                            subtitle = "Summarize & Q&A",
                            badgeText = "AI PRO",
                            badgeColor = Color(0xFF6366F1),
                            icon = Icons.Default.AutoAwesome,
                            iconTint = Color(0xFF6366F1),
                            iconBackground = Color(0xFF6366F1).copy(alpha = 0.15f),
                            onClick = { onNavigateToAiChat("general") },
                            modifier = Modifier.weight(1.1f)
                        )

                        BentoFeatureTile(
                            title = "Compress",
                            subtitle = "Save storage",
                            badgeText = "-70%",
                            badgeColor = Color(0xFF10B981),
                            icon = Icons.Default.Compress,
                            iconTint = Color(0xFF10B981),
                            iconBackground = Color(0xFF10B981).copy(alpha = 0.15f),
                            onClick = { onNavigateToTool("SF-017") },
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Row 2: PDF Studio (Merge & Split) + Security & Sign Vault
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BentoFeatureTile(
                            title = "PDF Studio",
                            subtitle = "Merge & split pages",
                            badgeText = "ORGANIZE",
                            badgeColor = Color(0xFF0284C7),
                            icon = Icons.AutoMirrored.Filled.CallMerge,
                            iconTint = Color(0xFF0284C7),
                            iconBackground = Color(0xFF0284C7).copy(alpha = 0.15f),
                            onClick = { onNavigateToTool("SF-001") },
                            modifier = Modifier.weight(1f)
                        )

                        BentoFeatureTile(
                            title = "Sign & Protect",
                            subtitle = "AES-256 vault",
                            badgeText = "SECURE",
                            badgeColor = Color(0xFFF59E0B),
                            icon = Icons.Outlined.Draw,
                            iconTint = Color(0xFFF59E0B),
                            iconBackground = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            onClick = { onNavigateToTool("SF-084") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Bento Metrics & Privacy Status Strip
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Metric Card: Total Documents Stats
                    BentoMetricsTile(
                        count = totalDocCount,
                        label = "Local Docs",
                        formats = "PDF: $pdfCount • IMG: $imgCount",
                        onClick = onNavigateToDocuments,
                        modifier = Modifier.weight(1f)
                    )

                    // Right Privacy Card: Air-Gap Offline Shield
                    BentoAirGapTile(
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Quick Tools Horizontal Carousel (Bento Pills)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Quick Utilities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Import Files",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .bentoClickable(onClick = {
                                    importLauncher.launch(arrayOf("application/pdf", "image/*", "text/*"))
                                })
                                .padding(4.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        BentoCarouselChip(
                            title = "Import Files",
                            icon = Icons.Default.DriveFolderUpload,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                importLauncher.launch(arrayOf("application/pdf", "image/*", "text/*"))
                            }
                        )
                        BentoCarouselChip(
                            title = "OCR Text",
                            icon = Icons.Default.TextFields,
                            iconTint = Color(0xFF6366F1),
                            onClick = { onNavigateToTool("SF-059") }
                        )
                        BentoCarouselChip(
                            title = "Compare PDF",
                            icon = Icons.Default.Compare,
                            iconTint = Color(0xFF0284C7),
                            onClick = onNavigateToCompare
                        )
                        BentoCarouselChip(
                            title = "Protect PDF",
                            icon = Icons.Default.Lock,
                            iconTint = Color(0xFFF59E0B),
                            onClick = { onNavigateToTool("SF-079") }
                        )
                        BentoCarouselChip(
                            title = "Extract Text",
                            icon = Icons.Default.ContentCopy,
                            iconTint = Color(0xFF10B981),
                            onClick = { onNavigateToTool("SF-098") }
                        )
                        BentoCarouselChip(
                            title = "Split Pages",
                            icon = Icons.Default.Splitscreen,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigateToTool("SF-002") }
                        )
                    }
                }
            }

            // 6. Recent Documents Section Header with Inline Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.recent_documents),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalDocCount",
                                    style = MetricMonoSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .bentoClickable(onClick = { onNavigateToDocuments() })
                                .padding(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.see_all),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Inline Filter Chips for Quick Switch
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = currentFilter == DocFilterType.ALL,
                            onClick = { viewModel.setFilterType(DocFilterType.ALL) },
                            label = { Text("All ($totalDocCount)", style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = currentFilter == DocFilterType.PDF,
                            onClick = { viewModel.setFilterType(DocFilterType.PDF) },
                            label = { Text("PDFs ($pdfCount)", style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = currentFilter == DocFilterType.IMAGE,
                            onClick = { viewModel.setFilterType(DocFilterType.IMAGE) },
                            label = { Text("Images ($imgCount)", style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            val displayList = if (searchQuery.isNotBlank()) searchResults else displayedDocs

            if (displayList.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.DocumentScanner,
                        title = if (searchQuery.isNotBlank()) "No Matching Documents" else "No Documents Yet",
                        description = if (searchQuery.isNotBlank()) "Try refining your search keyword" else "Tap 'START SCAN' or 'Import Files' to build your local vault",
                        actionButtonText = "Scan Document",
                        onActionClick = onNavigateToScanner
                    )
                }
            } else {
                items(displayList, key = { it.id }) { doc ->
                    DocumentItem(
                        document = doc,
                        onClick = { onNavigateToViewer(doc.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                        onShare = { onShareDocument(doc) },
                        onRename = {
                            documentToRename = doc
                            renameInput = doc.name
                        },
                        onDelete = {
                            documentToDelete = doc
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Rename Dialog
        if (documentToRename != null) {
            AlertDialog(
                onDismissRequest = { documentToRename = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Rename Document", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Enter a new title for this file:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = documentToRename
                            if (target != null && renameInput.isNotBlank()) {
                                viewModel.renameDocument(target.id, renameInput.trim())
                            }
                            documentToRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (documentToDelete != null) {
            AlertDialog(
                onDismissRequest = { documentToDelete = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Delete Document", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to permanently delete \"${documentToDelete?.name}\"? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            documentToDelete?.let { viewModel.deleteDocument(it.id) }
                            documentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Filter & Sort Dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Filter & Sort Vault", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Filter by Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentFilter == DocFilterType.ALL,
                                onClick = { viewModel.setFilterType(DocFilterType.ALL) },
                                label = { Text("All") },
                                shape = RoundedCornerShape(50)
                            )
                            FilterChip(
                                selected = currentFilter == DocFilterType.PDF,
                                onClick = { viewModel.setFilterType(DocFilterType.PDF) },
                                label = { Text("PDFs Only") },
                                shape = RoundedCornerShape(50)
                            )
                            FilterChip(
                                selected = currentFilter == DocFilterType.IMAGE,
                                onClick = { viewModel.setFilterType(DocFilterType.IMAGE) },
                                label = { Text("Images") },
                                shape = RoundedCornerShape(50)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sort Order", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = currentSort == DocSortOption.DATE_DESC,
                                    onClick = { viewModel.setSortOption(DocSortOption.DATE_DESC) },
                                    label = { Text("Newest") },
                                    shape = RoundedCornerShape(50)
                                )
                                FilterChip(
                                    selected = currentSort == DocSortOption.DATE_ASC,
                                    onClick = { viewModel.setSortOption(DocSortOption.DATE_ASC) },
                                    label = { Text("Oldest") },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = currentSort == DocSortOption.NAME_ASC,
                                    onClick = { viewModel.setSortOption(DocSortOption.NAME_ASC) },
                                    label = { Text("Name (A-Z)") },
                                    shape = RoundedCornerShape(50)
                                )
                                FilterChip(
                                    selected = currentSort == DocSortOption.SIZE_DESC,
                                    onClick = { viewModel.setSortOption(DocSortOption.SIZE_DESC) },
                                    label = { Text("Largest Size") },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFilterDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// UI UX PRO MAX BENTO COMPONENTS & MICRO-INTERACTIONS
// -----------------------------------------------------------------------------

/**
 * Bento Clickable with tactile Spring Physics press micro-interaction.
 */
@Composable
fun Modifier.bentoClickable(
    onClick: () -> Unit,
    scaleDown: Float = 0.965f
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bentoPressScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            onClick = onClick
        )
}

/**
 * Pulsing live breathing dot indicator.
 */
@Composable
fun PulsingStatusDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(14.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Modern Bento Search Bar with quick clear, voice search, and filter status.
 */
@Composable
private fun ModernBentoSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    isFilterActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search documents, tools, or smart OCR...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )

        AnimatedVisibility(
            visible = searchQuery.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(
            onClick = onVoiceSearchClick,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (isFilterActive) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Bento Hero Scanner Card (2x1 Span).
 * Apple/Linear style with dynamic gradient, live pulse indicator, and glowing scanner art.
 */
@Composable
private fun BentoHeroScannerCard(
    onStartScan: () -> Unit,
    onImagesToPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        )
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Live AI Vision Badge with breathing dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        PulsingStatusDot(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI VISION ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Scan New Document",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Auto-detect edges, perspective correction & instant offline OCR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Modern Glowing Scanner Icon Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons with Spring Press Feedback
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartScan,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .bentoClickable(onClick = onStartScan)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START SCAN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onImagesToPdf,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bentoClickable(onClick = onImagesToPdf)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Images to PDF", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Asymmetric Bento Feature Tile.
 * Distinct accent tint, squircle icon, badge pill, and smooth spring physics.
 */
@Composable
private fun BentoFeatureTile(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .bentoClickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBackground)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Bento Metrics Tile (Total Scans & Formats breakdown).
 */
@Composable
private fun BentoMetricsTile(
    count: Int,
    label: String,
    formats: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.bentoClickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$count",
                        style = MetricMono,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = formats,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Bento Air-Gap Privacy Tile.
 */
@Composable
private fun BentoAirGapTile(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingStatusDot(color = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "AIR-GAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "100% Offline Vault",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Horizontal Carousel Tool Chip with tactile press feedback.
 */
@Composable
private fun BentoCarouselChip(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
        modifier = Modifier.bentoClickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

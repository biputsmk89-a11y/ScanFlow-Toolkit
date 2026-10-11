package com.scanflow.app.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.app.ScanFlowApplication
import com.scanflow.app.core.theme.MetricMono
import com.scanflow.app.core.theme.MetricMonoSmall
import com.scanflow.app.domain.model.Document
import com.scanflow.app.ui.components.AppHeader
import com.scanflow.app.ui.components.DocumentGridItem
import com.scanflow.app.ui.components.DocumentItem
import com.scanflow.app.ui.components.EmptyStateView
import com.scanflow.app.ui.viewmodel.DocumentFilter
import com.scanflow.app.ui.viewmodel.DocumentSortOrder
import com.scanflow.app.ui.viewmodel.DocumentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    onNavigateToViewer: (String) -> Unit,
    onShareDocument: (Document) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var documentToDelete by remember { mutableStateOf<Document?>(null) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        AppHeader(
            subtitle = "Documents Hub",
            onProfileClick = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                "Search documents, invoices, files...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
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
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Top Controls: Title, Count, Import, List/Grid toggle, Sort
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "All Documents",
                            style = MaterialTheme.typography.titleLarge,
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
                                text = "${documents.size}",
                                style = MetricMonoSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Toggles & Import Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Import Action Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { importLauncher.launch(arrayOf("*/*")) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // View & Sort Toggle Group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(2.dp)
                        ) {
                            // List view toggle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (!isGridView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { isGridView = false }
                            ) {
                                Icon(
                                    Icons.Default.ViewList,
                                    contentDescription = "List View",
                                    tint = if (!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Grid view toggle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isGridView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { isGridView = true }
                            ) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = "Grid View",
                                    tint = if (isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Sort Button with styled dropdown
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showSortMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = "Sort",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (sortOrder) {
                                            DocumentSortOrder.DATE_DESC -> "Newest"
                                            DocumentSortOrder.DATE_ASC -> "Oldest"
                                            DocumentSortOrder.NAME_ASC -> "Name A-Z"
                                            DocumentSortOrder.NAME_DESC -> "Name Z-A"
                                            DocumentSortOrder.SIZE_DESC -> "Size"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    shadowElevation = 8.dp
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Newest first", fontWeight = if (sortOrder == DocumentSortOrder.DATE_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            viewModel.setSortOrder(DocumentSortOrder.DATE_DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Oldest first", fontWeight = if (sortOrder == DocumentSortOrder.DATE_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            viewModel.setSortOrder(DocumentSortOrder.DATE_ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (A to Z)", fontWeight = if (sortOrder == DocumentSortOrder.NAME_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.SortByAlpha, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            viewModel.setSortOrder(DocumentSortOrder.NAME_ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (Z to A)", fontWeight = if (sortOrder == DocumentSortOrder.NAME_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.SortByAlpha, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            viewModel.setSortOrder(DocumentSortOrder.NAME_DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Largest size first", fontWeight = if (sortOrder == DocumentSortOrder.SIZE_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            viewModel.setSortOrder(DocumentSortOrder.SIZE_DESC)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Filter Chips (All, PDF, Images, Favorites)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChipTab(
                        label = "All",
                        selected = filter == DocumentFilter.ALL,
                        onClick = { viewModel.setFilter(DocumentFilter.ALL) }
                    )
                    FilterChipTab(
                        label = "PDF",
                        selected = filter == DocumentFilter.PDF_ONLY,
                        onClick = { viewModel.setFilter(DocumentFilter.PDF_ONLY) }
                    )
                    FilterChipTab(
                        label = "Images",
                        selected = filter == DocumentFilter.IMAGES_ONLY,
                        onClick = { viewModel.setFilter(DocumentFilter.IMAGES_ONLY) }
                    )
                    FilterChipTab(
                        label = "Favorites",
                        icon = Icons.Default.Star,
                        selected = filter == DocumentFilter.FAVORITES,
                        onClick = { viewModel.setFilter(DocumentFilter.FAVORITES) }
                    )
                }
            }

            // Document List
            if (documents.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Folder,
                        title = if (searchQuery.isNotBlank()) "No Matching Documents" else "No Documents Found",
                        description = if (searchQuery.isNotBlank()) "Try refining your search keyword" else "Documents you convert or import will be organized here",
                        actionButtonText = if (searchQuery.isBlank()) "Import Documents" else null,
                        onActionClick = { importLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
 else if (isGridView) {
                val chunkedDocs = documents.chunked(2)
                items(chunkedDocs, key = { row -> row.joinToString("_") { it.id } }) { rowDocs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (doc in rowDocs) {
                            Box(modifier = Modifier.weight(1f)) {
                                DocumentGridItem(
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
                        if (rowDocs.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(documents, key = { it.id }) { doc ->
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
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Rename Dialog
        if (documentToRename != null) {
            AlertDialog(
                onDismissRequest = { documentToRename = null },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
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
                        Text("Save")
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
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
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
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { documentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterChipTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

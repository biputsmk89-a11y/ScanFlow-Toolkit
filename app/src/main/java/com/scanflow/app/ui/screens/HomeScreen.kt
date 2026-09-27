package com.scanflow.app.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.app.domain.model.Document
import com.scanflow.app.ui.components.AppHeader
import com.scanflow.app.ui.components.DocumentItem
import com.scanflow.app.ui.components.EmptyStateView
import com.scanflow.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    onNavigateToViewer: (String) -> Unit,
    onNavigateToAiChat: (String) -> Unit,
    onShareDocument: (Document) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val favoriteDocs by viewModel.favoriteDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        AppHeader(
            title = "ScanFlow",
            subtitle = "All-in-One Document Toolkit"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search documents by name or OCR text...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Actions Bar
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "Scan",
                        icon = Icons.Default.CameraAlt,
                        color = Color(0xFF2563EB),
                        onClick = onNavigateToScanner
                    )
                    QuickActionButton(
                        title = "Merge",
                        icon = Icons.Default.MergeType,
                        color = Color(0xFF059669),
                        onClick = { onNavigateToTool("SF-001") }
                    )
                    QuickActionButton(
                        title = "Compress",
                        icon = Icons.Default.Compress,
                        color = Color(0xFFD97706),
                        onClick = { onNavigateToTool("SF-017") }
                    )
                    QuickActionButton(
                        title = "OCR",
                        icon = Icons.Default.FindInPage,
                        color = Color(0xFF7C3AED),
                        onClick = { onNavigateToTool("SF-025") }
                    )
                    QuickActionButton(
                        title = "Protect",
                        icon = Icons.Default.Lock,
                        color = Color(0xFFDC2626),
                        onClick = { onNavigateToTool("SF-079") }
                    )
                    QuickActionButton(
                        title = "Convert",
                        icon = Icons.Default.Transform,
                        color = Color(0xFF0284C7),
                        onClick = { onNavigateToTool("SF-094") }
                    )
                }
            }

            // Search Results Mode vs Normal Dashboard Mode
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (searchResults.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No documents found",
                            description = "No documents match \"$searchQuery\"."
                        )
                    }
                } else {
                    items(searchResults) { doc ->
                        DocumentItem(
                            document = doc,
                            onClick = { onNavigateToViewer(doc.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                            onShare = { onShareDocument(doc) },
                            onRename = { },
                            onDelete = { viewModel.deleteDocument(doc.id) }
                        )
                    }
                }
            } else {
                // Favorites Section (if any)
                if (favoriteDocs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Starred Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(favoriteDocs) { doc ->
                        DocumentItem(
                            document = doc,
                            onClick = { onNavigateToViewer(doc.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                            onShare = { onShareDocument(doc) },
                            onRename = { },
                            onDelete = { viewModel.deleteDocument(doc.id) }
                        )
                    }
                }

                // Recent Documents Section
                item {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (recentDocs.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Description,
                            title = "No documents yet",
                            description = "Scan paper sheets or process PDF files to view them here.",
                            actionButtonText = "Start Scanning",
                            onActionClick = onNavigateToScanner
                        )
                    }
                } else {
                    items(recentDocs) { doc ->
                        DocumentItem(
                            document = doc,
                            onClick = { onNavigateToViewer(doc.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                            onShare = { onShareDocument(doc) },
                            onRename = { },
                            onDelete = { viewModel.deleteDocument(doc.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

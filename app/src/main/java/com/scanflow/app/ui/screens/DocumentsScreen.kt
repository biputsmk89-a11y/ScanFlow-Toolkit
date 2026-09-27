package com.scanflow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scanflow.app.domain.model.Document
import com.scanflow.app.ui.components.AppHeader
import com.scanflow.app.ui.components.DocumentItem
import com.scanflow.app.ui.components.EmptyStateView
import com.scanflow.app.ui.viewmodel.DocumentFilter
import com.scanflow.app.ui.viewmodel.DocumentSortOrder
import com.scanflow.app.ui.viewmodel.DocumentsViewModel

@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    onNavigateToViewer: (String) -> Unit,
    onShareDocument: (Document) -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        AppHeader(
            title = "My Documents",
            subtitle = "${documents.size} files"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Filter documents...") },
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

            // Filters & Sort Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filter == DocumentFilter.ALL,
                        onClick = { viewModel.setFilter(DocumentFilter.ALL) },
                        label = { Text("All") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = filter == DocumentFilter.FAVORITES,
                        onClick = { viewModel.setFilter(DocumentFilter.FAVORITES) },
                        label = { Text("Favorites") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val nextSort = when (sortOrder) {
                            DocumentSortOrder.DATE_DESC -> DocumentSortOrder.NAME_ASC
                            DocumentSortOrder.NAME_ASC -> DocumentSortOrder.SIZE_DESC
                            else -> DocumentSortOrder.DATE_DESC
                        }
                        viewModel.setSortOrder(nextSort)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort order",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (documents.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Folder,
                        title = "No documents found",
                        description = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "Import or scan documents to manage them here."
                    )
                }
            } else {
                items(documents) { doc ->
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

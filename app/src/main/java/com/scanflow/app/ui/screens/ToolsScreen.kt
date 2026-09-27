package com.scanflow.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.scanflow.app.core.registry.FeatureCategory
import com.scanflow.app.core.registry.FeatureDefinition
import com.scanflow.app.ui.components.AppHeader
import com.scanflow.app.ui.components.ToolCard
import com.scanflow.app.ui.viewmodel.ToolsViewModel

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onNavigateToTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val features = viewModel.getFilteredFeatures()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        AppHeader(
            title = "All Tools",
            subtitle = "${features.size} capabilities"
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
                    placeholder = { Text("Search tools (e.g. merge, compress, protect)...") },
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

            // Category Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") }
                    )
                    FeatureCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }

            items(features) { feat ->
                val (icon, color) = getFeatureVisuals(feat)
                ToolCard(
                    title = feat.name,
                    description = feat.description,
                    icon = icon,
                    iconColor = color,
                    badgeText = if (feat.offline) "Offline" else "Online",
                    onClick = { onNavigateToTool(feat.id) }
                )
            }
        }
    }
}

private fun getFeatureVisuals(feature: FeatureDefinition): Pair<ImageVector, Color> {
    return when (feature.category) {
        FeatureCategory.ORGANIZE -> Pair(Icons.Default.MergeType, Color(0xFF2563EB))
        FeatureCategory.OPTIMIZE -> Pair(Icons.Default.Compress, Color(0xFFD97706))
        FeatureCategory.SCANNER -> Pair(Icons.Default.CameraAlt, Color(0xFF059669))
        FeatureCategory.OCR -> Pair(Icons.Default.FindInPage, Color(0xFF7C3AED))
        FeatureCategory.EDIT -> Pair(Icons.Default.Edit, Color(0xFF0284C7))
        FeatureCategory.SECURITY -> Pair(Icons.Default.Lock, Color(0xFFDC2626))
        FeatureCategory.FORMS -> Pair(Icons.Default.Description, Color(0xFF4F46E5))
        FeatureCategory.COMPARE -> Pair(Icons.Default.Compare, Color(0xFF0891B2))
        FeatureCategory.CONVERTER -> Pair(Icons.Default.Transform, Color(0xFF059669))
        FeatureCategory.AI -> Pair(Icons.Default.AutoAwesome, Color(0xFF9333EA))
        FeatureCategory.WORKFLOW -> Pair(Icons.Default.Route, Color(0xFFEA580C))
        else -> Pair(Icons.Default.Build, Color(0xFF2563EB))
    }
}

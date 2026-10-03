package com.scanflow.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.app.core.theme.CategoryConvert
import com.scanflow.app.core.theme.CategoryConvertContainer
import com.scanflow.app.core.theme.CategoryEdit
import com.scanflow.app.core.theme.CategoryEditContainer
import com.scanflow.app.core.theme.CategoryIntelligence
import com.scanflow.app.core.theme.CategoryIntelligenceContainer
import com.scanflow.app.core.theme.CategoryOptimize
import com.scanflow.app.core.theme.CategoryOptimizeContainer
import com.scanflow.app.core.theme.CategoryOrganize
import com.scanflow.app.core.theme.CategoryOrganizeContainer
import com.scanflow.app.core.theme.CategorySecurity
import com.scanflow.app.core.theme.CategorySecurityContainer
import com.scanflow.app.core.theme.CategoryWorkflow
import com.scanflow.app.core.theme.CategoryWorkflowContainer
import com.scanflow.app.core.theme.MetricMonoSmall
import com.scanflow.app.core.theme.SectionHeaderTitle
import com.scanflow.app.core.theme.StitchPrimary
import com.scanflow.app.core.theme.StitchPrimaryFixed
import com.scanflow.app.core.theme.TagBadge
import com.scanflow.app.ui.components.AppHeader
import com.scanflow.app.ui.components.EmptyStateView
import com.scanflow.app.ui.viewmodel.ToolsViewModel

/**
 * 8 Top Categories strictly mirroring iLovePDF capability benchmark.
 */
enum class ToolTabCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Apps),
    WORKFLOWS("Workflows", Icons.Default.AccountTree),
    ORGANIZE("Organize PDF", Icons.Default.Layers),
    OPTIMIZE("Optimize PDF", Icons.Default.Compress),
    CONVERT("Convert PDF", Icons.Default.Transform),
    EDIT("Edit PDF", Icons.Default.Edit),
    SECURITY("PDF Security", Icons.Default.Security),
    INTELLIGENCE("PDF Intelligence", Icons.Default.AutoAwesome)
}

fun getCategoryColors(category: ToolTabCategory): Pair<Color, Color> = when (category) {
    ToolTabCategory.WORKFLOWS -> CategoryWorkflow to CategoryWorkflowContainer
    ToolTabCategory.ORGANIZE -> CategoryOrganize to CategoryOrganizeContainer
    ToolTabCategory.OPTIMIZE -> CategoryOptimize to CategoryOptimizeContainer
    ToolTabCategory.CONVERT -> CategoryConvert to CategoryConvertContainer
    ToolTabCategory.EDIT -> CategoryEdit to CategoryEditContainer
    ToolTabCategory.SECURITY -> CategorySecurity to CategorySecurityContainer
    ToolTabCategory.INTELLIGENCE -> CategoryIntelligence to CategoryIntelligenceContainer
    ToolTabCategory.ALL -> StitchPrimary to StitchPrimaryFixed
}

data class ToolItemDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolTabCategory,
    val icon: ImageVector,
    val badge: String? = null,
    val onClick: () -> Unit
)

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onNavigateToTool: (String) -> Unit,
    onNavigateToScanner: () -> Unit = {},
    onNavigateToCompare: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchInput by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(ToolTabCategory.ALL) }

    // Complete 31-Tool Matrix mirroring iLovePDF Full Dashboard
    val allTools = remember(onNavigateToScanner, onNavigateToTool, onNavigateToCompare) {
        listOf(
            // --- 1. WORKFLOWS ---
            ToolItemDefinition(
                id = "SF-133",
                title = "Create a Workflow",
                description = "Automate document pipelines (e.g. OCR -> Compress -> Protect)",
                category = ToolTabCategory.WORKFLOWS,
                icon = Icons.Default.AccountTree,
                badge = "Automate",
                onClick = { onNavigateToTool("SF-133") }
            ),
            ToolItemDefinition(
                id = "SF-137",
                title = "Batch Workflow",
                description = "Execute multi-step recipe over batch files in background",
                category = ToolTabCategory.WORKFLOWS,
                icon = Icons.Default.DynamicFeed,
                badge = "Batch",
                onClick = { onNavigateToTool("SF-137") }
            ),

            // --- 2. ORGANIZE PDF ---
            ToolItemDefinition(
                id = "SF-001",
                title = "Merge PDF",
                description = "Combine multiple PDFs in the order you want",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.AutoMirrored.Filled.CallMerge,
                badge = "Easiest",
                onClick = { onNavigateToTool("SF-001") }
            ),
            ToolItemDefinition(
                id = "SF-002",
                title = "Split PDF",
                description = "Separate one page or a whole set into independent files",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.AutoMirrored.Filled.CallSplit,
                onClick = { onNavigateToTool("SF-002") }
            ),
            ToolItemDefinition(
                id = "SF-005",
                title = "Organize PDF",
                description = "Sort, delete, and reorder pages of your PDF file",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.Default.DashboardCustomize,
                onClick = { onNavigateToTool("SF-005") }
            ),
            ToolItemDefinition(
                id = "SF-006",
                title = "Scan to PDF",
                description = "Capture document scans with camera auto-crop & filters",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.Default.DocumentScanner,
                badge = "CameraX",
                onClick = onNavigateToScanner
            ),
            ToolItemDefinition(
                id = "SF-008",
                title = "Rotate PDF",
                description = "Rotate your PDFs the way you need them (90°, 180°, 270°)",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.AutoMirrored.Filled.RotateRight,
                onClick = { onNavigateToTool("SF-008") }
            ),
            ToolItemDefinition(
                id = "SF-003",
                title = "Remove Pages",
                description = "Delete unwanted pages from your PDF document",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.Default.Delete,
                onClick = { onNavigateToTool("SF-003") }
            ),
            ToolItemDefinition(
                id = "SF-004",
                title = "Extract Pages",
                description = "Extract chosen pages into a clean, separate document",
                category = ToolTabCategory.ORGANIZE,
                icon = Icons.Default.ContentCut,
                onClick = { onNavigateToTool("SF-004") }
            ),

            // --- 3. OPTIMIZE PDF ---
            ToolItemDefinition(
                id = "SF-017",
                title = "Compress PDF",
                description = "Reduce file size while optimizing for maximal PDF quality",
                category = ToolTabCategory.OPTIMIZE,
                icon = Icons.Default.Compress,
                badge = "-70%",
                onClick = { onNavigateToTool("SF-017") }
            ),
            ToolItemDefinition(
                id = "SF-024",
                title = "Repair PDF",
                description = "Repair damaged PDF and recover data from corrupt files",
                category = ToolTabCategory.OPTIMIZE,
                icon = Icons.Default.Build,
                badge = "Recovery",
                onClick = { onNavigateToTool("SF-024") }
            ),
            ToolItemDefinition(
                id = "SF-025",
                title = "OCR PDF",
                description = "Easily convert scanned PDF into searchable and selectable text",
                category = ToolTabCategory.OPTIMIZE,
                icon = Icons.Default.TextFields,
                badge = "Searchable",
                onClick = { onNavigateToTool("SF-025") }
            ),

            // --- 4. CONVERT PDF ---
            ToolItemDefinition(
                id = "SF-094",
                title = "JPG to PDF",
                description = "Convert JPG images to PDF. Adjust orientation and margins",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.PictureAsPdf,
                onClick = { onNavigateToTool("SF-094") }
            ),
            ToolItemDefinition(
                id = "SF-096",
                title = "PDF to JPG",
                description = "Convert each PDF page into a JPG or extract images",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.Image,
                onClick = { onNavigateToTool("SF-096") }
            ),
            ToolItemDefinition(
                id = "SF-099",
                title = "Text to PDF",
                description = "Convert plain text files (.txt) and notes into formatted PDF",
                category = ToolTabCategory.CONVERT,
                icon = Icons.AutoMirrored.Filled.Article,
                badge = "TXT",
                onClick = { onNavigateToTool("SF-099") }
            ),
            ToolItemDefinition(
                id = "SF-100",
                title = "CSV to PDF",
                description = "Render spreadsheet tables and CSV files into clean PDF tables",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.GridOn,
                badge = "CSV",
                onClick = { onNavigateToTool("SF-100") }
            ),
            ToolItemDefinition(
                id = "SF-102",
                title = "PDF to Structured Text",
                description = "Extract structured text formatted for Word & text editors",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.Description,
                badge = "Export",
                onClick = { onNavigateToTool("SF-102") }
            ),
            ToolItemDefinition(
                id = "SF-103",
                title = "PDF to CSV / Excel",
                description = "Pull tabular data straight from PDFs into Excel / CSV",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.TableView,
                badge = "Spreadsheet",
                onClick = { onNavigateToTool("SF-103") }
            ),
            ToolItemDefinition(
                id = "SF-104",
                title = "PDF to PNG (Lossless)",
                description = "Export high-resolution lossless PNG pages from PDF",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.Image,
                badge = "PNG",
                onClick = { onNavigateToTool("SF-104") }
            ),
            ToolItemDefinition(
                id = "SF-101",
                title = "Images to PDF",
                description = "Compile multiple photos and document scans into a PDF",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.Collections,
                onClick = { onNavigateToTool("SF-101") }
            ),
            ToolItemDefinition(
                id = "SF-105",
                title = "HTML to PDF",
                description = "Convert HTML webpages and rich text to PDF with a click",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.Html,
                onClick = { onNavigateToTool("SF-105") }
            ),
            ToolItemDefinition(
                id = "SF-106",
                title = "PDF to PDF/A",
                description = "Transform PDF to ISO-standardized PDF/A for long-term archiving",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.Archive,
                badge = "ISO Archival",
                onClick = { onNavigateToTool("SF-106") }
            ),
            ToolItemDefinition(
                id = "SF-098",
                title = "PDF to Plain Text",
                description = "Strip selectable text and extract raw text from document",
                category = ToolTabCategory.CONVERT,
                icon = Icons.Default.FormatColorText,
                onClick = { onNavigateToTool("SF-098") }
            ),

            // --- 5. EDIT PDF ---
            ToolItemDefinition(
                id = "SF-066",
                title = "Edit PDF",
                description = "Add text, drawings, and freehand annotations to a PDF",
                category = ToolTabCategory.EDIT,
                icon = Icons.Default.Edit,
                badge = "Overlay",
                onClick = { onNavigateToTool("SF-066") }
            ),
            ToolItemDefinition(
                id = "SF-078",
                title = "Watermark",
                description = "Stamp custom text over your PDF with angle & opacity",
                category = ToolTabCategory.EDIT,
                icon = Icons.AutoMirrored.Filled.BrandingWatermark,
                onClick = { onNavigateToTool("SF-078") }
            ),
            ToolItemDefinition(
                id = "SF-077",
                title = "Page numbers",
                description = "Add page numbers into PDFs (header/footer positions)",
                category = ToolTabCategory.EDIT,
                icon = Icons.Default.FormatListNumbered,
                onClick = { onNavigateToTool("SF-077") }
            ),
            ToolItemDefinition(
                id = "SF-075",
                title = "Crop PDF",
                description = "Crop margins of PDF documents or select specific page area",
                category = ToolTabCategory.EDIT,
                icon = Icons.Default.Crop,
                onClick = { onNavigateToTool("SF-075") }
            ),
            ToolItemDefinition(
                id = "SF-086",
                title = "PDF Forms",
                description = "Detect and fill interactive PDF forms, checkboxes and text",
                category = ToolTabCategory.EDIT,
                icon = Icons.AutoMirrored.Filled.Assignment,
                badge = "New!",
                onClick = { onNavigateToTool("SF-086") }
            ),

            // --- 6. PDF SECURITY ---
            ToolItemDefinition(
                id = "SF-084",
                title = "Sign PDF",
                description = "Sign yourself with digital signature stamp and annotations",
                category = ToolTabCategory.SECURITY,
                icon = Icons.Outlined.Draw,
                onClick = { onNavigateToTool("SF-084") }
            ),
            ToolItemDefinition(
                id = "SF-079",
                title = "Protect PDF",
                description = "Encrypt PDF with AES-128 / AES-256 password protection",
                category = ToolTabCategory.SECURITY,
                icon = Icons.Default.Lock,
                badge = "AES-256",
                onClick = { onNavigateToTool("SF-079") }
            ),
            ToolItemDefinition(
                id = "SF-081",
                title = "Unlock PDF",
                description = "Remove PDF password security and generate unlocked file",
                category = ToolTabCategory.SECURITY,
                icon = Icons.Default.LockOpen,
                onClick = { onNavigateToTool("SF-081") }
            ),
            ToolItemDefinition(
                id = "SF-083",
                title = "Redact PDF",
                description = "Permanently mask sensitive text and private graphics",
                category = ToolTabCategory.SECURITY,
                icon = Icons.Default.VisibilityOff,
                badge = "Permanent",
                onClick = { onNavigateToTool("SF-083") }
            ),
            ToolItemDefinition(
                id = "SF-082",
                title = "Remove Metadata",
                description = "Scrub author, creation history, and hidden system tags",
                category = ToolTabCategory.SECURITY,
                icon = Icons.Default.CleaningServices,
                onClick = { onNavigateToTool("SF-082") }
            ),

            // --- 7. PDF INTELLIGENCE ---
            ToolItemDefinition(
                id = "SF-107",
                title = "AI Summarizer",
                description = "Quickly generate concise key points and executive summaries",
                category = ToolTabCategory.INTELLIGENCE,
                icon = Icons.Default.AutoAwesome,
                badge = "New!",
                onClick = { onNavigateToTool("SF-107") }
            ),
            ToolItemDefinition(
                id = "SF-108",
                title = "Ask PDF / Chat",
                description = "Natural language question answering with page citations",
                category = ToolTabCategory.INTELLIGENCE,
                icon = Icons.Default.Psychology,
                badge = "AI",
                onClick = { onNavigateToTool("SF-108") }
            ),
            ToolItemDefinition(
                id = "SF-110",
                title = "Translate PDF",
                description = "Translate document text while preserving section structure",
                category = ToolTabCategory.INTELLIGENCE,
                icon = Icons.Default.Translate,
                badge = "New!",
                onClick = { onNavigateToTool("SF-110") }
            ),
            ToolItemDefinition(
                id = "SF-093",
                title = "Compare PDF",
                description = "Side-by-side comparison with pixel difference heatmap",
                category = ToolTabCategory.INTELLIGENCE,
                icon = Icons.Default.Compare,
                badge = "Visual Diff",
                onClick = onNavigateToCompare
            )
        )
    }

    val searchResults = remember(searchInput, allTools) {
        if (searchInput.isBlank()) emptyList()
        else {
            val query = searchInput.trim()
            allTools.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
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
            title = "ScanFlow",
            subtitle = "Tools",
            onProfileClick = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Tools
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
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = {
                            Text(
                                "Find any PDF or scanner tool...",
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
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { searchInput = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Quick Actions Hero Bento (Shown when not searching)
            if (searchInput.isBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "POPULAR UTILITIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Fast Launch",
                                style = MetricMonoSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // 2x2 Bento Quick Action Grid
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BentoQuickCard(
                                title = "Scan to PDF",
                                subtitle = "CameraX auto-crop",
                                icon = Icons.Default.DocumentScanner,
                                accentColor = CategorySecurity,
                                containerColor = CategorySecurityContainer,
                                onClick = onNavigateToScanner,
                                modifier = Modifier.weight(1f)
                            )
                            BentoQuickCard(
                                title = "Merge PDF",
                                subtitle = "Combine multiple",
                                icon = Icons.AutoMirrored.Filled.CallMerge,
                                accentColor = CategoryOrganize,
                                containerColor = CategoryOrganizeContainer,
                                onClick = { onNavigateToTool("SF-001") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BentoQuickCard(
                                title = "Smart Compress",
                                subtitle = "Up to -70% size",
                                icon = Icons.Default.Compress,
                                accentColor = CategoryOptimize,
                                containerColor = CategoryOptimizeContainer,
                                onClick = { onNavigateToTool("SF-010") },
                                modifier = Modifier.weight(1f)
                            )
                            BentoQuickCard(
                                title = "AI Assistant",
                                subtitle = "Ask & Summarize",
                                icon = Icons.Default.AutoAwesome,
                                accentColor = CategoryIntelligence,
                                containerColor = CategoryIntelligenceContainer,
                                onClick = { onNavigateToTool("SF-108") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Offline Guarantee Banner
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "100% Offline Engine Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Every tool processes locally with zero cloud upload",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Private",
                                style = TagBadge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Top Category Tabs with Count Badges
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    ToolTabCategory.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        val count = remember(allTools, tab) {
                            if (tab == ToolTabCategory.ALL) allTools.size
                            else allTools.count { it.category == tab }
                        }
                        val (tabAccent, _) = getCategoryColors(tab)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { activeTab = tab }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else tabAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    style = MetricMonoSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time Search Results
            if (searchInput.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No Matching Tools",
                            description = "No utilities match \"$searchInput\". Try searching for 'merge', 'word', 'forms', or 'ocr'."
                        )
                    }
                } else {
                    item {
                        ToolSectionHeader(title = "Matching Tools", utilityCount = searchResults.size)
                    }
                    items(searchResults) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }
            } else {
                // Section 1: Workflows
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.WORKFLOWS) {
                    val items = allTools.filter { it.category == ToolTabCategory.WORKFLOWS }
                    item { ToolSectionHeader(title = "Workflows", utilityCount = items.size, categoryColor = CategoryWorkflow) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }

                // Section 2: Organize PDF
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.ORGANIZE) {
                    val items = allTools.filter { it.category == ToolTabCategory.ORGANIZE }
                    item { ToolSectionHeader(title = "Organize PDF", utilityCount = items.size, categoryColor = CategoryOrganize) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }

                // Section 3: Optimize PDF
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.OPTIMIZE) {
                    val items = allTools.filter { it.category == ToolTabCategory.OPTIMIZE }
                    item { ToolSectionHeader(title = "Optimize PDF", utilityCount = items.size, categoryColor = CategoryOptimize) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }

                // Section 4: Convert PDF
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.CONVERT) {
                    val items = allTools.filter { it.category == ToolTabCategory.CONVERT }
                    item { ToolSectionHeader(title = "Convert PDF", utilityCount = items.size, categoryColor = CategoryConvert) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }

                // Section 5: Edit PDF
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.EDIT) {
                    val items = allTools.filter { it.category == ToolTabCategory.EDIT }
                    item { ToolSectionHeader(title = "Edit PDF", utilityCount = items.size, categoryColor = CategoryEdit) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }

                // Section 6: PDF Security
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.SECURITY) {
                    val items = allTools.filter { it.category == ToolTabCategory.SECURITY }
                    item { ToolSectionHeader(title = "PDF Security", utilityCount = items.size, categoryColor = CategorySecurity) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }

                // Section 7: PDF Intelligence
                if (activeTab == ToolTabCategory.ALL || activeTab == ToolTabCategory.INTELLIGENCE) {
                    val items = allTools.filter { it.category == ToolTabCategory.INTELLIGENCE }
                    item { ToolSectionHeader(title = "PDF Intelligence", utilityCount = items.size, categoryColor = CategoryIntelligence) }
                    items(items) { item ->
                        StitchToolItem(
                            title = item.title,
                            description = item.description,
                            icon = item.icon,
                            category = item.category,
                            badge = item.badge,
                            onClick = item.onClick
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ToolSectionHeader(
    title: String,
    utilityCount: Int,
    categoryColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = SectionHeaderTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(categoryColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$utilityCount tools",
                style = MetricMonoSmall,
                fontWeight = FontWeight.Bold,
                color = categoryColor
            )
        }
    }
}

@Composable
private fun BentoQuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bentoQuickPress"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StitchToolItem(
    title: String,
    description: String,
    icon: ImageVector,
    category: ToolTabCategory,
    onClick: () -> Unit,
    badge: String? = null
) {
    val (accentColor, containerColor) = getCategoryColors(category)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toolItemPressScale"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isBadgeAlert = badge == "-70%" || badge == "New!" || badge == "Lossless"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isBadgeAlert) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                    } else {
                                        accentColor.copy(alpha = 0.14f)
                                    }
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                style = TagBadge,
                                color = if (isBadgeAlert) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    accentColor
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

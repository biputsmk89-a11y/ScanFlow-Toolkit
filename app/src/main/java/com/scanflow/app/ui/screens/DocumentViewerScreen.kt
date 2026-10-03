package com.scanflow.app.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.scanflow.app.core.theme.MetricMono
import com.scanflow.app.core.theme.MetricMonoSmall
import com.scanflow.app.domain.model.Document
import com.scanflow.app.ui.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

private enum class EyeComfortTheme(val label: String, val pageBg: Color, val canvasBg: Color, val textColor: Color) {
    SEPIA("Sepia", Color(0xFFFFFDF8), Color(0xFFFAF8FF), Color(0xFF24211A)),
    DAY("Crisp Day", Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFF0F172A)),
    DARK("Dark Mode", Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFFF1F5F9))
}

private enum class ReadingFormat(val label: String) {
    CONTINUOUS("Continuous"),
    FIT_WIDTH("Fit Width (100%)"),
    TEXT_REFLOW("Text Reflow"),
    AUTO_CROP("Auto-Crop Margins")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentId: String,
    viewModel: ViewerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAiChat: (String) -> Unit,
    onShare: (Document) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    var activeTheme by remember(isSystemDark) {
        mutableStateOf(if (isSystemDark) EyeComfortTheme.DARK else EyeComfortTheme.SEPIA)
    }
    var activeFormat by remember { mutableStateOf(ReadingFormat.CONTINUOUS) }
    var isMarkerActive by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var sliderPage by remember { mutableFloatStateOf(1f) }
    var showThumbnailSheet by remember { mutableStateOf(false) }
    val highlighterStrokes = remember { mutableStateListOf<List<Offset>>() }
    val pageStrokesMap = remember { mutableMapOf<Int, List<List<Offset>>>() }
    var currentHighlighterStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var reflowText by remember { mutableStateOf<String?>(null) }
    var isLoadingReflow by remember { mutableStateOf(false) }

    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var localTts: TextToSpeech? = null
        localTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                localTts?.language = java.util.Locale.getDefault()
            }
        }
        tts = localTts
        onDispose {
            localTts?.stop()
            localTts?.shutdown()
        }
    }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    LaunchedEffect(uiState.currentPageIndex) {
        sliderPage = (uiState.currentPageIndex + 1).toFloat()
        zoomScale = 1f
        panOffset = Offset.Zero
        highlighterStrokes.clear()
        pageStrokesMap[uiState.currentPageIndex]?.let {
            highlighterStrokes.addAll(it)
        }
        currentHighlighterStroke = emptyList()
        reflowText = null
    }

    LaunchedEffect(activeFormat, uiState.currentPageIndex) {
        if (activeFormat == ReadingFormat.TEXT_REFLOW && reflowText == null) {
            isLoadingReflow = true
            reflowText = viewModel.getPageText(uiState.currentPageIndex)
            isLoadingReflow = false
        }
    }

    val doc = uiState.document
    val totalPages = if (uiState.totalPages > 0) uiState.totalPages else 1
    val progressPercent = ((uiState.currentPageIndex + 1) * 100) / totalPages

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(activeTheme.canvasBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = doc?.name ?: "PDF Document Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        val formattedSize = doc?.let { Formatter.formatFileSize(context, it.sizeBytes) } ?: ""
                        Text(
                            text = "${doc?.name ?: "document.pdf"} • ${uiState.totalPages} pgs $formattedSize",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search in document",
                            tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onNavigateToAiChat(documentId) }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask PDF with AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    val isDocFavorite = uiState.document?.isFavorite == true
                    IconButton(onClick = {
                        viewModel.toggleBookmark()
                        scope.launch {
                            snackbarHostState.showSnackbar(if (!isDocFavorite) "Document bookmarked in favorites" else "Bookmark removed")
                        }
                    }) {
                        Icon(
                            imageVector = if (isDocFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isDocFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    doc?.let {
                        IconButton(onClick = { onShare(it) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Document")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )

            // Inline Search Bar (Real Full-Document Search)
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search text across all pages...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = ""; searchMatches = emptyList() }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    isSearching = true
                                    scope.launch {
                                        val matches = viewModel.searchInDocument(searchQuery.trim())
                                        searchMatches = matches
                                        currentMatchIndex = 0
                                        isSearching = false
                                        if (matches.isNotEmpty()) {
                                            viewModel.goToPage(matches[0])
                                            snackbarHostState.showSnackbar("Found on page ${matches[0] + 1} (${matches.size} total)")
                                        } else {
                                            snackbarHostState.showSnackbar("No matching text found")
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Find")
                        }

                        if (searchMatches.size > 1) {
                            IconButton(
                                onClick = {
                                    if (currentMatchIndex > 0) {
                                        currentMatchIndex--
                                        viewModel.goToPage(searchMatches[currentMatchIndex])
                                    }
                                },
                                enabled = currentMatchIndex > 0
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Match")
                            }

                            Text(
                                text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                style = MetricMonoSmall,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    if (currentMatchIndex < searchMatches.size - 1) {
                                        currentMatchIndex++
                                        viewModel.goToPage(searchMatches[currentMatchIndex])
                                    }
                                },
                                enabled = currentMatchIndex < searchMatches.size - 1
                            ) {
                                Icon(Icons.Default.NavigateNext, contentDescription = "Next Match")
                            }
                        }

                        IconButton(onClick = {
                            showSearchBar = false
                            searchQuery = ""
                            searchMatches = emptyList()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    }
                }
            }

            // Reading Environment Strip (Sticky HUD)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Eye Comfort Status Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Eye-Comfort ${activeTheme.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pg ${uiState.currentPageIndex + 1}/$totalPages ($progressPercent%)",
                                style = MetricMonoSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Theme switch & TTS buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    val nextIndex = (activeTheme.ordinal + 1) % EyeComfortTheme.values().size
                                    activeTheme = EyeComfortTheme.values()[nextIndex]
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Theme: ${activeTheme.label}")
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Contrast,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeTheme.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Real Text-To-Speech Button
                            IconButton(
                                onClick = {
                                    if (isTtsSpeaking) {
                                        tts?.stop()
                                        isTtsSpeaking = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Speech stopped")
                                        }
                                    } else {
                                        isTtsSpeaking = true
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Reading Page ${uiState.currentPageIndex + 1} aloud...")
                                            val text = viewModel.getCurrentPageText()
                                            if (text.isNotBlank()) {
                                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DocViewerTts")
                                            } else {
                                                isTtsSpeaking = false
                                                snackbarHostState.showSnackbar("No readable text found on Page ${uiState.currentPageIndex + 1}")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isTtsSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isTtsSpeaking) "Stop Reading" else "Read Aloud",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Highlighter Marker Toggle Button
                            IconButton(
                                onClick = {
                                    isMarkerActive = !isMarkerActive
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (isMarkerActive) "Highlighter active: draw directly on page" else "Highlighter turned off")
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isMarkerActive) Color(0xFFFDE047) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Highlighter",
                                    tint = if (isMarkerActive) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Permanent Save Annotation to PDF Button
                            if (highlighterStrokes.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Burning annotations into PDF...")
                                            val success = viewModel.saveAnnotationToPdf(
                                                pageIndex = uiState.currentPageIndex,
                                                strokes = highlighterStrokes.toList(),
                                                canvasWidth = 1080f,
                                                canvasHeight = 1500f
                                            )
                                            if (success) {
                                                snackbarHostState.showSnackbar("Annotations saved permanently to PDF!")
                                                pageStrokesMap.remove(uiState.currentPageIndex)
                                                highlighterStrokes.clear()
                                                isMarkerActive = false
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to save annotation.")
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Format Strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        ReadingFormat.values().forEach { format ->
                            val isSelected = activeFormat == format
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    activeFormat = format
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Format mode: ${format.label}")
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    val icon = when (format) {
                                        ReadingFormat.CONTINUOUS -> Icons.Default.ViewStream
                                        ReadingFormat.FIT_WIDTH -> Icons.Default.FitScreen
                                        ReadingFormat.TEXT_REFLOW -> Icons.Default.WrapText
                                        ReadingFormat.AUTO_CROP -> Icons.Default.Crop
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = format.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PDF Document Viewport
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    uiState.errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "Failed to render PDF page",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    uiState.currentPageBitmap != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (activeFormat == ReadingFormat.TEXT_REFLOW) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 3.dp,
                                    color = activeTheme.pageBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Text Reflow Reader",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "PAGE ${uiState.currentPageIndex + 1} / $totalPages",
                                                style = MetricMonoSmall,
                                                color = activeTheme.textColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        if (isLoadingReflow) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.padding(vertical = 24.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                Text("Reflowing page typography...", style = MaterialTheme.typography.bodyMedium, color = activeTheme.textColor)
                                            }
                                        } else if (!reflowText.isNullOrBlank()) {
                                            Text(
                                                text = reflowText!!,
                                                style = MaterialTheme.typography.bodyLarge,
                                                lineHeight = 28.sp,
                                                color = activeTheme.textColor,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = "No readable text stream found on this page.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 3.dp,
                                    color = activeTheme.pageBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(
                                        if (activeFormat == ReadingFormat.FIT_WIDTH || activeFormat == ReadingFormat.AUTO_CROP) 0.dp else 10.dp
                                    )) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = doc?.name ?: "Document Page",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = "PAGE ${uiState.currentPageIndex + 1} / $totalPages",
                                                style = MetricMonoSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = activeTheme.textColor
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clipToBounds()
                                                .pointerInput(isMarkerActive) {
                                                    if (!isMarkerActive) {
                                                        detectTapGestures(
                                                            onDoubleTap = {
                                                                if (zoomScale > 1.05f) {
                                                                    zoomScale = 1f
                                                                    panOffset = Offset.Zero
                                                                } else {
                                                                    zoomScale = 2.5f
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                                .pointerInput(isMarkerActive) {
                                                    if (!isMarkerActive) {
                                                        detectTransformGestures { _, pan, zoom, _ ->
                                                            zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                                            if (zoomScale <= 1f) {
                                                                panOffset = Offset.Zero
                                                            } else {
                                                                val maxPanX = 500f * (zoomScale - 1f)
                                                                val maxPanY = 800f * (zoomScale - 1f)
                                                                panOffset = Offset(
                                                                    x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                                                    y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                .graphicsLayer(
                                                    scaleX = zoomScale,
                                                    scaleY = zoomScale,
                                                    translationX = panOffset.x,
                                                    translationY = panOffset.y
                                                )
                                        ) {
                                            // Determine ContentScale and padding based on activeFormat
                                            val imageContentScale = when (activeFormat) {
                                                ReadingFormat.AUTO_CROP -> ContentScale.Crop
                                                else -> ContentScale.FillWidth
                                            }
                                            val imagePadding = when (activeFormat) {
                                                ReadingFormat.FIT_WIDTH -> 0.dp
                                                ReadingFormat.CONTINUOUS -> 0.dp
                                                ReadingFormat.AUTO_CROP -> 0.dp
                                                else -> 0.dp
                                            }
                                            Image(
                                                bitmap = uiState.currentPageBitmap!!.asImageBitmap(),
                                                contentDescription = "Page ${uiState.currentPageIndex + 1}",
                                                contentScale = imageContentScale,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(imagePadding)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )

                                            // Interactive Highlighter Canvas
                                            Canvas(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .pointerInput(isMarkerActive) {
                                                        if (isMarkerActive) {
                                                            detectDragGestures(
                                                                onDragStart = { offset ->
                                                                    currentHighlighterStroke = listOf(offset)
                                                                },
                                                                onDrag = { change, _ ->
                                                                    change.consume()
                                                                    currentHighlighterStroke = currentHighlighterStroke + change.position
                                                                },
                                                                onDragEnd = {
                                                                    if (currentHighlighterStroke.isNotEmpty()) {
                                                                        highlighterStrokes.add(currentHighlighterStroke)
                                                                        pageStrokesMap[uiState.currentPageIndex] = highlighterStrokes.toList()
                                                                        currentHighlighterStroke = emptyList()
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                            ) {
                                                highlighterStrokes.forEach { stroke ->
                                                    if (stroke.size == 1) {
                                                        drawCircle(
                                                            color = Color(0x66FDE047),
                                                            radius = 13f,
                                                            center = stroke[0]
                                                        )
                                                    } else {
                                                        for (i in 0 until stroke.size - 1) {
                                                            drawLine(
                                                                color = Color(0x66FDE047),
                                                                start = stroke[i],
                                                                end = stroke[i + 1],
                                                                strokeWidth = 26f,
                                                                cap = StrokeCap.Round
                                                            )
                                                        }
                                                    }
                                                }
                                                if (currentHighlighterStroke.size == 1) {
                                                    drawCircle(
                                                        color = Color(0x66FDE047),
                                                        radius = 13f,
                                                        center = currentHighlighterStroke[0]
                                                    )
                                                } else {
                                                    for (i in 0 until currentHighlighterStroke.size - 1) {
                                                        drawLine(
                                                            color = Color(0x66FDE047),
                                                            start = currentHighlighterStroke[i],
                                                            end = currentHighlighterStroke[i + 1],
                                                            strokeWidth = 26f,
                                                            cap = StrokeCap.Round
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, start = 6.dp, end = 6.dp)
                                        ) {
                                            Text(
                                                text = "Local Storage",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Page ${uiState.currentPageIndex + 1} of $totalPages",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Next page peek indicator
                            if (uiState.currentPageIndex < totalPages - 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(2.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "END OF PAGE ${uiState.currentPageIndex + 1} • TAP NEXT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(2.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // Floating Jump & Page Controls (Right Dock)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 90.dp)
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(44.dp)
                    .clickable(enabled = uiState.currentPageIndex > 0) {
                        viewModel.prevPage()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous Page",
                        tint = if (uiState.currentPageIndex > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(44.dp)
                    .clickable(enabled = uiState.currentPageIndex < totalPages - 1) {
                        viewModel.nextPage()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next Page",
                        tint = if (uiState.currentPageIndex < totalPages - 1) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Floating Annotation Palette (Left Dock)
        Surface(
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 90.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                IconButton(
                    onClick = {
                        isMarkerActive = !isMarkerActive
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isMarkerActive) "Marker mode enabled. Touch & drag to highlight." else "Marker mode disabled")
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isMarkerActive) Color(0xFFFEF3C7) else Color.Transparent)
                ) {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = "Highlight Marker",
                        tint = if (isMarkerActive) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isMarkerActive && highlighterStrokes.isNotEmpty()) {
                    TextButton(onClick = { highlighterStrokes.clear() }) {
                        Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Ask AI dock icon
                IconButton(
                    onClick = { onNavigateToAiChat(documentId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Ask AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Page Scrubber Dock (Floating Capsule matching Stitch)
        Surface(
            shape = RoundedCornerShape(50),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { showThumbnailSheet = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.GridView, contentDescription = "Page Thumbnails", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${uiState.currentPageIndex + 1}",
                    style = MetricMono,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = sliderPage,
                    onValueChange = { newPage ->
                        sliderPage = newPage
                    },
                    onValueChangeFinished = {
                        val targetPage = (sliderPage.toInt() - 1).coerceIn(0, totalPages - 1)
                        viewModel.goToPage(targetPage)
                    },
                    valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                    steps = if (totalPages > 1) totalPages - 2 else 0,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = "$totalPages",
                    style = MetricMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        activeFormat = ReadingFormat.FIT_WIDTH
                        viewModel.goToPage(uiState.currentPageIndex)
                        scope.launch {
                            snackbarHostState.showSnackbar("Fit to screen: ${ReadingFormat.FIT_WIDTH.label}")
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.FitScreen, contentDescription = "Fit to Screen", modifier = Modifier.size(20.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        )

        // Modal Page Thumbnail Grid Sheet
        if (showThumbnailSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThumbnailSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Page Thumbnails", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$totalPages Total Pages", style = MetricMonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(totalPages) { pageIdx ->
                            val isCurrent = pageIdx == uiState.currentPageIndex
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                border = BorderStroke(
                                    if (isCurrent) 2.dp else 1.dp,
                                    if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .height(84.dp)
                                    .clickable {
                                        viewModel.goToPage(pageIdx)
                                        showThumbnailSheet = false
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Page ${pageIdx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

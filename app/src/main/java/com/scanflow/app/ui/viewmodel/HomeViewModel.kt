package com.scanflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class DocSortOption {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC
}

enum class DocFilterType {
    ALL,
    PDF,
    IMAGE
}

class HomeViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    val recentDocuments: StateFlow<List<Document>> = documentRepository
        .getRecentDocuments(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<Document>> = documentRepository
        .getFavoriteDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Document>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _sortOption = MutableStateFlow(DocSortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()

    private val _filterType = MutableStateFlow(DocFilterType.ALL)
    val filterType = _filterType.asStateFlow()

    val displayedDocuments: StateFlow<List<Document>> = combine(
        documentRepository.getAllDocuments(),
        _searchQuery,
        _sortOption,
        _filterType
    ) { docs, query, sort, filter ->
        var filtered = if (query.isBlank()) docs else docs.filter { it.name.contains(query, ignoreCase = true) }
        filtered = when (filter) {
            DocFilterType.PDF -> filtered.filter { it.mimeType.contains("pdf", ignoreCase = true) || it.name.endsWith(".pdf", ignoreCase = true) }
            DocFilterType.IMAGE -> filtered.filter { it.mimeType.contains("image", ignoreCase = true) || it.name.endsWith(".jpg", ignoreCase = true) || it.name.endsWith(".png", ignoreCase = true) }
            DocFilterType.ALL -> filtered
        }
        when (sort) {
            DocSortOption.DATE_DESC -> filtered.sortedByDescending { it.modifiedAt }
            DocSortOption.DATE_ASC -> filtered.sortedBy { it.modifiedAt }
            DocSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            DocSortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            DocSortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        } else {
            viewModelScope.launch {
                _searchResults.value = documentRepository.searchDocuments(query)
            }
        }
    }

    fun setSortOption(option: DocSortOption) {
        _sortOption.value = option
    }

    fun setFilterType(type: DocFilterType) {
        _filterType.value = type
    }

    fun importFile(file: File, mimeType: String = "application/pdf", pageCount: Int = 1) {
        viewModelScope.launch {
            val doc = Document(
                id = java.util.UUID.randomUUID().toString(),
                name = file.name,
                uri = file.toURI().toString(),
                path = file.absolutePath,
                sizeBytes = file.length(),
                pageCount = pageCount,
                mimeType = mimeType,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            documentRepository.insertDocument(doc)
        }
    }

    fun toggleFavorite(document: Document) {
        viewModelScope.launch {
            documentRepository.setFavorite(document.id, !document.isFavorite)
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
        }
    }

    fun renameDocument(id: String, newName: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(id, newName)
        }
    }
}

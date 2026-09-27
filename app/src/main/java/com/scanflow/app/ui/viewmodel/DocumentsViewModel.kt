package com.scanflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.model.Folder
import com.scanflow.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DocumentSortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC
}

enum class DocumentFilter {
    ALL,
    FAVORITES
}

class DocumentsViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(DocumentSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filter = MutableStateFlow(DocumentFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val folders: StateFlow<List<Folder>> = documentRepository
        .getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<Document>> = combine(
        documentRepository.getAllDocuments(),
        _filter,
        _sortOrder,
        _searchQuery
    ) { allDocs, currentFilter, currentSort, query ->
        var list = when (currentFilter) {
            DocumentFilter.ALL -> allDocs
            DocumentFilter.FAVORITES -> allDocs.filter { it.isFavorite }
        }

        if (query.isNotBlank()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }

        when (currentSort) {
            DocumentSortOrder.DATE_DESC -> list.sortedByDescending { it.modifiedAt }
            DocumentSortOrder.DATE_ASC -> list.sortedBy { it.modifiedAt }
            DocumentSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            DocumentSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            DocumentSortOrder.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOrder(order: DocumentSortOrder) {
        _sortOrder.value = order
    }

    fun setFilter(filter: DocumentFilter) {
        _filter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
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

    fun createFolder(name: String) {
        viewModelScope.launch {
            documentRepository.createFolder(name)
        }
    }
}

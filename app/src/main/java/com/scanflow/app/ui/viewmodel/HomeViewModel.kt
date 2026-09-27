package com.scanflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

package com.scanflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.scanflow.app.core.registry.FeatureCategory
import com.scanflow.app.core.registry.FeatureDefinition
import com.scanflow.app.core.registry.FeatureRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolsViewModel : ViewModel() {

    private val _selectedCategory = MutableStateFlow<FeatureCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun selectCategory(category: FeatureCategory?) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredFeatures(): List<FeatureDefinition> {
        val category = _selectedCategory.value
        val query = _searchQuery.value.trim()

        var list = if (category != null) {
            FeatureRegistry.getByCategory(category)
        } else {
            FeatureRegistry.getAll()
        }

        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.category.displayName.contains(query, ignoreCase = true)
            }
        }
        return list
    }
}

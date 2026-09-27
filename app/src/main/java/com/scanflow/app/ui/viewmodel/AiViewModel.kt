package com.scanflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.app.di.AppContainer
import com.scanflow.app.domain.model.Document
import com.scanflow.app.engine.AiAnswer
import com.scanflow.app.engine.AiDocumentInsight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AiMessage(
    val isUser: Boolean,
    val text: String,
    val citations: List<com.scanflow.app.engine.AiCitation> = emptyList()
)

data class AiUiState(
    val document: Document? = null,
    val summary: String? = null,
    val insights: AiDocumentInsight? = null,
    val messages: List<AiMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class AiViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            val doc = container.documentRepository.getDocumentById(documentId)
            if (doc != null) {
                _uiState.value = _uiState.value.copy(document = doc, isProcessing = true)
                val file = File(doc.path)

                // 1. Get insights
                val insights = container.aiUseCases.getInsights(file)
                // 2. Get summary
                val summary = container.aiUseCases.summarize(file)

                _uiState.value = _uiState.value.copy(
                    insights = insights,
                    summary = summary,
                    isProcessing = false
                )
            }
        }
    }

    fun askQuestion(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val state = _uiState.value
        val doc = state.document ?: return

        val userMsg = AiMessage(isUser = true, text = trimmed)
        _uiState.value = state.copy(
            messages = state.messages + userMsg,
            isProcessing = true
        )

        viewModelScope.launch {
            val answer = container.aiUseCases.ask(File(doc.path), trimmed)
            val aiMsg = AiMessage(
                isUser = false,
                text = answer.answer,
                citations = answer.citations
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + aiMsg,
                isProcessing = false
            )
        }
    }
}

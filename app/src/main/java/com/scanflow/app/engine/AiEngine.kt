package com.scanflow.app.engine

import java.io.File

data class AiDocumentInsight(
    val title: String,
    val wordCount: Int,
    val estimatedReadingTimeMinutes: Int,
    val detectedLanguage: String,
    val detectedDocumentType: String,
    val keyTopics: List<String>
)

data class AiCitation(
    val pageNumber: Int,
    val snippet: String
)

data class AiAnswer(
    val question: String,
    val answer: String,
    val citations: List<AiCitation> = emptyList(),
    val isLocalProcessing: Boolean = true
)

interface AiEngine {
    suspend fun summarizeDocument(file: File, maxBullets: Int = 5): String
    suspend fun askDocument(file: File, question: String): AiAnswer
    suspend fun getDocumentInsights(file: File): AiDocumentInsight
    suspend fun classifyDocument(file: File): String
    suspend fun translateDocument(file: File, targetLanguage: String = "id"): String
}

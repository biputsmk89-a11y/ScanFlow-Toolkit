package com.scanflow.app.domain.usecase

import com.scanflow.app.engine.AiAnswer
import com.scanflow.app.engine.AiDocumentInsight
import com.scanflow.app.engine.AiEngine
import java.io.File

fun sanitizePdfFileName(name: String): String {
    val clean = name.trim()
    return if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
}


/**
 * Domain Use Case for on-device AI document analysis.
 * Orchestrates document summarization, QA citations, insight extraction,
 * language translation, and document category classification.
 */
class AiUseCases(
    private val aiEngine: AiEngine
) {
    suspend fun summarize(file: File, maxBullets: Int = 5): String = aiEngine.summarizeDocument(file, maxBullets)
    suspend fun ask(file: File, query: String): AiAnswer = aiEngine.askDocument(file, query)
    suspend fun getInsights(file: File): AiDocumentInsight = aiEngine.getDocumentInsights(file)
    suspend fun translate(file: File, targetLanguage: String = "id"): String = aiEngine.translateDocument(file, targetLanguage)
    suspend fun classify(file: File): String = aiEngine.classifyDocument(file)
}

package com.scanflow.app.engine.impl

import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.engine.AiAnswer
import com.scanflow.app.engine.AiCitation
import com.scanflow.app.engine.AiDocumentInsight
import com.scanflow.app.engine.AiEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class AiEngineImpl : AiEngine {

    companion object {
        private const val TAG = "AiEngine"
    }

    override suspend fun summarizeDocument(file: File, maxBullets: Int): String = withContext(Dispatchers.Default) {
        val text = extractText(file)
        if (text.isBlank()) return@withContext "Document contains no readable text."

        // Split into sentences
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim().replace("\n", " ") }
            .filter { it.length in 30..300 }

        if (sentences.isEmpty()) return@withContext "Document has insufficient text for summarization."

        // Word frequency ranking
        val wordFreq = mutableMapOf<String, Int>()
        val stopWords = setOf("the", "and", "or", "to", "a", "in", "that", "is", "was", "for", "on", "with", "as", "by", "at", "an", "be", "this", "which", "from", "dan", "yang", "di", "ke", "dari", "ini", "itu", "untuk", "pada", "adalah")

        sentences.forEach { sentence ->
            sentence.lowercase().split(Regex("[^a-zA-Z0-9]+")).forEach { word ->
                if (word.length > 3 && word !in stopWords) {
                    wordFreq[word] = (wordFreq[word] ?: 0) + 1
                }
            }
        }

        // Score sentences by word frequency
        val scoredSentences = sentences.map { sentence ->
            var score = 0
            sentence.lowercase().split(Regex("[^a-zA-Z0-9]+")).forEach { word ->
                score += wordFreq[word] ?: 0
            }
            Pair(sentence, score)
        }

        val topSentences = scoredSentences
            .sortedByDescending { it.second }
            .take(maxBullets)
            .map { "• ${it.first}" }

        topSentences.joinToString("\n\n")
    }

    override suspend fun askDocument(file: File, question: String): AiAnswer = withContext(Dispatchers.Default) {
        val pagesText = extractPagesText(file)
        if (pagesText.isEmpty()) {
            return@withContext AiAnswer(
                question = question,
                answer = "No readable text found in document to answer the question.",
                citations = emptyList()
            )
        }

        val queryTerms = question.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 }.toSet()

        // Score each page
        val scoredPages = pagesText.map { (pageNum, pageText) ->
            val paragraphs = pageText.split("\n\n").filter { it.isNotBlank() }
            var bestParagraph = ""
            var bestScore = 0

            paragraphs.forEach { paragraph ->
                val words = paragraph.lowercase().split(Regex("[^a-zA-Z0-9]+")).toSet()
                val overlap = words.intersect(queryTerms).size
                if (overlap > bestScore) {
                    bestScore = overlap
                    bestParagraph = paragraph.trim().replace("\n", " ")
                }
            }

            Triple(pageNum, bestScore, bestParagraph)
        }

        val topMatch = scoredPages.maxByOrNull { it.second }

        if (topMatch != null && topMatch.second > 0) {
            val snippet = topMatch.third.take(250)
            val citation = AiCitation(pageNumber = topMatch.first, snippet = snippet)
            AiAnswer(
                question = question,
                answer = "Based on page ${topMatch.first}: \"$snippet...\"",
                citations = listOf(citation),
                isLocalProcessing = true
            )
        } else {
            AiAnswer(
                question = question,
                answer = "The document does not appear to contain relevant sections answering: \"$question\".",
                citations = emptyList(),
                isLocalProcessing = true
            )
        }
    }

    override suspend fun getDocumentInsights(file: File): AiDocumentInsight = withContext(Dispatchers.Default) {
        val text = extractText(file)
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val readingTime = max(1, (wordCount / 200))
        val classification = classifyDocument(file)

        AiDocumentInsight(
            title = file.nameWithoutExtension.replace("_", " ").capitalizeWords(),
            wordCount = wordCount,
            estimatedReadingTimeMinutes = readingTime,
            detectedLanguage = detectLanguage(text),
            detectedDocumentType = classification,
            keyTopics = extractKeyTopics(words)
        )
    }

    override suspend fun classifyDocument(file: File): String = withContext(Dispatchers.Default) {
        val text = extractText(file).lowercase()
        when {
            text.contains("invoice") || text.contains("faktur") || text.contains("tagihan") -> "Invoice / Billing"
            text.contains("receipt") || text.contains("kwitansi") || text.contains("struk") -> "Receipt / Payment"
            text.contains("agreement") || text.contains("contract") || text.contains("perjanjian") -> "Legal Contract"
            text.contains("resume") || text.contains("curriculum vitae") || text.contains("riwayat hidup") -> "Resume / CV"
            text.contains("report") || text.contains("laporan") || text.contains("executive summary") -> "Business Report"
            text.contains("certificate") || text.contains("sertifikat") || text.contains("ijazah") -> "Certificate / Diploma"
            else -> "General Document"
        }
    }

    private fun extractText(file: File): String {
        return try {
            PDDocument.load(file).use { doc ->
                PDFTextStripper().getText(doc)
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to extract text for AI: ${e.message}")
            ""
        }
    }

    private fun extractPagesText(file: File): List<Pair<Int, String>> {
        val list = mutableListOf<Pair<Int, String>>()
        try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                for (p in 1..doc.numberOfPages) {
                    stripper.startPage = p
                    stripper.endPage = p
                    list.add(Pair(p, stripper.getText(doc)))
                }
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to extract per-page text for AI: ${e.message}")
        }
        return list
    }

    private fun detectLanguage(text: String): String {
        val indonesianWords = setOf("yang", "dan", "dengan", "untuk", "dari", "dalam", "ini", "itu", "atau", "adalah")
        val words = text.lowercase().split(Regex("\\s+")).take(200)
        val indonesianMatches = words.count { it in indonesianWords }
        return if (indonesianMatches >= 3) "Indonesian (id)" else "English (en)"
    }

    private fun extractKeyTopics(words: List<String>): List<String> {
        val counts = mutableMapOf<String, Int>()
        val skip = setOf("this", "that", "with", "from", "have", "were", "which", "there", "their", "about", "would", "these", "other")
        words.forEach { w ->
            val clean = w.lowercase().filter { it.isLetter() }
            if (clean.length in 5..15 && clean !in skip) {
                counts[clean] = (counts[clean] ?: 0) + 1
            }
        }
        return counts.entries.sortedByDescending { it.value }.take(5).map { it.key.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

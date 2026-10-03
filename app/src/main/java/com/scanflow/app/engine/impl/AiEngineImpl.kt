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

class AiEngineImpl(
    private val ocrEngine: com.scanflow.app.engine.OcrEngine? = null
) : AiEngine {

    companion object {
        private const val TAG = "AiEngine"
    }

    override suspend fun summarizeDocument(file: File, maxBullets: Int): String = withContext(Dispatchers.Default) {
        val text = extractText(file)
        if (text.isBlank()) return@withContext "Document contains no readable text."
        processTextSummary(text, maxBullets)
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
        processAsk(pagesText, question)
    }

    override suspend fun getDocumentInsights(file: File): AiDocumentInsight = withContext(Dispatchers.Default) {
        val text = extractText(file)
        processDocumentInsights(text, file.nameWithoutExtension)
    }

    override suspend fun classifyDocument(file: File): String = withContext(Dispatchers.Default) {
        val text = extractText(file)
        processClassify(text)
    }

    override suspend fun translateDocument(file: File, targetLanguage: String): String = withContext(Dispatchers.Default) {
        val text = extractText(file)
        if (text.isBlank()) return@withContext "Document contains no extractable text for translation."
        processTranslation(text, targetLanguage, file.name)
    }

    // --- Modular, fully unit-testable algorithmic implementations ---

    fun processTextSummary(text: String, maxBullets: Int = 5): String {
        if (text.isBlank()) return "Document contains no readable text."

        // Split into sentences
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim().replace("\n", " ") }
            .filter { it.length in 30..300 }

        if (sentences.isEmpty()) return "Document has insufficient text for summarization."

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

        return topSentences.joinToString("\n\n")
    }

    fun processAsk(pagesText: List<Pair<Int, String>>, question: String): AiAnswer {
        if (pagesText.isEmpty()) {
            return AiAnswer(
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

        return if (topMatch != null && topMatch.second > 0) {
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

    fun processDocumentInsights(text: String, titleFallback: String): AiDocumentInsight {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val readingTime = max(1, (wordCount / 200))
        val classification = processClassify(text)

        return AiDocumentInsight(
            title = titleFallback.replace("_", " ").capitalizeWords(),
            wordCount = wordCount,
            estimatedReadingTimeMinutes = readingTime,
            detectedLanguage = detectLanguage(text),
            detectedDocumentType = classification,
            keyTopics = extractKeyTopics(words)
        )
    }

    fun processClassify(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("invoice") || lower.contains("faktur") || lower.contains("tagihan") -> "Invoice / Billing"
            lower.contains("receipt") || lower.contains("kwitansi") || lower.contains("struk") -> "Receipt / Payment"
            lower.contains("agreement") || lower.contains("contract") || lower.contains("perjanjian") -> "Legal Contract"
            lower.contains("resume") || lower.contains("curriculum vitae") || lower.contains("riwayat hidup") -> "Resume / CV"
            lower.contains("report") || lower.contains("laporan") || lower.contains("executive summary") -> "Business Report"
            lower.contains("certificate") || lower.contains("sertifikat") || lower.contains("ijazah") -> "Certificate / Diploma"
            else -> "General Document"
        }
    }

    fun processTranslation(text: String, targetLanguage: String, sourceFileName: String = "document.pdf"): String {
        if (text.isBlank()) return "Document contains no extractable text for translation."

        val isTargetIndonesian = targetLanguage.equals("id", ignoreCase = true) || targetLanguage.contains("indo", ignoreCase = true)

        val enToIdDictionary = mapOf(
            "invoice" to "faktur", "bill" to "tagihan", "receipt" to "bukti pembayaran",
            "agreement" to "perjanjian", "contract" to "kontrak", "summary" to "ringkasan",
            "report" to "laporan", "overview" to "ikhtisar", "document" to "dokumen",
            "confidential" to "rahasia", "date" to "tanggal", "amount" to "jumlah",
            "total" to "total", "subtotal" to "subtotal", "payment" to "pembayaran",
            "due date" to "jatuh tempo", "signature" to "tanda tangan", "approved" to "disetujui",
            "status" to "status", "pending" to "tertunda", "completed" to "selesai",
            "customer" to "pelanggan", "vendor" to "pemasok", "client" to "klien",
            "account" to "rekening", "balance" to "saldo", "tax" to "pajak",
            "discount" to "diskon", "terms" to "syarat dan ketentuan", "conditions" to "kondisi",
            "parties" to "para pihak", "hereby" to "dengan ini", "agrees" to "menyetujui",
            "service" to "layanan", "product" to "produk", "description" to "deskripsi",
            "quantity" to "kuantitas", "price" to "harga", "rate" to "tarif",
            "notes" to "catatan", "reference" to "referensi", "address" to "alamat",
            "phone" to "telepon", "email" to "surel", "valid until" to "berlaku hingga",
            "authorized" to "berwenang", "representative" to "perwakilan", "purpose" to "tujuan",
            "background" to "latar belakang", "conclusion" to "kesimpulan", "recommendation" to "rekomendasi",
            "education" to "pendidikan", "experience" to "pengalaman", "skills" to "keahlian",
            "projects" to "proyek", "certifications" to "sertifikasi", "languages" to "bahasa",
            "meeting" to "pertemuan", "agenda" to "agenda", "minutes" to "notula",
            "financial" to "keuangan", "statement" to "laporan", "audit" to "audit",
            "policy" to "kebijakan", "privacy" to "privasi", "security" to "keamanan"
        )

        val idToEnDictionary = enToIdDictionary.entries.associate { (k, v) -> v to k }
        val dict = if (isTargetIndonesian) enToIdDictionary else idToEnDictionary

        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
        val translatedParagraphs = paragraphs.take(15).map { paragraph ->
            var translated = paragraph
            dict.forEach { (src, dst) ->
                val regex = Regex("\\b(?i)${Regex.escape(src)}\\b")
                translated = translated.replace(regex) { matchResult ->
                    if (matchResult.value.first().isUpperCase()) {
                        dst.replaceFirstChar { it.uppercase() }
                    } else {
                        dst
                    }
                }
            }
            translated
        }

        val header = if (isTargetIndonesian) {
            "--- TERJEMAHAN DOKUMEN OFFLINE (ID) ---\nSumber Dokumen: $sourceFileName\nBahasa Sasaran: Bahasa Indonesia\n\n"
        } else {
            "--- OFFLINE DOCUMENT TRANSLATION (EN) ---\nSource Document: $sourceFileName\nTarget Language: English\n\n"
        }

        return header + translatedParagraphs.joinToString("\n\n")
    }

    private suspend fun extractText(file: File): String {
        val ext = file.extension.lowercase()

        // For plain text / CSV files, read directly — do NOT pass to PDFBox
        if (ext in listOf("txt", "csv", "md", "log")) {
            return try {
                file.readText(Charsets.UTF_8).trim()
            } catch (e: Exception) {
                SafeLogger.w(TAG, "Failed to read text file for AI: ${e.message}")
                ""
            }
        }

        // PDF files — use PDFBox
        var text = ""
        try {
            PDDocument.load(file).use { doc ->
                text = PDFTextStripper().getText(doc).trim()
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to extract text for AI: ${e.message}")
        }
        if (text.isBlank() && ocrEngine != null) {
            try {
                val ocrResult = ocrEngine.recognizePdf(file)
                if (ocrResult.fullText.isNotBlank()) {
                    text = ocrResult.fullText.trim()
                }
            } catch (e: Exception) {
                SafeLogger.w(TAG, "OCR fallback failed for AI: ${e.message}")
            }
        }
        return text
    }

    private suspend fun extractPagesText(file: File): List<Pair<Int, String>> {
        val ext = file.extension.lowercase()

        // For plain text / CSV files, treat entire content as page 1
        if (ext in listOf("txt", "csv", "md", "log")) {
            return try {
                val content = file.readText(Charsets.UTF_8).trim()
                if (content.isNotBlank()) listOf(Pair(1, content)) else emptyList()
            } catch (e: Exception) {
                SafeLogger.w(TAG, "Failed to read text file for AI pages: ${e.message}")
                emptyList()
            }
        }

        // PDF files — use PDFBox per-page extraction
        val list = mutableListOf<Pair<Int, String>>()
        try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                for (p in 1..doc.numberOfPages) {
                    stripper.startPage = p
                    stripper.endPage = p
                    val pageText = stripper.getText(doc).trim()
                    if (pageText.isNotBlank()) {
                        list.add(Pair(p, pageText))
                    }
                }
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to extract per-page text for AI: ${e.message}")
        }
        if (list.isEmpty() && ocrEngine != null) {
            try {
                val ocrResult = ocrEngine.recognizePdf(file)
                ocrResult.pageResults.forEach { pr ->
                    if (pr.fullText.isNotBlank()) {
                        list.add(Pair(pr.pageNumber, pr.fullText.trim()))
                    }
                }
            } catch (e: Exception) {
                SafeLogger.w(TAG, "OCR fallback per-page failed for AI: ${e.message}")
            }
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

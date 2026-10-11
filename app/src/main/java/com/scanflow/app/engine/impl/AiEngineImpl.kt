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
        val cleanedText = OcrTextCorrector.correctDocumentText(text)

        // Split into candidate lines / sentences
        val rawSentences = cleanedText.split(Regex("(?<=[.!?])\\s+|\n+"))
            .map { it.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.length in 15..350 && !it.startsWith("--- Page") }

        if (rawSentences.isEmpty()) return "Document has insufficient text for summarization."

        // Detect structured segments
        val definitions = mutableListOf<String>()
        val keyPoints = mutableListOf<String>()
        val questions = mutableListOf<String>()
        val generalBullets = mutableListOf<String>()

        rawSentences.forEach { s ->
            val lower = s.lowercase()
            val cleanSentence = s.removePrefix("•").removePrefix("-").trim()
            val bulletItem = "• $cleanSentence"

            when {
                // Questions / exercises
                s.endsWith("?") || lower.startsWith("sebutkan") || lower.startsWith("jelaskan") || lower.startsWith("apa ") || lower.startsWith("bagaimana") -> {
                    if (questions.size < 3 && !questions.contains(bulletItem)) {
                        questions.add(bulletItem)
                    }
                }
                // Definitions
                (lower.contains(" adalah ") || lower.contains(" merupakan ") || lower.contains(" is defined as ") || lower.contains(" refers to ")) && !s.endsWith("?") -> {
                    if (definitions.size < 2 && !definitions.contains(bulletItem)) {
                        definitions.add(bulletItem)
                    }
                }
                // Key Points / Numbered items / Principles
                (s.matches(Regex("""^\d+[\.:\)].*""")) || lower.startsWith("sebagai ") || lower.contains("asas ") || lower.contains("karakteristik ") || lower.contains("fungsi ")) && !s.endsWith("?") -> {
                    if (keyPoints.size < 4 && !keyPoints.contains(bulletItem)) {
                        keyPoints.add(bulletItem)
                    }
                }
                else -> {
                    generalBullets.add(bulletItem)
                }
            }
        }

        // If structured items were identified, construct an executive summary
        if (definitions.isNotEmpty() || keyPoints.isNotEmpty() || questions.isNotEmpty()) {
            val sections = mutableListOf<String>()
            if (definitions.isNotEmpty()) {
                sections.add("📌 Definisi / Konsep Utama:\n" + definitions.joinToString("\n"))
            }
            if (keyPoints.isNotEmpty()) {
                sections.add("🔑 Poin Kunci & Karakteristik:\n" + keyPoints.joinToString("\n"))
            }
            if (questions.isNotEmpty()) {
                sections.add("📝 Pertanyaan & Evaluasi Dokumen:\n" + questions.joinToString("\n"))
            }
            return sections.joinToString("\n\n")
        }

        // Standard frequency-based summarizer fallback
        val wordFreq = mutableMapOf<String, Int>()
        val stopWords = setOf("the", "and", "or", "to", "a", "in", "that", "is", "was", "for", "on", "with", "as", "by", "at", "an", "be", "this", "which", "from", "dan", "yang", "di", "ke", "dari", "ini", "itu", "untuk", "pada", "adalah")

        rawSentences.forEach { sentence ->
            sentence.lowercase().split(Regex("[^a-zA-Z0-9]+")).forEach { word ->
                if (word.length > 3 && word !in stopWords) {
                    wordFreq[word] = (wordFreq[word] ?: 0) + 1
                }
            }
        }

        val scoredSentences = rawSentences.map { sentence ->
            var score = 0
            sentence.lowercase().split(Regex("[^a-zA-Z0-9]+")).forEach { word ->
                score += wordFreq[word] ?: 0
            }
            Pair(sentence, score)
        }

        val topSentences = scoredSentences
            .sortedByDescending { it.second }
            .take(maxBullets)
            .map { "• ${it.first.removePrefix("•").removePrefix("-").trim()}" }

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

        val cleanedQuestion = OcrTextCorrector.correctLine(question)
        val qLower = cleanedQuestion.lowercase()

        // Extract query terms (filter out question stopwords)
        val questionStopWords = setOf(
            "apa", "itu", "yang", "dimaksud", "dengan", "bagaimana", "mengapa", "kenapa", "sebutkan", "jelaskan",
            "tuliskan", "berapa", "adakah", "apakah", "pada", "dari", "dalam", "untuk",
            "what", "is", "the", "are", "how", "why", "when", "where", "which", "does", "can", "tell", "about"
        )
        val queryTerms = qLower.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && it !in questionStopWords }
            .toSet()

        val effectiveQueryTerms = if (queryTerms.isEmpty()) {
            qLower.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 }.toSet()
        } else {
            queryTerms
        }

        var bestPageNumber = 1
        var bestParagraph = ""
        var bestScore = 0

        pagesText.forEach { (pageNum, rawPageText) ->
            val pageText = OcrTextCorrector.correctDocumentText(rawPageText)
            val paragraphs = pageText.split("\n\n").filter { it.isNotBlank() }

            paragraphs.forEach { paragraph ->
                val pLower = paragraph.lowercase()
                val pWords = pLower.split(Regex("[^a-zA-Z0-9]+")).toSet()

                var score = 0
                // Exact term matches
                score += pWords.intersect(effectiveQueryTerms).size * 4

                // Fuzzy matches (Levenshtein <= 1 or prefix match)
                effectiveQueryTerms.forEach { qTerm ->
                    if (pWords.none { it == qTerm }) {
                        if (pWords.any { it.startsWith(qTerm) || qTerm.startsWith(it) || OcrTextCorrector.levenshteinDistance(it, qTerm) <= 1 }) {
                            score += 2
                        }
                    }
                }

                // Intent matching bonus
                if ((qLower.contains("apa") || qLower.contains("definisi") || qLower.contains("what is")) &&
                    (pLower.contains("adalah") || pLower.contains("merupakan") || pLower.contains("refers to") || pLower.contains("is defined as"))) {
                    score += 6
                }
                if (qLower.contains("asas")) {
                    if (pLower.contains("asas")) score += 4
                    if (pLower.contains("sentralisasi") || pLower.contains("desentralisasi") || pLower.contains("gabungan")) {
                        score += 8
                    }
                }
                if (qLower.contains("karakteristik") && (pLower.contains("karakteristik") || pLower.contains("autentik") || pLower.contains("unik"))) {
                    score += 6
                }
                // Root word match: kearsipan / pengarsipan -> arsip
                if ((qLower.contains("kearsipan") || qLower.contains("arsip")) && (pLower.contains("arsip") || pLower.contains("pengarsipan"))) {
                    score += 4
                }

                // If paragraph is merely an exercise question ending in '?' or starting with 'sebutkan', penalize so informative content is preferred
                if ((paragraph.trim().endsWith("?") || pLower.startsWith("sebutkan") || pLower.startsWith("• sebutkan")) &&
                    !qLower.contains("pertanyaan") && !qLower.contains("soal")) {
                    score -= 8
                }

                if (score > bestScore) {
                    bestScore = score
                    bestParagraph = paragraph.trim().replace("\n", " ")
                    bestPageNumber = pageNum
                }
            }
        }

        return if (bestScore > 0 && bestParagraph.isNotBlank()) {
            val snippet = bestParagraph.take(350)
            val citation = AiCitation(pageNumber = bestPageNumber, snippet = snippet)

            val synthesizedAnswer = if (isIndonesianQuery(question)) {
                "Berdasarkan dokumen (Halaman $bestPageNumber):\n\"$snippet...\""
            } else {
                "Based on page $bestPageNumber: \"$snippet...\""
            }

            AiAnswer(
                question = question,
                answer = synthesizedAnswer,
                citations = listOf(citation),
                isLocalProcessing = true
            )
        } else {
            val notFoundAnswer = if (isIndonesianQuery(question)) {
                "Dokumen tidak memuat informasi yang spesifik untuk menjawab: \"$question\"."
            } else {
                "The document does not appear to contain relevant sections answering: \"$question\"."
            }
            AiAnswer(
                question = question,
                answer = notFoundAnswer,
                citations = emptyList(),
                isLocalProcessing = true
            )
        }
    }

    private fun isIndonesianQuery(query: String): Boolean {
        val idWords = setOf("apa", "siapa", "bagaimana", "mengapa", "sebutkan", "jelaskan", "apakah", "arsip", "adalah")
        val lower = query.lowercase()
        return idWords.any { lower.contains(it) }
    }

    fun processDocumentInsights(text: String, titleFallback: String): AiDocumentInsight {
        val cleanedText = OcrTextCorrector.correctDocumentText(text)
        val words = cleanedText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val readingTime = max(1, (wordCount / 200))
        val classification = processClassify(cleanedText)

        return AiDocumentInsight(
            title = titleFallback.replace("_", " ").capitalizeWords(),
            wordCount = wordCount,
            estimatedReadingTimeMinutes = readingTime,
            detectedLanguage = detectLanguage(cleanedText),
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
            lower.contains("arsip") || lower.contains("kearsipan") || lower.contains("pengarsipan") || lower.contains("tata naskah") -> "Dokumen Kearsipan / Administrasi"
            lower.contains("sebutkan") || lower.contains("jelaskan") || lower.contains("ujian") || lower.contains("soal") -> "Materi Pembelajaran / Ujian"
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

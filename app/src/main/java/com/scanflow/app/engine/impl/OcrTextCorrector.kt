package com.scanflow.app.engine.impl

import java.util.Locale
import kotlin.math.min

/**
 * High-performance, offline OCR text corrector and normalizer.
 * Corrects typical mobile scanner OCR misrecognitions, character confusion,
 * casing glitches, split words, and OCR artifact bullet points for Indonesian
 * and English documents.
 */
object OcrTextCorrector {

    // Regex-based phrase & word corrections for OCR mangled text
    private val PATTERN_CORRECTIONS = listOf(
        // Prefixes & Bullet fixes
        Regex("""(?i)^(?:SS|S)\s*ebut(?:\s*kan|kan)""") to "• Sebutkan",
        Regex("""(?i)^LY\s*sebut(?:\s*kan|kan)""") to "• Sebutkan",
        Regex("""(?i)^(?:LY|ss|SS|yy|YY)\s*(?=sebut\s*kan|sebutkan|jelaskan|apa|bagaimana|mengapa|tuliskan)""") to "• ",
        Regex("""(?i)\bsebut\s+kan\b""") to "sebutkan",
        Regex("""(?i)\balas\s+(?=kearsipan|arsip|sentralisasi)""") to "asas ",
        Regex("""(?i)\b(sebutkan|jelaskan|tuliskan)(\d+)\b""") to "$1 $2",

        // Split phrases & compounds
        Regex("""(?i)\bleoMUni\s+kasi\b""") to "komunikasi",
        Regex("""(?i)\bkomuni\s+kasi\b""") to "komunikasi",
        Regex("""(?i)\bSPenciP\s+taan\b""") to "penciptaan",
        Regex("""(?i)\bpencip\s+taan\b""") to "penciptaan",
        Regex("""(?i)\bee\s*Rarsi\b""") to "kearsipan",
        Regex("""(?i)\bogoni\s*Sas\b""") to "organisasi",
        Regex("""(?i)\borgani\s*sasi\b""") to "organisasi",
        Regex("""(?i)\binfor\s*masi\b""") to "informasi",
        Regex("""(?i)\bter\s*percaya\b""") to "terpercaya",
        Regex("""(?i)\bde\s*sentralisasi\b""") to "desentralisasi",
        Regex("""(?i)\bsentra\s*lisasi\b""") to "sentralisasi",
        Regex("""(?i)\bpeng\s*urusan\b""") to "pengurusan",
        Regex("""(?i)\bpeng\s*arsipan\b""") to "pengarsipan",
        Regex("""(?i)\bpeng\s*endalian\b""") to "pengendalian",
        Regex("""(?i)\bkarak\s*teristik\b""") to "karakteristik",

        // Specific high-frequency Indonesian OCR confusion patterns
        Regex("""(?i)\bo4a[\\/l]ah\b""") to "adalah",
        Regex("""(?i)\bo4alah\b""") to "adalah",
        Regex("""(?i)\badaiah\b""") to "adalah",
        Regex("""(?i)\bada1ah\b""") to "adalah",
        Regex("""(?i)\bsit[uU]is[Jj]i\b""") to "ditulis",
        Regex("""(?i)\bsit[uU]isi\b""") to "ditulis",
        Regex("""(?i)\bditu1is\b""") to "ditulis",
        Regex("""(?i)\b[Jj]ik[Pp]tik\b""") to "diketik",
        Regex("""(?i)\bdik[Pp]tik\b""") to "diketik",
        Regex("""(?i)\bdiketlk\b""") to "diketik",
        Regex("""(?i)\bbentuw\b""") to "bentuk",
        Regex("""(?i)\burup\b""") to "huruf",
        Regex("""(?i)\bhurup\b""") to "huruf",
        Regex("""(?i)\b[Qq]a[Mm]bor\b""") to "gambar",
        Regex("""(?i)\b[Qq]ambar\b""") to "gambar",
        Regex("""(?i)\bgambor\b""") to "gambar",
        Regex("""(?i)\byong\b""") to "yang",
        Regex("""(?i)\b[Mm]e[Mm][Pp][Uu]na\b""") to "mempunyai",
        Regex("""(?i)\bmempunyal\b""") to "mempunyai",
        Regex("""(?i)\bart[l1]\b""") to "arti",
        Regex("""(?i)\btuluan\b""") to "tujuan",
        Regex("""(?i)\bsbogai\b""") to "sebagai",
        Regex("""(?i)\bselagai\b""") to "sebagai",
        Regex("""(?i)\bsegai\b""") to "sebagai",
        Regex("""(?i)\baict\b""") to "alat",
        Regex("""(?i)\baat\b""") to "alat",
        Regex("""(?i)\bpereantar\b""") to "perantara",
        Regex("""(?i)\btagi\b""") to "bagi",
        Regex("""(?i)\bogoni[Ss]as\b""") to "organisasi",
        Regex("""(?i)\bogonisasi\b""") to "organisasi",
        Regex("""(?i)\bbwwti\b""") to "bukti",
        Regex("""(?i)\bbuktl\b""") to "bukti",
        Regex("""(?i)\bautenik\b""") to "autentik",
        Regex("""(?i)\bterpeeaya\b""") to "terpercaya",
        Regex("""(?i)\by[Aa]sos\b""") to "Asas",
        Regex("""(?i)\b[Aa]cas\b""") to "Asas",
        Regex("""(?i)\bsentraisaci\b""") to "sentralisasi",
        Regex("""(?i)\b[Pp]esentausasi\b""") to "desentralisasi",
        Regex("""(?i)\bgobun9an\b""") to "gabungan",
        Regex("""(?i)\bgobungan\b""") to "gabungan",
        Regex("""(?i)\botqu\b""") to "ataupun",
        Regex("""(?i)\b[Oo][Hh]unika\b""") to "autentikasi",
        Regex("""(?i)\b[Ss][Pp]enci[Pp]\s*taan\b""") to "penciptaan",
        Regex("""(?i)\b[Pp]ergarusan\b""") to "pengurusan",
        Regex("""(?i)\baan\b""") to "dan",
        Regex("""(?i)\b[Pp]ergenian\b""") to "pengendalian",
        Regex("""(?i)\b[Pp]engi[Rr]aon\b""") to "pengarsipan",
        Regex("""(?i)\bassir\b""") to "arsip",
        Regex("""(?i)\barsif\b""") to "arsip",
        Regex("""(?i)\bee[Rr]arsi\b""") to "kearsipan",
        Regex("""(?i)\bpon\b""") to "dan",
        Regex("""(?i)\b[Ss]lutkan\b""") to "sebutkan",
        Regex("""(?i)\bjaur\b""") to "jalur",
        Regex("""(?i)\bwarsilan\b""") to "warisan",
        Regex("""(?i)\bkarauteristik\b""") to "karakteristik",
        Regex("""(?i)\b[Ll]nfo[Mm]as\b""") to "informasi",
        Regex("""(?i)\binfomas\b""") to "informasi",
        Regex("""(?i)\beream\b""") to "terekam",
        Regex("""(?i)\b[Pp]aa\b""") to "pada",
        Regex("""(?i)\bberbogai\b""") to "berbagai",
        Regex("""(?i)\bmejia\b""") to "media",
        Regex("""(?i)\bjan\b""") to "dan"
    )

    // Standard Indonesian vocabulary dictionary for fuzzy spell check
    private val INDONESIAN_VOCAB = setOf(
        "arsip", "kearsipan", "pengarsipan", "dokumen", "penciptaan", "pengurusan",
        "pengendalian", "karakteristik", "asas", "sentralisasi", "desentralisasi",
        "gabungan", "autentik", "unik", "terpercaya", "organisasi", "perantara",
        "informasi", "komunikasi", "media", "terekam", "berbagai", "catatan",
        "tertulis", "ditulis", "dicetak", "diketik", "bentuk", "huruf", "angka",
        "gambar", "mempunyai", "arti", "tujuan", "sebagai", "bahan", "bukti",
        "sebutkan", "jelaskan", "pertanyaan", "definisi", "fungsi", "tugas",
        "administrasi", "perkantoran", "laporan", "surat", "keputusan", "peraturan",
        "undang", "pasal", "halaman", "tersebut", "tentang", "dengan", "adalah",
        "alat", "bagi", "pada", "dan", "atau", "dari", "dalam", "jalur", "warisan",
        "berkas", "lembar", "kantor", "data", "pemerintah", "resmi", "tata", "naskah",
        "yang", "juga", "akan", "oleh", "bisa", "tidak", "sudah", "karena", "serta",
        "antara", "setiap", "semua", "banyak", "lebih", "sangat", "dapat", "secara"
    )

    /**
     * Cleans and restores an entire document text string.
     */
    fun correctDocumentText(rawText: String): String {
        if (rawText.isBlank()) return ""

        val lines = rawText.lines()
        val correctedLines = lines.map { correctLine(it) }
        return correctedLines.joinToString("\n")
    }

    /**
     * Cleans and normalizes a single OCR text line.
     */
    fun correctLine(line: String): String {
        var text = line.trim()
        if (text.isEmpty()) return ""

        // 1. Convert initial numbering with dash or dollar: "2-$eLagai" -> "2. Sebagai"
        text = text.replace(Regex("""^(\d+)[\-\$]+(?:\$|s|S)?(?=[a-zA-Z])""")) { match ->
            "${match.groupValues[1]}. S"
        }
        text = text.replace(Regex("""^(\d+)[\-\s]+(?=[a-zA-Z])""")) { match ->
            "${match.groupValues[1]}. "
        }

        // 2. Apply pattern and phrase corrections
        PATTERN_CORRECTIONS.forEach { (pattern, replacement) ->
            text = text.replace(pattern, replacement)
        }

        // 3. Tokenize and fix casing/fuzzy matching
        val tokens = text.split(" ")
        val cleanedTokens = tokens.mapIndexed { idx, token ->
            val isBulletFollower = idx > 0 && (tokens[idx - 1] in listOf("•", "-", "*") || tokens[idx - 1].matches(Regex("""\d+[\.:\)]""")))
            cleanWordToken(token, isFirstWord = idx == 0 || isBulletFollower)
        }

        var result = cleanedTokens.joinToString(" ")

        // 4. Post-clean punctuation spacing
        result = result
            .replace(Regex("""\s+,"""), ",")
            .replace(Regex("""\s+\."""), ".")
            .replace(Regex("""\s+\?"""), "?")
            .replace(Regex("""\s+:"""), ":")
            .replace(Regex("""\s+;"""), ";")
            .replace(Regex("""\s{2,}"""), " ")

        // 5. Ensure sentence starting with letter is capitalized
        if (result.isNotEmpty() && result[0].isLowerCase()) {
            result = result.replaceFirstChar { it.uppercase() }
        }

        return result
    }

    /**
     * Cleans an individual word token: casing normalization, punctuation preservation,
     * and Levenshtein fuzzy match.
     */
    private fun cleanWordToken(token: String, isFirstWord: Boolean): String {
        if (token.isBlank()) return ""
        if (token.none { it.isLetterOrDigit() }) return token

        val leadingPunct = token.takeWhile { !it.isLetterOrDigit() }
        val trailingPunct = token.reversed().takeWhile { !it.isLetterOrDigit() }.reversed()

        val core = token.substring(leadingPunct.length, token.length - trailingPunct.length)
        if (core.isEmpty()) return token

        // Normalize erratic internal casing like "arsiP" -> "Arsip"
        val normalizedCore = normalizeCasing(core)
        val lowerCore = normalizedCore.lowercase(Locale.ROOT)

        var candidate = if (lowerCore.length >= 4 && lowerCore !in INDONESIAN_VOCAB) {
            findFuzzyMatch(lowerCore) ?: normalizedCore
        } else {
            normalizedCore
        }

        val shouldCapitalize = (isFirstWord && leadingPunct.none { it == '•' || it == '-' }) ||
                (core.isNotEmpty() && core[0].isUpperCase() && core.count { it.isUpperCase() } <= 2)

        if (shouldCapitalize && candidate.isNotEmpty()) {
            candidate = candidate.replaceFirstChar { it.uppercase() }
        }

        return "$leadingPunct$candidate$trailingPunct"
    }

    /**
     * Normalizes casing for words with chaotic OCR capitalization like "arsiP".
     * Preserves real acronyms like "PDF", "AI", "KTP", "SIM".
     */
    private fun normalizeCasing(word: String): String {
        if (word.length <= 1) return word

        val upperCount = word.count { it.isUpperCase() }
        val lowerCount = word.count { it.isLowerCase() }

        if (lowerCount == 0 && word.length <= 4) return word

        if (upperCount > 0 && lowerCount > 0) {
            val isFirstUpper = word[0].isUpperCase()
            val internalUpperCount = word.substring(1).count { it.isUpperCase() }

            if (internalUpperCount > 0) {
                return if (isFirstUpper) {
                    word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
                } else {
                    word.lowercase(Locale.ROOT)
                }
            }
        }

        return word
    }

    /**
     * Finds closest vocabulary word using Levenshtein distance (max distance 2).
     */
    private fun findFuzzyMatch(target: String): String? {
        var bestMatch: String? = null
        var bestDistance = 3

        for (vocab in INDONESIAN_VOCAB) {
            if (kotlin.math.abs(vocab.length - target.length) > 2) continue

            val dist = levenshteinDistance(target, vocab)
            if (dist < bestDistance) {
                bestDistance = dist
                bestMatch = vocab
            }
        }

        return bestMatch
    }

    /**
     * Fast iterative Levenshtein distance calculation.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len0 = s1.length + 1
        val len1 = s2.length + 1

        var cost = IntArray(len0) { it }
        var newCost = IntArray(len0)

        for (i in 1 until len1) {
            newCost[0] = i
            for (j in 1 until len0) {
                val match = if (s1[j - 1] == s2[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[len0 - 1]
    }
}

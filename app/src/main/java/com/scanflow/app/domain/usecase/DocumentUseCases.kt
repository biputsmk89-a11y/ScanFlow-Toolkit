package com.scanflow.app.domain.usecase

import android.graphics.Bitmap
import android.graphics.PointF
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.repository.DocumentRepository
import com.scanflow.app.engine.AiAnswer
import com.scanflow.app.engine.AiDocumentInsight
import com.scanflow.app.engine.AiEngine
import com.scanflow.app.engine.PdfEngine
import com.scanflow.app.engine.ScanFilterType
import com.scanflow.app.engine.ScanSession
import com.scanflow.app.engine.ScannerEngine
import com.scanflow.app.engine.StorageEngine
import java.io.File
import java.util.UUID

fun sanitizePdfFileName(name: String): String {
    val clean = name.trim()
    return if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
}

/**
 * Domain Use Case for Document Scanning workflows.
 * Orchestrates camera frame corner detection, perspective warping,
 * image enhancement filters, and multi-page session PDF compilation.
 */
class ScannerUseCases(
    private val scannerEngine: ScannerEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEngine
) {
    fun detectCorners(bitmap: Bitmap): List<PointF>? = scannerEngine.detectDocumentCorners(bitmap)

    suspend fun processPage(bitmap: Bitmap, corners: List<PointF>?, filter: ScanFilterType): Bitmap =
        scannerEngine.processScannedPage(bitmap, corners, filter)

    suspend fun compileSession(session: ScanSession, documentName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), sanitizePdfFileName(documentName))
        val result = scannerEngine.compileSessionToPdf(session, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile),
                    contentHash = storageEngine.calculateSha256(destFile)
                )
            )
        }
        return result
    }
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

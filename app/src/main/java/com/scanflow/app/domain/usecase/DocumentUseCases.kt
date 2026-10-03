package com.scanflow.app.domain.usecase

import android.graphics.Bitmap
import android.graphics.PointF
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.model.PageNumberConfig
import com.scanflow.app.domain.model.SecurityConfig
import com.scanflow.app.domain.model.WatermarkConfig
import com.scanflow.app.domain.model.Workflow
import com.scanflow.app.domain.repository.DocumentRepository
import com.scanflow.app.domain.repository.OperationRecord
import com.scanflow.app.domain.repository.OperationRepository
import com.scanflow.app.engine.AiAnswer
import com.scanflow.app.engine.AiDocumentInsight
import com.scanflow.app.engine.AiEngine
import com.scanflow.app.engine.CompareEngine
import com.scanflow.app.engine.ComparisonReport
import com.scanflow.app.engine.CompressionEngine
import com.scanflow.app.engine.ConversionEngine
import com.scanflow.app.engine.FormEngine
import com.scanflow.app.engine.FormFieldInfo
import com.scanflow.app.domain.model.OcrResult
import com.scanflow.app.engine.OcrEngine
import com.scanflow.app.engine.PdfEngine
import com.scanflow.app.engine.ScanFilterType
import com.scanflow.app.engine.ScanSession
import com.scanflow.app.engine.ScannerEngine
import com.scanflow.app.engine.SecurityEngine
import com.scanflow.app.engine.StorageEngine
import com.scanflow.app.engine.WorkflowEngine
import java.io.File
import java.util.UUID

fun sanitizePdfFileName(name: String): String {
    val clean = name.trim()
    return if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
}

class OrganizeUseCases(
    private val pdfEngine: PdfEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val operationRepository: OperationRepository
) {
    suspend fun merge(files: List<File>, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.merge(files, destFile)
        recordAndRegister(result, files.firstOrNull()?.absolutePath ?: "", destFile)
        return result
    }

    suspend fun split(file: File, ranges: List<IntRange>): List<OperationResult> {
        val results = pdfEngine.split(file, ranges, storageEngine.getDocumentsDirectory())
        results.forEach { res ->
            if (res.success && res.outputPath != null) {
                recordAndRegister(res, file.absolutePath, File(res.outputPath))
            }
        }
        return results
    }

    suspend fun removePages(file: File, pagesToRemove: Set<Int>, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.removePages(file, pagesToRemove, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun extractPages(file: File, pagesToExtract: List<Int>, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.extractPages(file, pagesToExtract, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun reorderPages(file: File, newOrder: List<Int>, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.reorderPages(file, newOrder, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun rotatePages(file: File, rotationMap: Map<Int, Int>, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.rotatePages(file, rotationMap, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun watermark(file: File, config: WatermarkConfig, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.watermark(file, config, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun addPageNumbers(file: File, config: PageNumberConfig, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.addPageNumbers(file, config, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun duplicatePages(file: File, pages: List<Int>, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.duplicatePages(file, pages, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    suspend fun insertBlankPage(file: File, atIndex: Int, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.insertBlankPage(file, atIndex, destFile)
        recordAndRegister(result, file.absolutePath, destFile)
        return result
    }

    private suspend fun recordAndRegister(res: OperationResult, inputPath: String, destFile: File) {
        val opId = UUID.randomUUID().toString()
        operationRepository.recordOperation(
            OperationRecord(
                id = opId,
                operationType = res.operationType,
                status = if (res.success) OperationStatus.SUCCESS else OperationStatus.FAILED,
                inputUri = inputPath,
                outputUri = destFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                durationMs = res.durationMs,
                details = res.errorMessage
            )
        )
        if (res.success && destFile.exists() && destFile.length() > 0L) {
            val pageCount = pdfEngine.getPageCount(destFile)
            val hash = storageEngine.calculateSha256(destFile)
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pageCount,
                    contentHash = hash
                )
            )
        }
    }
}

class OptimizeUseCases(
    private val compressionEngine: CompressionEngine,
    private val pdfEngine: PdfEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val operationRepository: OperationRepository
) {
    suspend fun compress(
        file: File,
        config: CompressionConfig,
        outputName: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = compressionEngine.compressPdf(file, config, destFile, onProgress)

        val opId = UUID.randomUUID().toString()
        operationRepository.recordOperation(
            OperationRecord(
                id = opId,
                operationType = OperationType.COMPRESS_PDF,
                status = if (result.success) OperationStatus.SUCCESS else OperationStatus.FAILED,
                inputUri = file.absolutePath,
                outputUri = destFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                durationMs = result.durationMs,
                details = result.metadata["reduction"]
            )
        )

        if (result.success && destFile.exists() && destFile.length() > 0L) {
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

    suspend fun repair(file: File, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.repair(file, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }

    suspend fun convertToPdfA(file: File, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.convertToPdfA(file, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }


    suspend fun crop(file: File, marginPoints: Float, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = pdfEngine.cropPages(file, marginPoints, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }
}

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

class OcrUseCases(
    private val ocrEngine: OcrEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEngine
) {
    suspend fun ocrPdf(file: File, onProgress: ((Int, Int) -> Unit)? = null): OcrResult =
        ocrEngine.recognizePdf(file, onProgress)

    suspend fun makeSearchablePdf(file: File, outputName: String, onProgress: ((Int, Int) -> Unit)? = null): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = ocrEngine.generateSearchablePdf(file, destFile, onProgress)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile),
                    hasOcr = true
                )
            )
        }
        return result
    }

    suspend fun extractText(file: File, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.txt")
        return ocrEngine.extractTextToTextFile(file, destFile)
    }
}

class SecurityUseCases(
    private val securityEngine: SecurityEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEngine
) {
    suspend fun protect(file: File, config: SecurityConfig, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = securityEngine.protectPdf(file, config, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile),
                    isEncrypted = true
                )
            )
        }
        return result
    }

    suspend fun unlock(file: File, password: String, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = securityEngine.unlockPdf(file, password, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile),
                    isEncrypted = false
                )
            )
        }
        return result
    }

    suspend fun removeMetadata(file: File, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        return securityEngine.removeMetadata(file, destFile)
    }
}

class FormUseCases(
    private val formEngine: FormEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEngine
) {
    suspend fun getFields(file: File): List<FormFieldInfo> = formEngine.getFormFields(file)

    suspend fun fillAndExport(file: File, values: Map<String, String>, flatten: Boolean, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = formEngine.fillForm(file, values, flatten, destFile)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }
}

class CompareUseCases(
    private val compareEngine: CompareEngine
) {
    suspend fun compare(
        fileA: File,
        fileB: File,
        generateDiff: Boolean = true,
        onProgress: ((Int, Int) -> Unit)? = null
    ): ComparisonReport = compareEngine.comparePdfs(fileA, fileB, generateDiff, onProgress)
}

class ConversionUseCases(
    private val conversionEngine: ConversionEngine,
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEngine
) {
    suspend fun imagesToPdf(
        images: List<File>,
        outputName: String,
        fitPage: Boolean = true,
        pageSize: String = "A4",
        orientation: String = "AUTO",
        margin: String = "SMALL"
    ): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = conversionEngine.imagesToPdf(images, destFile, fitPage, pageSize, orientation, margin)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }

    suspend fun pdfToImages(
        file: File,
        format: String,
        dpi: Int = 300,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<OperationResult> {
        val outputDir = File(storageEngine.getDocumentsDirectory(), "${file.nameWithoutExtension}_images")
        return conversionEngine.pdfToImages(file, outputDir, format, dpi, onProgress)
    }

    suspend fun pdfToText(file: File, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.txt")
        return conversionEngine.pdfToText(file, destFile)
    }

    suspend fun htmlToPdf(
        htmlFile: File,
        outputName: String,
        title: String? = null
    ): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = conversionEngine.htmlToPdf(htmlFile, destFile, title)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }

    suspend fun textToPdf(
        textFile: File,
        outputName: String,
        title: String? = null,
        fontSize: Float = 11f
    ): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = conversionEngine.textToPdf(textFile, destFile, title, fontSize)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }

    suspend fun pdfToCsv(file: File, outputName: String): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.csv")
        return conversionEngine.pdfToCsv(file, destFile)
    }

    suspend fun csvToPdf(
        csvFile: File,
        outputName: String,
        title: String? = null,
        orientation: String = "AUTO",
        styleTheme: String = "MODERN_NAVY"
    ): OperationResult {
        val destFile = File(storageEngine.getDocumentsDirectory(), "$outputName.pdf")
        val result = conversionEngine.csvToPdf(csvFile, destFile, title, orientation, styleTheme)
        if (result.success && destFile.exists()) {
            documentRepository.insertDocument(
                Document(
                    id = UUID.randomUUID().toString(),
                    name = destFile.name,
                    uri = destFile.toURI().toString(),
                    path = destFile.absolutePath,
                    sizeBytes = destFile.length(),
                    pageCount = pdfEngine.getPageCount(destFile)
                )
            )
        }
        return result
    }
}


class AiUseCases(
    private val aiEngine: AiEngine
) {
    suspend fun summarize(file: File, maxBullets: Int = 5): String = aiEngine.summarizeDocument(file, maxBullets)
    suspend fun ask(file: File, query: String): AiAnswer = aiEngine.askDocument(file, query)
    suspend fun getInsights(file: File): AiDocumentInsight = aiEngine.getDocumentInsights(file)
    suspend fun translate(file: File, targetLanguage: String = "id"): String = aiEngine.translateDocument(file, targetLanguage)
    suspend fun classify(file: File): String = aiEngine.classifyDocument(file)
}

class WorkflowUseCases(
    private val workflowEngine: WorkflowEngine,
    private val storageEngine: StorageEngine
) {
    suspend fun execute(
        workflow: Workflow,
        files: List<File>,
        onProgress: ((com.scanflow.app.engine.WorkflowStepProgress) -> Unit)? = null
    ): List<OperationResult> {
        return workflowEngine.executeWorkflow(workflow, files, storageEngine.getDocumentsDirectory(), onProgress)
    }
}

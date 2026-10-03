package com.scanflow.app

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.dispatcher.DocumentOperationDispatcherImpl
import com.scanflow.app.domain.dispatcher.ToolExecutionRequest
import com.scanflow.app.domain.model.*
import com.scanflow.app.domain.repository.DocumentRepository
import com.scanflow.app.domain.repository.OperationRecord
import com.scanflow.app.domain.repository.OperationRepository
import com.scanflow.app.engine.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DocumentOperationDispatcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var docDir: File
    private lateinit var fakeStorage: StorageEngine
    private lateinit var fakePdfEngine: PdfEngine
    private lateinit var fakeFormEngine: FormEngine
    private val recordedOperations = mutableListOf<OperationRecord>()
    private val registeredDocuments = mutableListOf<Document>()

    private lateinit var dispatcher: DocumentOperationDispatcherImpl

    @Before
    fun setup() {
        docDir = tempFolder.newFolder("documents")
        recordedOperations.clear()
        registeredDocuments.clear()

        fakeStorage = object : StorageEngine {
            override fun getDocumentsDirectory(): File = docDir
            override fun getCacheDirectory(): File = docDir
            override fun getTempDirectory(): File = docDir
            override fun getExportsDirectory(): File = docDir
            override suspend fun createTempFile(prefix: String, suffix: String): File = File(docDir, "$prefix$suffix")
            override suspend fun copyUriToTempFile(uri: Uri, tempFile: File): Boolean = true
            override suspend fun exportFile(sourceFile: File, displayName: String, mimeType: String): Uri? = null
            override suspend fun calculateSha256(file: File): String = "mock_sha256_hash"
            override suspend fun validateFile(file: File, expectedMimeType: String?): Boolean = true
            override suspend fun cleanupTempFiles() {}
            override suspend fun getStorageUsage(): StorageUsage = StorageUsage(0, 0, 0, 0)
            override suspend fun clearCache(): Boolean = true
        }

        fakePdfEngine = object : PdfEngine {
            override suspend fun merge(inputFiles: List<File>, outputFile: File): OperationResult {
                outputFile.writeText("merged_pdf_content")
                return OperationResult.success(OperationType.MERGE_PDF, outputFile.absolutePath, outputFile.length(), 100L, 5)
            }
            override suspend fun split(inputFile: File, pageRanges: List<IntRange>, outputDirectory: File): List<OperationResult> = emptyList()
            override suspend fun removePages(inputFile: File, pagesToRemove: Set<Int>, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REMOVE_PAGES, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun extractPages(inputFile: File, pagesToExtract: List<Int>, outputFile: File): OperationResult =
                OperationResult.success(OperationType.EXTRACT_PAGES, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun reorderPages(inputFile: File, newOrder: List<Int>, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REORDER_PAGES, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun rotatePages(inputFile: File, pagesToRotate: Map<Int, Int>, outputFile: File): OperationResult =
                OperationResult.success(OperationType.ROTATE_PAGES, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun duplicatePages(inputFile: File, pageIndices: List<Int>, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REORDER_PAGES, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun insertBlankPage(inputFile: File, atIndex: Int, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REORDER_PAGES, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun watermark(inputFile: File, config: WatermarkConfig, outputFile: File): OperationResult =
                OperationResult.success(OperationType.WATERMARK, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun addPageNumbers(inputFile: File, config: PageNumberConfig, outputFile: File): OperationResult =
                OperationResult.success(OperationType.PAGE_NUMBERS, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun repair(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REPAIR_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun convertToPdfA(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REPAIR_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun cropPages(inputFile: File, marginPoints: Float, outputFile: File): OperationResult =
                OperationResult.success(OperationType.CROP_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun getPageCount(inputFile: File): Int = 5
            override suspend fun isEncrypted(inputFile: File): Boolean = false
        }

        fakeFormEngine = object : FormEngine {
            override suspend fun getFormFields(inputFile: File): List<FormFieldInfo> = listOf(
                FormFieldInfo("fullName", "John Doe", false, false, FormFieldType.TEXT),
                FormFieldInfo("agreeTerms", "Yes", false, false, FormFieldType.CHECKBOX)
            )
            override suspend fun fillForm(inputFile: File, fieldValues: Map<String, String>, flatten: Boolean, outputFile: File): OperationResult =
                OperationResult.success(OperationType.FILL_FORM, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun resetForm(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.FILL_FORM, outputFile.absolutePath, 100L, 10L, 1)
        }

        val fakeDocRepo = object : DocumentRepository {
            override fun getAllDocuments(): Flow<List<Document>> = flowOf(registeredDocuments)
            override fun getRecentDocuments(limit: Int): Flow<List<Document>> = flowOf(emptyList())
            override fun getFavoriteDocuments(): Flow<List<Document>> = flowOf(emptyList())
            override fun getDocumentsInFolder(folderId: String): Flow<List<Document>> = flowOf(emptyList())
            override suspend fun getDocumentById(id: String): Document? = registeredDocuments.find { it.id == id }
            override suspend fun searchDocuments(query: String): List<Document> = emptyList()
            override suspend fun insertDocument(document: Document) { registeredDocuments.add(document) }
            override suspend fun updateDocument(document: Document) {}
            override suspend fun setFavorite(id: String, isFavorite: Boolean) {}
            override suspend fun renameDocument(id: String, newName: String) {}
            override suspend fun deleteDocument(id: String): Boolean = true
            override suspend fun findDuplicates(contentHash: String, excludeId: String): List<Document> = emptyList()
            override fun getAllFolders(): Flow<List<Folder>> = flowOf(emptyList())
            override suspend fun createFolder(name: String): Folder = Folder("id", name, 0L)
            override suspend fun deleteFolder(folderId: String): Boolean = true
            override suspend fun moveDocumentToFolder(documentId: String, folderId: String?) {}
        }

        val fakeOpRepo = object : OperationRepository {
            override fun getRecentOperations(limit: Int): Flow<List<OperationRecord>> = flowOf(recordedOperations)
            override suspend fun recordOperation(record: OperationRecord) { recordedOperations.add(record) }
            override suspend fun clearHistory() { recordedOperations.clear() }
        }

        val emptyAiEngine = object : AiEngine {
            override suspend fun summarizeDocument(file: File, maxBullets: Int): String = "Summary"
            override suspend fun askDocument(file: File, question: String): AiAnswer = AiAnswer(question, "Answer")
            override suspend fun getDocumentInsights(file: File): AiDocumentInsight = AiDocumentInsight("Title", 100, 1, "en", "PDF", emptyList())
            override suspend fun classifyDocument(file: File): String = "Invoice"
            override suspend fun translateDocument(file: File, targetLanguage: String): String = "Terjemahan"
        }

        val emptySecurityEngine = object : SecurityEngine {
            override suspend fun protectPdf(inputFile: File, config: SecurityConfig, outputFile: File): OperationResult =
                OperationResult.success(OperationType.PROTECT_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun unlockPdf(inputFile: File, userPassword: String, outputFile: File): OperationResult =
                OperationResult.success(OperationType.UNLOCK_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun removeMetadata(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.REMOVE_METADATA, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun redactPdf(inputFile: File, redactionsPerPage: Map<Int, List<RectF>>, outputFile: File): OperationResult =
                OperationResult.success(OperationType.PROTECT_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun stampSignature(inputFile: File, signatureBitmap: Bitmap, pageIndex: Int, rect: RectF, outputFile: File): OperationResult =
                OperationResult.success(OperationType.PROTECT_PDF, outputFile.absolutePath, 100L, 10L, 1)
        }

        val emptyCompEngine = object : CompressionEngine {
            override suspend fun compressPdf(inputFile: File, config: CompressionConfig, outputFile: File, onProgress: ((Int, Int) -> Unit)?): OperationResult =
                OperationResult.success(OperationType.COMPRESS_PDF, outputFile.absolutePath, 50L, 10L, 1)
        }

        val emptyOcrEngine = object : OcrEngine {
            override suspend fun recognizeImage(bitmap: Bitmap): OcrPageResult =
                OcrPageResult(1, "test", emptyList(), 1200, 1600)
            override suspend fun recognizePdf(inputFile: File, onProgress: ((Int, Int) -> Unit)?): OcrResult =
                OcrResult("doc1", emptyList(), "test", 10L)
            override suspend fun generateSearchablePdf(inputFile: File, outputFile: File, onProgress: ((Int, Int) -> Unit)?): OperationResult =
                OperationResult.success(OperationType.SEARCHABLE_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun extractTextToTextFile(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.OCR_PDF, outputFile.absolutePath, 100L, 10L, 1)
        }

        val emptyCompareEngine = object : CompareEngine {
            override suspend fun comparePdfs(fileA: File, fileB: File, generateDiffImages: Boolean, onProgress: ((Int, Int) -> Unit)?): ComparisonReport =
                ComparisonReport(fileA, fileB, 1, 1, emptyList(), 100f)
        }

        val emptyConversionEngine = object : ConversionEngine {
            override suspend fun imagesToPdf(imageFiles: List<File>, outputFile: File, fitPage: Boolean, pageSize: String, orientation: String, margin: String): OperationResult =
                OperationResult.success(OperationType.IMAGE_TO_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun pdfToImages(inputFile: File, outputDirectory: File, format: String, dpi: Int, onProgress: ((Int, Int) -> Unit)?): List<OperationResult> = emptyList()
            override suspend fun pdfToText(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.PDF_TO_TEXT, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun htmlToPdf(htmlFile: File, outputFile: File, title: String?): OperationResult =
                OperationResult.success(OperationType.HTML_TO_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun textToPdf(textFile: File, outputFile: File, title: String?, fontSize: Float): OperationResult =
                OperationResult.success(OperationType.TEXT_TO_PDF, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun pdfToCsv(inputFile: File, outputFile: File): OperationResult =
                OperationResult.success(OperationType.PDF_TO_TEXT, outputFile.absolutePath, 100L, 10L, 1)
            override suspend fun csvToPdf(csvFile: File, outputFile: File, title: String?, orientation: String, styleTheme: String): OperationResult =
                OperationResult.success(OperationType.TEXT_TO_PDF, outputFile.absolutePath, 100L, 10L, 1)
        }

        val emptyWorkflowEngine = object : WorkflowEngine {
            override suspend fun executeWorkflow(workflow: Workflow, inputFiles: List<File>, outputDirectory: File, onProgress: ((WorkflowStepProgress) -> Unit)?): List<OperationResult> = emptyList()
        }

        dispatcher = DocumentOperationDispatcherImpl(
            storageEngine = fakeStorage,
            pdfEngine = fakePdfEngine,
            compressionEngine = emptyCompEngine,
            ocrEngine = emptyOcrEngine,
            securityEngine = emptySecurityEngine,
            formEngine = fakeFormEngine,
            compareEngine = emptyCompareEngine,
            conversionEngine = emptyConversionEngine,
            workflowEngine = emptyWorkflowEngine,
            aiEngine = emptyAiEngine,
            documentRepository = fakeDocRepo,
            operationRepository = fakeOpRepo
        )
    }

    @Test
    fun dispatch_emptyFiles_returnsFailureFast() = runTest {
        val request = ToolExecutionRequest(
            featureId = "SF-001",
            inputFiles = emptyList(),
            outputName = "test_merged"
        )

        val result = dispatcher.dispatch(request)
        assertFalse(result.success)
        assertEquals(ErrorCode.FILE_NOT_FOUND, result.errorCode)
        assertTrue(recordedOperations.isEmpty())
        assertTrue(registeredDocuments.isEmpty())
    }

    @Test
    fun dispatch_unsupportedFeature_returnsFailure() = runTest {
        val testFile = tempFolder.newFile("sample.pdf").apply { writeText("dummy content") }
        val request = ToolExecutionRequest(
            featureId = "SF-999_UNKNOWN",
            inputFiles = listOf(testFile),
            outputName = "test_out"
        )

        val result = dispatcher.dispatch(request)
        assertFalse(result.success)
        assertEquals(ErrorCode.INVALID_FILE, result.errorCode)
    }

    @Test
    fun dispatch_mergePdf_executesAndRegistersOutputDocument() = runTest {
        val file1 = tempFolder.newFile("part1.pdf").apply { writeText("content1") }
        val file2 = tempFolder.newFile("part2.pdf").apply { writeText("content2") }

        val request = ToolExecutionRequest(
            featureId = "SF-001",
            inputFiles = listOf(file1, file2),
            outputName = "merged_output"
        )

        val result = dispatcher.dispatch(request)
        assertTrue(result.success)
        assertEquals(OperationType.MERGE_PDF, result.operationType)

        // Verify audit log was recorded
        assertEquals(1, recordedOperations.size)
        assertEquals(OperationType.MERGE_PDF, recordedOperations.first().operationType)

        // Verify document was registered in DB
        assertEquals(1, registeredDocuments.size)
        val registered = registeredDocuments.first()
        assertEquals("merged_output.pdf", registered.name)
        assertEquals("mock_sha256_hash", registered.contentHash)
        assertEquals(5, registered.pageCount)
    }

    @Test
    fun inspectFormFields_delegatesToFormEngine() = runTest {
        val testFile = tempFolder.newFile("form.pdf").apply { writeText("form content") }
        val fields = dispatcher.inspectFormFields(testFile)

        assertEquals(2, fields.size)
        assertEquals("fullName", fields[0].name)
        assertEquals("John Doe", fields[0].value)
    }
}

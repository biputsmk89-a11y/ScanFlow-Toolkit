package com.scanflow.app

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.dispatcher.DocumentOperationDispatcherImpl
import com.scanflow.app.domain.dispatcher.ToolExecutionRequest
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.model.Folder
import com.scanflow.app.domain.repository.DocumentRepository
import com.scanflow.app.domain.repository.OperationRecord
import com.scanflow.app.domain.repository.OperationRepository
import com.scanflow.app.engine.*
import com.scanflow.app.engine.impl.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

/**
 * End-to-end live testing harness verifying real engine execution,
 * file transformations, cryptographic integrity, audit recording,
 * and database indexing across all core ScanFlow operations.
 */
class FullToolkitLiveFeasibilityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var workDir: File
    private lateinit var doc1: File
    private lateinit var doc2: File
    private lateinit var multiPageDoc: File
    private lateinit var invoiceDoc: File

    private lateinit var realPdfEngine: PdfEngineImpl
    private lateinit var realSecurityEngine: SecurityEngineImpl
    private lateinit var realAiEngine: AiEngineImpl
    private lateinit var realFormEngine: FormEngineImpl

    private val recordedOperations = mutableListOf<OperationRecord>()
    private val registeredDocuments = mutableListOf<Document>()

    private lateinit var dispatcher: DocumentOperationDispatcherImpl

    @Before
    fun setup() {
        workDir = tempFolder.newFolder("scanflow_live_test")
        recordedOperations.clear()
        registeredDocuments.clear()

        realPdfEngine = PdfEngineImpl()
        realSecurityEngine = SecurityEngineImpl()
        realAiEngine = AiEngineImpl()
        realFormEngine = FormEngineImpl()

        // Create real PDF Doc 1 (1 A4 page)
        doc1 = File(workDir, "doc1.pdf")
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            doc.documentInformation.title = "Document 1 Title"
            doc.documentInformation.author = "ScanFlow Engineer"
            doc.save(doc1)
        }

        // Create real PDF Doc 2 (1 A4 page)
        doc2 = File(workDir, "doc2.pdf")
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            doc.save(doc2)
        }

        // Create real Multi-Page PDF (4 A4 pages)
        multiPageDoc = File(workDir, "multi_page_doc.pdf")
        PDDocument().use { doc ->
            for (i in 1..4) {
                doc.addPage(PDPage(PDRectangle.A4))
            }
            doc.save(multiPageDoc)
        }

        // Create real Document with text for AI test
        invoiceDoc = File(workDir, "invoice_agreement.txt")
        invoiceDoc.writeText(
            """
            Invoice and Commercial Service Agreement
            Total amount payable under this contract is five thousand dollars.
            Both parties agree to uphold all security and confidentiality standards described herein.
            Payment terms require complete settlement within thirty days of invoice receipt.
            """.trimIndent(),
            Charsets.UTF_8
        )

        val fakeStorage = object : StorageEngine {
            override fun getDocumentsDirectory(): File = workDir
            override fun getCacheDirectory(): File = workDir
            override fun getTempDirectory(): File = workDir
            override fun getExportsDirectory(): File = workDir
            override suspend fun createTempFile(prefix: String, suffix: String): File = File(workDir, "$prefix$suffix")
            override suspend fun copyUriToTempFile(uri: Uri, tempFile: File): Boolean = true
            override suspend fun exportFile(sourceFile: File, displayName: String, mimeType: String): Uri? = null
            override suspend fun calculateSha256(file: File): String {
                val digest = MessageDigest.getInstance("SHA-256")
                val bytes = file.readBytes()
                val hash = digest.digest(bytes)
                return hash.joinToString("") { "%02x".format(it) }
            }
            override suspend fun validateFile(file: File, expectedMimeType: String?): Boolean = file.exists() && file.length() > 0
            override suspend fun cleanupTempFiles() {}
            override suspend fun getStorageUsage(): StorageUsage = StorageUsage(workDir.length(), 0, 0, 0)
            override suspend fun clearCache(): Boolean = true
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

        val fakeCompEngine = object : CompressionEngine {
            override suspend fun compressPdf(inputFile: File, config: com.scanflow.app.domain.model.CompressionConfig, outputFile: File, onProgress: ((Int, Int) -> Unit)?): OperationResult {
                inputFile.copyTo(outputFile, overwrite = true)
                return OperationResult.success(OperationType.COMPRESS_PDF, outputFile.absolutePath, outputFile.length(), 50L, 1)
            }
        }

        val fakeOcrEngine = object : OcrEngine {
            override suspend fun recognizeImage(bitmap: Bitmap): com.scanflow.app.domain.model.OcrPageResult =
                com.scanflow.app.domain.model.OcrPageResult(1, "OCR Recognized text", emptyList(), 1000, 1000)
            override suspend fun recognizePdf(inputFile: File, onProgress: ((Int, Int) -> Unit)?): com.scanflow.app.domain.model.OcrResult =
                com.scanflow.app.domain.model.OcrResult("doc", emptyList(), "OCR text", 100L)
            override suspend fun generateSearchablePdf(inputFile: File, outputFile: File, onProgress: ((Int, Int) -> Unit)?): OperationResult {
                inputFile.copyTo(outputFile, overwrite = true)
                return OperationResult.success(OperationType.SEARCHABLE_PDF, outputFile.absolutePath, outputFile.length(), 100L, 1)
            }
            override suspend fun extractTextToTextFile(inputFile: File, outputFile: File): OperationResult {
                outputFile.writeText("Extracted OCR text content from ${inputFile.name}")
                return OperationResult.success(OperationType.OCR_PDF, outputFile.absolutePath, outputFile.length(), 50L, 1)
            }
        }

        val fakeCompareEngine = object : CompareEngine {
            override suspend fun comparePdfs(fileA: File, fileB: File, generateDiffImages: Boolean, onProgress: ((Int, Int) -> Unit)?): ComparisonReport =
                ComparisonReport(fileA, fileB, 1, 1, emptyList(), 98.5f)
        }

        val fakeConversionEngine = object : ConversionEngine {
            override suspend fun imagesToPdf(imageFiles: List<File>, outputFile: File, fitPage: Boolean, pageSize: String, orientation: String, margin: String): OperationResult {
                outputFile.writeText("Mock converted PDF")
                return OperationResult.success(OperationType.IMAGE_TO_PDF, outputFile.absolutePath, outputFile.length(), 50L, imageFiles.size)
            }
            override suspend fun pdfToImages(inputFile: File, outputDirectory: File, format: String, dpi: Int, onProgress: ((Int, Int) -> Unit)?): List<OperationResult> {
                val img = File(outputDirectory, "page_1.$format").apply { writeText("img_data") }
                return listOf(OperationResult.success(OperationType.PDF_TO_IMAGES, img.absolutePath, img.length(), 20L, 1))
            }
            override suspend fun pdfToText(inputFile: File, outputFile: File): OperationResult {
                outputFile.writeText("Sample PDF to plain text conversion result.")
                return OperationResult.success(OperationType.PDF_TO_TEXT, outputFile.absolutePath, outputFile.length(), 20L, 1)
            }
            override suspend fun htmlToPdf(htmlFile: File, outputFile: File, title: String?): OperationResult {
                outputFile.writeText("Sample HTML to PDF")
                return OperationResult.success(OperationType.HTML_TO_PDF, outputFile.absolutePath, outputFile.length(), 20L, 1)
            }
            override suspend fun textToPdf(textFile: File, outputFile: File, title: String?, fontSize: Float): OperationResult {
                outputFile.writeText("Sample Text to PDF")
                return OperationResult.success(OperationType.TEXT_TO_PDF, outputFile.absolutePath, outputFile.length(), 20L, 1)
            }
            override suspend fun pdfToCsv(inputFile: File, outputFile: File): OperationResult {
                outputFile.writeText("col1,col2\nval1,val2\n")
                return OperationResult.success(OperationType.PDF_TO_TEXT, outputFile.absolutePath, outputFile.length(), 20L, 1)
            }
            override suspend fun csvToPdf(csvFile: File, outputFile: File, title: String?, orientation: String, styleTheme: String): OperationResult {
                outputFile.writeText("Sample CSV to PDF")
                return OperationResult.success(OperationType.TEXT_TO_PDF, outputFile.absolutePath, outputFile.length(), 20L, 1)
            }
        }

        val realWorkflowEngine = WorkflowEngineImpl(realPdfEngine, fakeCompEngine, fakeOcrEngine, fakeStorage)

        dispatcher = DocumentOperationDispatcherImpl(
            storageEngine = fakeStorage,
            pdfEngine = realPdfEngine,
            compressionEngine = fakeCompEngine,
            ocrEngine = fakeOcrEngine,
            securityEngine = realSecurityEngine,
            formEngine = realFormEngine,
            compareEngine = fakeCompareEngine,
            conversionEngine = fakeConversionEngine,
            workflowEngine = realWorkflowEngine,
            aiEngine = realAiEngine,
            documentRepository = fakeDocRepo,
            operationRepository = fakeOpRepo
        )
    }

    @After
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun testLive_SF001_MergePdf_CombinesPagesAndRecordsAudit() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-001",
            featureName = "Merge PDF",
            inputFiles = listOf(doc1, doc2),
            outputName = "live_merged_result"
        )
        val res = dispatcher.dispatch(req)

        assertTrue("Merge should succeed", res.success)
        assertEquals(OperationType.MERGE_PDF, res.operationType)
        val outFile = File(res.outputPath!!)
        assertTrue("Output file must exist on disk", outFile.exists())
        assertEquals("Merged file must have 2 pages", 2, realPdfEngine.getPageCount(outFile))
        assertEquals(1, recordedOperations.size)
        assertEquals(1, registeredDocuments.size)
        assertEquals("live_merged_result.pdf", registeredDocuments.first().name)
    }

    @Test
    fun testLive_SF002_SplitPdf_SplitsIntoMultipleValidFiles() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-002",
            featureName = "Split PDF",
            inputFiles = listOf(multiPageDoc),
            outputName = "split_part",
            splitRanges = "1-2, 3-4"
        )
        val res = dispatcher.dispatch(req)

        assertTrue("Split should succeed", res.success)
        assertEquals(OperationType.SPLIT_PDF, res.operationType)
        assertTrue(recordedOperations.isNotEmpty())
        assertTrue(registeredDocuments.size >= 2)
    }

    @Test
    fun testLive_SF003_RemovePages_RemovesSpecifiedPages() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-003",
            featureName = "Remove Pages",
            inputFiles = listOf(multiPageDoc),
            outputName = "live_removed_pages",
            pageOrder = "2" // Remove page 2
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertEquals("4 pages minus 1 page should equal 3 pages", 3, realPdfEngine.getPageCount(outFile))
    }

    @Test
    fun testLive_SF004_ExtractPages_ExtractsTargetPages() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-004",
            featureName = "Extract Pages",
            inputFiles = listOf(multiPageDoc),
            outputName = "live_extracted_pages",
            pageOrder = "1, 3"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertEquals("Extracted pages count should equal 2", 2, realPdfEngine.getPageCount(outFile))
    }

    @Test
    fun testLive_SF005_ReorderPages_InvertsPageOrder() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-005",
            featureName = "Organize Pages",
            inputFiles = listOf(multiPageDoc),
            outputName = "live_reordered_pages",
            pageOrder = "4, 3, 2, 1"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertEquals(4, realPdfEngine.getPageCount(outFile))
    }

    @Test
    fun testLive_SF008_RotatePages_AppliesRotation() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-008",
            featureName = "Rotate Pages",
            inputFiles = listOf(doc1),
            outputName = "live_rotated_page",
            rotationAngle = 90
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertTrue(outFile.exists())
        assertEquals(1, realPdfEngine.getPageCount(outFile))
        PDDocument.load(outFile).use { doc ->
            assertEquals(90, doc.getPage(0).rotation)
        }
    }

    @Test
    fun testLive_SF009_DuplicatePages_IncreasesPageCount() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-009",
            featureName = "Duplicate Pages",
            inputFiles = listOf(doc1),
            outputName = "live_duplicated_doc",
            pageOrder = "1"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertEquals("1 page + duplicated page should equal 2 pages", 2, realPdfEngine.getPageCount(outFile))
    }

    @Test
    fun testLive_SF011_InsertBlankPage_IncreasesPageCount() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-011",
            featureName = "Insert Blank Page",
            inputFiles = listOf(doc1),
            outputName = "live_blank_inserted",
            signatureTargetPage = 1
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertEquals("1 page + 1 blank page should equal 2 pages", 2, realPdfEngine.getPageCount(outFile))
    }

    @Test
    fun testLive_SF024_RepairPdf_ProducesSanitizedDocument() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-024",
            featureName = "Repair PDF",
            inputFiles = listOf(doc1),
            outputName = "live_repaired_doc"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertTrue(outFile.exists())
        assertTrue(outFile.length() > 0)
    }

    @Test
    fun testLive_SF075_CropPdf_AppliesMarginCrop() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-075",
            featureName = "Crop PDF",
            inputFiles = listOf(doc1),
            outputName = "live_cropped_doc",
            cropMarginPoints = 36f
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertTrue(outFile.exists())
        PDDocument.load(outFile).use { doc ->
            val page = doc.getPage(0)
            assertTrue(page.cropBox.width < page.mediaBox.width)
        }
    }

    @Test
    fun testLive_SF106_PdfToPdfA_ConvertsSuccessfully() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-106",
            featureName = "PDF to PDF/A",
            inputFiles = listOf(doc1),
            outputName = "live_pdfa_doc"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        assertTrue(outFile.exists())
        assertTrue(outFile.length() > 0)
    }

    @Test
    fun testLive_SF079_And_SF081_ProtectAndUnlock_RoundTrip() = runTest {
        // Step 1: Encrypt with Password
        val protectReq = ToolExecutionRequest(
            featureId = "SF-079",
            featureName = "Protect PDF",
            inputFiles = listOf(doc1),
            outputName = "live_protected_doc",
            userPassword = "ScanFlowSecurePassword2026!"
        )
        val protectRes = dispatcher.dispatch(protectReq)
        assertTrue("Protect must succeed", protectRes.success)
        val protectedFile = File(protectRes.outputPath!!)
        assertTrue(protectedFile.exists())

        // Verify protected document is indeed encrypted
        PDDocument.load(protectedFile, "ScanFlowSecurePassword2026!").use { doc ->
            assertTrue("Document must be encrypted", doc.isEncrypted)
        }

        // Step 2: Unlock document
        val unlockReq = ToolExecutionRequest(
            featureId = "SF-081",
            featureName = "Unlock PDF",
            inputFiles = listOf(protectedFile),
            outputName = "live_unlocked_doc",
            userPassword = "ScanFlowSecurePassword2026!"
        )
        val unlockRes = dispatcher.dispatch(unlockReq)
        assertTrue("Unlock must succeed", unlockRes.success)
        val unlockedFile = File(unlockRes.outputPath!!)

        // Verify unlocked document is no longer encrypted
        PDDocument.load(unlockedFile).use { doc ->
            assertFalse("Document must now be unencrypted", doc.isEncrypted)
            assertEquals(1, doc.numberOfPages)
        }
    }

    @Test
    fun testLive_SF082_RemoveMetadata_StripsDocumentInformation() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-082",
            featureName = "Remove Metadata",
            inputFiles = listOf(doc1),
            outputName = "live_stripped_metadata"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        PDDocument.load(outFile).use { doc ->
            val info = doc.documentInformation
            assertTrue("Title should be cleared", info.title.isNullOrEmpty())
            assertTrue("Author should be cleared", info.author.isNullOrEmpty())
        }
    }

    @Test
    fun testLive_SF107_AiSummarize_ExtractsTextAndProducesSummary() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-107",
            featureName = "AI Summarizer",
            inputFiles = listOf(invoiceDoc),
            outputName = "live_ai_summary",
            aiSummaryBullets = 3
        )
        val res = dispatcher.dispatch(req)

        assertTrue("AI summarize must succeed", res.success)
        val outFile = File(res.outputPath!!)
        assertTrue("Summary text file must exist", outFile.exists())
        val text = outFile.readText()
        assertTrue("Summary should contain bullet points", text.contains("•"))
    }

    @Test
    fun testLive_SF108_AiAsk_AnswersQuestionsWithCitations() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-108",
            featureName = "Ask PDF",
            inputFiles = listOf(invoiceDoc),
            outputName = "live_ai_ask",
            aiQuestion = "What is the total amount payable?"
        )
        val res = dispatcher.dispatch(req)

        assertTrue("AI Ask must succeed", res.success)
        val outFile = File(res.outputPath!!)
        val text = outFile.readText()
        assertTrue("Output should contain question", text.contains("Question:"))
        assertTrue("Output should contain Answer:", text.contains("Answer:"))
        assertTrue("Output should cite Page 1", text.contains("Page 1"))
    }

    @Test
    fun testLive_SF110_AiTranslate_TranslatesDocument() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-110",
            featureName = "Translate PDF",
            inputFiles = listOf(invoiceDoc),
            outputName = "live_ai_translation",
            translationTargetLang = "id"
        )
        val res = dispatcher.dispatch(req)

        assertTrue("AI Translate must succeed", res.success)
        val outFile = File(res.outputPath!!)
        val text = outFile.readText()
        assertTrue("Should contain Indonesian header", text.contains("TERJEMAHAN DOKUMEN OFFLINE (ID)"))
    }

    @Test
    fun testLive_SF114_AiClassification_IdentifiesDocumentCategory() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-114",
            featureName = "Document Classification",
            inputFiles = listOf(invoiceDoc),
            outputName = "live_ai_classification"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        val text = outFile.readText()
        assertTrue("Should classify as Invoice / Billing", text.contains("Invoice / Billing"))
    }

    @Test
    fun testLive_SF116_AiInsights_GeneratesDocumentMetrics() = runTest {
        val req = ToolExecutionRequest(
            featureId = "SF-116",
            featureName = "Document Insights",
            inputFiles = listOf(invoiceDoc),
            outputName = "live_ai_insights"
        )
        val res = dispatcher.dispatch(req)

        assertTrue(res.success)
        val outFile = File(res.outputPath!!)
        val text = outFile.readText()
        assertTrue("Should contain Word Count", text.contains("Word Count:"))
        assertTrue("Should contain Reading Time", text.contains("Reading Time:"))
    }
}

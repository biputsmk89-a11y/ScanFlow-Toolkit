package com.scanflow.app

import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.WatermarkConfig
import com.scanflow.app.engine.impl.PdfEngineImpl
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PdfEngineTest {

    private val pdfEngine = PdfEngineImpl()
    private lateinit var tempDir: File
    private lateinit var docA: File
    private lateinit var docB: File

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scanflow_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        docA = File(tempDir, "docA.pdf")
        docB = File(tempDir, "docB.pdf")

        // Create docA with 1 page
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            doc.save(docA)
        }

        // Create docB with 2 pages
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            doc.addPage(PDPage(PDRectangle.A4))
            doc.save(docB)
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testMerge_validatesPageCountAndOutput() = runBlocking {
        val mergedFile = File(tempDir, "merged.pdf")
        val result = pdfEngine.merge(listOf(docA, docB), mergedFile)

        assertTrue("Merge operation should succeed", result.success)
        assertEquals(OperationType.MERGE_PDF, result.operationType)
        assertTrue("Merged file must exist on disk", mergedFile.exists())
        assertTrue("Merged file size must be non-zero", mergedFile.length() > 0)
        assertEquals("Total pages in merged document should be 3", 3, result.pagesProcessed)

        val verifiedCount = pdfEngine.getPageCount(mergedFile)
        assertEquals(3, verifiedCount)
    }

    @Test
    fun testRemovePages_validatesOutput() = runBlocking {
        val removedFile = File(tempDir, "removed.pdf")
        // Remove page 2 from docB (which has 2 pages)
        val result = pdfEngine.removePages(docB, setOf(2), removedFile)

        assertTrue("Remove pages should succeed", result.success)
        assertTrue(removedFile.exists())
        assertEquals(1, result.pagesProcessed)
    }

    @Test
    fun testExtractPages_validatesOutput() = runBlocking {
        val extractedFile = File(tempDir, "extracted.pdf")
        // Extract page 1 from docB
        val result = pdfEngine.extractPages(docB, listOf(1), extractedFile)

        assertTrue("Extract pages should succeed", result.success)
        assertTrue(extractedFile.exists())
        assertEquals(1, result.pagesProcessed)
    }

    @Test
    fun testReorderPages_validatesOutput() = runBlocking {
        val reorderedFile = File(tempDir, "reordered.pdf")
        // Reverse pages of docB: page 2 then page 1
        val result = pdfEngine.reorderPages(docB, listOf(2, 1), reorderedFile)

        assertTrue("Reorder operation should succeed", result.success)
        assertTrue(reorderedFile.exists())
        assertEquals(2, result.pagesProcessed)
    }

    @Test
    fun testRepair_validatesOutput() = runBlocking {
        val repairedFile = File(tempDir, "repaired.pdf")
        val result = pdfEngine.repair(docA, repairedFile)

        assertTrue("Repair operation should succeed", result.success)
        assertTrue(repairedFile.exists())
        assertTrue(repairedFile.length() > 0)
    }

    @Test
    fun testIsEncrypted_falseForPlainPdf() = runBlocking {
        val encrypted = pdfEngine.isEncrypted(docA)
        assertFalse("Plain PDF should not be marked encrypted", encrypted)
    }

    @Test
    fun testRotatePages_validatesOutput() = runBlocking {
        val rotatedFile = File(tempDir, "rotated.pdf")
        val result = pdfEngine.rotatePages(docA, mapOf(1 to 90), rotatedFile)

        assertTrue("Rotate operation should succeed", result.success)
        assertTrue(rotatedFile.exists())

        PDDocument.load(rotatedFile).use { doc ->
            assertEquals(90, doc.getPage(0).rotation)
        }
    }
}

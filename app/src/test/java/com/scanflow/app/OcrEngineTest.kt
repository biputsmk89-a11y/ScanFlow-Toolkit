package com.scanflow.app

import android.graphics.Bitmap
import android.graphics.RectF
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.OcrBlock
import com.scanflow.app.domain.model.OcrLine
import com.scanflow.app.domain.model.OcrPageResult
import com.scanflow.app.engine.PdfRendererEngine
import com.scanflow.app.engine.impl.OcrEngineImpl
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class OcrEngineTest {

    private lateinit var tempDir: File
    private lateinit var samplePdf: File
    private lateinit var fakePdfRendererEngine: PdfRendererEngine
    private lateinit var ocrEngine: OcrEngineImpl

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scanflow_ocr_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        samplePdf = File(tempDir, "sample.pdf")
        PDDocument().use { doc ->
            doc.addPage(PDPage(PDRectangle.A4))
            doc.addPage(PDPage(PDRectangle.A4))
            doc.save(samplePdf)
        }

        fakePdfRendererEngine = object : PdfRendererEngine {
            override suspend fun open(file: File): Boolean = true
            override suspend fun getPageCount(): Int = 2
            override suspend fun renderPage(pageIndex: Int, targetWidth: Int, targetHeight: Int, renderMode: Int): Bitmap? {
                // In headless JVM unit test, return null to avoid Android native bitmap dependencies
                return null
            }
            override suspend fun renderThumbnail(file: File, pageIndex: Int, size: Int): Bitmap? = null
            override fun close() {}
        }

        ocrEngine = OcrEngineImpl(fakePdfRendererEngine)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testOcrLineAndBlockStructure() {
        val line1 = OcrLine(text = "Hello World", boundingBox = RectF(10f, 10f, 100f, 30f))
        val line2 = OcrLine(text = "Line Two", boundingBox = RectF(10f, 35f, 100f, 55f))
        val block = OcrBlock(
            text = "Hello World\nLine Two",
            boundingBox = RectF(10f, 10f, 100f, 55f),
            lines = listOf(line1, line2)
        )

        assertEquals(2, block.lines.size)
        assertEquals("Hello World", block.lines[0].text)
        assertEquals("Line Two", block.lines[1].text)
        org.junit.Assert.assertNotNull(block.lines[0].boundingBox)
    }

    @Test
    fun testGenerateSearchablePdf_createsValidOutput() = runBlocking {
        val outputFile = File(tempDir, "searchable_out.pdf")
        val result = ocrEngine.generateSearchablePdf(samplePdf, outputFile)

        assertTrue("Searchable PDF operation should succeed", result.success)
        assertEquals(OperationType.SEARCHABLE_PDF, result.operationType)
        assertTrue("Output file must exist", outputFile.exists())
        assertTrue("Output file size must be non-zero", outputFile.length() > 0)

        PDDocument.load(outputFile).use { doc ->
            assertEquals("Output document must preserve original page count", 2, doc.numberOfPages)
        }
    }

    @Test
    fun testExtractTextToTextFile_createsValidTextFile() = runBlocking {
        val outputFile = File(tempDir, "extracted.txt")
        val result = ocrEngine.extractTextToTextFile(samplePdf, outputFile)

        assertTrue("Extract text operation should succeed", result.success)
        assertEquals(OperationType.OCR_PDF, result.operationType)
        assertTrue("Output text file must exist", outputFile.exists())
    }
}

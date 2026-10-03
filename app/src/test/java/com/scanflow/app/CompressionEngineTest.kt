package com.scanflow.app

import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.CompressionConfig
import com.scanflow.app.domain.model.CompressionLevel
import com.scanflow.app.engine.impl.CompressionEngineImpl
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class CompressionEngineTest {

    private val compressionEngine = CompressionEngineImpl()
    private lateinit var tempDir: File
    private lateinit var samplePdf: File

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scanflow_comp_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        samplePdf = File(tempDir, "Large_Document.pdf")
        val doc = PDDocument()
        for (i in 1..3) {
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
        }
        doc.documentInformation.author = "Test Author"
        doc.documentInformation.title = "Test Document Title"
        doc.save(samplePdf)
        doc.close()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testCompressPdf_basicCompression() = runBlocking {
        val compressedFile = File(tempDir, "compressed_basic.pdf")
        val result = compressionEngine.compressPdf(
            inputFile = samplePdf,
            config = CompressionConfig(
                level = CompressionLevel.MEDIUM,
                compressImages = true,
                customDpi = 150,
                customQuality = 75,
                stripMetadata = true
            ),
            outputFile = compressedFile
        )

        assertTrue("Compression should succeed", result.success)
        assertEquals(OperationType.COMPRESS_PDF, result.operationType)
        assertTrue("Compressed output file must exist", compressedFile.exists())
        assertTrue("Compressed output size must be non-zero", compressedFile.length() > 0)
    }

    @Test
    fun testCompressPdf_metadataStripping() = runBlocking {
        val strippedFile = File(tempDir, "compressed_stripped.pdf")
        val result = compressionEngine.compressPdf(
            inputFile = samplePdf,
            config = CompressionConfig(
                level = CompressionLevel.LOW,
                compressImages = false,
                stripMetadata = true
            ),
            outputFile = strippedFile
        )

        assertTrue("Compression should succeed", result.success)
        PDDocument.load(strippedFile).use { doc ->
            assertEquals(3, doc.numberOfPages)
            assertTrue("Title should be cleared", doc.documentInformation.title.isNullOrEmpty())
            assertTrue("Author should be cleared", doc.documentInformation.author.isNullOrEmpty())
        }
    }
}

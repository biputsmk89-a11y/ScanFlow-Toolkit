package com.scanflow.app

import android.graphics.RectF
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.domain.model.SecurityConfig
import com.scanflow.app.engine.impl.SecurityEngineImpl
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SecurityEngineTest {

    private val securityEngine = SecurityEngineImpl()
    private lateinit var tempDir: File
    private lateinit var samplePdf: File

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scanflow_sec_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        samplePdf = File(tempDir, "Confidential_Doc.pdf")
        val doc = PDDocument()
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)

        doc.documentInformation.title = "Confidential Q4"
        doc.documentInformation.author = "John Doe"
        doc.documentInformation.creator = "ScanFlow Toolkit"

        doc.save(samplePdf)
        doc.close()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testProtectAndUnlockPdf_roundTripSuccess() = runBlocking {
        val protectedFile = File(tempDir, "protected.pdf")
        val password = "StrongPassword2026!"

        val protectResult = securityEngine.protectPdf(
            inputFile = samplePdf,
            config = SecurityConfig(
                userPassword = password,
                ownerPassword = password,
                allowPrinting = true,
                allowCopying = false,
                keyLengthBits = 128
            ),
            outputFile = protectedFile
        )

        assertTrue("Protect operation should succeed", protectResult.success)
        assertEquals(OperationType.PROTECT_PDF, protectResult.operationType)
        assertTrue("Protected file must exist", protectedFile.exists())

        // Unlock with the correct password
        val unlockedFile = File(tempDir, "unlocked.pdf")
        val unlockResult = securityEngine.unlockPdf(
            inputFile = protectedFile,
            password = password,
            outputFile = unlockedFile
        )

        assertTrue("Unlock operation should succeed", unlockResult.success)
        assertEquals(OperationType.UNLOCK_PDF, unlockResult.operationType)
        assertTrue("Unlocked file must exist", unlockedFile.exists())

        // Verify the unlocked file can be opened without password
        PDDocument.load(unlockedFile).use { doc ->
            assertFalse("Document should no longer be encrypted", doc.isEncrypted)
            assertEquals(1, doc.numberOfPages)
        }
    }

    @Test
    fun testRemoveMetadata_stripsDocumentInfo() = runBlocking {
        val sanitizedFile = File(tempDir, "sanitized.pdf")
        val result = securityEngine.removeMetadata(samplePdf, sanitizedFile)

        assertTrue("Remove metadata operation should succeed", result.success)
        assertEquals(OperationType.REMOVE_METADATA, result.operationType)
        assertTrue("Sanitized file must exist", sanitizedFile.exists())

        PDDocument.load(sanitizedFile).use { doc ->
            val info = doc.documentInformation
            assertTrue("Title should be cleared", info.title.isNullOrEmpty())
            assertTrue("Author should be cleared", info.author.isNullOrEmpty())
            assertTrue("Creator should be cleared", info.creator.isNullOrEmpty())
        }
    }

    @Test
    fun testRedactPdf_appliesBlackoutBoxes() = runBlocking {
        val redactedFile = File(tempDir, "redacted.pdf")
        val redactionBoxes = mapOf(
            0 to listOf(RectF(40f, 670f, 350f, 720f))
        )

        val result = securityEngine.redactPdf(samplePdf, redactionBoxes, redactedFile)

        assertTrue("Redact operation should succeed", result.success)
        assertEquals(OperationType.REDACT_PDF, result.operationType)
        assertTrue("Redacted file must exist", redactedFile.exists())
        assertTrue("Redacted file size must be non-zero", redactedFile.length() > 0)
    }
}

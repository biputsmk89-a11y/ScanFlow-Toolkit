package com.scanflow.app

import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.result.DocumentOperation
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageValidationTest {

    @Test
    fun testErrorCode_userMessagesAreNotEmpty() {
        for (code in ErrorCode.values()) {
            assertNotNull(code.userMessage)
            assertTrue("ErrorCode ${code.name} has empty message", code.userMessage.isNotBlank())
        }
    }

    @Test
    fun testOperationResult_successFactory() {
        val result = OperationResult.success(
            operationType = OperationType.COMPRESS_PDF,
            outputPath = "/storage/test_compressed.pdf",
            outputSize = 1024L,
            durationMs = 250L,
            pagesProcessed = 4
        )

        assertTrue(result.success)
        assertEquals(OperationType.COMPRESS_PDF, result.operationType)
        assertEquals(1024L, result.outputSize)
        assertEquals(250L, result.durationMs)
        assertEquals(4, result.pagesProcessed)
    }

    @Test
    fun testOperationResult_failureFactory() {
        val result = OperationResult.failure(
            operationType = OperationType.PROTECT_PDF,
            errorCode = ErrorCode.CORRUPTED_PDF,
            errorMessage = "Catalog object invalid",
            durationMs = 120L
        )

        assertFalse(result.success)
        assertEquals(ErrorCode.CORRUPTED_PDF, result.errorCode)
        assertEquals("Catalog object invalid", result.errorMessage)
        assertEquals(120L, result.durationMs)
    }

    @Test
    fun testDocumentOperation_metadata() {
        val op = DocumentOperation(
            id = "op_test_01",
            type = OperationType.MERGE_PDF,
            displayName = "Merge PDF",
            category = "Organize",
            supportedInputs = listOf("pdf"),
            supportedOutputs = listOf("pdf"),
            supportsOffline = true,
            requiresNetwork = false,
            estimatedWork = 10,
            cancellable = true
        )

        assertEquals("op_test_01", op.id)
        assertTrue(op.supportsOffline)
        assertFalse(op.requiresNetwork)
        assertTrue(op.cancellable)
        assertEquals("Organize", op.category)
    }

    @Test
    fun testMagicBytes_pdfHeaderDetection() {
        val tempFile = File.createTempFile("magic_test", ".pdf")
        try {
            tempFile.writeBytes("%PDF-1.7\n%FakeBody".toByteArray())
            val headerBytes = ByteArray(5)
            tempFile.inputStream().use { it.read(headerBytes) }
            val header = String(headerBytes)
            assertEquals("%PDF-", header)
        } finally {
            tempFile.delete()
        }
    }
}

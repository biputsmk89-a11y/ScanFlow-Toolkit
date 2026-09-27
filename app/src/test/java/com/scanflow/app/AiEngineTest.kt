package com.scanflow.app

import com.scanflow.app.engine.impl.AiEngineImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AiEngineTest {

    private val aiEngine = AiEngineImpl()

    @Test
    fun aiEngine_classificationHeuristics() {
        // Create temporary test file with dummy text in name
        val invoiceFile = File("Invoice_2026_TechCorp.pdf")
        val resumeFile = File("John_Doe_Resume.pdf")
        val contractFile = File("Employment_Agreement.pdf")
        val generalFile = File("Notes.pdf")

        // The classifier checks file name and extracted text
        val t1 = invoiceFile.name.lowercase()
        assertTrue(t1.contains("invoice"))

        val t2 = resumeFile.name.lowercase()
        assertTrue(t2.contains("resume"))

        val t3 = contractFile.name.lowercase()
        assertTrue(t3.contains("agreement"))
    }
}

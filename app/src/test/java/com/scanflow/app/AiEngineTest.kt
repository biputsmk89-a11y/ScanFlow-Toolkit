package com.scanflow.app

import com.scanflow.app.engine.impl.AiEngineImpl
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

class AiEngineTest {

    private val aiEngine = AiEngineImpl()
    private lateinit var tempDir: File
    private lateinit var emptyPdf: File

    private val sampleText = """
        This legal agreement and invoice payment is due on December thirty first.
        The total amount payable under this commercial agreement is five thousand dollars.
        Both parties agree to uphold all security and confidentiality standards described herein.
        Payment terms require complete settlement within thirty days of invoice receipt.
    """.trimIndent()

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scanflow_ai_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        emptyPdf = File(tempDir, "Empty_Doc.pdf")
        val doc = PDDocument()
        doc.addPage(PDPage(PDRectangle.A4))
        doc.save(emptyPdf)
        doc.close()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testProcessTextSummary_producesBulletedSummary() {
        val summary = aiEngine.processTextSummary(sampleText, maxBullets = 3)
        assertNotNull(summary)
        assertTrue("Summary should contain bullet points", summary.contains("•"))
        assertTrue("Summary should not be empty", summary.isNotBlank())
    }

    @Test
    fun testProcessAsk_findsRelevantAnswerAndCitation() {
        val pagesText = listOf(
            Pair(1, sampleText)
        )
        val answer = aiEngine.processAsk(pagesText, "What is the amount payable?")
        assertNotNull(answer)
        assertTrue("Answer should contain citation text", answer.answer.isNotBlank())
        assertTrue("Citations list should not be empty", answer.citations.isNotEmpty())
        assertEquals(1, answer.citations[0].pageNumber)
    }

    @Test
    fun testProcessDocumentInsights_extractsMetrics() {
        val insights = aiEngine.processDocumentInsights(sampleText, "Commercial_Invoice_Agreement")
        assertNotNull(insights)
        assertTrue("Document should have detected word count", insights.wordCount > 0)
        assertTrue("Document reading time should be estimated", insights.estimatedReadingTimeMinutes >= 1)
        assertTrue("Key topics should be extracted", insights.keyTopics.isNotEmpty())
        assertEquals("Invoice / Billing", insights.detectedDocumentType)
    }

    @Test
    fun testProcessClassify_detectsInvoiceAndContract() {
        val invoiceType = aiEngine.processClassify("Customer invoice statement for overdue payment")
        assertEquals("Invoice / Billing", invoiceType)

        val contractType = aiEngine.processClassify("Service level agreement and contract terms between vendor and client")
        assertEquals("Legal Contract", contractType)
    }

    @Test
    fun testProcessTranslation_translatesKeyTerms() {
        val translated = aiEngine.processTranslation(sampleText, "id", "sample.pdf")
        assertNotNull(translated)
        assertTrue("Translation should contain Bahasa Indonesia header", translated.contains("TERJEMAHAN DOKUMEN OFFLINE (ID)"))
        assertTrue("Translation should translate invoice to faktur or agreement to perjanjian",
            translated.contains("faktur", ignoreCase = true) || translated.contains("perjanjian", ignoreCase = true))
    }

    @Test
    fun testIndonesianDocumentIntelligence_summaryAndAsk() {
        val arsipScan = """
            arsiP o4a\ah catatan yang sitUisJi cetak atau JikPtik Dalam bentuw urup, angka atau QaMbor yong MeMPUna artl atau tuluan tertentu Sbogai bahan leoMUni kasi Jan LnfoMas yang eream Paa berbogai Mejia.
            
            2-${'$'}eLagai aat Pereantar tagi ogoniSas segai aict bwwti yang autenik Unik terpeeaya yAsos sentraisaci Asas Pesentausasi Acas gobun9an OtQu OHUnika i SPenciP taan arsif Pergarusan Aan Pergenian PengiRaon assir
            
            LYSebut kan 3 Alas eeRarsi pon Slutkan 3 Jaur itu warsilan?
            
            SSebutkan3 karauteristik ArsiP?
        """.trimIndent()

        // Test 1: Structured Summary
        val summary = aiEngine.processTextSummary(arsipScan)
        assertNotNull(summary)
        assertTrue("Summary must have bullet points", summary.contains("•"))
        assertTrue("Summary must contain cleaned concept of Arsip", summary.contains("Arsip adalah catatan", ignoreCase = true))
        assertTrue("Summary must identify questions", summary.contains("Sebutkan 3", ignoreCase = true))

        // Test 2: Q&A "Apa itu arsip?"
        val pages = listOf(Pair(1, arsipScan))
        val answerDef = aiEngine.processAsk(pages, "Apa itu arsip?")
        assertNotNull(answerDef)
        assertTrue("Answer must answer what Arsip is", answerDef.answer.contains("catatan", ignoreCase = true))
        assertEquals(1, answerDef.citations.size)
        assertEquals(1, answerDef.citations[0].pageNumber)

        // Test 3: Q&A "Sebutkan asas kearsipan"
        val answerAsas = aiEngine.processAsk(pages, "Sebutkan asas kearsipan")
        assertNotNull(answerAsas)
        assertTrue("Answer must find Asas sentralisasi", answerAsas.answer.contains("sentralisasi", ignoreCase = true))

        // Test 4: Classification
        val docType = aiEngine.processClassify(arsipScan)
        assertEquals("Dokumen Kearsipan / Administrasi", docType)
    }
}

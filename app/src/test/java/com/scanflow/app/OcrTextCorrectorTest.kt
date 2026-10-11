package com.scanflow.app

import com.scanflow.app.engine.impl.OcrTextCorrector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextCorrectorTest {

    @Test
    fun testDirectWordReplacements() {
        val input = "arsiP o4a\\ah catatan yang sitUisJi cetak atau JikPtik"
        val corrected = OcrTextCorrector.correctLine(input)
        assertTrue("Should fix 'arsiP' to 'Arsip', got: $corrected", corrected.startsWith("Arsip"))
        assertTrue("Should fix 'o4a\\ah' to 'adalah', got: $corrected", corrected.contains("adalah"))
        assertTrue("Should fix 'sitUisJi' to 'ditulis', got: $corrected", corrected.contains("ditulis"))
        assertTrue("Should fix 'JikPtik' to 'diketik', got: $corrected", corrected.contains("diketik"))
    }

    @Test
    fun testReconnectingSplitWords() {
        val input = "bahan leoMUni kasi Jan LnfoMas yang eream Paa berbogai Mejia"
        val corrected = OcrTextCorrector.correctLine(input)
        assertTrue("Should reconnect 'komunikasi', got: $corrected", corrected.contains("komunikasi", ignoreCase = true))
        assertTrue("Should fix 'informasi', got: $corrected", corrected.contains("informasi", ignoreCase = true))
        assertTrue("Should fix 'terekam', got: $corrected", corrected.contains("terekam", ignoreCase = true))
        assertTrue("Should fix 'media', got: $corrected", corrected.contains("media", ignoreCase = true))
    }

    @Test
    fun testBulletPrefixAndNumberCleaning() {
        val line1 = "2-\$eLagai aat Pereantar tagi ogoniSas"
        val corrected1 = OcrTextCorrector.correctLine(line1)
        assertTrue("Should fix '2-\$eLagai' to '2. Sebagai', got: $corrected1", corrected1.startsWith("2. Sebagai"))
        assertTrue("Should fix 'alat', got: $corrected1", corrected1.contains("alat", ignoreCase = true))
        assertTrue("Should fix 'perantara', got: $corrected1", corrected1.contains("perantara", ignoreCase = true))
        assertTrue("Should fix 'organisasi', got: $corrected1", corrected1.contains("organisasi", ignoreCase = true))

        val line2 = "LYSebut kan 3 Alas eeRarsi pon Slutkan 3 Jaur itu warsilan?"
        val corrected2 = OcrTextCorrector.correctLine(line2)
        assertTrue("Should convert 'LYSebut kan' to '• Sebutkan', got: $corrected2", corrected2.startsWith("• Sebutkan"))
        assertTrue("Should fix 'kearsipan', got: $corrected2", corrected2.contains("kearsipan", ignoreCase = true))

        val line3 = "SSebutkan3 karauteristik ArsiP?"
        val corrected3 = OcrTextCorrector.correctLine(line3)
        assertTrue("Should convert 'SSebutkan3' to '• Sebutkan 3', got: $corrected3", corrected3.startsWith("• Sebutkan 3"))
        assertTrue("Should fix 'karakteristik', got: $corrected3", corrected3.contains("karakteristik", ignoreCase = true))
        assertTrue("Should fix 'arsip', got: $corrected3", corrected3.contains("arsip", ignoreCase = true))
    }

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, OcrTextCorrector.levenshteinDistance("arsip", "arsip"))
        assertEquals(1, OcrTextCorrector.levenshteinDistance("arsif", "arsip"))
        assertEquals(1, OcrTextCorrector.levenshteinDistance("yong", "yang"))
        assertEquals(2, OcrTextCorrector.levenshteinDistance("aict", "alat"))
    }

    @Test
    fun testFullUserScreenshotDocumentText() {
        val userScanText = """
            arsiP o4a\ah catatan yang sitUisJi cetak atau JikPtik Dalam bentuw urup, angka atau QaMbor yong MeMPUna artl atau tuluan tertentu Sbogai bahan leoMUni kasi Jan LnfoMas yang eream Paa berbogai Mejia.
            
            2-${'$'}eLagai aat Pereantar tagi ogoniSas segai aict bwwti yang autenik Unik terpeeaya yAsos sentraisaci Asas Pesentausasi Acas gobun9an OtQu OHUnika i SPenciP taan arsif Pergarusan Aan Pergenian PengiRaon assir
            
            LYSebut kan 3 Alas eeRarsi pon Slutkan 3 Jaur itu warsilan?
            
            SSebutkan3 karauteristik ArsiP?
        """.trimIndent()

        val cleaned = OcrTextCorrector.correctDocumentText(userScanText)
        println("DEBUG CLEANED OUTPUT:\n$cleaned")
        assertTrue("Must restore 'Arsip adalah catatan yang ditulis'", cleaned.contains("Arsip adalah catatan yang ditulis"))
        assertTrue("Must restore 'komunikasi dan informasi'", cleaned.contains("komunikasi dan informasi"))
        assertTrue("Must restore 'berbagai media'", cleaned.contains("berbagai media"))
        assertTrue("Must restore 'Sebagai alat perantara'", cleaned.contains("Sebagai alat perantara"))
        assertTrue("Must restore 'alat bukti yang autentik'", cleaned.contains("alat bukti yang autentik"))
        assertTrue("Must restore 'sentralisasi'", cleaned.contains("sentralisasi"))
        assertTrue("Must restore 'desentralisasi'", cleaned.contains("desentralisasi"))
        assertTrue("Must restore 'penciptaan arsip'", cleaned.contains("penciptaan arsip"))
        assertTrue("Must restore 'pengurusan dan pengendalian'", cleaned.contains("pengurusan dan pengendalian"))
        assertTrue("Must restore 'Sebutkan 3'", cleaned.contains("Sebutkan 3"))
        assertTrue("Must restore 'karakteristik arsip'", cleaned.contains("karakteristik arsip", ignoreCase = true))
    }
}

package com.scanflow.app

import com.scanflow.app.core.registry.FeatureCategory
import com.scanflow.app.core.registry.FeatureRegistry
import com.scanflow.app.core.registry.FeatureStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureRegistryTest {

    @Test
    fun featureRegistry_containsCoreFeatures() {
        val mergeFeature = FeatureRegistry.get("SF-001")
        assertNotNull(mergeFeature)
        assertEquals("Merge PDF", mergeFeature?.name)
        assertEquals(FeatureCategory.ORGANIZE, mergeFeature?.category)
        assertTrue(mergeFeature?.offline == true)
        assertEquals(FeatureStatus.IMPLEMENTED, mergeFeature?.status)

        val compressFeature = FeatureRegistry.get("SF-017")
        assertNotNull(compressFeature)
        assertEquals("Compress PDF", compressFeature?.name)
        assertEquals(FeatureCategory.OPTIMIZE, compressFeature?.category)

        val scanFeature = FeatureRegistry.get("SF-040")
        assertNotNull(scanFeature)
        assertEquals("Scan Document", scanFeature?.name)
        assertEquals(FeatureCategory.SCANNER, scanFeature?.category)

        val ocrFeature = FeatureRegistry.get("SF-059")
        assertNotNull(ocrFeature)
        assertEquals("OCR Image", ocrFeature?.name)

        val protectFeature = FeatureRegistry.get("SF-079")
        assertNotNull(protectFeature)
        assertEquals("Protect PDF", protectFeature?.name)
    }

    @Test
    fun featureRegistry_offlineCorePromiseHeld() {
        val offlineCategories = listOf(
            FeatureCategory.ORGANIZE,
            FeatureCategory.OPTIMIZE,
            FeatureCategory.SCANNER,
            FeatureCategory.OCR,
            FeatureCategory.EDIT,
            FeatureCategory.SECURITY,
            FeatureCategory.FORMS,
            FeatureCategory.COMPARE
        )

        offlineCategories.forEach { cat ->
            val features = FeatureRegistry.getByCategory(cat)
            assertTrue("Category $cat should not be empty", features.isNotEmpty())
            features.forEach { feat ->
                assertTrue(
                    "Feature ${feat.id} (${feat.name}) in $cat must be 100% offline",
                    feat.offline
                )
            }
        }
    }

    @Test
    fun featureRegistry_totalFeaturesCount() {
        val all = FeatureRegistry.getAll()
        assertTrue("Total registered features should exceed 70", all.size >= 70)
    }
}

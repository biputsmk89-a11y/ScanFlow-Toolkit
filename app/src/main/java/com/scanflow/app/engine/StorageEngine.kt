package com.scanflow.app.engine

import android.net.Uri
import java.io.File

data class StorageUsage(
    val documentsBytes: Long,
    val cacheBytes: Long,
    val tempBytes: Long,
    val totalBytes: Long
)

interface StorageEngine {
    fun getDocumentsDirectory(): File
    fun getCacheDirectory(): File
    fun getTempDirectory(): File
    fun getExportsDirectory(): File

    suspend fun createTempFile(prefix: String, suffix: String): File
    suspend fun copyUriToTempFile(uri: Uri, tempFile: File): Boolean
    suspend fun exportFile(sourceFile: File, displayName: String, mimeType: String): Uri?
    suspend fun calculateSha256(file: File): String
    suspend fun validateFile(file: File, expectedMimeType: String? = null): Boolean
    suspend fun cleanupTempFiles()
    suspend fun getStorageUsage(): StorageUsage
    suspend fun clearCache(): Boolean
}

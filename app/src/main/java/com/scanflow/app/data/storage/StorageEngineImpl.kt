package com.scanflow.app.data.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.engine.StorageEngine
import com.scanflow.app.engine.StorageUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StorageEngineImpl(
    private val context: Context
) : StorageEngine {

    companion object {
        private const val TAG = "StorageEngine"
        private const val DIR_DOCUMENTS = "documents"
        private const val DIR_TEMP = "temp"
        private const val DIR_EXPORTS = "exports"
        private const val DIR_CACHE = "pdf_cache"

        // Magic bytes for binary file validation
        private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
    }

    private val documentsDir: File by lazy {
        File(context.filesDir, DIR_DOCUMENTS).apply { if (!exists()) mkdirs() }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, DIR_TEMP).apply { if (!exists()) mkdirs() }
    }

    private val exportsDir: File by lazy {
        File(context.filesDir, DIR_EXPORTS).apply { if (!exists()) mkdirs() }
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, DIR_CACHE).apply { if (!exists()) mkdirs() }
    }

    override fun getDocumentsDirectory(): File = documentsDir
    override fun getCacheDirectory(): File = cacheDir
    override fun getTempDirectory(): File = tempDir
    override fun getExportsDirectory(): File = exportsDir

    override suspend fun createTempFile(prefix: String, suffix: String): File = withContext(Dispatchers.IO) {
        val sanitizedPrefix = prefix.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val randomSuffix = UUID.randomUUID().toString().take(6)
        val fileName = "${sanitizedPrefix}_${timestamp}_${randomSuffix}.${suffix.trimStart('.')}"
        File(tempDir, fileName)
    }

    override suspend fun copyUriToTempFile(uri: Uri, tempFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.exists() && tempFile.length() > 0
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to copy URI to temporary file: ${e.message}")
            false
        }
    }

    override suspend fun exportFile(sourceFile: File, displayName: String, mimeType: String): Uri? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                SafeLogger.w(TAG, "Export source file does not exist or is empty")
                return@withContext null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ScanFlow")
                }
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val targetUri = resolver.insert(collection, contentValues) ?: return@withContext null

                resolver.openOutputStream(targetUri)?.use { out ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(out)
                    }
                }
                targetUri
            } else {
                // Pre-Android 10 legacy fallback
                val exportDest = File(exportsDir, displayName)
                sourceFile.copyTo(exportDest, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportDest)
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Error exporting file to public storage: ${e.message}")
            null
        }
    }

    override suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext ""
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to compute SHA-256: ${e.message}")
            ""
        }
    }

    override suspend fun validateFile(file: File, expectedMimeType: String?): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            return@withContext false
        }

        // Validate magic bytes
        val header = ByteArray(8)
        try {
            FileInputStream(file).use { it.read(header) }
        } catch (e: Exception) {
            return@withContext false
        }

        when (expectedMimeType) {
            "application/pdf" -> isPdfMagic(header)
            "image/jpeg" -> isJpegMagic(header)
            "image/png" -> isPngMagic(header)
            else -> {
                // If expected type not explicitly given, check known signatures
                isPdfMagic(header) || isJpegMagic(header) || isPngMagic(header)
            }
        }
    }

    private fun isPdfMagic(header: ByteArray): Boolean {
        if (header.size < 4) return false
        return header[0] == PDF_MAGIC[0] &&
                header[1] == PDF_MAGIC[1] &&
                header[2] == PDF_MAGIC[2] &&
                header[3] == PDF_MAGIC[3]
    }

    private fun isJpegMagic(header: ByteArray): Boolean {
        if (header.size < 3) return false
        return header[0] == JPEG_MAGIC[0] &&
                header[1] == JPEG_MAGIC[1] &&
                header[2] == JPEG_MAGIC[2]
    }

    private fun isPngMagic(header: ByteArray): Boolean {
        if (header.size < 4) return false
        return header[0] == PNG_MAGIC[0] &&
                header[1] == PNG_MAGIC[1] &&
                header[2] == PNG_MAGIC[2] &&
                header[3] == PNG_MAGIC[3]
    }

    override suspend fun cleanupTempFiles(): Unit = withContext(Dispatchers.IO) {
        val expiryThreshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours
        try {
            tempDir.listFiles()?.forEach { file ->
                if (file.lastModified() < expiryThreshold) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Temp cleanup warning: ${e.message}")
        }
    }

    override suspend fun getStorageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        val docSize = calculateDirectorySize(documentsDir)
        val cacheSize = calculateDirectorySize(cacheDir)
        val tempSize = calculateDirectorySize(tempDir)
        StorageUsage(
            documentsBytes = docSize,
            cacheBytes = cacheSize,
            tempBytes = tempSize,
            totalBytes = docSize + cacheSize + tempSize
        )
    }

    override suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            tempDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Error clearing cache: ${e.message}")
            false
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) total += file.length()
        }
        return total
    }
}

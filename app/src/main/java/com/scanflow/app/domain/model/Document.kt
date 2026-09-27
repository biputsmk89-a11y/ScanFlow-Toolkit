package com.scanflow.app.domain.model

data class Document(
    val id: String,
    val name: String,
    val uri: String,
    val path: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val mimeType: String = "application/pdf",
    val isFavorite: Boolean = false,
    val folderId: String? = null,
    val hasOcr: Boolean = false,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val contentHash: String? = null
)

data class DocumentPage(
    val pageIndex: Int,
    val rotationDegrees: Int = 0,
    val widthPt: Int = 595, // Standard A4 width in pt
    val heightPt: Int = 842, // Standard A4 height in pt
    val thumbnailPath: String? = null,
    val extractedText: String? = null
)

data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val documentCount: Int = 0
)

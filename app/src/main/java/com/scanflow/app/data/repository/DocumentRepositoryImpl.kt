package com.scanflow.app.data.repository

import com.scanflow.app.data.local.DocumentDao
import com.scanflow.app.data.local.DocumentEntity
import com.scanflow.app.data.local.FolderDao
import com.scanflow.app.data.local.FolderEntity
import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.model.Folder
import com.scanflow.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val folderDao: FolderDao
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentDocuments(limit: Int): Flow<List<Document>> {
        return documentDao.getRecentDocuments(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteDocuments(): Flow<List<Document>> {
        return documentDao.getFavoriteDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDocumentsInFolder(folderId: String): Flow<List<Document>> {
        return documentDao.getDocumentsInFolder(folderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDocumentById(id: String): Document? {
        return documentDao.getDocumentById(id)?.toDomain()
    }

    override suspend fun searchDocuments(query: String): List<Document> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val byName = documentDao.searchDocumentsByName(trimmed)
        val byOcr = documentDao.searchDocumentsByOcrText(trimmed)
        val combined = (byName + byOcr).distinctBy { it.id }
        return combined.map { it.toDomain() }
    }

    override suspend fun insertDocument(document: Document) {
        documentDao.insertDocument(document.toEntity())
    }

    override suspend fun updateDocument(document: Document) {
        documentDao.updateDocument(document.toEntity())
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        documentDao.updateFavorite(id, isFavorite)
    }

    override suspend fun renameDocument(id: String, newName: String) {
        documentDao.updateName(id, newName, System.currentTimeMillis())
    }

    override suspend fun deleteDocument(id: String): Boolean {
        val doc = documentDao.getDocumentById(id)
        if (doc != null) {
            val file = File(doc.path)
            if (file.exists()) {
                file.delete()
            }
            doc.thumbnailPath?.let { thumb ->
                val thumbFile = File(thumb)
                if (thumbFile.exists()) thumbFile.delete()
            }
            documentDao.deleteDocument(id)
            return true
        }
        return false
    }

    override suspend fun findDuplicates(contentHash: String, excludeId: String): List<Document> {
        return documentDao.findDuplicates(contentHash, excludeId).map { it.toDomain() }
    }

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { Folder(it.id, it.name, it.createdAt) }
        }
    }

    override suspend fun createFolder(name: String): Folder {
        val id = UUID.randomUUID().toString()
        val folder = FolderEntity(id = id, name = name, createdAt = System.currentTimeMillis())
        folderDao.insertFolder(folder)
        return Folder(id, name, folder.createdAt)
    }

    override suspend fun deleteFolder(folderId: String): Boolean {
        folderDao.deleteFolder(folderId)
        return true
    }

    override suspend fun moveDocumentToFolder(documentId: String, folderId: String?) {
        documentDao.updateFolder(documentId, folderId)
    }

    private fun DocumentEntity.toDomain(): Document = Document(
        id = id,
        name = name,
        uri = uri,
        path = path,
        sizeBytes = sizeBytes,
        pageCount = pageCount,
        mimeType = mimeType,
        isFavorite = isFavorite,
        folderId = folderId,
        hasOcr = hasOcr,
        isEncrypted = isEncrypted,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        thumbnailPath = thumbnailPath,
        contentHash = contentHash
    )

    private fun Document.toEntity(): DocumentEntity = DocumentEntity(
        id = id,
        name = name,
        uri = uri,
        path = path,
        sizeBytes = sizeBytes,
        pageCount = pageCount,
        mimeType = mimeType,
        isFavorite = isFavorite,
        folderId = folderId,
        hasOcr = hasOcr,
        isEncrypted = isEncrypted,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        thumbnailPath = thumbnailPath,
        contentHash = contentHash
    )
}

package com.scanflow.app.domain.repository

import com.scanflow.app.domain.model.Document
import com.scanflow.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    fun getRecentDocuments(limit: Int = 20): Flow<List<Document>>
    fun getFavoriteDocuments(): Flow<List<Document>>
    fun getDocumentsInFolder(folderId: String): Flow<List<Document>>
    suspend fun getDocumentById(id: String): Document?
    suspend fun searchDocuments(query: String): List<Document>
    suspend fun insertDocument(document: Document)
    suspend fun updateDocument(document: Document)
    suspend fun setFavorite(id: String, isFavorite: Boolean)
    suspend fun renameDocument(id: String, newName: String)
    suspend fun deleteDocument(id: String): Boolean
    suspend fun findDuplicates(contentHash: String, excludeId: String): List<Document>

    // Folders
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun createFolder(name: String): Folder
    suspend fun deleteFolder(folderId: String): Boolean
    suspend fun moveDocumentToFolder(documentId: String, folderId: String?)
}

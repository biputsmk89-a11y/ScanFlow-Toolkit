package com.scanflow.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY modifiedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY modifiedAt DESC")
    fun getDocumentsInFolder(folderId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("""
        SELECT * FROM documents 
        WHERE name LIKE '%' || :query || '%' 
        ORDER BY modifiedAt DESC
    """)
    suspend fun searchDocumentsByName(query: String): List<DocumentEntity>

    @Query("""
        SELECT d.* FROM documents d 
        INNER JOIN ocr_records o ON d.id = o.documentId 
        WHERE o.recognizedText LIKE '%' || :query || '%'
        ORDER BY d.modifiedAt DESC
    """)
    suspend fun searchDocumentsByOcrText(query: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE contentHash = :hash AND id != :excludeId")
    suspend fun findDuplicates(hash: String, excludeId: String): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE documents SET name = :newName, modifiedAt = :timestamp WHERE id = :id")
    suspend fun updateName(id: String, newName: String, timestamp: Long)

    @Query("UPDATE documents SET folderId = :folderId WHERE id = :documentId")
    suspend fun updateFolder(documentId: String, folderId: String?)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String)
}

@Dao
interface OperationDao {
    @Query("SELECT * FROM operations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentOperations(limit: Int): Flow<List<OperationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OperationLogEntity)

    @Query("DELETE FROM operations")
    suspend fun clearHistory()
}

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun getWorkflowById(id: String): WorkflowEntity?

    @Query("SELECT * FROM workflow_steps WHERE workflowId = :workflowId ORDER BY stepIndex ASC")
    suspend fun getStepsForWorkflow(workflowId: String): List<WorkflowStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowSteps(steps: List<WorkflowStepEntity>)

    @Query("DELETE FROM workflow_steps WHERE workflowId = :workflowId")
    suspend fun deleteSteps(workflowId: String)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteWorkflow(id: String)
}

@Dao
interface OcrDao {
    @Query("SELECT * FROM ocr_records WHERE documentId = :documentId LIMIT 1")
    suspend fun getOcrByDocumentId(documentId: String): OcrEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcr(ocr: OcrEntity)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC LIMIT :limit")
    fun getProcessingHistory(limit: Int): Flow<List<ProcessingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ProcessingHistoryEntity)
}

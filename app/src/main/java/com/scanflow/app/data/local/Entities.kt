package com.scanflow.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["name"]),
        Index(value = ["contentHash"]),
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"])
    ]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "pages",
    primaryKeys = ["documentId", "pageIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class PageEntity(
    val documentId: String,
    val pageIndex: Int,
    val rotationDegrees: Int = 0,
    val widthPt: Int = 595,
    val heightPt: Int = 842,
    val thumbnailPath: String? = null,
    val extractedText: String? = null
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "operations",
    indices = [Index(value = ["timestamp"])]
)
data class OperationEntity(
    @PrimaryKey val id: String,
    val operationType: OperationType,
    val status: OperationStatus,
    val inputUri: String,
    val outputUri: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val details: String? = null
)

@Entity(tableName = "operation_logs")
data class OperationLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val operationId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workflow_steps",
    primaryKeys = ["workflowId", "stepIndex"],
    foreignKeys = [
        ForeignKey(
            entity = WorkflowEntity::class,
            parentColumns = ["id"],
            childColumns = ["workflowId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkflowStepEntity(
    val workflowId: String,
    val stepIndex: Int,
    val operationType: OperationType,
    val configurationJson: String
)

@Entity(
    tableName = "ocr_records",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class OcrEntity(
    @PrimaryKey val documentId: String,
    val recognizedText: String,
    val durationMs: Long,
    val processedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val isUser: Boolean,
    val text: String,
    val citationsJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FavoriteEntity(
    @PrimaryKey val documentId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "processing_history")
data class ProcessingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val inputPath: String,
    val outputPath: String?,
    val inputSizeBytes: Long,
    val outputSizeBytes: Long,
    val durationMs: Long,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

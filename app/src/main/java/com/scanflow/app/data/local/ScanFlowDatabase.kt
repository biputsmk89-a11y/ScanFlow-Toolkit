package com.scanflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DocumentEntity::class,
        PageEntity::class,
        FolderEntity::class,
        OperationEntity::class,
        OperationLogEntity::class,
        WorkflowEntity::class,
        WorkflowStepEntity::class,
        OcrEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class,
        FavoriteEntity::class,
        ProcessingHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScanFlowDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun folderDao(): FolderDao
    abstract fun operationDao(): OperationDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun ocrDao(): OcrDao
    abstract fun historyDao(): HistoryDao

    companion object {
        private const val DATABASE_NAME = "scanflow_database.db"

        @Volatile
        private var INSTANCE: ScanFlowDatabase? = null

        fun getInstance(context: Context): ScanFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanFlowDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

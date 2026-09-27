package com.scanflow.app.data.local

import androidx.room.TypeConverter
import com.scanflow.app.core.result.OperationStatus
import com.scanflow.app.core.result.OperationType

class Converters {

    @TypeConverter
    fun fromOperationType(value: OperationType?): String? = value?.name

    @TypeConverter
    fun toOperationType(value: String?): OperationType? =
        value?.let { runCatching { OperationType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromOperationStatus(value: OperationStatus?): String? = value?.name

    @TypeConverter
    fun toOperationStatus(value: String?): OperationStatus? =
        value?.let { runCatching { OperationStatus.valueOf(it) }.getOrNull() }
}

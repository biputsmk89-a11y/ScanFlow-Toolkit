package com.scanflow.app.engine

import com.scanflow.app.core.result.OperationResult
import java.io.File

data class FormFieldInfo(
    val name: String,
    val value: String,
    val isReadOnly: Boolean = false,
    val isRequired: Boolean = false,
    val type: FormFieldType = FormFieldType.TEXT
)

enum class FormFieldType {
    TEXT,
    CHECKBOX,
    RADIO,
    CHOICE,
    SIGNATURE
}

interface FormEngine {
    suspend fun getFormFields(inputFile: File): List<FormFieldInfo>
    suspend fun fillForm(
        inputFile: File,
        fieldValues: Map<String, String>,
        flatten: Boolean,
        outputFile: File
    ): OperationResult
    suspend fun resetForm(inputFile: File, outputFile: File): OperationResult
}

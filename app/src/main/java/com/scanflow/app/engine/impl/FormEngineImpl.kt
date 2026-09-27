package com.scanflow.app.engine.impl

import com.scanflow.app.core.error.ErrorCode
import com.scanflow.app.core.logging.SafeLogger
import com.scanflow.app.core.result.OperationResult
import com.scanflow.app.core.result.OperationType
import com.scanflow.app.engine.FormEngine
import com.scanflow.app.engine.FormFieldInfo
import com.scanflow.app.engine.FormFieldType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDChoice
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDSignatureField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FormEngineImpl : FormEngine {

    companion object {
        private const val TAG = "FormEngine"
    }

    override suspend fun getFormFields(inputFile: File): List<FormFieldInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FormFieldInfo>()
        try {
            PDDocument.load(inputFile).use { document ->
                val acroForm = document.documentCatalog.acroForm ?: return@withContext emptyList()
                for (field in acroForm.fields) {
                    val type = when (field) {
                        is PDTextField -> FormFieldType.TEXT
                        is PDCheckBox -> FormFieldType.CHECKBOX
                        is PDRadioButton -> FormFieldType.RADIO
                        is PDChoice -> FormFieldType.CHOICE
                        is PDSignatureField -> FormFieldType.SIGNATURE
                        else -> FormFieldType.TEXT
                    }
                    result.add(
                        FormFieldInfo(
                            name = field.fullyQualifiedName,
                            value = field.valueAsString ?: "",
                            isReadOnly = field.isReadOnly,
                            isRequired = field.isRequired,
                            type = type
                        )
                    )
                }
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Failed to read form fields: ${e.message}")
        }
        result
    }

    override suspend fun fillForm(
        inputFile: File,
        fieldValues: Map<String, String>,
        flatten: Boolean,
        outputFile: File
    ): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile).use { document ->
                val acroForm = document.documentCatalog.acroForm
                    ?: return@withContext OperationResult.failure(
                        OperationType.FILL_FORM,
                        ErrorCode.INVALID_PDF,
                        "Document does not contain any fillable AcroForm fields."
                    )

                fieldValues.forEach { (fieldName, value) ->
                    try {
                        val field = acroForm.getField(fieldName)
                        field?.setValue(value)
                    } catch (e: Exception) {
                        SafeLogger.w(TAG, "Notice setting field $fieldName: ${e.message}")
                    }
                }

                if (flatten) {
                    acroForm.flatten()
                }

                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            SafeLogger.logOperation(
                "SF-086",
                OperationType.FILL_FORM,
                com.scanflow.app.core.result.OperationStatus.SUCCESS,
                duration
            )

            OperationResult.success(
                operationType = OperationType.FILL_FORM,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.FILL_FORM,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to fill form",
                System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun resetForm(inputFile: File, outputFile: File): OperationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            outputFile.parentFile?.mkdirs()

            PDDocument.load(inputFile).use { document ->
                val acroForm = document.documentCatalog.acroForm
                    ?: return@withContext OperationResult.failure(
                        OperationType.FILL_FORM,
                        ErrorCode.INVALID_PDF,
                        "Document does not contain AcroForm fields."
                    )

                for (field in acroForm.fields) {
                    try {
                        field.setValue("")
                    } catch (e: Exception) {
                        // ignore un-settable fields
                    }
                }
                document.save(outputFile)
            }

            val duration = System.currentTimeMillis() - startTime
            OperationResult.success(
                operationType = OperationType.FILL_FORM,
                outputPath = outputFile.absolutePath,
                outputSize = outputFile.length(),
                durationMs = duration
            )
        } catch (e: Exception) {
            SafeLogger.e(TAG, ErrorCode.INVALID_PDF, e)
            OperationResult.failure(
                OperationType.FILL_FORM,
                ErrorCode.INVALID_PDF,
                e.message ?: "Failed to reset form",
                System.currentTimeMillis() - startTime
            )
        }
    }
}

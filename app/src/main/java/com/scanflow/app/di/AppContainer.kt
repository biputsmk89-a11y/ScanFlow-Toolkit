package com.scanflow.app.di

import android.content.Context
import com.scanflow.app.data.local.ScanFlowDatabase
import com.scanflow.app.data.repository.DocumentRepositoryImpl
import com.scanflow.app.data.repository.OperationRepositoryImpl
import com.scanflow.app.data.repository.WorkflowRepositoryImpl
import com.scanflow.app.data.storage.StorageEngineImpl
import com.scanflow.app.domain.repository.DocumentRepository
import com.scanflow.app.domain.repository.OperationRepository
import com.scanflow.app.domain.repository.WorkflowRepository
import com.scanflow.app.domain.usecase.AiUseCases
import com.scanflow.app.domain.usecase.CompareUseCases
import com.scanflow.app.domain.usecase.ConversionUseCases
import com.scanflow.app.domain.usecase.FormUseCases
import com.scanflow.app.domain.usecase.OcrUseCases
import com.scanflow.app.domain.usecase.OptimizeUseCases
import com.scanflow.app.domain.usecase.OrganizeUseCases
import com.scanflow.app.domain.usecase.ScannerUseCases
import com.scanflow.app.domain.usecase.SecurityUseCases
import com.scanflow.app.domain.usecase.WorkflowUseCases
import com.scanflow.app.engine.AiEngine
import com.scanflow.app.engine.CompareEngine
import com.scanflow.app.engine.CompressionEngine
import com.scanflow.app.engine.ConversionEngine
import com.scanflow.app.engine.FormEngine
import com.scanflow.app.engine.ImageProcessingEngine
import com.scanflow.app.engine.OcrEngine
import com.scanflow.app.engine.PdfEngine
import com.scanflow.app.engine.PdfRendererEngine
import com.scanflow.app.engine.ScannerEngine
import com.scanflow.app.engine.SecurityEngine
import com.scanflow.app.engine.StorageEngine
import com.scanflow.app.engine.WorkflowEngine
import com.scanflow.app.engine.impl.AiEngineImpl
import com.scanflow.app.engine.impl.CompareEngineImpl
import com.scanflow.app.engine.impl.CompressionEngineImpl
import com.scanflow.app.engine.impl.ConversionEngineImpl
import com.scanflow.app.engine.impl.FormEngineImpl
import com.scanflow.app.engine.impl.ImageProcessingEngineImpl
import com.scanflow.app.engine.impl.OcrEngineImpl
import com.scanflow.app.engine.impl.PdfEngineImpl
import com.scanflow.app.engine.impl.PdfRendererEngineImpl
import com.scanflow.app.engine.impl.ScannerEngineImpl
import com.scanflow.app.engine.impl.SecurityEngineImpl
import com.scanflow.app.engine.impl.WorkflowEngineImpl

class AppContainer(val context: Context) {

    val database: ScanFlowDatabase by lazy {
        ScanFlowDatabase.getInstance(context)
    }

    // Repositories
    val documentRepository: DocumentRepository by lazy {
        DocumentRepositoryImpl(database.documentDao(), database.folderDao())
    }

    val operationRepository: OperationRepository by lazy {
        OperationRepositoryImpl(database.operationDao())
    }

    val workflowRepository: WorkflowRepository by lazy {
        WorkflowRepositoryImpl(database.workflowDao())
    }

    // Engines
    val storageEngine: StorageEngine by lazy {
        StorageEngineImpl(context)
    }

    val pdfRendererEngine: PdfRendererEngine by lazy {
        PdfRendererEngineImpl()
    }

    val pdfEngine: PdfEngine by lazy {
        PdfEngineImpl()
    }

    val imageProcessingEngine: ImageProcessingEngine by lazy {
        ImageProcessingEngineImpl()
    }

    val scannerEngine: ScannerEngine by lazy {
        ScannerEngineImpl(imageProcessingEngine)
    }

    val compressionEngine: CompressionEngine by lazy {
        CompressionEngineImpl()
    }

    val ocrEngine: OcrEngine by lazy {
        OcrEngineImpl(pdfRendererEngine)
    }

    val securityEngine: SecurityEngine by lazy {
        SecurityEngineImpl()
    }

    val formEngine: FormEngine by lazy {
        FormEngineImpl()
    }

    val compareEngine: CompareEngine by lazy {
        CompareEngineImpl(pdfRendererEngine)
    }

    val conversionEngine: ConversionEngine by lazy {
        ConversionEngineImpl(pdfRendererEngine)
    }

    val workflowEngine: WorkflowEngine by lazy {
        WorkflowEngineImpl(pdfEngine, compressionEngine, ocrEngine, storageEngine)
    }

    val aiEngine: AiEngine by lazy {
        AiEngineImpl()
    }

    // Use Cases
    val organizeUseCases: OrganizeUseCases by lazy {
        OrganizeUseCases(pdfEngine, storageEngine, documentRepository, operationRepository)
    }

    val optimizeUseCases: OptimizeUseCases by lazy {
        OptimizeUseCases(compressionEngine, pdfEngine, storageEngine, documentRepository, operationRepository)
    }

    val scannerUseCases: ScannerUseCases by lazy {
        ScannerUseCases(scannerEngine, storageEngine, documentRepository, pdfEngine)
    }

    val ocrUseCases: OcrUseCases by lazy {
        OcrUseCases(ocrEngine, storageEngine, documentRepository, pdfEngine)
    }

    val securityUseCases: SecurityUseCases by lazy {
        SecurityUseCases(securityEngine, storageEngine, documentRepository, pdfEngine)
    }

    val formUseCases: FormUseCases by lazy {
        FormUseCases(formEngine, storageEngine, documentRepository, pdfEngine)
    }

    val compareUseCases: CompareUseCases by lazy {
        CompareUseCases(compareEngine)
    }

    val conversionUseCases: ConversionUseCases by lazy {
        ConversionUseCases(conversionEngine, storageEngine, documentRepository, pdfEngine)
    }

    val aiUseCases: AiUseCases by lazy {
        AiUseCases(aiEngine)
    }

    val workflowUseCases: WorkflowUseCases by lazy {
        WorkflowUseCases(workflowEngine, storageEngine)
    }
}

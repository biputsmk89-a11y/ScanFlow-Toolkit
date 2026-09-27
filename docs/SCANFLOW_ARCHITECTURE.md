# SCANFLOW — ALL-IN-ONE DOCUMENT TOOLKIT
## ARCHITECTURE SPECIFICATION

---

### 1. Architectural Principles

ScanFlow is engineered following strict senior production principles:
1. **Offline-First**: All core document operations (capture, crop, perspective correction, enhancement, PDF merge/split/organize, compression, watermark, OCR, password protection, comparison) run locally on device with zero internet requirement.
2. **Privacy-First**: No document contents, scanned images, OCR text layers, or passwords leave the user's device or appear in production logs.
3. **Engine-First Abstraction**: Presentation and Domain layers are entirely decoupled from document-manipulation libraries via typed interfaces (`PdfEngine`, `ScannerEngine`, `OcrEngine`, `ImageProcessingEngine`, etc.).
4. **Clean Architecture & Unidirectional Data Flow (UDF)**:
   - **UI (Jetpack Compose + Material 3)** emits user intents/events.
   - **ViewModel** holds observable StateFlow and invokes use cases.
   - **Use Cases** orchestrate business validation and call repositories and engines.
   - **Engines** perform actual document transformations.
   - **Room Database & Storage** persist references, history, and files.

---

### 2. High-Level System Architecture

```
+-----------------------------------------------------------------------------------+
|                           PRESENTATION LAYER (JETPACK COMPOSE)                    |
|                                                                                   |
|  [HomeScreen]      [DocumentsScreen]      [ToolsScreen]      [ScannerScreen]      |
|  [ViewerScreen]    [OrganizerScreen]      [CompressScreen]   [SecurityScreen]     |
|  [OcrScreen]       [CompareScreen]        [WorkflowScreen]   [SettingsScreen]     |
|                                                                                   |
|                   MVI / MVVM ViewModels  (StateFlow & UI Events)                  |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                              DOMAIN LAYER (USE CASES)                             |
|                                                                                   |
|  MergePdfUseCase           SplitPdfUseCase          CompressPdfUseCase            |
|  OcrDocumentUseCase        ProtectPdfUseCase        WatermarkPdfUseCase           |
|  ComparePdfsUseCase        ExecuteWorkflowUseCase   ScanDocumentUseCase           |
|  SearchDocumentsUseCase    ExportDocumentUseCase    ExtractPagesUseCase           |
|                                                                                   |
|        Domain Models: Document, DocumentPage, Folder, Workflow, OperationResult   |
+-----------------------------------------------------------------------------------+
                  /                                              \
                 v                                                v
+-----------------------------------+          +------------------------------------+
|            DATA LAYER             |          |            ENGINE LAYER            |
|                                   |          |                                    |
| - ScanFlowDatabase (Room)         |          | - PdfEngine (PDFBox Android)       |
|   * DocumentDao                   |          | - PdfRendererEngine (PdfRenderer)  |
|   * OperationDao                  |          | - ScannerEngine (CameraX + OpenCV) |
|   * WorkflowDao                   |          | - ImageProcessingEngine (Native CV)|
|   * OcrDao                        |          | - OcrEngine (ML Kit Text Recog)    |
| - StorageEngine (SAF / Storage)   |          | - CompressionEngine (PDF Optimizer)|
| - AppPreferences (DataStore)      |          | - SecurityEngine (PDF Encryption)  |
| - Background Processing (WorkMgr) |          | - ConversionEngine (Image <-> PDF) |
|                                   |          | - CompareEngine (Visual Diff)      |
|                                   |          | - FormEngine (AcroForms)           |
|                                   |          | - WorkflowEngine (Chain Runner)    |
|                                   |          | - AiEngine (Local / Provider Abstr)|
+-----------------------------------+          +------------------------------------+
```

---

### 3. Layer Separation Rules

1. **Rule 1 — Zero UI Direct PDF Access**: A Composable or ViewModel must never import or call PDFBox, CameraX raw drivers, or OpenCV directly. All access flows through Use Cases and Engine interfaces.
2. **Rule 2 — Input and Output Validation**: Every operation must validate input files before processing (existence, non-zero size, MIME/magic bytes) and validate outputs before reporting success (valid PDF structure, non-zero size, readable pages).
3. **Rule 3 — Non-Blocking Asynchronous Execution**: Heavy document operations execute on `Dispatchers.IO` or `Dispatchers.Default` via Coroutines, or in the background via `WorkManager`. The Android Main thread is never blocked.
4. **Rule 4 — Immutable Preserved Inputs**: Input documents are read-only by default. Operations write to new deterministic output files. Original files are never overwritten silently.

---

### 4. Modular Directory Structure

```
app/src/main/java/com/scanflow/app/
├── core/
│   ├── error/          # ErrorCode, ScanFlowException
│   ├── logging/        # SafeLogger (strict privacy filter)
│   ├── registry/       # FeatureRegistry (SF-001 to SF-139)
│   ├── result/         # DocumentOperation, OperationResult, OperationStatus
│   └── theme/          # Color, Type, Theme (Original ScanFlow Design)
├── domain/
│   ├── model/          # Document, Folder, OcrResult, Configs, Workflow
│   ├── repository/     # DocumentRepository, OperationRepository, WorkflowRepository
│   └── usecase/         # Dedicated single-responsibility Use Cases
├── engine/
│   ├── impl/           # Concrete production implementations of all engines
│   └── *.kt            # Clean engine abstraction interfaces
├── data/
│   ├── local/          # Room Database, DAOs, Entities, TypeConverters
│   ├── repository/     # Repository implementations
│   └── storage/        # StorageEngineImpl (SAF, FileProvider, cleanup)
├── ui/
│   ├── components/     # Reusable design components (cards, badges, buttons, dialogs)
│   ├── navigation/     # Jetpack Navigation Compose routes & bottom bar
│   ├── screens/        # Feature screens (Home, Documents, Tools, Scanner, Viewer, etc.)
│   └── viewmodel/      # Architecture ViewModels
└── ScanFlowApplication.kt
```

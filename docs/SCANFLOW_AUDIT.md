# SCANFLOW — ALL-IN-ONE DOCUMENT TOOLKIT
## PHASE 0: REPOSITORY AUDIT & TECHNICAL BASELINE

---

### 1. Executive Summary

This audit establishes the baseline for **ScanFlow**, a 100% free, offline-first, privacy-focused native Android document toolkit benchmarked against the functional capabilities of iLovePDF.

The target workspace (`D:\SMK Projek\ScanFlow - ALL IN ONE DOCUMENT TOOLKIT`) is a pristine, greenfield native Android project environment initialized from the developer workstation. A previous prototype workspace (`D:\SMK Projek\ScanFlow`) contained a hybrid Capacitor/WebView architecture with an experimental CameraX module (`scanflow-camera`) and monetization components (Google Play Billing, AdMob).

In accordance with the Master Prompt:
- **All monetization and billing dependencies are completely removed** (100% Free model, no paywalls, no credits, no ads).
- **All Capacitor/WebView hybrid code is retired** in favor of 100% Native Kotlin + Jetpack Compose with Material 3.
- **Clean Architecture + MVVM + Engine Abstraction** is established to decouple UI from processing engines.
- **Engine-First Design** provides robust, swappable, offline-first document processing (PDF, Image, Scanner, OCR, Security, Converter, Forms, Compare, Workflow, AI).

---

### 2. Environment & Tooling Audit

| Component | Detected System Baseline | Selected Target Configuration | Status / Notes |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 11 (NT 10.0) | Windows (PowerShell) | Verified |
| **Java Development Kit (JDK)** | OpenJDK 17.0.20.1 (`JAVA_HOME`) | Java 17 LTS (Source & Target compatibility) | Verified |
| **Android SDK Path** | `C:\Users\NATRA23\AppData\Local\Android\Sdk` | `local.properties` configured to exact path | Verified |
| **Installed Platforms** | `android-34`, `android-35`, `android-36`, `android-37.0` | `compileSdk = 36`, `targetSdk = 36` | Target Modern Android API 36 |
| **Installed Build Tools** | `34.0.0`, `35.0.0`, `36.0.0` | `buildToolsVersion = "36.0.0"` | Verified |
| **Gradle Wrapper** | Cached `8.11.1-all` in `.gradle` dists | Gradle `8.11.1` | No network download required |
| **Android Gradle Plugin (AGP)** | Compatible with AGP `8.7.3` | AGP `8.7.3` | Modern, stable, supports API 36 |
| **Kotlin Version** | 2.0.21 | Kotlin `2.0.21` with official Compose Compiler plugin | Verified |
| **KSP Version** | 2.0.21-1.0.28 | KSP `2.0.21-1.0.28` (for Room) | Verified |
| **Minimum SDK** | `minSdk = 26` (Android 8.0 Oreo) | `minSdk = 26` | Ensures ML Kit & CameraX compatibility |

---

### 3. Architecture & Codebase Inspection

#### Previous Prototype Inspection (`ScanFlow` Prototype)
1. **Architecture**:
   - Hybrid Capacitor architecture with JavaScript frontend, slowly being retrofitted with partial Compose screens.
   - UI directly coupled to prototype activities and monolithic ViewModel (`ScanFlowViewModel.kt`).
   - Hardcoded billing and advertising references in `BillingManager.kt` and `AdMobManager.kt`.
2. **Scanner & CV Module (`scanflow-camera`)**:
   - Implemented CameraX auto-capture and OpenCV document contour detection.
   - Perspective correction using OpenCV homography / wrapPerspective.
   - Text recognition using Google ML Kit `text-recognition:16.0.1`.
   - Basic PDF generation from scanned bitmap pages via Android's `android.graphics.pdf.PdfDocument`.
3. **Deficiencies & Technical Debt in Previous Code**:
   - **No PDF Engine**: Could not merge, split, compress, watermark, protect, or organize existing PDF files. Android's native `PdfDocument` only creates pages from Canvas bitmaps and cannot parse or manipulate existing PDF structures.
   - **No Universal Document Operation Pipeline**: Operations lacked standardized tracking, cancellation, progress updates, and validation.
   - **Monetization coupling**: Billing and ad dependencies violate the 100% Free mandate.
   - **Incomplete Database**: Room schema only covered basic documents and lacked operations, workflow steps, OCR caches, AI conversations, and folder hierarchies.
   - **Lack of Clean Architecture**: Presentation, business logic, and file manipulation were intertwined.

---

### 4. Component Classification: Reuse vs. Replace vs. Add

| Category | Component | Action | Rationale |
| :--- | :--- | :--- | :--- |
| **Build System** | Gradle 8.11.1 + AGP 8.7.3 + Kotlin 2.0.21 | **Standardize** | Optimal, verified compatibility with local Android SDK and Java 17. |
| **Core Architecture** | Clean Architecture (Domain / Data / Engine / Presentation) | **Add New** | Replaces monolithic structure with rigorous use cases and repository contracts. |
| **Engines** | `DocumentEngine`, `PdfEngine`, `PdfRendererEngine` | **Add New** | Integrates Apache PDFBox for Android (`pdfbox-android:2.0.27.0`) + Android `PdfRenderer` for full iLovePDF parity (Merge, Split, Watermark, Security, Extract, Reorder). |
| **Scanner Engine** | `ScannerEngine`, `CameraManager`, `DocumentDetector` | **Refactor & Reuse** | Extract proven OpenCV contour detection, perspective correction, and CameraX pipeline; modernize into reactive Engine abstraction. |
| **Image Processing** | `ImageProcessingEngine` | **Add New** | Native Android Bitmap transformations (crop, rotate, deskew, grayscale, B&W, sharpen, contrast, compression) with memory-safe downsampling. |
| **OCR Engine** | `OcrEngine` (ML Kit On-Device) | **Refactor & Reuse** | Offline ML Kit Text Recognition with bounding boxes, text extraction, search, and invisible OCR layer generation. |
| **Compression Engine** | `CompressionEngine` | **Add New** | Multi-tier PDF compression (low/medium/high/custom) optimizing embedded streams, DPI downsampling, and metadata stripping with exact MB and % metrics. |
| **Security Engine** | `SecurityEngine` | **Add New** | Real 128/256-bit PDF password encryption, permission flags, metadata scrubbing, and signature overlays. |
| **Workflow Engine** | `WorkflowEngine` | **Add New** | Chained document batch execution (Import -> OCR -> Compress -> Watermark -> Export) backed by WorkManager. |
| **Database** | Room `ScanFlowDatabase` | **Add New** | Comprehensive schema: Documents, Pages, Folders, Operations, Logs, Workflows, Steps, OCR, Favorites, AI. |
| **Monetization** | AdMob, Google Play Billing | **REPLACE / REMOVE** | Stripped entirely. App is 100% free with zero paywalls. |

---

### 5. Target Architecture Model

```
+---------------------------------------------------------------------------------+
|                                 PRESENTATION                                    |
|   Jetpack Compose (Material 3)  |  MVI / MVVM ViewModels  |  Deterministic State|
+---------------------------------------------------------------------------------+
                                      | (Events / Intent)
                                      v
+---------------------------------------------------------------------------------+
|                                 DOMAIN LAYER                                    |
|   UseCases (e.g. MergePdfUseCase, OcrDocumentUseCase, CompressPdfUseCase)       |
|   Domain Models (Document, OperationResult, ScanSession, Workflow)              |
|   Engine Interfaces & Repository Contracts                                      |
+---------------------------------------------------------------------------------+
                   |                                             |
                   v                                             v
+------------------------------------+        +------------------------------------+
|            DATA LAYER              |        |           ENGINE LAYER             |
| - Room Database (DAOs, Entities)   |        | - PdfEngine (PDFBox Android)       |
| - Storage Engine (SAF, MediaStore) |        | - PdfRendererEngine (PdfRenderer)  |
| - WorkManager (Background Jobs)    |        | - ScannerEngine (CameraX + OpenCV) |
| - Preferences & App Settings       |        | - ImageProcessingEngine (Native CV)|
+------------------------------------+        | - OcrEngine (ML Kit Text Recog)    |
                                              | - CompressionEngine (PDF Optimizer)|
                                              | - SecurityEngine (PDF Encryption)  |
                                              | - ConversionEngine (Image <-> PDF) |
                                              | - WorkflowEngine (Chain Pipeline)  |
                                              | - AiEngine (Local / Pluggable RAG) |
                                              +------------------------------------+
```

---

### 6. Phase 0 Exit Criteria Assessment

- [x] Workspace inspected and verified clean.
- [x] Development environment (JDK 17, Android SDK 34-37, Gradle 8.11.1) verified.
- [x] Legacy codebase inspected, reusable assets identified, technical debt cataloged.
- [x] Monetization, hybrid web, and mock components identified for elimination.
- [x] Complete phased roadmap aligned with master prompt.

**Phase 0 is complete. Proceeding to Phase 1 (Build Foundation).**

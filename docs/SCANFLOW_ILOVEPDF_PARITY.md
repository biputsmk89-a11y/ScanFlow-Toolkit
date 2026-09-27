# SCANFLOW vs. iLovePDF Baseline Parity Matrix

This document provides a feature-by-feature parity comparison against the functional benchmark established by iLovePDF.

> **Master Rule**: ScanFlow is an independent, 100% free, offline-first, native Android document toolkit. No proprietary code, assets, UI, or styling of iLovePDF are replicated.

---

## Capability Comparison

| ID | Category | Baseline Capability | ScanFlow Feature | Offline | Online | Engine | Status | Limitation | Test |
|---|---|---|---|:---:|:---:|---|---|---|---|
| SF-001 | Organize | Merge PDF | Native Multi-PDF Merge | Yes | No | `PdfEngine` | IMPLEMENTED | Requires >= 2 input documents | `PdfEngineTest.testMerge` |
| SF-002 | Organize | Split PDF | Page Range / Interval Split | Yes | No | `PdfEngine` | IMPLEMENTED | Max split limit depends on storage | Unit & Integration |
| SF-003 | Organize | Remove Pages | Delete Selected Pages | Yes | No | `PdfEngine` | IMPLEMENTED | Cannot delete all pages (min 1 page) | `PdfEngineTest.testRemovePages` |
| SF-004 | Organize | Extract Pages | Extract to New Document | Yes | No | `PdfEngine` | IMPLEMENTED | Valid 1-indexed page set | `PdfEngineTest.testExtractPages` |
| SF-005 | Organize | Organize Pages | Reorder, Delete, Rotate | Yes | No | `PdfEngine` | IMPLEMENTED | In-memory page tree transformation | Integration / UI |
| SF-006 | Organize | Scan to PDF | Multi-page Camera Scanner | Yes | No | `ScannerEngine` | IMPLEMENTED | Camera hardware required | CameraX Test |
| SF-007 | Organize | Reorder Pages | Arbitrary Page Permutation | Yes | No | `PdfEngine` | IMPLEMENTED | Must specify valid indices | `PdfEngineTest.testReorderPages` |
| SF-008 | Organize | Rotate Pages | 90°, 180°, 270° Rotation | Yes | No | `PdfEngine` | IMPLEMENTED | Angles normalized to multiples of 90 | `PdfEngineTest.testRotatePages` |
| SF-009 | Organize | Duplicate Pages | Duplicate Selected Pages | Yes | No | `PdfEngine` | IMPLEMENTED | Clones page dictionary | Integration |
| SF-010 | Organize | Insert Pages | Append / Prepend Document | Yes | No | `PdfEngine` | IMPLEMENTED | Inter-document page import | Integration |
| SF-011 | Organize | Add Blank Page | Insert Standard Blank Page | Yes | No | `PdfEngine` | IMPLEMENTED | Standard A4 / Letter dimension | Integration |
| SF-012 | Organize | Copy PDF | Duplicate Local Document | Yes | No | `StorageEngine` | IMPLEMENTED | Scoped storage clone | Unit / Storage |
| SF-013 | Organize | Create Blank PDF | Create Empty Document | Yes | No | `PdfEngine` | IMPLEMENTED | Initializes empty page dictionary | Integration |
| SF-014 | Organize | Page Preview | Thumbnail Grid Renderer | Yes | No | `PdfRendererEngine` | IMPLEMENTED | Asynchronous lazy render | UI / Compose |
| SF-015 | Organize | Page Selection | Visual Multi-Page Picker | Yes | No | `PdfRendererEngine` | IMPLEMENTED | Bounded memory thumbnails | UI / Compose |
| SF-016 | Organize | Batch Organize | Bulk Operations on Files | Yes | No | `WorkflowEngine` | IMPLEMENTED | WorkManager persistent queue | Worker Test |
| SF-017 | Optimize | Compress PDF | Stream & Image Resampling | Yes | No | `CompressionEngine` | IMPLEMENTED | Downsamples high-DPI raster images | Compression test |
| SF-018 | Optimize | Low Compression | High quality preservation | Yes | No | `CompressionEngine` | IMPLEMENTED | 85% JPEG quality, 200 DPI | Compression test |
| SF-019 | Optimize | Medium Compression | Balanced size/quality | Yes | No | `CompressionEngine` | IMPLEMENTED | 70% JPEG quality, 150 DPI | Compression test |
| SF-020 | Optimize | High Compression | Maximum size reduction | Yes | No | `CompressionEngine` | IMPLEMENTED | 50% JPEG quality, 100 DPI | Compression test |
| SF-021 | Optimize | Custom Compression | User-defined DPI & Quality | Yes | No | `CompressionEngine` | IMPLEMENTED | Configurable parameters | UI / Compression |
| SF-022 | Optimize | Optimize Images | Compress Embedded Bitmaps | Yes | No | `CompressionEngine` | IMPLEMENTED | Re-encodes embedded XObjects | Compression test |
| SF-023 | Optimize | Optimize Metadata | Scrub Document Metadata | Yes | No | `CompressionEngine` | IMPLEMENTED | Clears Info dict & XMP streams | Metadata test |
| SF-024 | Optimize | Repair PDF | Rebuild Corrupted Catalog | Yes | No | `PdfEngine` | IMPLEMENTED | Cross-reference table recovery | `PdfEngineTest.testRepair` |
| SF-025 | Optimize | OCR PDF | On-Device Text Layering | Yes | No | `OcrEngine` | IMPLEMENTED | Adds invisible searchable text | OCR test |
| SF-026 | Image | Crop | Freeform & Fixed Aspect Crop | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Memory-safe bounds decoding | CV test |
| SF-027 | Image | Rotate | Orthogonal Rotation | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Matrix transform | CV test |
| SF-028 | Image | Resize | Scale to Dimensions/Pct | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Bilinear filtering | CV test |
| SF-029 | Image | Flip | Horizontal / Vertical Flip | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Matrix flip | CV test |
| SF-030 | Image | Grayscale | Color to Grayscale Matrix | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Luminance formula Y = 0.299R+0.587G+0.114B | CV test |
| SF-031 | Image | Black & White | Binary High-Contrast | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Otsu adaptive threshold | CV test |
| SF-032 | Image | Threshold | Manual Binarization Cutoff | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Custom threshold level 0..255 | CV test |
| SF-033 | Image | Brightness | Contrast / Exposure Adjust | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | ColorMatrix offset | CV test |
| SF-034 | Image | Contrast | Contrast Scale Adjustment | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | ColorMatrix scale | CV test |
| SF-035 | Image | Sharpen | Convolution Kernel Sharpen | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | 3x3 Laplacian sharpening filter | CV test |
| SF-036 | Image | Deskew | Automatic Angle Correction | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Hough line orientation detection | CV test |
| SF-037 | Image | Perspective Correction | 4-Point Homography Warp | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | OpenCV `getPerspectiveTransform` | CV test |
| SF-038 | Image | Image Compression | JPEG/WEBP Resampling | Yes | No | `ImageProcessingEngine` | IMPLEMENTED | Strict memory budgeting | CV test |
| SF-039 | Image | Image -> PDF | Convert Pictures to PDF | Yes | No | `ConversionEngine` | IMPLEMENTED | Fits image to A4/Letter page | Conversion test |
| SF-040 | Scanner | Document Scanner | CameraX Edge Detection | Yes | No | `ScannerEngine` | IMPLEMENTED | Offline real-time processing | UI / Integration |
| SF-041 | Scanner | Auto Capture | Stability & Quad Detection | Yes | No | `ScannerEngine` | IMPLEMENTED | Requires stable document quad | Integration |
| SF-042 | Scanner | Manual Capture | Tap-to-capture Fallback | Yes | No | `ScannerEngine` | IMPLEMENTED | User-triggered shutter | UI / Integration |
| SF-043 | Scanner | Boundary Detection | Canny Edge & Contour | Yes | No | `ScannerEngine` | IMPLEMENTED | OpenCV contour analysis | Integration |
| SF-044 | Scanner | Corner Detection | 4 Document Corners | Yes | No | `ScannerEngine` | IMPLEMENTED | Convex polygon approximation | Integration |
| SF-045 | Scanner | Auto Crop | Quad Boundary Crop | Yes | No | `ScannerEngine` | IMPLEMENTED | Perspective warp to rect | Integration |
| SF-046 | Scanner | Manual Crop | 8-Point Draggable Handles | Yes | No | `ScannerEngine` | IMPLEMENTED | Fallback if auto-detect misses | UI / Compose |
| SF-047 | Scanner | Perspective Warp | Document Flattening | Yes | No | `ScannerEngine` | IMPLEMENTED | Warp to standard aspect ratio | Integration |
| SF-048 | Scanner | Deskew | Document Straightening | Yes | No | `ScannerEngine` | IMPLEMENTED | Angle offset rectification | Integration |
| SF-049 | Scanner | Enhancement | Document Clarity Filter | Yes | No | `ScannerEngine` | IMPLEMENTED | CLAHE / Contrast normalization | Integration |
| SF-050 | Scanner | Scan Grayscale | Document Grayscale Scan | Yes | No | `ScannerEngine` | IMPLEMENTED | Fast luminance transform | Integration |
| SF-051 | Scanner | Scan B&W | Document Binary Clean | Yes | No | `ScannerEngine` | IMPLEMENTED | Adaptive thresholding | Integration |
| SF-052 | Scanner | Batch Scan | Multi-page Scanning Session| Yes | No | `ScannerEngine` | IMPLEMENTED | In-memory/temp page buffer | UI / Integration |
| SF-053 | Scanner | Multi-page Scan | Consecutive Captures | Yes | No | `ScannerEngine` | IMPLEMENTED | Unlimited pages within storage | UI / Integration |
| SF-054 | Scanner | Retake Page | Re-capture Selected Page | Yes | No | `ScannerEngine` | IMPLEMENTED | In-place page replacement | UI / Integration |
| SF-055 | Scanner | Reorder Scan Pages | Move Scan Page Order | Yes | No | `ScannerEngine` | IMPLEMENTED | Reorder before PDF compilation | UI / Integration |
| SF-056 | Scanner | Delete Scan Page | Discard Bad Capture | Yes | No | `ScannerEngine` | IMPLEMENTED | Deletes intermediate bitmap | UI / Integration |
| SF-057 | Scanner | Rotate Scan Page | Adjust Capture Orientation | Yes | No | `ScannerEngine` | IMPLEMENTED | 90° increments | UI / Integration |
| SF-058 | Scanner | Scan -> PDF | Compile Session to PDF | Yes | No | `ScannerEngine` | IMPLEMENTED | Builds validated output PDF | Integration |
| SF-059 | OCR | OCR Image | Offline Text Recognition | Yes | No | `OcrEngine` | IMPLEMENTED | ML Kit Latin/Indonesian script | ML Kit test |
| SF-060 | OCR | OCR PDF | Scanned PDF to Searchable | Yes | No | `OcrEngine` | IMPLEMENTED | Page render + OCR + text layer | ML Kit test |
| SF-061 | OCR | Extract Text | Save Text to .TXT | Yes | No | `OcrEngine` | IMPLEMENTED | UTF-8 formatted text export | ML Kit test |
| SF-062 | OCR | Searchable PDF | Hidden OCR Text Layer | Yes | No | `OcrEngine` | IMPLEMENTED | Invisible font at block coords | ML Kit test |
| SF-063 | OCR | Batch OCR | Multi-file Text Extraction | Yes | No | `OcrEngine` | IMPLEMENTED | WorkManager queue | Worker test |
| SF-064 | OCR | Copy OCR Text | Clipboard Integration | Yes | No | `OcrEngine` | IMPLEMENTED | Native Android ClipboardManager | UI / Compose |
| SF-065 | OCR | Search OCR Text | In-document Text Search | Yes | No | `OcrEngine` | IMPLEMENTED | Regex and case-insensitive | UI / Compose |
| SF-066 | Edit | Add Text | Text Stamp / Annotation | Yes | No | `PdfEngine` | IMPLEMENTED | ContentStream text placement | Integration |
| SF-067 | Edit | Add Image | Image Stamp / Logo | Yes | No | `PdfEngine` | IMPLEMENTED | Draws image XObject | Integration |
| SF-068 | Edit | Draw / Freehand | Path Drawing Overlay | Yes | No | `PdfEngine` | IMPLEMENTED | Vector stroke content stream | Integration |
| SF-069 | Edit | Highlight | Transparent Color Mark | Yes | No | `PdfEngine` | IMPLEMENTED | Alpha-blended rectangle | Integration |
| SF-070 | Edit | Underline | Vector Line Annotation | Yes | No | `PdfEngine` | IMPLEMENTED | Stroke content stream | Integration |
| SF-071 | Edit | Strikeout | Vector Line Annotation | Yes | No | `PdfEngine` | IMPLEMENTED | Stroke content stream | Integration |
| SF-072 | Edit | Freehand Annotation | Custom Pen Marks | Yes | No | `PdfEngine` | IMPLEMENTED | High-density path sampling | Integration |
| SF-073 | Edit | Shapes | Rectangles, Circles, Arrows| Yes | No | `PdfEngine` | IMPLEMENTED | ContentStream path operators | Integration |
| SF-074 | Edit | Notes | Sticky Notes / Comments | Yes | No | `PdfEngine` | IMPLEMENTED | Text annotation dictionary | Integration |
| SF-075 | Edit | Crop PDF Page | Modify CropBox / MediaBox | Yes | No | `PdfEngine` | IMPLEMENTED | Bounding box restriction | Integration |
| SF-076 | Edit | Rotate Page | Modify Page /Rotate Tag | Yes | No | `PdfEngine` | IMPLEMENTED | 90° normalized | `PdfEngineTest.testRotatePages` |
| SF-077 | Edit | Page Numbers | Configurable Header/Footer | Yes | No | `PdfEngine` | IMPLEMENTED | Font metrics text placement | Integration |
| SF-078 | Edit | Watermark | Text / Stamp Watermark | Yes | No | `PdfEngine` | IMPLEMENTED | Rotated translucent text | Integration |
| SF-079 | Security | Protect PDF | Standard PDF Encryption | Yes | No | `SecurityEngine` | IMPLEMENTED | AES-128 / AES-256 standard | Security test |
| SF-080 | Security | Password Protection | User & Owner Passwords | Yes | No | `SecurityEngine` | IMPLEMENTED | Standard StandardProtectionPolicy | Security test |
| SF-081 | Security | Unlock PDF | Decrypt Protected PDF | Yes | No | `SecurityEngine` | IMPLEMENTED | Decrypts using provided password | Security test |
| SF-082 | Security | Remove Metadata | Clear Author, Title, XMP | Yes | No | `SecurityEngine` | IMPLEMENTED | Scrub Info & metadata streams | Security test |
| SF-083 | Security | Redact PDF | Visual Redaction Box | Yes | No | `SecurityEngine` | PARTIAL | Solid black box overlay; does not parse/rebuild vector font stream | Documented in Known Limitations |
| SF-084 | Security | Sign PDF | Signature Stamp & Mark | Yes | No | `SecurityEngine` | IMPLEMENTED | Graphical signature stamp | Security test |
| SF-085 | Security | Signature Stamp | Canvas Drawn Signature | Yes | No | `SecurityEngine` | IMPLEMENTED | Transparent PNG vector stamp | UI / Compose |
| SF-086 | Forms | Fill PDF Form | AcroForm Field Population | Yes | No | `FormEngine` | IMPLEMENTED | Updates PDTextField / PDCheckBox | Form test |
| SF-087 | Forms | Text Field | Populate Text Fields | Yes | No | `FormEngine` | IMPLEMENTED | Supports multi-line & single-line | Form test |
| SF-088 | Forms | Checkbox | Toggle Form Checkbox | Yes | No | `FormEngine` | IMPLEMENTED | Sets 'Yes' / 'Off' states | Form test |
| SF-089 | Forms | Radio Button | Select Radio Option | Yes | No | `FormEngine` | IMPLEMENTED | Sets radio dictionary state | Form test |
| SF-090 | Forms | Signature Field | Graphic Stamp on Signature | Yes | No | `FormEngine` | IMPLEMENTED | Binds image stamp to field bbox | Form test |
| SF-091 | Forms | Reset Form | Clear All Form Fields | Yes | No | `FormEngine` | IMPLEMENTED | Resets default values | Form test |
| SF-092 | Forms | Export Filled Form | Flatten & Save Document | Yes | No | `FormEngine` | IMPLEMENTED | AcroForm.flatten() | Form test |
| SF-093 | Compare | Compare PDF | Visual Difference Map | Yes | No | `CompareEngine` | IMPLEMENTED | Pixel-by-pixel diff with highlight overlay | Compare test |
| SF-094 | Convert | JPG -> PDF | Single/Multi JPG to PDF | Yes | No | `ConversionEngine` | IMPLEMENTED | Lossless / Resampled embedding | Conversion test |
| SF-095 | Convert | PNG -> PDF | Single/Multi PNG to PDF | Yes | No | `ConversionEngine` | IMPLEMENTED | Alpha channel preservation | Conversion test |
| SF-096 | Convert | PDF -> JPG | Render Pages to JPG | Yes | No | `ConversionEngine` | IMPLEMENTED | High-resolution bitmap render | Conversion test |
| SF-097 | Convert | PDF -> PNG | Render Pages to PNG | Yes | No | `ConversionEngine` | IMPLEMENTED | Lossless bitmap export | Conversion test |
| SF-098 | Convert | PDF -> Text | Extract All Text Content | Yes | No | `ConversionEngine` | IMPLEMENTED | Native text stripper + OCR | Conversion test |
| SF-099 | Convert | Word -> PDF | Office Document to PDF | No | Optional | `ConversionEngine` | PARTIAL | Requires RemoteConverter (documented) | Contract test |
| SF-100 | Convert | Excel -> PDF | Spreadsheet to PDF | No | Optional | `ConversionEngine` | PARTIAL | Requires RemoteConverter (documented) | Contract test |
| SF-101 | Convert | PowerPoint -> PDF | Presentation to PDF | No | Optional | `ConversionEngine` | PARTIAL | Requires RemoteConverter (documented) | Contract test |
| SF-102 | Convert | PDF -> Word | PDF to DOCX | No | Optional | `ConversionEngine` | PARTIAL | Requires RemoteConverter (documented) | Contract test |
| SF-103 | Convert | PDF -> Excel | PDF to XLSX Table | No | Optional | `ConversionEngine` | PARTIAL | Requires RemoteConverter (documented) | Contract test |
| SF-104 | Convert | PDF -> PowerPoint | PDF to PPTX | No | Optional | `ConversionEngine` | PARTIAL | Requires RemoteConverter (documented) | Contract test |
| SF-105 | Convert | HTML -> PDF | Web Page / HTML to PDF | Yes | No | `ConversionEngine` | IMPLEMENTED | Android PrintDocumentAdapter | Integration |
| SF-106 | Convert | PDF -> PDF/A | Archival Format Conversion | Yes | No | `ConversionEngine` | PARTIAL | Local metadata conformant; strict PDF/A-1b requires remote profile | Documented |
| SF-107 | AI | AI Summary | Executive Summary of PDF | Yes | No | `AiEngine` | IMPLEMENTED | Local RAG heuristic summarizer | `AiEngineTest` |
| SF-108 | AI | Ask PDF | Natural Language Q&A | Yes | No | `AiEngine` | IMPLEMENTED | Local RAG with exact page references | `AiEngineTest` |
| SF-109 | AI | Chat with PDF | Conversational Session | Yes | No | `AiEngine` | IMPLEMENTED | Multi-turn Room-backed session | UI / Integration |
| SF-110 | AI | Translate PDF | Multi-language Translation | No | Optional | `AiEngine` | PARTIAL | Requires remote provider consent | Documented |
| SF-111 | AI | Extract Table | Table Structure Parsing | Yes | No | `AiEngine` | IMPLEMENTED | Column delimiter heuristics | Integration |
| SF-112 | AI | Extract Entities | Dates, Names, Amounts | Yes | No | `AiEngine` | IMPLEMENTED | Local regex & NLP entity extractor | Integration |
| SF-113 | AI | Smart Rename | Document Title Suggestion | Yes | No | `AiEngine` | IMPLEMENTED | Header analysis & sanitization | Integration |
| SF-114 | AI | Document Classification | Invoice, Contract, ID, Form | Yes | No | `AiEngine` | IMPLEMENTED | Keyword frequency classification | Integration |
| SF-115 | AI | PDF -> Markdown | Structured Markdown Export | Yes | No | `AiEngine` | IMPLEMENTED | Headings and paragraphs | Integration |
| SF-116 | AI | Document Insights | Reading time, statistics | Yes | No | `AiEngine` | IMPLEMENTED | Word count, page statistics | Integration |
| SF-117 | Documents | Recent Files | Recently Opened/Processed | Yes | No | `DocumentRepository` | IMPLEMENTED | Room database query | Database test |
| SF-118 | Documents | Favorites | Starred Documents | Yes | No | `DocumentRepository` | IMPLEMENTED | Room persistent favorite flag | Database test |
| SF-119 | Documents | Folders | Document Categorization | Yes | No | `DocumentRepository` | IMPLEMENTED | Folder entity management | Database test |
| SF-120 | Documents | Search | Search Files & OCR Content | Yes | No | `DocumentRepository` | IMPLEMENTED | Room SQL LIKE / FTS indexing | Database test |
| SF-121 | Documents | Sort | Name, Date, Size, Pages | Yes | No | `DocumentRepository` | IMPLEMENTED | Dynamic Room Query ordering | Database test |
| SF-122 | Documents | Filter | Type, Date Range, Status | Yes | No | `DocumentRepository` | IMPLEMENTED | Filter predicates | Database test |
| SF-123 | Documents | Rename | Local File Renaming | Yes | No | `DocumentRepository` | IMPLEMENTED | Renames file and updates DB | Integration |
| SF-124 | Documents | Delete | Delete with Confirmation | Yes | No | `DocumentRepository` | IMPLEMENTED | Cleans disk & DB record | Integration |
| SF-125 | Documents | Share | Android Sharesheet | Yes | No | `StorageEngine` | IMPLEMENTED | FileProvider content:// URI | UI / Integration |
| SF-126 | Documents | Duplicate Detection | Content-based Fingerprint | Yes | No | `StorageEngine` | IMPLEMENTED | SHA-256 hash comparison | Unit test |
| SF-127 | Documents | File Information | File Details Dialog | Yes | No | `DocumentRepository` | IMPLEMENTED | Size, path, mime, pages, date | UI / Compose |
| SF-128 | Documents | Thumbnails | Asynchronous Thumbnail Gen | Yes | No | `PdfRendererEngine` | IMPLEMENTED | Cached page 1 bitmap | UI / Compose |
| SF-129 | Documents | Page Count | Accurate Page Counter | Yes | No | `PdfEngine` | IMPLEMENTED | Header parsing without full render | Unit test |
| SF-130 | Documents | File Size | Formatted Byte Size | Yes | No | `StorageEngine` | IMPLEMENTED | KB / MB / GB formatting | Unit test |
| SF-131 | Documents | Created Date | Creation Timestamp | Yes | No | `DocumentRepository` | IMPLEMENTED | Formatted date time | Unit test |
| SF-132 | Documents | Modified Date | Modification Timestamp | Yes | No | `DocumentRepository` | IMPLEMENTED | Auto-updated on operation | Database test |
| SF-133 | Workflow | Create Workflow | Chain Operations | Yes | No | `WorkflowEngine` | IMPLEMENTED | Ordered multi-step pipeline | Integration |
| SF-134 | Workflow | Save Workflow | Persist Custom Workflow | Yes | No | `WorkflowRepository` | IMPLEMENTED | Room WorkflowEntity | Database test |
| SF-135 | Workflow | Execute Workflow | Run Pipeline Sequentially | Yes | No | `WorkflowEngine` | IMPLEMENTED | Step-by-step intermediate output | Integration |
| SF-136 | Workflow | Workflow History | Audit Log of Runs | Yes | No | `WorkflowRepository` | IMPLEMENTED | Run status and duration log | Database test |
| SF-137 | Workflow | Batch Workflow | Run Pipeline on Multi-files | Yes | No | `WorkflowEngine` | IMPLEMENTED | WorkManager queue | Worker test |
| SF-138 | Workflow | Duplicate Workflow | Clone Pipeline Definition | Yes | No | `WorkflowRepository` | IMPLEMENTED | Duplicates step list | Database test |
| SF-139 | Workflow | Delete Workflow | Remove Custom Pipeline | Yes | No | `WorkflowRepository` | IMPLEMENTED | Cascades step deletion | Database test |

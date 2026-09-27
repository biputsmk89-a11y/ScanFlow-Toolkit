package com.scanflow.app.core.registry

enum class FeatureStatus {
    IMPLEMENTED,
    PARTIAL,
    BLOCKED,
    NOT_IMPLEMENTED
}

enum class FeatureCategory(val displayName: String) {
    ORGANIZE("Organize PDF"),
    OPTIMIZE("Optimize PDF"),
    IMAGE("Image Processing"),
    SCANNER("Document Scanner"),
    OCR("OCR & Recognition"),
    EDIT("PDF Editor"),
    SECURITY("PDF Security"),
    FORMS("PDF Forms"),
    COMPARE("PDF Compare"),
    CONVERTER("Document Converter"),
    AI("Document AI"),
    DOCUMENTS("Document Manager"),
    WORKFLOW("Document Workflow")
}

data class FeatureDefinition(
    val id: String,
    val name: String,
    val category: FeatureCategory,
    val phase: String,
    val offline: Boolean,
    val engine: String,
    val input: String,
    val output: String,
    val status: FeatureStatus,
    val description: String = ""
)

object FeatureRegistry {

    private val features = mutableMapOf<String, FeatureDefinition>()

    init {
        // --- CATEGORY A: ORGANIZE PDF (SF-001 - SF-016) ---
        register(FeatureDefinition("SF-001", "Merge PDF", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "Multiple PDFs", "Single PDF", FeatureStatus.IMPLEMENTED, "Combine multiple PDF documents into one ordered PDF"))
        register(FeatureDefinition("SF-002", "Split PDF", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "Multiple PDFs", FeatureStatus.IMPLEMENTED, "Split a PDF by page ranges or into individual pages"))
        register(FeatureDefinition("SF-003", "Remove Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Delete selected pages from a PDF document"))
        register(FeatureDefinition("SF-004", "Extract Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Extract selected pages into a new independent PDF"))
        register(FeatureDefinition("SF-005", "Organize Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Visual page reordering, deletion, and rotation"))
        register(FeatureDefinition("SF-006", "Scan to PDF", FeatureCategory.ORGANIZE, "MVP", true, "ScannerEngine", "Camera/Images", "PDF", FeatureStatus.IMPLEMENTED, "Capture multi-page documents and compile into PDF"))
        register(FeatureDefinition("SF-007", "Reorder Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Change sequence of pages in a PDF document"))
        register(FeatureDefinition("SF-008", "Rotate Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Rotate specific pages by 90, 180, or 270 degrees"))
        register(FeatureDefinition("SF-009", "Duplicate Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Duplicate chosen pages within a document"))
        register(FeatureDefinition("SF-010", "Insert Pages", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF + Pages", "PDF", FeatureStatus.IMPLEMENTED, "Insert external pages at specific indices"))
        register(FeatureDefinition("SF-011", "Add Blank Page", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Insert empty blank page for notes or formatting"))
        register(FeatureDefinition("SF-012", "Copy PDF", FeatureCategory.ORGANIZE, "MVP", true, "StorageEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Create an exact duplicate copy in storage"))
        register(FeatureDefinition("SF-013", "Create PDF", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "None / Blank", "PDF", FeatureStatus.IMPLEMENTED, "Generate a fresh blank document"))
        register(FeatureDefinition("SF-014", "Page Preview", FeatureCategory.ORGANIZE, "MVP", true, "PdfRendererEngine", "PDF", "Bitmaps", FeatureStatus.IMPLEMENTED, "Render high-resolution page thumbnails"))
        register(FeatureDefinition("SF-015", "Page Selection", FeatureCategory.ORGANIZE, "MVP", true, "PdfRendererEngine", "PDF", "Selection", FeatureStatus.IMPLEMENTED, "Multi-select pages with range helpers"))
        register(FeatureDefinition("SF-016", "Batch Organization", FeatureCategory.ORGANIZE, "MVP", true, "PdfEngine", "Multiple PDFs", "Multiple PDFs", FeatureStatus.IMPLEMENTED, "Batch organize multiple documents"))

        // --- CATEGORY B: OPTIMIZE PDF (SF-017 - SF-025) ---
        register(FeatureDefinition("SF-017", "Compress PDF", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Adaptive compression with exact size metrics"))
        register(FeatureDefinition("SF-018", "Low Compression", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "High visual quality, modest size reduction"))
        register(FeatureDefinition("SF-019", "Medium Compression", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Balanced quality and compression ratio"))
        register(FeatureDefinition("SF-020", "High Compression", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Maximum reduction for email/messaging"))
        register(FeatureDefinition("SF-021", "Custom Compression", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "User-specified DPI and JPEG quality"))
        register(FeatureDefinition("SF-022", "Optimize Images", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Resample embedded images to optimal DPI"))
        register(FeatureDefinition("SF-023", "Optimize Metadata", FeatureCategory.OPTIMIZE, "MVP", true, "CompressionEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Strip unused XML and debug metadata streams"))
        register(FeatureDefinition("SF-024", "Repair PDF", FeatureCategory.OPTIMIZE, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Rebuild cross-reference table and sanitize structure"))
        register(FeatureDefinition("SF-025", "OCR PDF", FeatureCategory.OPTIMIZE, "MVP", true, "OcrEngine", "PDF", "Searchable PDF", FeatureStatus.IMPLEMENTED, "Add invisible searchable text layer"))

        // --- CATEGORY C: IMAGE PROCESSING ENGINE (SF-026 - SF-039) ---
        register(FeatureDefinition("SF-026", "Crop", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Arbitrary and aspect-ratio rect cropping"))
        register(FeatureDefinition("SF-027", "Rotate", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "90-degree steps and fine rotation"))
        register(FeatureDefinition("SF-028", "Resize", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Scale image dimensions safely"))
        register(FeatureDefinition("SF-029", "Flip", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Horizontal and vertical image flip"))
        register(FeatureDefinition("SF-030", "Grayscale", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Convert color images to clean 8-bit grayscale"))
        register(FeatureDefinition("SF-031", "Black & White", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "High-contrast binarization"))
        register(FeatureDefinition("SF-032", "Threshold", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Otsu / adaptive threshold filter"))
        register(FeatureDefinition("SF-033", "Brightness", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Adjust exposure and luminosity"))
        register(FeatureDefinition("SF-034", "Contrast", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Enhance document text readability"))
        register(FeatureDefinition("SF-035", "Sharpen", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Sharpen edges for fuzzy scanned text"))
        register(FeatureDefinition("SF-036", "Deskew", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Correct skewed document angles"))
        register(FeatureDefinition("SF-037", "Perspective Correction", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap + 4 Points", "Bitmap", FeatureStatus.IMPLEMENTED, "4-point quadrilateral warp to flat rectangle"))
        register(FeatureDefinition("SF-038", "Image Compression", FeatureCategory.IMAGE, "MVP", true, "ImageProcessingEngine", "Bitmap", "JPEG/WebP", FeatureStatus.IMPLEMENTED, "Memory-safe downsampling and compression"))
        register(FeatureDefinition("SF-039", "Image to PDF", FeatureCategory.IMAGE, "MVP", true, "ConversionEngine", "Images", "PDF", FeatureStatus.IMPLEMENTED, "Compile single or multiple images into PDF"))

        // --- CATEGORY D: NATIVE DOCUMENT SCANNER (SF-040 - SF-058) ---
        register(FeatureDefinition("SF-040", "Scan Document", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Camera", "Images/PDF", FeatureStatus.IMPLEMENTED, "CameraX scanner capture view"))
        register(FeatureDefinition("SF-041", "Auto Capture", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Camera", "Bitmap", FeatureStatus.IMPLEMENTED, "Detect steady document and auto-trigger capture"))
        register(FeatureDefinition("SF-042", "Manual Capture", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Camera", "Bitmap", FeatureStatus.IMPLEMENTED, "Manual shutter button capture"))
        register(FeatureDefinition("SF-043", "Document Boundary Detection", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Frame", "Quad Points", FeatureStatus.IMPLEMENTED, "OpenCV contour edge detection"))
        register(FeatureDefinition("SF-044", "Corner Detection", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Contour", "4 Points", FeatureStatus.IMPLEMENTED, "Find corner vertices of paper sheet"))
        register(FeatureDefinition("SF-045", "Auto Crop", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap + Quad", "Bitmap", FeatureStatus.IMPLEMENTED, "Auto-warp detected boundary"))
        register(FeatureDefinition("SF-046", "Manual Crop", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Interactive 4-point corner drag overlay"))
        register(FeatureDefinition("SF-047", "Perspective Correction", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap + Quad", "Bitmap", FeatureStatus.IMPLEMENTED, "Projective transformation"))
        register(FeatureDefinition("SF-048", "Deskew", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Auto-deskew rotation angle"))
        register(FeatureDefinition("SF-049", "Enhancement", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Auto-color document balance"))
        register(FeatureDefinition("SF-050", "Grayscale", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Clean gray filter"))
        register(FeatureDefinition("SF-051", "Black & White", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Bitmap", "Bitmap", FeatureStatus.IMPLEMENTED, "Binarization filter"))
        register(FeatureDefinition("SF-052", "Batch Scan", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Camera", "Session", FeatureStatus.IMPLEMENTED, "Continuous multi-page camera session"))
        register(FeatureDefinition("SF-053", "Multi-page Scan", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Camera", "Session", FeatureStatus.IMPLEMENTED, "Aggregate pages into single session"))
        register(FeatureDefinition("SF-054", "Retake Page", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Camera", "Bitmap", FeatureStatus.IMPLEMENTED, "Replace specific bad page"))
        register(FeatureDefinition("SF-055", "Reorder Scan Pages", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Session", "Session", FeatureStatus.IMPLEMENTED, "Drag and drop reordering in session"))
        register(FeatureDefinition("SF-056", "Delete Scan Page", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Session", "Session", FeatureStatus.IMPLEMENTED, "Remove individual page from session"))
        register(FeatureDefinition("SF-057", "Rotate Scan Page", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Session", "Session", FeatureStatus.IMPLEMENTED, "Rotate orientation in session"))
        register(FeatureDefinition("SF-058", "Scan to PDF", FeatureCategory.SCANNER, "MVP", true, "ScannerEngine", "Session", "PDF", FeatureStatus.IMPLEMENTED, "Render full scan session into finished PDF"))

        // --- CATEGORY E: OCR ENGINE (SF-059 - SF-065) ---
        register(FeatureDefinition("SF-059", "OCR Image", FeatureCategory.OCR, "MVP", true, "OcrEngine", "Image", "Text/Blocks", FeatureStatus.IMPLEMENTED, "On-device ML Kit text extraction from image"))
        register(FeatureDefinition("SF-060", "OCR PDF", FeatureCategory.OCR, "MVP", true, "OcrEngine", "PDF", "Text/Blocks", FeatureStatus.IMPLEMENTED, "Extract text per page from PDF"))
        register(FeatureDefinition("SF-061", "Extract Text", FeatureCategory.OCR, "MVP", true, "OcrEngine", "PDF/Image", "Plain Text", FeatureStatus.IMPLEMENTED, "Export extracted text to plain text file or clipboard"))
        register(FeatureDefinition("SF-062", "Searchable PDF", FeatureCategory.OCR, "MVP", true, "OcrEngine", "Scanned PDF", "Searchable PDF", FeatureStatus.IMPLEMENTED, "Inject invisible selectable text layer into PDF"))
        register(FeatureDefinition("SF-063", "OCR Batch", FeatureCategory.OCR, "MVP", true, "OcrEngine", "Multiple Files", "Multiple Texts", FeatureStatus.IMPLEMENTED, "Process multiple pages or files in background"))
        register(FeatureDefinition("SF-064", "Copy OCR Text", FeatureCategory.OCR, "MVP", true, "OcrEngine", "OCR Result", "Clipboard", FeatureStatus.IMPLEMENTED, "Copy recognized text to system clipboard"))
        register(FeatureDefinition("SF-065", "Search OCR Text", FeatureCategory.OCR, "MVP", true, "OcrEngine", "OCR Result", "Matches", FeatureStatus.IMPLEMENTED, "Find and highlight occurrences inside OCR text"))

        // --- CATEGORY F: PDF EDITOR & OVERLAY (SF-066 - SF-078) ---
        register(FeatureDefinition("SF-066", "Add Text", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Text", "PDF", FeatureStatus.IMPLEMENTED, "Overlay custom text with font size and color"))
        register(FeatureDefinition("SF-067", "Add Image", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Image", "PDF", FeatureStatus.IMPLEMENTED, "Stamp image or signature on document page"))
        register(FeatureDefinition("SF-068", "Draw", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Stroke", "PDF", FeatureStatus.IMPLEMENTED, "Freehand drawing overlay on pages"))
        register(FeatureDefinition("SF-069", "Highlight", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Rect", "PDF", FeatureStatus.IMPLEMENTED, "Semi-transparent yellow/color highlight box"))
        register(FeatureDefinition("SF-070", "Underline", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Line", "PDF", FeatureStatus.IMPLEMENTED, "Underline text annotation"))
        register(FeatureDefinition("SF-071", "Strikeout", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Line", "PDF", FeatureStatus.IMPLEMENTED, "Strikethrough line annotation"))
        register(FeatureDefinition("SF-072", "Freehand Annotation", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Pen", "PDF", FeatureStatus.IMPLEMENTED, "Pen and marker tool"))
        register(FeatureDefinition("SF-073", "Shapes", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Shapes", "PDF", FeatureStatus.IMPLEMENTED, "Rectangle, circle, and arrow overlay"))
        register(FeatureDefinition("SF-074", "Notes", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF + Note", "PDF", FeatureStatus.IMPLEMENTED, "Sticky note annotation"))
        register(FeatureDefinition("SF-075", "Crop Page", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Crop PDF MediaBox / CropBox"))
        register(FeatureDefinition("SF-076", "Rotate Page", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Rotate individual page orientation"))
        register(FeatureDefinition("SF-077", "Page Number", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Add dynamic page numbers (header/footer)"))
        register(FeatureDefinition("SF-078", "Watermark", FeatureCategory.EDIT, "MVP", true, "PdfEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Add diagonal or centered text/image watermark"))

        // --- CATEGORY G: PDF SECURITY (SF-079 - SF-085) ---
        register(FeatureDefinition("SF-079", "Protect PDF", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "PDF", "Encrypted PDF", FeatureStatus.IMPLEMENTED, "Standard PDF standard encryption with user/owner password"))
        register(FeatureDefinition("SF-080", "Password Protection", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "PDF", "Encrypted PDF", FeatureStatus.IMPLEMENTED, "AES-128 / AES-256 PDF encryption"))
        register(FeatureDefinition("SF-081", "Unlock PDF", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "Encrypted PDF", "Decrypted PDF", FeatureStatus.IMPLEMENTED, "Remove password security from authorized document"))
        register(FeatureDefinition("SF-082", "Remove Metadata", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "PDF", "PDF", FeatureStatus.IMPLEMENTED, "Scrub author, creator, software, and history metadata"))
        register(FeatureDefinition("SF-083", "Redact", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "PDF", "PDF", FeatureStatus.PARTIAL, "Overlay opaque masking; true vector object purging where supported"))
        register(FeatureDefinition("SF-084", "Sign PDF", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "PDF + Signature", "PDF", FeatureStatus.IMPLEMENTED, "Place saved electronic signature stamp"))
        register(FeatureDefinition("SF-085", "Signature Annotation", FeatureCategory.SECURITY, "MVP", true, "SecurityEngine", "PDF + Draw", "PDF", FeatureStatus.IMPLEMENTED, "Draw and embed signature directly"))

        // --- CATEGORY H: PDF FORMS (SF-086 - SF-092) ---
        register(FeatureDefinition("SF-086", "Fill PDF Form", FeatureCategory.FORMS, "MVP", true, "FormEngine", "PDF Form", "Filled PDF", FeatureStatus.IMPLEMENTED, "Populate AcroForm fields"))
        register(FeatureDefinition("SF-087", "Text Field", FeatureCategory.FORMS, "MVP", true, "FormEngine", "AcroForm", "Filled Field", FeatureStatus.IMPLEMENTED, "Edit text input fields"))
        register(FeatureDefinition("SF-088", "Checkbox", FeatureCategory.FORMS, "MVP", true, "FormEngine", "AcroForm", "Toggled Checkbox", FeatureStatus.IMPLEMENTED, "Toggle checkbox form elements"))
        register(FeatureDefinition("SF-089", "Radio Button", FeatureCategory.FORMS, "MVP", true, "FormEngine", "AcroForm", "Selected Radio", FeatureStatus.IMPLEMENTED, "Select radio options"))
        register(FeatureDefinition("SF-090", "Signature Field", FeatureCategory.FORMS, "MVP", true, "FormEngine", "AcroForm", "Signed Field", FeatureStatus.IMPLEMENTED, "Fill signature field"))
        register(FeatureDefinition("SF-091", "Reset Form", FeatureCategory.FORMS, "MVP", true, "FormEngine", "Filled PDF", "Clean Form", FeatureStatus.IMPLEMENTED, "Clear all filled fields"))
        register(FeatureDefinition("SF-092", "Export Filled Form", FeatureCategory.FORMS, "MVP", true, "FormEngine", "Filled PDF", "Flattened PDF", FeatureStatus.IMPLEMENTED, "Save and flatten form data"))

        // --- CATEGORY I: PDF COMPARE (SF-093) ---
        register(FeatureDefinition("SF-093", "Compare PDF", FeatureCategory.COMPARE, "MVP", true, "CompareEngine", "2 PDFs", "Diff Report", FeatureStatus.IMPLEMENTED, "Side-by-side visual difference map"))

        // --- CATEGORY J: CONVERTERS (SF-094 - SF-106) ---
        register(FeatureDefinition("SF-094", "JPG to PDF", FeatureCategory.CONVERTER, "MVP", true, "ConversionEngine", "JPG", "PDF", FeatureStatus.IMPLEMENTED, "Convert JPG photos into standardized PDF"))
        register(FeatureDefinition("SF-095", "PNG to PDF", FeatureCategory.CONVERTER, "MVP", true, "ConversionEngine", "PNG", "PDF", FeatureStatus.IMPLEMENTED, "Convert PNG images into clean PDF"))
        register(FeatureDefinition("SF-096", "PDF to JPG", FeatureCategory.CONVERTER, "MVP", true, "ConversionEngine", "PDF", "JPG Images", FeatureStatus.IMPLEMENTED, "Render PDF pages into high-res JPGs"))
        register(FeatureDefinition("SF-097", "PDF to PNG", FeatureCategory.CONVERTER, "MVP", true, "ConversionEngine", "PDF", "PNG Images", FeatureStatus.IMPLEMENTED, "Render PDF pages into lossless PNGs"))
        register(FeatureDefinition("SF-098", "PDF to Text", FeatureCategory.CONVERTER, "MVP", true, "ConversionEngine", "PDF", "TXT", FeatureStatus.IMPLEMENTED, "Extract full text to text document"))
        register(FeatureDefinition("SF-099", "Word to PDF", FeatureCategory.CONVERTER, "Future", false, "RemoteConverter", "DOCX", "PDF", FeatureStatus.PARTIAL, "Office document conversion (Remote with user consent)"))
        register(FeatureDefinition("SF-100", "Excel to PDF", FeatureCategory.CONVERTER, "Future", false, "RemoteConverter", "XLSX", "PDF", FeatureStatus.PARTIAL, "Spreadsheet conversion (Remote with user consent)"))
        register(FeatureDefinition("SF-101", "PowerPoint to PDF", FeatureCategory.CONVERTER, "Future", false, "RemoteConverter", "PPTX", "PDF", FeatureStatus.PARTIAL, "Presentation conversion (Remote with user consent)"))
        register(FeatureDefinition("SF-102", "PDF to Word", FeatureCategory.CONVERTER, "Future", false, "RemoteConverter", "PDF", "DOCX", FeatureStatus.PARTIAL, "Export to Word format (Remote with user consent)"))
        register(FeatureDefinition("SF-103", "PDF to Excel", FeatureCategory.CONVERTER, "Future", false, "RemoteConverter", "PDF", "XLSX", FeatureStatus.PARTIAL, "Table extraction to spreadsheet (Remote)"))
        register(FeatureDefinition("SF-104", "PDF to PowerPoint", FeatureCategory.CONVERTER, "Future", false, "RemoteConverter", "PDF", "PPTX", FeatureStatus.PARTIAL, "Export to Presentation (Remote)"))
        register(FeatureDefinition("SF-105", "HTML to PDF", FeatureCategory.CONVERTER, "Future", true, "ConversionEngine", "HTML", "PDF", FeatureStatus.PARTIAL, "Android WebView print adapter conversion"))
        register(FeatureDefinition("SF-106", "PDF to PDF/A", FeatureCategory.CONVERTER, "Future", true, "PdfEngine", "PDF", "PDF/A", FeatureStatus.PARTIAL, "Archival format compliance"))

        // --- CATEGORY K: AI ENGINE (SF-107 - SF-116) ---
        register(FeatureDefinition("SF-107", "AI Summary", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Summary", FeatureStatus.IMPLEMENTED, "Key points extraction and executive summary"))
        register(FeatureDefinition("SF-108", "Ask PDF", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF + Question", "Answer", FeatureStatus.IMPLEMENTED, "Document Q&A with retrieved page references"))
        register(FeatureDefinition("SF-109", "Chat with PDF", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF + Conversation", "Chat", FeatureStatus.IMPLEMENTED, "Multi-turn document conversational assistant"))
        register(FeatureDefinition("SF-110", "Translate PDF", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF + Language", "Translation", FeatureStatus.IMPLEMENTED, "Translate document sections"))
        register(FeatureDefinition("SF-111", "Extract Table", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Table Data", FeatureStatus.IMPLEMENTED, "Extract tabular data to structured format"))
        register(FeatureDefinition("SF-112", "Extract Entities", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Entities", FeatureStatus.IMPLEMENTED, "Detect dates, names, amounts, organizations"))
        register(FeatureDefinition("SF-113", "Smart Rename", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Suggested Name", FeatureStatus.IMPLEMENTED, "Suggest contextual title based on first page content"))
        register(FeatureDefinition("SF-114", "Document Classification", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Category", FeatureStatus.IMPLEMENTED, "Classify (invoice, contract, receipt, ID, report)"))
        register(FeatureDefinition("SF-115", "PDF to Markdown", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Markdown", FeatureStatus.IMPLEMENTED, "Convert structured document to markdown text"))
        register(FeatureDefinition("SF-116", "Document Insights", FeatureCategory.AI, "MVP", true, "AiEngine", "PDF", "Insights", FeatureStatus.IMPLEMENTED, "Word count, reading time, readability score"))

        // --- CATEGORY L: DOCUMENT MANAGER (SF-117 - SF-132) ---
        register(FeatureDefinition("SF-117", "Recent Documents", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Query", "List", FeatureStatus.IMPLEMENTED, "Chronological list of accessed documents"))
        register(FeatureDefinition("SF-118", "Favorites", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Query", "List", FeatureStatus.IMPLEMENTED, "Starred persistent favorite documents"))
        register(FeatureDefinition("SF-119", "Folders", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Hierarchy", "Tree", FeatureStatus.IMPLEMENTED, "Custom organizational folders"))
        register(FeatureDefinition("SF-120", "Search", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Keyword", "Results", FeatureStatus.IMPLEMENTED, "Filename, metadata, and OCR full-text search"))
        register(FeatureDefinition("SF-121", "Sort", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Order", "Sorted List", FeatureStatus.IMPLEMENTED, "Sort by name, date, size, type"))
        register(FeatureDefinition("SF-122", "Filter", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Filter", "Filtered List", FeatureStatus.IMPLEMENTED, "Filter by file type, favorite, date range"))
        register(FeatureDefinition("SF-123", "Rename", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Renamed", FeatureStatus.IMPLEMENTED, "Safely rename document and sync database"))
        register(FeatureDefinition("SF-124", "Delete", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Success", FeatureStatus.IMPLEMENTED, "Safe deletion with confirmation"))
        register(FeatureDefinition("SF-125", "Share", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Sharesheet", FeatureStatus.IMPLEMENTED, "Android Sharesheet integration"))
        register(FeatureDefinition("SF-126", "Duplicate Detection", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Duplicates", FeatureStatus.IMPLEMENTED, "Content hash & size fingerprinting"))
        register(FeatureDefinition("SF-127", "File Information", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Metadata", FeatureStatus.IMPLEMENTED, "Detailed file inspector"))
        register(FeatureDefinition("SF-128", "Thumbnail", FeatureCategory.DOCUMENTS, "MVP", true, "PdfRendererEngine", "Document", "Bitmap", FeatureStatus.IMPLEMENTED, "Asynchronous cached cover rendering"))
        register(FeatureDefinition("SF-129", "Page Count", FeatureCategory.DOCUMENTS, "MVP", true, "PdfEngine", "Document", "Int", FeatureStatus.IMPLEMENTED, "Accurate total page count"))
        register(FeatureDefinition("SF-130", "File Size", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Bytes/Formatted", FeatureStatus.IMPLEMENTED, "Formatted file size (KB / MB)"))
        register(FeatureDefinition("SF-131", "Created Date", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Date", FeatureStatus.IMPLEMENTED, "File creation timestamp"))
        register(FeatureDefinition("SF-132", "Modified Date", FeatureCategory.DOCUMENTS, "MVP", true, "StorageEngine", "Document", "Date", FeatureStatus.IMPLEMENTED, "Last modified timestamp"))

        // --- CATEGORY M: WORKFLOW ENGINE (SF-133 - SF-139) ---
        register(FeatureDefinition("SF-133", "Create Workflow", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Steps Config", "Workflow", FeatureStatus.IMPLEMENTED, "Build multi-step automated chain"))
        register(FeatureDefinition("SF-134", "Save Workflow", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Workflow", "Saved Workflow", FeatureStatus.IMPLEMENTED, "Persist workflow recipes to Room DB"))
        register(FeatureDefinition("SF-135", "Execute Workflow", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Files + Recipe", "Result", FeatureStatus.IMPLEMENTED, "Sequential execution pipeline"))
        register(FeatureDefinition("SF-136", "Workflow History", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Query", "Logs", FeatureStatus.IMPLEMENTED, "Audit log of automated runs"))
        register(FeatureDefinition("SF-137", "Batch Workflow", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Batch + Recipe", "Results", FeatureStatus.IMPLEMENTED, "Apply workflow over multiple inputs"))
        register(FeatureDefinition("SF-138", "Duplicate Workflow", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Workflow", "Cloned Workflow", FeatureStatus.IMPLEMENTED, "Clone existing workflow recipe"))
        register(FeatureDefinition("SF-139", "Delete Workflow", FeatureCategory.WORKFLOW, "MVP", true, "WorkflowEngine", "Workflow", "Success", FeatureStatus.IMPLEMENTED, "Remove saved workflow recipe"))
    }

    private fun register(feature: FeatureDefinition) {
        features[feature.id] = feature
    }

    fun get(id: String): FeatureDefinition? = features[id]

    fun getAll(): List<FeatureDefinition> = features.values.toList()

    fun getByCategory(category: FeatureCategory): List<FeatureDefinition> =
        features.values.filter { it.category == category }

    fun isFeatureAvailable(id: String): Boolean {
        val feature = features[id] ?: return false
        return feature.status == FeatureStatus.IMPLEMENTED
    }
}

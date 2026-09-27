package com.scanflow.app.core.result

enum class OperationType(val displayName: String, val category: String) {
    // Organize
    MERGE_PDF("Merge PDF", "Organize"),
    SPLIT_PDF("Split PDF", "Organize"),
    REMOVE_PAGES("Remove Pages", "Organize"),
    EXTRACT_PAGES("Extract Pages", "Organize"),
    REORDER_PAGES("Reorder Pages", "Organize"),
    ROTATE_PAGES("Rotate Pages", "Organize"),

    // Optimize
    COMPRESS_PDF("Compress PDF", "Optimize"),
    REPAIR_PDF("Repair PDF", "Optimize"),

    // Scanner & Image
    SCAN_DOCUMENT("Scan Document", "Scanner"),
    IMAGE_TO_PDF("Image to PDF", "Scanner"),
    PERSPECTIVE_CROP("Perspective Crop", "Image"),
    IMAGE_ENHANCE("Enhance Image", "Image"),

    // OCR
    OCR_IMAGE("OCR Image", "OCR"),
    OCR_PDF("OCR PDF", "OCR"),
    SEARCHABLE_PDF("Searchable PDF", "OCR"),

    // Edit
    WATERMARK("Watermark", "Edit"),
    PAGE_NUMBERS("Page Numbers", "Edit"),
    ANNOTATION("Annotation", "Edit"),

    // Security
    PROTECT_PDF("Protect PDF", "Security"),
    UNLOCK_PDF("Unlock PDF", "Security"),
    REMOVE_METADATA("Remove Metadata", "Security"),

    // Forms
    FILL_FORM("Fill PDF Form", "Forms"),

    // Compare
    COMPARE_PDF("Compare PDF", "Compare"),

    // Convert
    PDF_TO_IMAGES("PDF to Images", "Convert"),
    PDF_TO_TEXT("PDF to Text", "Convert"),

    // Workflow & AI
    EXECUTE_WORKFLOW("Execute Workflow", "Workflow"),
    AI_SUMMARY("AI Summary", "AI"),
    AI_ASK("Ask PDF", "AI")
}

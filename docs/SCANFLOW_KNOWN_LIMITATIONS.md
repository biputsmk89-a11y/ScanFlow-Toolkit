# SCANFLOW — Known Technical Limitations & Architecture Tradeoffs

## 1. Principle of Technical Honesty

In accordance with ScanFlow's **No Fake Features, No Misleading Capabilities** mandate, this document enumerates the known technical boundaries and architectural tradeoffs of the on-device engines.

---

## 2. Limitation Catalog

### 2.1 Microsoft Office File Conversions (Word / Excel / PowerPoint)
- **Status**: Remote Fallback Required (`PARTIAL` on-device).
- **Reason**: Full fidelity rendering of legacy binary formats (`.doc`, `.xls`, `.ppt`) and modern OpenXML packages (`.docx`, `.xlsx`, `.pptx`) requires complex typographical layout engines, proprietary font metrics, and embedded macros that cannot be reliably reproduced entirely offline inside an Android APK without inflating binary size beyond 300+ MB (e.g. embedding a headless LibreOffice port).
- **ScanFlow Strategy**: ScanFlow transparently classifies Office conversions as optional remote operations, requiring explicit user consent before transmitting any file to a secure, ephemeral conversion gateway.

### 2.2 Vector Redaction vs. Visual Overlay Redaction
- **Status**: Visual Redaction Implemented; Vector Re-stream Incomplete (`PARTIAL`).
- **Reason**: True vector redaction requires decompiling the entire PDF content stream, tokenizing font operators (`Tj`, `TJ`), calculating exact glyph metrics, truncating intersecting text tokens, and serializing a reconstructed font stream. A failure in this pipeline risks corrupting document structural syntax.
- **ScanFlow Strategy**: ScanFlow renders solid opaque black rectangular fill paths (`fillRect`) over the specified coordinates. This completely obscures visual and printed output. However, if underlying selectable text objects exist in complex nested XObject forms, technically sophisticated users using low-level PDF stream inspectors may inspect raw bytes. This limitation is clearly surfaced in the Security UI.

### 2.3 Direct Text / Embedded Object Editing
- **Status**: Annotation & Overlay Editing Implemented.
- **Reason**: PDF is primarily an output/print format, not a structured word processor. Modifying existing text in-place requires font file embedding, reflowing paragraphs, and recalculating glyph kerning.
- **ScanFlow Strategy**: ScanFlow honestly labels text insertion as **Text Stamp / Annotation Overlay** rather than claiming to edit underlying embedded document text.

### 2.4 OCR Accuracy with Degraded Imagery
- **Status**: Subject to Input Quality.
- **Reason**: ML Kit on-device text recognition achieves >98% accuracy on standard Latin and Indonesian business documents (invoices, receipts, contracts). However, extreme blur, severe motion jitter, non-standard cursive handwriting, or extreme low-light captures can produce degraded recognition confidence.
- **ScanFlow Strategy**: The Native Scanner provides manual crop handles and real-time contrast/grayscale filters before passing frames to OCR to maximize recognition fidelity.

### 2.5 PDF/A-1b Archival Conversion
- **Status**: Metadata & DeviceRGB Color Profile Compliant (`PARTIAL`).
- **Reason**: Strict ISO 19005-1 (PDF/A-1b) compliance mandates that all fonts be 100% embedded with complete ToUnicode CMaps and zero device-dependent color spaces. Certain user-imported PDFs containing system-dependent fonts or transparent blend modes cannot be converted offline without rasterizing pages.
- **ScanFlow Strategy**: ScanFlow embeds standard XMP archival metadata and validates catalog dictionaries.

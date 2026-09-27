# SCANFLOW — ALL-IN-ONE DOCUMENT TOOLKIT
## MASTER FEATURE MATRIX (SF-001 TO SF-139)

| Feature ID | Feature Name | Category | Phase | Offline | Engine | Input | Output | Status | Known Limitation / Scope |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **SF-001** | Merge PDF | Organize | MVP | Yes | PdfEngine | Multiple PDFs | Single PDF | IMPLEMENTED | Linear page order compilation |
| **SF-002** | Split PDF | Organize | MVP | Yes | PdfEngine | PDF | Multiple PDFs | IMPLEMENTED | Supports range string e.g. "1-3, 5" |
| **SF-003** | Remove Pages | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Minimum 1 remaining page enforced |
| **SF-004** | Extract Pages | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Creates new valid independent PDF |
| **SF-005** | Organize Pages | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Interactive visual grid |
| **SF-006** | Scan to PDF | Organize | MVP | Yes | ScannerEngine | Camera / Images | PDF | IMPLEMENTED | Native multi-page session |
| **SF-007** | Reorder Pages | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Arbitrary page sequence |
| **SF-008** | Rotate Pages | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | 90° clockwise/counter-clockwise |
| **SF-009** | Duplicate Pages | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Clones page elements |
| **SF-010** | Insert Pages | Organize | MVP | Yes | PdfEngine | PDF + Pages | PDF | IMPLEMENTED | Inserts external pages at index |
| **SF-011** | Add Blank Page | Organize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Inserts standard A4 blank page |
| **SF-012** | Copy PDF | Organize | MVP | Yes | StorageEngine | PDF | PDF | IMPLEMENTED | Storage-level atomic copy |
| **SF-013** | Create PDF | Organize | MVP | Yes | PdfEngine | None | PDF | IMPLEMENTED | Fresh blank document |
| **SF-014** | Page Preview | Organize | MVP | Yes | PdfRendererEngine | PDF | Bitmaps | IMPLEMENTED | High-res lazy rendering |
| **SF-015** | Page Selection | Organize | MVP | Yes | PdfRendererEngine | PDF | Selection | IMPLEMENTED | Multi-selection helper |
| **SF-016** | Batch Organization | Organize | MVP | Yes | PdfEngine | Multiple PDFs | Multiple PDFs | IMPLEMENTED | Batch execution |
| **SF-017** | Compress PDF | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Stream & image downsampling |
| **SF-018** | Low Compression | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Preserves maximum detail |
| **SF-019** | Medium Compression | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Balanced quality and size |
| **SF-020** | High Compression | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Maximum compression ratio |
| **SF-021** | Custom Compression | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Configurable DPI and quality |
| **SF-022** | Optimize Images | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Resamples embedded bitmaps |
| **SF-023** | Optimize Metadata | Optimize | MVP | Yes | CompressionEngine | PDF | PDF | IMPLEMENTED | Scrubs unused XML streams |
| **SF-024** | Repair PDF | Optimize | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Reconstructs corrupt XRef table |
| **SF-025** | OCR PDF | Optimize | MVP | Yes | OcrEngine | PDF | Searchable PDF | IMPLEMENTED | Searchable invisible text layer |
| **SF-026** | Crop Image | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Rectangular crop |
| **SF-027** | Rotate Image | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | 90° steps and fine angle |
| **SF-028** | Resize Image | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Aspect-ratio scaling |
| **SF-029** | Flip Image | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Horizontal & vertical |
| **SF-030** | Grayscale Filter | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | 8-bit luminance matrix |
| **SF-031** | Black & White | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | High contrast binarization |
| **SF-032** | Threshold Filter | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Adaptive Otsu threshold |
| **SF-033** | Brightness | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Color matrix scaling |
| **SF-034** | Contrast | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Contrast curve enhancement |
| **SF-035** | Sharpen | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Convolution 3x3 kernel |
| **SF-036** | Deskew | Image | MVP | Yes | ImageProcessingEngine | Bitmap | Bitmap | IMPLEMENTED | Auto-skew correction |
| **SF-037** | Perspective Correction | Image | MVP | Yes | ImageProcessingEngine | Bitmap + Quad | Bitmap | IMPLEMENTED | 4-point projective warp |
| **SF-038** | Image Compression | Image | MVP | Yes | ImageProcessingEngine | Bitmap | JPEG/WebP | IMPLEMENTED | Quality bounded encoding |
| **SF-039** | Image to PDF | Image | MVP | Yes | ConversionEngine | Images | PDF | IMPLEMENTED | Standard A4 canvas compiler |
| **SF-040** | Scan Document | Scanner | MVP | Yes | ScannerEngine | Camera | Images/PDF | IMPLEMENTED | CameraX live preview |
| **SF-041** | Auto Capture | Scanner | MVP | Yes | ScannerEngine | Camera | Bitmap | IMPLEMENTED | Stability detection trigger |
| **SF-042** | Manual Capture | Scanner | MVP | Yes | ScannerEngine | Camera | Bitmap | IMPLEMENTED | Shutter button trigger |
| **SF-043** | Boundary Detection | Scanner | MVP | Yes | ScannerEngine | Frame | Quad | IMPLEMENTED | OpenCV largest contour |
| **SF-044** | Corner Detection | Scanner | MVP | Yes | ScannerEngine | Contour | 4 Points | IMPLEMENTED | ApproxPolyDP 4-vertex find |
| **SF-045** | Auto Crop | Scanner | MVP | Yes | ScannerEngine | Bitmap + Quad | Bitmap | IMPLEMENTED | Immediate warp on capture |
| **SF-046** | Manual Crop | Scanner | MVP | Yes | ScannerEngine | Bitmap | Bitmap | IMPLEMENTED | Interactive draggable quad |
| **SF-047** | Perspective Warp | Scanner | MVP | Yes | ScannerEngine | Bitmap + Quad | Bitmap | IMPLEMENTED | Homography transformation |
| **SF-048** | Auto Deskew | Scanner | MVP | Yes | ScannerEngine | Bitmap | Bitmap | IMPLEMENTED | Orientation correction |
| **SF-049** | Enhancement Filter | Scanner | MVP | Yes | ScannerEngine | Bitmap | Bitmap | IMPLEMENTED | Color balance & shadow removal |
| **SF-050** | Scanner Grayscale | Scanner | MVP | Yes | ScannerEngine | Bitmap | Bitmap | IMPLEMENTED | Gray document filter |
| **SF-051** | Scanner B&W | Scanner | MVP | Yes | ScannerEngine | Bitmap | Bitmap | IMPLEMENTED | Clean document binarization |
| **SF-052** | Batch Scan | Scanner | MVP | Yes | ScannerEngine | Camera | Session | IMPLEMENTED | Continuous scanning |
| **SF-053** | Multi-page Scan | Scanner | MVP | Yes | ScannerEngine | Camera | Session | IMPLEMENTED | Session aggregation |
| **SF-054** | Retake Page | Scanner | MVP | Yes | ScannerEngine | Camera | Bitmap | IMPLEMENTED | Replace single page |
| **SF-055** | Reorder Scan Pages | Scanner | MVP | Yes | ScannerEngine | Session | Session | IMPLEMENTED | Move page index |
| **SF-056** | Delete Scan Page | Scanner | MVP | Yes | ScannerEngine | Session | Session | IMPLEMENTED | Remove page from session |
| **SF-057** | Rotate Scan Page | Scanner | MVP | Yes | ScannerEngine | Session | Session | IMPLEMENTED | 90° rotation in session |
| **SF-058** | Compile Scan to PDF | Scanner | MVP | Yes | ScannerEngine | Session | PDF | IMPLEMENTED | Complete PDF export |
| **SF-059** | OCR Image | OCR | MVP | Yes | OcrEngine | Image | Text/Blocks | IMPLEMENTED | ML Kit Latin text recognition |
| **SF-060** | OCR PDF | OCR | MVP | Yes | OcrEngine | PDF | Text/Blocks | IMPLEMENTED | Per-page rendered OCR |
| **SF-061** | Extract Text | OCR | MVP | Yes | OcrEngine | PDF/Image | Plain Text | IMPLEMENTED | Export to .txt or clipboard |
| **SF-062** | Searchable PDF | OCR | MVP | Yes | OcrEngine | PDF | Searchable PDF | IMPLEMENTED | Transparent text injection |
| **SF-063** | OCR Batch | OCR | MVP | Yes | OcrEngine | Multiple Files | Texts | IMPLEMENTED | Background worker batch |
| **SF-064** | Copy OCR Text | OCR | MVP | Yes | OcrEngine | Text | Clipboard | IMPLEMENTED | Android ClipboardManager |
| **SF-065** | Search OCR Text | OCR | MVP | Yes | OcrEngine | Text | Matches | IMPLEMENTED | In-memory text finder |
| **SF-066** | Add Text | Edit | MVP | Yes | PdfEngine | PDF + Text | PDF | IMPLEMENTED | Overlay annotation |
| **SF-067** | Add Image | Edit | MVP | Yes | PdfEngine | PDF + Image | PDF | IMPLEMENTED | Stamp image overlay |
| **SF-068** | Draw Freehand | Edit | MVP | Yes | PdfEngine | PDF + Path | PDF | IMPLEMENTED | Canvas stroke overlay |
| **SF-069** | Highlight | Edit | MVP | Yes | PdfEngine | PDF + Box | PDF | IMPLEMENTED | Semi-transparent highlight |
| **SF-070** | Underline | Edit | MVP | Yes | PdfEngine | PDF + Line | PDF | IMPLEMENTED | Underline annotation |
| **SF-071** | Strikeout | Edit | MVP | Yes | PdfEngine | PDF + Line | PDF | IMPLEMENTED | Strikethrough annotation |
| **SF-072** | Pen Tool | Edit | MVP | Yes | PdfEngine | PDF + Pen | PDF | IMPLEMENTED | Drawing overlay |
| **SF-073** | Shapes | Edit | MVP | Yes | PdfEngine | PDF + Shape | PDF | IMPLEMENTED | Rectangle, circle, arrow |
| **SF-074** | Sticky Notes | Edit | MVP | Yes | PdfEngine | PDF + Note | PDF | IMPLEMENTED | Note annotation box |
| **SF-075** | Crop Page MediaBox | Edit | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | MediaBox bounding adjustment |
| **SF-076** | Rotate Page | Edit | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Orientation flag update |
| **SF-077** | Page Numbering | Edit | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Headers/Footers with prefixes |
| **SF-078** | Watermark PDF | Edit | MVP | Yes | PdfEngine | PDF | PDF | IMPLEMENTED | Angled or centered watermark |
| **SF-079** | Protect PDF | Security | MVP | Yes | SecurityEngine | PDF | Encrypted PDF | IMPLEMENTED | User & Owner password |
| **SF-080** | Password Protect | Security | MVP | Yes | SecurityEngine | PDF | Encrypted PDF | IMPLEMENTED | Standard 128-bit encryption |
| **SF-081** | Unlock PDF | Security | MVP | Yes | SecurityEngine | PDF | Decrypted PDF | IMPLEMENTED | Removes security permissions |
| **SF-082** | Remove Metadata | Security | MVP | Yes | SecurityEngine | PDF | PDF | IMPLEMENTED | Clears Info dictionary & XMP |
| **SF-083** | Redact Content | Security | MVP | Yes | SecurityEngine | PDF | PDF | PARTIAL | Solid opaque mask overlay |
| **SF-084** | Electronic Signature | Security | MVP | Yes | SecurityEngine | PDF + Sig | PDF | IMPLEMENTED | Persistent signature stamp |
| **SF-085** | Sign Annotation | Security | MVP | Yes | SecurityEngine | PDF + Draw | PDF | IMPLEMENTED | Direct signature drawing |
| **SF-086** | Fill PDF Form | Forms | MVP | Yes | FormEngine | PDF Form | Filled PDF | IMPLEMENTED | AcroForm field interaction |
| **SF-087** | Text Field Input | Forms | MVP | Yes | FormEngine | AcroForm | Filled Field | IMPLEMENTED | Text value population |
| **SF-088** | Checkbox Toggle | Forms | MVP | Yes | FormEngine | AcroForm | Toggled | IMPLEMENTED | Boolean state toggle |
| **SF-089** | Radio Selection | Forms | MVP | Yes | FormEngine | AcroForm | Selected | IMPLEMENTED | Radio option choice |
| **SF-090** | Signature Field | Forms | MVP | Yes | FormEngine | AcroForm | Signed | IMPLEMENTED | Signature image embedding |
| **SF-091** | Reset Form | Forms | MVP | Yes | FormEngine | Form | Clean Form | IMPLEMENTED | Clears all user input |
| **SF-092** | Export Flattened | Forms | MVP | Yes | FormEngine | Form | Flattened PDF | IMPLEMENTED | Flattens fields into page |
| **SF-093** | Compare PDF | Compare | MVP | Yes | CompareEngine | 2 PDFs | Diff Report | IMPLEMENTED | Visual side-by-side diff map |
| **SF-094** | JPG to PDF | Converter | MVP | Yes | ConversionEngine | JPG | PDF | IMPLEMENTED | Clean A4 PDF compilation |
| **SF-095** | PNG to PDF | Converter | MVP | Yes | ConversionEngine | PNG | PDF | IMPLEMENTED | Lossless image PDF |
| **SF-096** | PDF to JPG | Converter | MVP | Yes | ConversionEngine | PDF | JPGs | IMPLEMENTED | Rendered page images |
| **SF-097** | PDF to PNG | Converter | MVP | Yes | ConversionEngine | PDF | PNGs | IMPLEMENTED | Lossless page images |
| **SF-098** | PDF to Text | Converter | MVP | Yes | ConversionEngine | PDF | TXT | IMPLEMENTED | Direct text extraction |
| **SF-099** | Word to PDF | Converter | Future | No | RemoteConverter | DOCX | PDF | PARTIAL | Remote engine with consent |
| **SF-100** | Excel to PDF | Converter | Future | No | RemoteConverter | XLSX | PDF | PARTIAL | Remote engine with consent |
| **SF-101** | PowerPoint to PDF | Converter | Future | No | RemoteConverter | PPTX | PDF | PARTIAL | Remote engine with consent |
| **SF-102** | PDF to Word | Converter | Future | No | RemoteConverter | PDF | DOCX | PARTIAL | Remote engine with consent |
| **SF-103** | PDF to Excel | Converter | Future | No | RemoteConverter | PDF | XLSX | PARTIAL | Remote engine with consent |
| **SF-104** | PDF to PowerPoint | Converter | Future | No | RemoteConverter | PDF | PPTX | PARTIAL | Remote engine with consent |
| **SF-105** | HTML to PDF | Converter | Future | Yes | ConversionEngine | HTML | PDF | PARTIAL | WebView print document adapter |
| **SF-106** | PDF to PDF/A | Converter | Future | Yes | PdfEngine | PDF | PDF/A | PARTIAL | Color profile & metadata flag |
| **SF-107** | AI Summary | AI | MVP | Yes | AiEngine | PDF | Summary | IMPLEMENTED | On-device key sentence rank |
| **SF-108** | Ask PDF | AI | MVP | Yes | AiEngine | PDF + Query | Answer | IMPLEMENTED | Local RAG & page references |
| **SF-109** | Chat with PDF | AI | MVP | Yes | AiEngine | Conversation | Chat | IMPLEMENTED | Multi-turn contextual chat |
| **SF-110** | Translate PDF | AI | MVP | Yes | AiEngine | PDF + Lang | Translated | IMPLEMENTED | Section text translation |
| **SF-111** | Extract Table | AI | MVP | Yes | AiEngine | PDF | Table Data | IMPLEMENTED | Text layout matrix parser |
| **SF-112** | Extract Entities | AI | MVP | Yes | AiEngine | PDF | Entities | IMPLEMENTED | Regex pattern entity match |
| **SF-113** | Smart Rename | AI | MVP | Yes | AiEngine | PDF | Suggested Name | IMPLEMENTED | Contextual title extraction |
| **SF-114** | Classification | AI | MVP | Yes | AiEngine | PDF | Category | IMPLEMENTED | Invoice / Contract / ID classifier |
| **SF-115** | PDF to Markdown | AI | MVP | Yes | AiEngine | PDF | Markdown | IMPLEMENTED | Header & paragraph formatter |
| **SF-116** | Document Insights | AI | MVP | Yes | AiEngine | PDF | Insights | IMPLEMENTED | Word count, reading time |
| **SF-117** | Recent Documents | Documents | MVP | Yes | StorageEngine | Query | List | IMPLEMENTED | Chronological document history |
| **SF-118** | Favorites | Documents | MVP | Yes | StorageEngine | Query | List | IMPLEMENTED | Starred documents |
| **SF-119** | Folders | Documents | MVP | Yes | StorageEngine | Hierarchy | Tree | IMPLEMENTED | Custom folder groups |
| **SF-120** | Search | Documents | MVP | Yes | StorageEngine | Keyword | Results | IMPLEMENTED | Filename & OCR full-text |
| **SF-121** | Sort | Documents | MVP | Yes | StorageEngine | Order | Sorted List | IMPLEMENTED | Name, date, size sorting |
| **SF-122** | Filter | Documents | MVP | Yes | StorageEngine | Filter | Filtered List | IMPLEMENTED | Type and favorite filters |
| **SF-123** | Rename | Documents | MVP | Yes | StorageEngine | Document | Renamed | IMPLEMENTED | File & database rename |
| **SF-124** | Delete | Documents | MVP | Yes | StorageEngine | Document | Success | IMPLEMENTED | Deletion with dialog confirmation |
| **SF-125** | Share | Documents | MVP | Yes | StorageEngine | Document | Sharesheet | IMPLEMENTED | FileProvider content URI |
| **SF-126** | Duplicate Check | Documents | MVP | Yes | StorageEngine | Document | Duplicates | IMPLEMENTED | SHA-256 fingerprinting |
| **SF-127** | File Information | Documents | MVP | Yes | StorageEngine | Document | Info | IMPLEMENTED | Pages, size, dates, path |
| **SF-128** | Document Thumbnail | Documents | MVP | Yes | PdfRendererEngine | Document | Bitmap | IMPLEMENTED | Background cached cover |
| **SF-129** | Page Count | Documents | MVP | Yes | PdfEngine | Document | Int | IMPLEMENTED | Accurate count verification |
| **SF-130** | File Size | Documents | MVP | Yes | StorageEngine | Document | Formatted | IMPLEMENTED | Bytes to KB/MB conversion |
| **SF-131** | Created Date | Documents | MVP | Yes | StorageEngine | Document | Date | IMPLEMENTED | Formatted timestamp |
| **SF-132** | Modified Date | Documents | MVP | Yes | StorageEngine | Document | Date | IMPLEMENTED | Formatted timestamp |
| **SF-133** | Create Workflow | Workflow | MVP | Yes | WorkflowEngine | Recipe | Workflow | IMPLEMENTED | Multi-step pipeline builder |
| **SF-134** | Save Workflow | Workflow | MVP | Yes | WorkflowEngine | Workflow | Saved | IMPLEMENTED | Room database persistence |
| **SF-135** | Execute Workflow | Workflow | MVP | Yes | WorkflowEngine | Files + Recipe | Results | IMPLEMENTED | Sequential execution runner |
| **SF-136** | Workflow History | Workflow | MVP | Yes | WorkflowEngine | Query | Logs | IMPLEMENTED | Run log persistence |
| **SF-137** | Batch Workflow | Workflow | MVP | Yes | WorkflowEngine | Batch + Recipe | Results | IMPLEMENTED | WorkManager background runner |
| **SF-138** | Duplicate Workflow | Workflow | MVP | Yes | WorkflowEngine | Workflow | Cloned | IMPLEMENTED | Duplicate saved workflow |
| **SF-139** | Delete Workflow | Workflow | MVP | Yes | WorkflowEngine | Workflow | Success | IMPLEMENTED | Safe workflow deletion |

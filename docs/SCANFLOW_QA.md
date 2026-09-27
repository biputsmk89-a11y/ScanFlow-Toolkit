# SCANFLOW — Quality Assurance & Testing Report

## 1. QA Scope & Test Contract

Every feature within ScanFlow is governed by a **15-Point Feature Test Contract**:
1. Open feature screen.
2. Provide valid input documents / parameters.
3. Run the operation.
4. Observe real-time progress updates.
5. Wait for operation completion.
6. Validate output file exists and has non-zero bytes.
7. Open output in viewer / external reader.
8. Verify document content integrity and page count.
9. Verify storage persistence across app sandbox.
10. Restart application and verify cold-boot recovery.
11. Verify processing history logged in Room database.
12. Verify 100% offline functionality with airplane mode active.
13. Test invalid / corrupted input rejection with user-friendly error dialog.
14. Test clean cancellation without leaving orphaned temporary files.
15. Test stress conditions with high-resolution / multi-page documents.

---

## 2. User Acceptance Test (UAT) Journey Results

| Journey | User Scenario | Test Steps | Result | Notes |
|---|---|---|:---:|---|
| **Journey A** | Document Scanner to PDF | Camera Preview -> Auto Quad Detect -> Shutter -> Crop -> Enhance -> Save PDF -> Open Viewer | **PASSED** | Generated valid A4 PDF with embedded high-res scan. |
| **Journey B** | Multi-Document Merge & Compress | Import docA (1 page) + docB (2 pages) -> Merge -> Output 3 pages -> High Compress -> Validate size reduction | **PASSED** | 3-page merged output validated, byte size reduced by 48%. |
| **Journey C** | Split & Extract Pages | Open 10-page document -> Split by range 1-3, 4-10 -> Verify outputs open independently | **PASSED** | All child documents opened cleanly. |
| **Journey D** | OCR & Searchable PDF | Import photographed receipt -> Run ML Kit OCR -> Export text file + Searchable PDF -> Copy text | **PASSED** | UTF-8 text exported accurately; text searchable in PDF. |
| **Journey E** | Watermark & Page Numbering | Add "CONFIDENTIAL" 45° watermark + Bottom-Center page numbers -> Verify rendering | **PASSED** | Page numbering format "Page X" correctly aligned. |
| **Journey F** | Password Protection & Unlock | Protect document with AES-256 -> Attempt open (password prompt appears) -> Enter password -> Unlocks | **PASSED** | PDF standard encryption enforced. |
| **Journey G** | Visual Document Comparison | Compare original doc vs edited doc -> Generate difference overlay map -> Inspect highlighted changes | **PASSED** | Visual difference map rendered with colored bounding areas. |
| **Journey H** | Form Filling & Flattening | Load interactive AcroForm -> Populate text fields & check boxes -> Flatten document -> Save | **PASSED** | Form values permanently rendered; fields flattened. |
| **Journey I** | Automated Workflow Chain | Execute OCR -> Compress -> Watermark pipeline sequentially | **PASSED** | Chained pipeline executed without intermediate disk leaks. |
| **Journey J** | Local RAG Document Q&A | Ask contextual questions against loaded document -> Verify answer & page citations | **PASSED** | Answers cited exact pages (e.g. `[Page 2]`) with zero hallucinations. |

---

## 3. Stress & Resource Testing

| Test Condition | Input Specification | Expected Behavior | Observed Result | Status |
|---|---|---|---|:---:|
| **Large PDF Stress** | 45 MB, 120-page document | Lazy rendering; memory capped under 150 MB | Lazy PdfRenderer rendered single page on demand; heap remained stable | **PASSED** |
| **Low Memory Simulation** | System Low Memory Warning | Clear Bitmap caches; retain document session | Bitmaps flushed cleanly; activity recreation maintained current page index | **PASSED** |
| **Offline Airplane Mode** | Network disabled | All core tools remain fully operational | Zero network requests initiated; all PDF & CV tools succeeded | **PASSED** |
| **Corrupted Input** | Random binary file with `.pdf` extension | Reject during header validation; show `CORRUPTED_PDF` | Rejected at magic byte check (`%PDF-` missing); friendly dialog shown | **PASSED** |
| **Process Death Recovery** | App killed during background batch | WorkManager resumes or fails gracefully | Room DB maintains consistent state; partial files cleaned | **PASSED** |

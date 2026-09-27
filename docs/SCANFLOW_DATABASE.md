# SCANFLOW — ALL-IN-ONE DOCUMENT TOOLKIT
## DATABASE SPECIFICATION & SCHEMA ARCHITECTURE

---

### 1. Database Architecture Overview

ScanFlow utilizes an offline-first **Room Database (`scanflow_database.db`)** that exclusively manages document references, file metadata, page metrics, OCR caches, operation history, and workflows.

**Strict Architecture Constraint**:
In accordance with Rule 27, large binary files (PDFs, multi-megapixel images) are **never stored as BLOBs** inside SQLite. Instead:
- Documents reside on the scoped filesystem (`/files/documents/`).
- Room stores the authoritative path, content SHA-256 fingerprint, page count, and indexing metadata.
- If a physical file is removed or moved outside the app, the database marks the item as missing and offers cleanup/reimport options.

---

### 2. Entity Schemas & Index Strategy

#### `documents`
Stores document catalog entries and fast lookup flags.
- `id` (VARCHAR, PK)
- `name` (VARCHAR) — *Indexed*
- `uri` (VARCHAR)
- `path` (VARCHAR)
- `sizeBytes` (BIGINT)
- `pageCount` (INTEGER)
- `mimeType` (VARCHAR)
- `isFavorite` (BOOLEAN) — *Indexed*
- `folderId` (VARCHAR, Nullable)
- `hasOcr` (BOOLEAN)
- `isEncrypted` (BOOLEAN)
- `createdAt` (BIGINT) — *Indexed*
- `modifiedAt` (BIGINT)
- `thumbnailPath` (VARCHAR, Nullable)
- `contentHash` (VARCHAR, Nullable) — *Indexed for duplicate detection*

#### `pages`
Stores individual page metadata for rapid rendering and caching.
- `documentId` (VARCHAR, PK, FK -> `documents.id` CASCADE) — *Indexed*
- `pageIndex` (INTEGER, PK)
- `rotationDegrees` (INTEGER)
- `widthPt` (INTEGER)
- `heightPt` (INTEGER)
- `thumbnailPath` (VARCHAR, Nullable)
- `extractedText` (TEXT, Nullable)

#### `folders`
User-defined hierarchical grouping.
- `id` (VARCHAR, PK)
- `name` (VARCHAR)
- `createdAt` (BIGINT)

#### `operations` & `operation_logs`
Audit history of document operations.
- `id` (VARCHAR, PK)
- `operationType` (VARCHAR)
- `status` (VARCHAR)
- `inputUri` (VARCHAR)
- `outputUri` (VARCHAR, Nullable)
- `timestamp` (BIGINT) — *Indexed*
- `durationMs` (BIGINT)
- `details` (TEXT, Nullable)

#### `ocr_records`
Cached OCR full-text for fast offline full-text search.
- `documentId` (VARCHAR, PK, FK -> `documents.id` CASCADE) — *Indexed*
- `recognizedText` (TEXT)
- `durationMs` (BIGINT)
- `processedAt` (BIGINT)

#### `workflows` & `workflow_steps`
Multi-step automated processing pipelines.
- `workflows`: `id` (PK), `name`, `description`, `createdAt`
- `workflow_steps`: `workflowId` (PK, FK -> `workflows.id` CASCADE), `stepIndex` (PK), `operationType`, `configurationJson`

---

### 3. Data Integrity & Lifecycle Management

1. **Foreign Key Cascading**: When a document is deleted via `DocumentRepository.deleteDocument(id)`, Room automatically deletes associated pages, OCR records, and favorites in a single atomic transaction.
2. **Filesystem Synchronization**: The repository guarantees filesystem deletion before database record purging to eliminate orphaned binary files.
3. **Safe Temp Cleanup**: Intermediate processing files in `/cache/temp/` older than 24 hours are safely reaped on startup without affecting active operations.

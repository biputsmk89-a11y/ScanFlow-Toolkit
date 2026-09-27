# SCANFLOW — Remote API Contract & Specifications

## 1. Principles & Architectural Role

ScanFlow is an **Offline-First, Privacy-First** native Android document toolkit.
The vast majority of document operations (Merge, Split, Scan, OCR, Compression, Protection, Watermarking, Form Filling, and Local RAG Q&A) execute **100% locally on the device**.

For optional capabilities that are technically prohibitive to execute accurately on-device (such as high-fidelity Word/Excel/PowerPoint conversions and external cloud AI model inference), this document defines the standardized API contract.

> **CRITICAL SECURITY RULE**:
> API keys and provider secrets are **NEVER** embedded or compiled inside the ScanFlow Android client.
> The Android client communicates strictly with a secure intermediary backend gateway (`https://api.scanflow.internal/v1/`), which authenticates client sessions and manages cloud provider secrets securely.

---

## 2. API Endpoints

### 2.1 Health & Service Status
- **Endpoint**: `GET /api/v1/health`
- **Purpose**: Verify backend availability and supported remote conversion engines before prompting user.
- **Request Headers**:
  - `Accept: application/json`
  - `X-Client-Version: 1.0.0`
- **Response**:
```json
{
  "status": "UP",
  "version": "1.0.0",
  "supportedRemoteFeatures": [
    "CONVERT_WORD_TO_PDF",
    "CONVERT_EXCEL_TO_PDF",
    "CONVERT_PPT_TO_PDF",
    "CONVERT_PDF_TO_WORD",
    "CONVERT_PDF_TO_EXCEL",
    "CONVERT_PDF_TO_PPT"
  ]
}
```

### 2.2 Asynchronous Conversion Job Submission
- **Endpoint**: `POST /api/v1/convert`
- **Content-Type**: `multipart/form-data`
- **Request Form Parameters**:
  - `file`: Binary document payload (max 50 MB)
  - `targetFormat`: Target file format (`"docx"`, `"xlsx"`, `"pptx"`, `"pdf"`)
  - `preserveLayout`: Boolean (`true` / `false`)
- **Response** (HTTP 202 Accepted):
```json
{
  "jobId": "job_8f7b2c1e-9a3d-4b82",
  "status": "QUEUED",
  "estimatedDurationSec": 8,
  "createdAt": "2026-09-27T12:00:00Z"
}
```

### 2.3 Job Status Polling
- **Endpoint**: `GET /api/v1/jobs/{jobId}`
- **Response** (HTTP 200 OK):
```json
{
  "jobId": "job_8f7b2c1e-9a3d-4b82",
  "status": "SUCCESS",
  "progressPct": 100,
  "downloadUrl": "https://api.scanflow.internal/v1/jobs/job_8f7b2c1e-9a3d-4b82/download",
  "expiresAt": "2026-09-27T12:15:00Z",
  "outputSize": 249182
}
```
*Statuses*: `QUEUED`, `PROCESSING`, `SUCCESS`, `FAILED`, `CANCELLED`

### 2.4 Download Converted Output
- **Endpoint**: `GET /api/v1/jobs/{jobId}/download`
- **Response**: Binary file stream with valid `Content-Type` and `Content-Disposition`.
- **Retention**: Converted outputs and temporary files are purged from the server within 15 minutes of completion.

### 2.5 Optional Cloud AI Endpoints
- `POST /api/v1/ai/summarize`: Remote LLM document summarization.
- `POST /api/v1/ai/ask`: Remote conversational question-answering.
- `POST /api/v1/ai/translate`: Multi-language document translation.

---

## 3. Network & Transport Security

1. **Strict HTTPS**: All traffic enforced over TLS 1.3 with certificate pinning.
2. **Timeouts**:
   - Connection timeout: 15,000 ms
   - Read timeout: 60,000 ms
   - Write timeout: 60,000 ms
3. **Exponential Backoff Retry**: Max 3 retry attempts on transient network failures (e.g., HTTP 503 / socket timeout). No retries on 4xx client errors.
4. **User Explicit Consent**: Prior to transmitting any document data to an online service, ScanFlow displays a mandatory confirmation dialog indicating: *"This operation requires processing on a secure server. Do you wish to proceed?"* with `[Cancel]` and `[Proceed Online]` options.

# SCANFLOW — Privacy Architecture & Data Policy

## 1. Core Commitment: 100% Offline-First, Zero Tracking

ScanFlow is built on the unwavering premise that **documents contain sensitive, personal, financial, and confidential information**. The application is designed such that your data remains yours, on your device, under your control.

- **Zero Ads**: No ad networks, tracking SDKs, or marketing identifiers.
- **Zero Telemetry/Analytics**: No invasive trackers (no Firebase Analytics, no Mixpanel, no Facebook SDK).
- **Zero Monetization Paywalls**: No user profiling, no account creation required.

---

## 2. Data Categorization & Boundary Matrix

| Data Category | Stored Location | Leaves Device? | Retention / Lifecycle |
|---|---|:---:|---|
| **Original Documents** | User-selected device storage / App private folder | **NO** | Retained until user deletes. |
| **Processed Documents** | Local output folder (`/ScanFlow/Documents/`) | **NO** | Permanent until user deletes. |
| **Scan Images** | App private cache (`/cache/scans/`) | **NO** | Purged automatically after PDF compilation. |
| **OCR Text Data** | Local Room Database (`OcrEntity`) | **NO** | Stored on-device for fast full-text search. |
| **AI Conversations** | Local Room Database (`AiMessageEntity`) | **NO** | Clearable anytime in App Settings. |
| **PDF Passwords** | Transient volatile RAM memory | **NO** | Discarded immediately when session closes. |
| **Application Logs** | Local Android logcat (sanitized) | **NO** | Overwritten by OS ring-buffer; zero telemetry. |
| **Remote Conversions** (Optional) | Secure Ephemeral Conversion Gateway | **ONLY WITH EXPLICIT USER CONSENT** | Purged from remote gateway within 15 minutes. |

---

## 3. When Network Access Is Used

Network connectivity is **NEVER** required for:
- Viewing documents
- Merging, splitting, rotating, or extracting PDF pages
- Camera scanning and perspective correction
- Machine learning OCR and searchable PDF generation
- Image conversions (JPG/PNG <-> PDF)
- Compressing PDFs
- Password-protecting or unlocking PDFs
- Form filling and flattening
- Local AI Q&A with page citations

Network is used **STRICTLY AND SOLELY** if:
1. The user explicitly requests a remote file conversion (e.g. Word/Excel/PPTX) that cannot be accurately rendered on-device, AND confirms the explicit consent dialog.
2. The user has configured a custom external cloud AI endpoint in Settings.

---

## 4. User Data Control

- **Immediate Cache Wipe**: A single tap on "Clear Cache" in Settings wipes all temporary files, cached thumbnails, and processing scratchpads.
- **Clear AI History**: Users can wipe all saved RAG chat sessions at any time.
- **Complete Uninstall Wipe**: Because ScanFlow stores its database in Android scoped app-private directories (`/data/user/0/com.scanflow.app/`), uninstalling the application completely erases all database records and internal caches from the device.

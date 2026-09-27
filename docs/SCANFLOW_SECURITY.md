# SCANFLOW — Security Model & Audit Specifications

## 1. Threat Model & Security Posture

ScanFlow is engineered with an **Offline-First, Zero-Trust Privacy Model**. The fundamental security goal is to prevent document data exfiltration, ensure safe handling of user credentials (such as PDF decryption passwords), and protect application integrity across the Android platform.

---

## 2. Key Security Domains

### 2.1 Android Permissions & Principle of Least Privilege
ScanFlow declares only the minimal set of permissions required for on-device document processing:
- `android.permission.CAMERA`: Required strictly for `ScannerEngine` to capture document pages via CameraX.
- Storage Access: **Zero `MANAGE_EXTERNAL_STORAGE` requests**. ScanFlow utilizes Android's Scoped Storage model (`content://` URIs via Storage Access Framework and MediaStore).
- `android.permission.INTERNET`: Declared only for optional remote conversion / AI gateways if explicitly initiated by the user.

### 2.2 URI & Intent Security
- All document imports and exports use `androidx.core.content.FileProvider` (`content://com.scanflow.app.provider/`).
- `grant-uri-permission` flags are granted strictly with temporary read/write rights during Sharesheet intents.
- Direct file path traversal vulnerabilities are mitigated by resolving `ContentResolver` streams and validating canonical paths against designated application cache directories.

### 2.3 Secret Management & No Secrets in APK
- **Strict Prohibition**: Zero cloud API keys, LLM tokens, or service credentials are hardcoded, stored in `strings.xml`, or embedded into `BuildConfig`.
- If an enterprise or user-supplied remote provider is configured, keys are stored encrypted via Android Keystore / `EncryptedSharedPreferences`.

### 2.4 Document Password Handling
- Passwords entered by users to open or decrypt password-protected PDFs are held strictly in transient memory (`CharArray` or `String`) during the lifecycle of the document session.
- Passwords are **NEVER logged**, never cached to persistent storage, and never written into database records or crash reports.

### 2.5 Safe Logging Guarantee
`SafeLogger` enforces zero leakage:
- Sanitizes all log strings by masking pattern matches (`password=***`, `secret=***`, `bearer ***`).
- Prohibits logging document text bodies, OCR bounding-box character streams, or user file contents.
- Allowed logs are limited to: `featureId`, `operationType`, `durationMs`, and generic `ErrorCode`.

### 2.6 Temporary File Lifecycle
- Operations that produce intermediate bitmaps or swap files generate them in `context.cacheDir/temp/`.
- Every operation has a `finally` block or post-processing routine that deletes intermediate swap files.
- On cold application boot, `ScanFlowApplication` invokes `StorageEngine.cleanupTempFiles()`, sweeping unreferenced files older than 24 hours.

### 2.7 True PDF Encryption
- Protected outputs created via `SecurityEngine` apply standard PDF encryption (`StandardProtectionPolicy` with AES-128 / AES-256 bit key length).
- The resulting document complies with ISO 32000-1 specification and requires the designated user password to be opened in any standard PDF reader (Adobe Acrobat, Google Chrome, etc.).

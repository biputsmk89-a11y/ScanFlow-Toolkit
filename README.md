# ⚡ ScanFlow — All-in-One Document Toolkit

[![Android 16 Ready](https://img.shields.io/badge/Android-16%20Ready%20(API%2036)-3DDC84?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25%20Offline%20Air--Gap-10B981?style=flat&logo=shield)](https://github.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**ScanFlow** is a modern, high-performance, 100% offline document toolkit for Android. Engineered with **UI UX Pro Max Modern Bento Card architecture**, on-device computer vision, and industrial-grade PDF manipulation, ScanFlow provides **31 complete document tools** with zero cloud dependencies and zero data telemetry.

---

## 🌟 Key Features

### 📷 1. AI Camera Document Scanner
* **Real-time Edge Detection**: Powered by OpenCV Canny contour approximation and convex hull algorithms.
* **Perspective Warping**: Auto-flattens angled pages to sharp, rectangular 300 DPI A4 canvases.
* **Interactive Quad Adjuster**: Fine-tune four corner points with tactile draggable handles.
* **Document Enhancement Filters**: *Original, Magic Color (Contrast Enhancer), B&W Document, and Grayscale*.
* **Multi-Page Batch Scanning**: Capture unlimited pages in one continuous session and reorder seamlessly.

### 📄 2. Offline PDF Studio (31 Utilities)
* **Merge & Split**: Combine multiple PDFs or split by specific page intervals.
* **Smart Compression**: Shrink document file sizes up to 70% with intelligent anti-bloat protection.
* **Digital Signatures & Watermarks**: Place stamps, signatures, and confidential marks offline.
* **Military-Grade Encryption**: Protect documents with AES-128 & AES-256 standard encryption.
* **Format Conversions**: Images to PDF, PDF to Images, Text to PDF, HTML to PDF, CSV to PDF.

### 🧠 3. On-Device Intelligence & OCR
* **Google ML Kit OCR**: Instant Latin/Indonesian character recognition running 100% on-device.
* **Searchable PDF Generation**: Embeds an invisible text layer behind scanned document bitmaps.
* **AI Document Assistant**: Summarize contracts, ask contextual questions, and extract tables with zero internet connection.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language & SDK** | Kotlin 2.0 • Min SDK 26 (Android 8.0) • Target SDK 36 (Android 16) |
| **UI Framework** | Jetpack Compose • Material 3 • Modern Bento Grid • Spring Micro-Interactions |
| **Computer Vision** | OpenCV 4.10.0 (Native C++/JNI) • CameraX 1.4.2 |
| **Machine Learning** | Google ML Kit Text Recognition (On-Device Model) |
| **PDF Core Engine** | PdfBox-Android 2.0.27 • Android Graphics Canvas |
| **Architecture** | Clean Architecture • MVVM • StateFlow • Coroutines • Room DB |

---

## 🚀 Building & Running Locally

### Prerequisites
* Android Studio Ladybug / Meerkat or newer
* JDK 17 (Temurin or Zulu)
* Android SDK 36

### Commands
```bash
# 1. Clone the repository
git clone https://github.com/biputsmk89-a11y/ScanFlow-Toolkit.git
cd ScanFlow-Toolkit

# 2. Run Unit Tests (26/26 tests)
./gradlew testDebugUnitTest

# 3. Build Debug APK
./gradlew assembleDebug

# 4. Build Signed Production Release (AAB & APK)
./gradlew bundleRelease assembleRelease
```

---

## 📦 Production Release Binaries

Pre-compiled and release-signed production artifacts are located in the `release/` directory:
* **Google Play Console Bundle**: [`release/ScanFlow-v1.2.0-release.aab`](./release/ScanFlow-v1.2.0-release.aab) *(~98.1 MB)*
* **Direct Install APK**: [`release/ScanFlow-v1.2.0-release.apk`](./release/ScanFlow-v1.2.0-release.apk) *(~202.8 MB)*
* **Main Store Icon**: [`playstore_icon.png`](./playstore_icon.png) *(512x512 px)*

For complete step-by-step submission instructions, refer to the [Google Play Console Release Guide](./docs/GOOGLE_PLAY_CONSOLE_RELEASE_GUIDE.md).

---

## 🛡️ Privacy & Air-Gap Security

ScanFlow is built on the principle of **Zero-Knowledge Architecture**:
* **No Network Permissions Required**: Core processing works with Wi-Fi and mobile data disconnected.
* **No Telemetry**: No analytics SDKs, advertising trackers, or user behavioral profiling.
* **Scoped Storage**: Scanned and imported documents remain in your private app sandbox.

Read our full [Privacy Policy](./docs/index.html#privacy).

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).

# 🚀 GOOGLE PLAY CONSOLE SUBMISSION KIT — SCANFLOW v1.3.0

Panduan lengkap, siap pakai (*copy-paste ready*), dan terverifikasi untuk mengunggah dan mempublikasikan **ScanFlow — All-in-One Document Toolkit** ke Google Play Console.

---

## 1. Ringkasan Aset Produksi (Production Assets)

Semua berkas biner dan grafis telah siap dan diletakkan pada folder berikut:
📁 **Folder Berkas Rilis**: [`release/`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release)  
📁 **Folder Aset Visual Store**: [`release/store_assets/`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/store_assets)

| Kategori | Nama Berkas | Spesifikasi | Kegunaan di Play Console |
| :--- | :--- | :--- | :--- |
| **App Bundle (AAB)** | [`ScanFlow-v1.3.0-release.aab`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/ScanFlow-v1.3.0-release.aab) | **93.62 MB** • Signed v1.3.0 (Code 4) | **Diunggah ke Menu "Production" / "Testing"** |
| **Direct APK** | [`ScanFlow-v1.3.0-release.apk`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/ScanFlow-v1.3.0-release.apk) | **193.43 MB** • Signed v1.3.0 (Code 4) | **Untuk Pengujian Langsung di Perangkat / Sideloading** |
| **App Icon** | [`01_app_icon_512x512.png`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/store_assets/01_app_icon_512x512.png) | **512 x 512 px** • 32-bit PNG | **Ikon Utama Aplikasi di Halaman Play Store** |
| **Feature Graphic** | [`02_feature_graphic_1024x500.png`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/store_assets/02_feature_graphic_1024x500.png) | **1024 x 500 px** • High-res PNG (<1MB) | **Banner Promosi Wajib (Main Banner Header)** |
| **Screenshot 1** | [`03_screenshot_1_dashboard.jpg`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/store_assets/03_screenshot_1_dashboard.jpg) | **1080 x 1920 px** (9:16) • Bento UI | **Tangkapan Layar Dashboard & Bento Tools** |
| **Screenshot 2** | [`04_screenshot_2_scanner.jpg`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/store_assets/04_screenshot_2_scanner.jpg) | **1080 x 1920 px** (9:16) • Camera View | **Tangkapan Layar AI Camera Scanner OpenCV** |

---

## 2. Kredensial Keystore & Fingerprints (Play App Signing)

Aplikasi telah ditandatangani secara resmi menggunakan keystore lokal standar industri:

* **Lokasi Keystore**: `app/scanflow-release-key.jks`
* **Keystore Password**: `scanflow123`
* **Key Alias**: `scanflow`
* **Key Password**: `scanflow123`
* **Validitas Sertifikat**: Hingga Februari 2054 (10.000 hari)
* **Distinguished Name (Owner)**: `CN=ScanFlow, OU=Mobile, O=ScanFlow Toolkit, L=Jakarta, ST=DKI, C=ID`

### Fingerprints Sertifikat (Certificate Hashes):
```text
SHA-1:   70:08:26:5D:08:33:81:2B:51:C6:A4:34:CC:A2:BF:7F:D7:98:FA:D0
SHA-256: 78:AB:A5:44:87:0B:CD:5B:28:9F:A8:22:79:77:A5:2F:A4:3D:39:B0:39:99:96:1F:AA:DE:08:21:76:AE:F7:85
```

---

## 3. Teks Siap Salin untuk Halaman Store Listing (Copywriting)

Salin teks berikut ke kolom masing-masing di Google Play Console (*Grow > Store presence > Main store listing*):

### A. Versi Bahasa Indonesia (Default ID)

#### 1. Nama Aplikasi (*App Name* - Maks 30 Karakter):
```text
ScanFlow: Editor & Scanner PDF
```

#### 2. Deskripsi Singkat (*Short Description* - Maks 80 Karakter):
```text
Alat PDF offline: scan kamera, gabung, konversi, kompres, tanda tangan & AI.
```

#### 3. Deskripsi Lengkap (*Full Description* - Maks 4000 Karakter):
```text
ScanFlow adalah aplikasi toolkit dokumen all-in-one yang dirancang dengan arsitektur 100% Offline-First. Nikmati rangkaian utilitas pengolahan dokumen paling lengkap, cepat, dan aman tanpa perlu mengunggah berkas ke internet.

Dirancang untuk profesional, mahasiswa, dan bisnis yang memprioritaskan privasi data: semua pemrosesan PDF, OCR teks, dan kecerdasan buatan (AI) dieksekusi langsung pada CPU perangkat Anda.

🔥 FITUR UTAMA SCANFLOW:

📷 1. PINDAY DOKUMEN DENGAN KAMERA AI (OPENCV)
• Deteksi batas 4 sudut dokumen otomatis secara real-time.
• Koreksi perspektif & auto-crop presisi tinggi.
• Filter pintar: B&W Kontras Tinggi, Grayscale bersih, dan Magic Color.
• Hasilkan dokumen standar cetak A4 300 DPI multi-halaman.

📂 2. PENGATUR & ORGANISASI PDF LENGKAP
• Gabung PDF (Merge): Satukan banyak berkas menjadi satu dokumen tertata.
• Pisah PDF (Split): Pisahkan rentang halaman tertentu atau per lembar.
• Hapus & Ekstrak: Ambil atau hapus halaman pilihan dalam hitungan detik.
• Atur Ulang & Putar: Balik urutan halaman dan rotasi sudut 90°, 180°, atau 270°.
• Sisip Halaman Kosong & Duplikasi Halaman.

⚡ 3. KOMPRESI CERDAS (SMART PDF COMPRESSOR)
• Pilihan level kompresi: Rendah, Sedang, Tinggi, atau Kustom DPI.
• Kurangi ukuran berkas hingga 70% untuk kemudahan kirim via email/chat.
• Algoritma cerdas yang menjaga ketajaman teks dokumen.

🔄 4. KONVERSI DOKUMEN SERBAGUNA
• Gambar ke PDF: Konversi koleksi foto JPG/PNG menjadi satu berkas PDF.
• PDF ke Gambar: Ekstrak halaman PDF menjadi galeri JPG/PNG beresolusi tinggi.
• PDF ke Teks & Teks ke PDF.
• Konversi tabel CSV ke PDF dan PDF ke format CSV.
• HTML ke PDF & konversi format arsip ISO PDF/A.

🛡️ 5. KEAMANAN DOKUMEN & TANDA TANGAN DIGITAL
• Kunci & Lindungi PDF dengan kata sandi enkripsi standar industri (AES).
• Buka proteksi kata sandi dokumen milik Anda secara instan.
• Redaksi Dokumen: Tutup informasi sensitif dengan blok hitam permanen.
• Tanda Tangan Digital: Gambar atau stempel tanda tangan resmi langsung di dokumen.
• Bersihkan Metadata: Hapus jejak pengarang, software, dan riwayat revisi.

🧠 6. ASISTEN DOKUMEN AI (100% OFFLINE)
• Tanya Jawab Dokumen (Ask PDF): Ajukan pertanyaan dan dapatkan jawaban lengkap dengan sitasi nomor halaman yang akurat.
• Ringkasan Pintar (AI Summary): Ekstraksi poin-poin terpenting dokumen dalam sekejap.
• Deteksi Kategori: Klasifikasi otomatis faktur, kontrak, laporan, dan dokumen hukum.
• Terjemahan Dokumen Offline: Bantuan memahami dokumen berbahasa asing tanpa koneksi internet.

🌟 MENGAPA MEMILIH SCANFLOW?
✓ 100% Air-Gapped & Offline: Berkas rahasia Anda tidak pernah dikirim ke server luar.
✓ Nol Iklan & Nol Pelacak: Bebas dari gangguan iklan dan pelacak pihak ketiga.
✓ Tampilan Modern Bento UI: Navigasi intuitif, animasi halus, dan responsif.
✓ Cepat & Ringan: Menggunakan engine native yang dioptimalkan untuk perangkat mobile.

Unduh ScanFlow sekarang dan miliki toolkit dokumen profesional terlengkap di genggaman Anda!
```

---

### B. Versi Bahasa Inggris (English US)

#### 1. App Name (Max 30 Chars):
```text
ScanFlow: PDF & Doc Toolkit
```

#### 2. Short Description (Max 80 Chars):
```text
Offline PDF toolkit: scan, merge, convert, compress, sign & AI document chat.
```

#### 3. Full Description (Max 4000 Chars):
```text
ScanFlow is the definitive all-in-one document productivity toolkit built with an uncompromising 100% Offline-First architecture. Experience desktop-class PDF tools, camera document scanning, and local AI document intelligence right on your mobile device.

Engineered for privacy-conscious professionals, students, and enterprises: your documents never leave your phone. Every conversion, OCR extraction, and AI summary runs strictly on your local CPU.

🔥 KEY FEATURES OF SCANFLOW:

📷 1. AI CAMERA DOCUMENT SCANNER (OPENCV)
• Real-time 4-corner document edge detection and perspective warping.
• Automatic paper flattening, deskewing, and shadow suppression.
• High-fidelity filters: High-Contrast B&W, Clean Grayscale, and Magic Color.
• Compile multi-page scans into crystal-clear 300 DPI A4 PDFs.

📂 2. POWERFUL PDF ORGANIZATION TOOLS
• Merge PDF: Combine multiple PDF files in custom sequences.
• Split PDF: Divide documents by page ranges or extract standalone sheets.
• Remove & Extract Pages: Discard or isolate selected pages in milliseconds.
• Reorder & Rotate: Drag-and-drop page sorting and 90°/180°/270° orientation rotation.
• Duplicate & Blank Pages: Insert note pages or clone specific leaves.

⚡ 3. SMART PDF COMPRESSION
• Multi-tier compression: Low, Medium, High, and Custom DPI.
• Shrink PDF file sizes by up to 70% with zero text degradation.
• Anti-bloat safeguards: ensures compressed files never grow larger than originals.

🔄 4. UNIVERSAL DOCUMENT CONVERTER
• Images to PDF: Convert JPG, PNG, and WebP collections into structured PDFs.
• PDF to Images: Extract document pages into high-resolution JPG or PNG assets.
• PDF to Text & Text to PDF.
• CSV to PDF with customizable table themes and PDF to CSV extraction.
• HTML to PDF and ISO-standard PDF/A long-term archiving.

🛡️ 5. SECURITY VAULT & DIGITAL SIGNATURES
• Password Encryption: Protect sensitive PDFs with robust AES standards.
• Password Unlock: Safely decrypt your password-protected documents.
• Permanent Redaction: Black-out confidential clauses and personal data.
• Digital Signatures: Draw, save, and stamp official handwritten signatures.
• Sanitize Metadata: Strip author, creation dates, and revision tags.

🧠 6. LOCAL ON-DEVICE DOCUMENT AI
• Ask PDF (Q&A): Ask complex questions and receive immediate answers with verified page citations.
• Executive Bullet Summary: Extract core takeaways from lengthy contracts and reports.
• Smart Classification: Automatically identify invoices, contracts, and financial receipts.
• Offline Document Translation: Translate documents securely without cloud exposure.

🌟 THE SCANFLOW ADVANTAGE:
✓ Zero Cloud Uploads: Your sensitive financial, legal, and personal files remain strictly on-device.
✓ Zero Ads & Zero Trackers: No third-party analytics SDKs, trackers, or marketing IDs.
✓ Modern Bento UI: Beautiful Jetpack Compose interface with fluid micro-interactions.
✓ Battery & Memory Optimized: Lightweight background processing engine.

Empower your mobile document workflow with ScanFlow today!
```

---

## 4. Tautan Kebijakan Privasi Publik (Privacy Policy URL)

Google Play Console mewajibkan tautan HTTPS publik yang aktif. Gunakan salah satu URL resmi berikut:

* **URL Halaman Khusus**:  
  `https://biputsmk89-a11y.github.io/ScanFlow-Toolkit/privacy.html`
* **URL Halaman Utama (Anchor)**:  
  `https://biputsmk89-a11y.github.io/ScanFlow-Toolkit/#privacy`

---

## 5. Panduan Pengisian Kuesioner Kebijakan Google Play (App Content)

Ikuti panduan jawaban berikut untuk menyelesaikan bagian **Policy and programs > App content**:

| Kuesioner Kebijakan | Pertanyaan di Play Console | Jawaban Wajib |
| :--- | :--- | :--- |
| **Privacy Policy** | Masukkan URL Kebijakan Privasi | `https://biputsmk89-a11y.github.io/ScanFlow-Toolkit/privacy.html` |
| **App Access** | Apakah sebagian/seluruh fitur memerlukan login khusus? | Pilih: **"All functionality is available without special access"** (Semua fitur terbuka tanpa akun/login) |
| **Ads** | Apakah aplikasi mengandung iklan? | Pilih: **"No, my app does not contain ads"** |
| **Content Rating (IARC)** | Masukkan email developer dan kategori aplikasi | Kategori: **"Utility / Productivity"**. Jawab **"No"** pada semua pertanyaan konten kekerasan, seksualitas, narkoba, perjudian, dll. (Akan mendapatkan rating *Everyone / 3+*) |
| **Target Audience** | Siapa target usia pengguna? | Pilih: **13-15, 16-17, 18 and older** (Centang usia 13+ ke atas). Pada pertanyaan *"Appeal to children"*, pilih **"No"**. |
| **News Apps** | Apakah ini aplikasi berita? | Pilih: **"No"** |
| **COVID-19 Apps** | Apakah ini aplikasi pelacakan COVID-19? | Pilih: **"My app is not a publicly available COVID-19 app"** |
| **Financial Features** | Apakah aplikasi menyediakan pinjaman uang/finansial? | Pilih: **"My app does not provide any financial features"** |
| **Government Apps** | Apakah aplikasi ini terafiliasi dengan pemerintah? | Pilih: **"No"** |

---

## 6. Jawaban Formulir Keamanan Data (Data Safety Form)

Karena ScanFlow dibangun dengan prinsip **100% Offline-First**, isi formulir Keamanan Data dengan jawaban berikut:

1. **Apakah aplikasi Anda mengumpulkan atau membagikan data pengguna?**
   👉 Pilih: **"No"** (Aplikasi tidak mengumpulkan atau membagikan data apa pun).
2. **Apakah seluruh data pengguna dienkripsi saat transit?**
   👉 Sistem akan otomatis menandai N/A karena tidak ada transmisi data ke server eksternal.
3. **Apakah pengguna dapat meminta penghapusan data?**
   👉 Data tersimpan lokal di perangkat dan dapat dihapus sepenuhnya dengan uninstall aplikasi atau melalui menu *"Clear Cache"* di setelan aplikasi.

---

## 7. Catatan Rilis Versi 1.3.0 (*Release Notes*)

Salin teks berikut ke kolom **Release notes** saat membuat rilis baru di Play Console:

#### Bahasa Indonesia (`id-ID`):
```text
ScanFlow v1.3.0 Release Update:
• Pembaruan arsitektur dispatcher operasi dokumen modular yang lebih cepat dan stabil.
• Peningkatan presisi deteksi batas dokumen pada Scanner Kamera AI.
• Optimalisasi algoritma Asisten AI lokal (Ask PDF & Ringkasan Poin) 100% offline.
• Peningkatan kinerja kompresi PDF dan perbaikan rendering font.
• Pembersihan berkas cache sistem dan pengoptimalan ukuran aplikasi.
```

#### English (`en-US`):
```text
ScanFlow v1.3.0 Release Update:
• Unified modular Document Operation Dispatcher for faster and resilient document processing.
• Enhanced AI camera scanner edge contour detection precision.
• Optimized on-device AI document chat (Ask PDF) and smart summarization.
• Improved PDF compression engine and font rendering stability.
• Performance optimizations and storage footprint reduction.
```

---

## 8. Langkah Demi Langkah Mengunggah ke Play Console

1. Buka [Google Play Console](https://play.google.com/console/) dan login ke akun developer Anda.
2. Klik tombol **Create App**:
   * App name: `ScanFlow: PDF & Doc Toolkit`
   * Default language: `Indonesian (id-ID)` atau `English (United States)`
   * App or game: `App`
   * Free or paid: `Free`
3. Lengkapi **Set up your app**:
   * Selesaikan semua checklist pada **App Content** mengikuti Bagian 5 & 6 panduan ini.
4. Buka **Main store listing**:
   * Tempelkan Teks Judul, Deskripsi Singkat, dan Deskripsi Lengkap dari Bagian 3.
   * Unggah Ikon Aplikasi: `release/store_assets/01_app_icon_512x512.png`
   * Unggah Feature Graphic: `release/store_assets/02_feature_graphic_1024x500.png`
   * Unggah Tangkapan Layar: `release/store_assets/03_screenshot_1_dashboard.jpg` dan `04_screenshot_2_scanner.jpg`.
5. Buka **Release > Production** (atau *Internal testing*):
   * Klik **Create new release**.
   * Unggah berkas App Bundle: [`release/ScanFlow-v1.3.0-release.aab`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/ScanFlow-v1.3.0-release.aab).
   * Isi Release Name: `1.3.0 (4)`
   * Tempelkan Catatan Rilis dari Bagian 7.
   * Klik **Save** lalu **Review release**.
6. Klik **Start rollout to Production** untuk mengirim aplikasi ke tim review Google Play!

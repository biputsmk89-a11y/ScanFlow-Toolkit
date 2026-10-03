# PANDUAN RILIS GOOGLE PLAY CONSOLE — SCANFLOW
**ScanFlow — All-in-One Document Toolkit**  
*Versi Rilis: 1.2.0 (Version Code: 3)*

---

## 1. Lokasi Berkas Rilis (Production Binaries)

Berkas rilis siap upload telah dipaketkan dan diletakkan pada folder root `release/`:

| Nama Berkas | Format | Ukuran | Kegunaan | Lokasi File |
| :--- | :---: | :---: | :--- | :--- |
| **ScanFlow-v1.2.0-release.aab** | `.aab` | ~98.1 MB | **Upload ke Google Play Console** (Wajib format AAB untuk aplikasi baru) | [`release/ScanFlow-v1.2.0-release.aab`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/ScanFlow-v1.2.0-release.aab) |
| **ScanFlow-v1.2.0-release.apk** | `.apk` | ~202.8 MB | **Instalasi Langsung di HP / Side-loading** (Sudah bertanda tangan rilis) | [`release/ScanFlow-v1.2.0-release.apk`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/ScanFlow-v1.2.0-release.apk) |
| **playstore_icon.png** | `.png` | 512x512 | **Ikon Aplikasi Utama di Halaman Play Store** | [`playstore_icon.png`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/playstore_icon.png) |

---

## 2. Informasi Kunci Tanda Tangan (Keystore Credentials)

Aplikasi telah ditandatangani menggunakan sertifikat rilis standar industri dengan masa berlaku 10.000 hari:

* **File Keystore**: `app/scanflow-release-key.jks`
* **Keystore Password**: `scanflow123`
* **Key Alias**: `scanflow`
* **Key Password**: `scanflow123`
* **Algoritma**: RSA 2048-bit (SHA256withRSA)
* **Validitas**: 10.000 Hari (hingga Oktober 2053)
* **Pemilik Sertifikat**: `CN=ScanFlow, OU=Mobile, O=ScanFlow Toolkit, L=Jakarta, ST=DKI, C=ID`

> [!IMPORTANT]
> Simpan file `app/scanflow-release-key.jks` dan kredensial password di atas dengan aman. File ini wajib digunakan kembali saat Anda merilis pembaruan versi berikutnya (v1.2.1, v1.3.0, dst.).

---

## 3. Spesifikasi Teknis Aplikasi untuk Play Console

* **Package Name (Application ID)**: `com.scanflow.app`
* **Version Name**: `1.2.0`
* **Version Code**: `3`
* **Compile SDK**: `36` (Android 16 API)
* **Target SDK**: `36` (Memenuhi standar kebijakan terbaru Google Play)
* **Minimum SDK**: `26` (Mendukung Android 8.0 Oreo hingga Android 16+)
* **Model Monetisasi**: 100% Gratis (Tanpa iklan, tanpa in-app purchases)
* **Kebijakan Privasi**: 100% Offline-First (Nol pengumpulan data pribadi / zero telemetry)

---

## 4. Langkah-Langkah Upload di Google Play Console

1. Buka [Google Play Console](https://play.google.com/console).
2. Pilih **All apps** > klik **Create app**.
3. Isi rincian aplikasi:
   * **App name**: `ScanFlow — All-in-One Document Toolkit`
   * **Default language**: Indonesian (id) atau English (en-US).
   * **App or game**: App
   * **Free or paid**: Free
4. Pada menu **Release** > **Production** (atau Internal / Closed Testing):
   * Klik **Create new release**.
   * Drag & drop berkas [`release/ScanFlow-v1.0.0-release.aab`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/release/ScanFlow-v1.0.0-release.aab).
   * Masukkan Release notes (misal: *"Rilis perdana ScanFlow Toolkit v1.0.0 dengan 31 fitur dokumen offline"*).
5. Pada menu **Store presence** > **Main store listing**:
   * Upload logo resmi dari file [`playstore_icon.png`](file:///d:/SMK%20Projek/ScanFlow%20-%20ALL%20IN%20ONE%20DOCUMENT%20TOOLKIT/playstore_icon.png) (512x512 px).
   * Upload feature graphic (1024x500 px) dan tangkapan layar ponsel.
6. Simpan dan kirim untuk peninjauan (*Review*).

<div align="center">

<img src="app/src/main/res/drawable/banner_ranzz_music.png" alt="Ranzz Music Banner" width="100%" />

<br/><br/>

# 🎵 Ranzz Music
### *Pure Sound. Zero Noise. Mastered for Android.*

An ultra-lightweight, ad-free music streaming player built with **Jetpack Compose**, **AndroidX Media3**, & **Rust Native Core (`ranzz-core`)**.  
Designed with an uncompromising monochrome & dynamic palette aesthetic, studio-quality 1080p artwork, LiriQo multi-source karaoke lyrics, and zero telemetry.

<br/>

[![Latest Release](https://img.shields.io/github/v/release/RamzzzMD/Ranzz-Music?color=white&label=Release&logo=github&style=for-the-badge)](https://github.com/RamzzzMD/Ranzz-Music/releases/latest)
[![APK Size](https://img.shields.io/badge/APK%20Size-12.6%20MB-black?style=for-the-badge&logo=android)](https://github.com/RamzzzMD/Ranzz-Music/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/RamzzzMD/Ranzz-Music/build-apk.yml?branch=main&label=Build&style=for-the-badge&logo=githubactions)](https://github.com/RamzzzMD/Ranzz-Music/actions)
[![License](https://img.shields.io/badge/License-GPL%20v3-black?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-8.0%2B%20(SDK%2026%2B)-black?style=for-the-badge&logo=android)](https://developer.android.com)

<br/>

[📥 **Unduh APK (GitHub Releases)**](https://github.com/RamzzzMD/Ranzz-Music/releases/latest) • [🆕 **Changelog v3.8.4**](#-apa-yang-baru-di-v384) • [✨ **Features**](#-fitur-utama) • [🏗️ **Architecture**](#-arsitektur--performa) • [🚀 **Building**](#-kompilasi-dari-source)

---

</div>

<br/>

## 🆕 Apa yang Baru di v3.8.4

Pembaruan besar **Ranzz Music v3.8.4** menghadirkan mesin native **Rust (`ranzz-core`)**, sistem lirik multi-sumber **LiriQo**, **Widget Beranda Android**, **Dynamic Album Palette**, serta peningkatan pemutar musik & antrean:

- **📥 Download Lagu & Mode Offline Tanpa Internet (`OfflineDownloadManager`)**
  - **Unduh Lagu 1-Ketukan**: Tombol download langsung pada setiap baris lagu (`TrackItem`), layar pemutar penuh (`FullPlayerSheet`), dan menu opsi lagu (`TrackOptionsBottomSheet`).
  - **100% Putar Offline (Audio + Cover + Lirik)**: Menyimpan file audio kualitas tinggi (`.m4a`/`.webm`), sampul album HD, serta lirik tersinkronisasi ke penyimpanan internal agar bisa diputar tanpa kuota/internet sama sekali.
  - **Tab "Offline" di Library**: Kelola semua lagu yang telah diunduh lengkap dengan informasi ukuran penyimpanan serta tombol **Putar Offline** & **Acak Offline**.
- **🦀 Ranzz Native Core (`ranzz-core` Rust JNI Engine)**
  - Pemrosesan jaringan InnerTube (`WEB_REMIX`), pencarian lirik paralel, sanitasi metadata, penerjemah lirik, dan perhitungan skor rekomendasi kini dieksekusi secara native di Rust (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
  - **Smart Metadata Sanitizer**: Otomatis membersihkan judul lagu dan nama artis dari tag berlebih seperti `(Official Music Video)`, `[Lyrics]`, `VEVO`, `HD`, dll.
- **🎤 LiriQo Multi-Source Lyrics & Full-Screen Karaoke**
  - **4 Sumber Lirik Paralel**: Mengambil lirik secara paralel dari **LRCLIB**, **NetEase Cloud Music**, **Musixmatch**, dan **KuGou** dengan pencocokan kemiripan judul, artis, dan durasi yang presisi.
  - **Syllable / Word-by-Word Karaoke Sync**: Dukungan lirik karaoke per kata/suku kata (*enhanced LRC & KuGou KRC*) dengan animasi sorotan progresif.
  - **Full-Screen Lyrics Sheet**: Mode lirik layar penuh imersif dengan gradasi warna sampul album (*Dynamic Palette*) dan indikator jeda instrumen (`•••`).
- **📱 Android Homescreen Widgets (`RanzzWidgets`)**
  - **Ranzz Compact Widget (4×1)**: Widget bar horizontal ringkas di beranda HP dengan sampul album, judul lagu, artis, dan kontrol Play/Pause & Next.
  - **Ranzz Square Widget (2×2)**: Widget persegi dengan sampul album resolusi tinggi dan tombol Previous, Play/Pause, serta Next yang tersinkronisasi *real-time*.
- **🎛️ Pemutar Musik, Antrean Interaktif & Utilitas Baru**
  - **Dynamic Cover Palette**: Ekstraksi warna dominan sampul album untuk gradasi latar pemutar & lirik.
  - **Interactive Queue Sheet (Drag & Drop)**: Geser untuk mengubah urutan antrean lagu, hapus lagu tertentu, atau bersihkan seluruh antrean.
  - **Track Options Bottom Sheet**: Menu cepat untuk *Play Next*, *Add to Queue*, *Add to Playlist*, dan *Share Track*.
  - **Sleep Timer & Playback Speed**: Pengatur waktu tidur otomatis (`15m`, `30m`, `45m`, `60m`) dan kontrol kecepatan putar (`0.5x` – `2.0x`).
  - **Repeat Mode Lengkap & Swipe Artwork**: Dukungan *Repeat Off*, *Repeat All*, *Repeat One*, serta gestur *swipe* kiri/kanan pada sampul album untuk ganti lagu.
- **🔄 In-App Auto Updater & Dukungan Dwibahasa (ID / EN)**
  - **Pembaruan Langsung di Aplikasi (`UpdateManager`)**: Deteksi otomatis rilis terbaru dari GitHub Releases dengan dialog *What's New* dan pengunduh APK terintegrasi.
  - **Bilingual UI (Bahasa Indonesia & English)**: Pengaturan bahasa aplikasi (*Mengikuti Sistem*, *Bahasa Indonesia*, *English*) langsung dari menu Pengaturan, dilengkapi fitur kustomisasi profil (*Edit Profile*).

<br/>

---

## 🎯 Mengapa Ranzz Music?

Sebagian besar aplikasi pemutar musik modern dipenuhi iklan, rekomendasi algoritma yang berisik, animasi berat yang memakan baterai, dan ukuran aplikasi puluhan hingga ratusan megabyte.

**Ranzz Music hadir sebagai antitesis:**
- **🦀 Rust-Powered & R8 Optimized:** Mesin inti ditulis dengan Rust + dipangkas dengan R8 FullMode & ProGuard.
- **🚫 Zero Ads & Zero Tracking:** Bebas pelacak privasi, tanpa interupsi audio atau banner komersial.
- **🔋 OLED Pure Black (`#000000`) & Dynamic Palette:** Hemat baterai maksimal pada layar AMOLED/OLED dengan aksen warna sampul album yang elegan.
- **⚡ 120 FPS Fluid Motion:** Recomposition terisolasi tanpa overhead pada Main Thread.

<br/>

---

## 🎨 Identitas Visual & Ikon Aplikasi

<p align="center">
  <img src="RanzzMusic-assets/01-logo-ranzz-music.png" alt="Ranzz Music Logo" width="105" />
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="RanzzMusic-assets/02-app-icon-launcher.png" alt="Launcher Icon" width="105" />
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="RanzzMusic-assets/04-app-icon-circle.png" alt="Circle Icon" width="105" />
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="RanzzMusic-assets/05-logo-monochrome.png" alt="Monochrome Logo" width="105" />
</p>

<br/>

---

## ✨ Fitur Utama

### 🎨 1. Desain Pemutar Penuh Modern & Dynamic Palette
- **Full-Bleed Edge-to-Edge Artwork & Swipe Gesture:** Album cover membentang penuh dengan dukungan *swipe* kiri/kanan untuk berpindah lagu dan gradien vertikal dinamis yang menyatu dengan warna sampul.
- **Vibrant Favorite Bookmark & Quick Actions:** Pintasan satu-ketukan untuk menyimpan favorit, beralih output audio, membuka lirik layar penuh, mengatur antrean, *sleep timer*, dan kecepatan putar.

### 🎤 2. LiriQo Multi-Source Karaoke Lyrics & Terjemahan Dwibahasa
- **4 Sumber Lirik Otomatis:** Menggabungkan **LRCLIB**, **NetEase**, **Musixmatch**, dan **KuGou** untuk cakupan lirik paling lengkap.
- **Word-by-Word & Line Sync:** Mendukung sinkronisasi per kata (*syllable karaoke*) maupun per baris dengan indikator jeda instrumen (`•••`).
- **Bilingual Subtitle Toggle (`文A`):** Tampilkan atau sembunyikan terjemahan lirik secara instan di kartu lirik maupun tampilan layar penuh.

### 📱 3. Widget Beranda Android (Compact & Square)
- **Ranzz Compact (4×1) & Square (2×2):** Kontrol musik langsung dari layar utama Android Anda dengan tampilan sampul album yang tajam.

### 📁 4. Sistem Playlist Mandiri (Custom Playlists)
- **Manajemen Lengkap:** Buat playlist tanpa batas di tab Library.
- **Dynamic 2x2 Artwork Collage:** Otomatis merangkai kolase 4 cover lagu pertama menjadi thumbnail playlist yang estetik.
- **One-Tap Actions:** Tombol `Play All` berurutan dan `Shuffle` acak dengan manajemen antrean *drag-and-drop*.

### 🧠 5. On-Device Smart Recommendation (Taste Tracker)
- **Pelacak Afinitas Lokal (Rust Native Scorer):** Sistem privat menghitung preferensi musikmu di perangkat tanpa mengirim data ke server luar:
  - Menyelesaikan lagu = `+3 poin`
  - Menyukai / Favorit = `+5 poin`
  - Mengulang / Repeat = `+4 poin`
  - Skip cepat (<15 detik) = `-2 poin`
- **Rak Beranda Personal:** Otomatis menyajikan rak *"Lagi Sering Kamu Putar"* (*On Repeat*) dan *"Mix untuk Kamu"* di beranda.

### 🎧 6. Deteksi Output Perangkat Audio Pintar
- Pill interaktif di layar pemutar mendeteksi koneksi audio secara real-time:
  - 🔌 `Wired Connection` (Headset kabel 3.5mm / Type-C)
  - 📶 `Bluetooth: <Nama Device>` (TWS / Headphone nirkabel)
  - 📱 `Phone Speaker` (Speaker internal HP)
- Ketuk untuk langsung membuka panel kontrol output suara / Bluetooth sistem Android.

### 🖼️ 7. Ultra-HD 1080p Studio Artwork
- Gambar cover resmi YouTube Music (`googleusercontent.com`) otomatis ditarik langsung ke master asset studio beresolusi **1080×1080 px**.
- Video cover YouTube (`ytimg.com`) menggunakan fallback cerdas ke **1280×720 Ultra-HD** (`maxresdefault`).

### 📻 8. Automix Radio & Antrean Tanpa Henti
- Memutar lagu langsung memicu radio automix YouTube Music (`RDAMVM<videoId>`) berisi trek yang seirama dan mengisi ulang antrean secara otomatis.

<br/>

---

## 🏗️ Arsitektur & Performa

Ranzz Music dirancang menggunakan arsitektur hybrid **Kotlin Jetpack Compose + Native Rust (`ranzz-core`)**:

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│ (Home • Search • Library • FullPlayer • Lyrics • Widget)│
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow / DerivedState
┌───────────────────────────▼────────────────────────────┐
│            MusicViewModel & PlayerQueueManager         │
│  (Isolated Recomposition • Playback & Queue State)     │
└──────┬────────────────────┬────────────────────┬───────┘
       │                    │                    │
┌──────▼──────┐      ┌──────▼──────┐      ┌──────▼──────┐
│  Repository │      │ TasteManager│      │ Media3 /    │
│  & Caching  │      │ & Playlists │      │ ExoPlayer   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                    │
┌──────▼────────────────────▼────────────────────▼──────┐
│         RanzzNativeCore (Rust JNI - libranzz_core.so)  │
│  • LiriQo (LRCLIB / NetEase / Musixmatch / KuGou)      │
│  • InnerTube Client • Sanitizer • Translator • Scorer  │
└───────────────────────────────────────────────────────┘
```

| Komponen | Implementasi | Keuntungan Teknis |
|---|---|---|
| **UI Framework** | Jetpack Compose (M3) + Palette | Declarative UI, Dynamic Cover Theme, rendering GPU 120 FPS |
| **Native Engine** | Rust (`ranzz-core` via JNI) | Ekstraksi lirik LiriQo paralel, InnerTube API, & sanitasi ultra-cepat |
| **Media Engine** | AndroidX Media3 (1.3.1) | Foreground Service anti-kill, MediaSession lockscreen & widget sync |
| **Extractor** | NewPipeExtractor + InnerTube | Resolving audio stream M4A/Opus dengan fallback multi-klien |
| **Optimization** | R8 FullMode + ProGuard + LTO | Stripped native symbols (`opt-level = "z"`, LTO) & shrunk resources |
| **Caching** | Multi-tier LRU + Disk Cache | Home (10m), Search (5m), Radio (30m), Lyrics (7d) |

<br/>

---

## 📥 Unduh & Instalasi

Unduh berkas APK resmi langsung dari halaman **[GitHub Releases](https://github.com/RamzzzMD/Ranzz-Music/releases/latest)**:

🚀 **[Kunjungi Halaman Rilis Resmi (v3.8.4)](https://github.com/RamzzzMD/Ranzz-Music/releases/tag/v3.8.4)**

| Berkas APK | Format | Ukuran | Unduh Langsung (1-Klik) | Keterangan |
| :--- | :---: | :---: | :--- | :--- |
| **Ranzz Music Release** | `.apk` | **12.6 MB** | [📥 **Download Ranzz-Music-v3.8.4.apk**](https://github.com/RamzzzMD/Ranzz-Music/releases/download/v3.8.4/Ranzz-Music-v3.8.4.apk) | *Sangat disarankan.* Versi rilis final dengan optimasi R8 ProGuard + Native Rust (`arm64-v8a`, `armeabi-v7a`, `x86_64`), ditandatangani keystore resmi Ranzz Music. Siap pasang langsung di HP. |
| **Ranzz Music Debug** | `.apk` | **31.02 MB** | [📥 **Download Ranzz-Music-v3.8.4-debug.apk**](https://github.com/RamzzzMD/Ranzz-Music/releases/download/v3.8.4/Ranzz-Music-v3.8.4-debug.apk) | Versi pengujian debug yang memuat simbol inspeksi lengkap untuk pengembang & logcat. |

> **📱 Panduan Pasang APK di HP Android:**
> 1. Klik tautan [Download Ranzz-Music-v3.8.4.apk](https://github.com/RamzzzMD/Ranzz-Music/releases/download/v3.8.4/Ranzz-Music-v3.8.4.apk) di atas untuk langsung mengunduh file `.apk` ke HP Android Anda (atau gunakan fitur **Check for Updates** langsung dari dalam aplikasi).
> 2. Buka file hasil unduhan dan pilih **Install**.
> 3. Jika muncul notifikasi keamanan *"Install unknown apps"*, izinkan akses untuk browser atau file manager Anda.

<br/>

---

## 🚀 Kompilasi dari Source

Semua proses build (termasuk kompilasi cross-target Rust NDK dan Gradle) ditangani secara otomatis melalui **GitHub Actions Runner**.

Jika Anda ingin mengompilasi proyek secara lokal:

```bash
# 1. Clone repositori
git clone https://github.com/RamzzzMD/Ranzz-Music.git
cd Ranzz-Music

# 2. Build Native Rust Core (memerlukan cargo-ndk & Android NDK)
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o app/src/main/jniLibs build --release --manifest-path ranzz-core/Cargo.toml

# 3. Compile Release APK
chmod +x gradlew
./gradlew assembleRelease
```

Hasil berkas APK siap pakai akan tersedia di:
```
app/build/outputs/apk/release/app-release.apk
```

<br/>

---

## 🛡️ Privasi & Keamanan

- **Zero Data Collection:** Ranzz Music tidak memiliki server analitik, tidak mencatat riwayat pemutaran ke cloud, dan tidak menyematkan tracker pihak ketiga (Firebase, Google Analytics, dll).
- **Safe Google Login:** Sesi login YouTube Music diproses melalui WebView lokal terisolasi di perangkat Anda. Kredensial dan cookie hanya disimpan dalam `SharedPreferences` privat perangkat.
- **Open Source:** Seluruh kode sumber (Kotlin & Rust) terbuka untuk diaudit secara publik.

<br/>

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah lisensi **GNU General Public License v3.0 (GPL-3.0)** — lihat file [LICENSE](LICENSE) untuk informasi selengkapnya.

<br/>

<div align="center">

Dibuat dengan rasa cinta terhadap kesederhanaan & musik yang jernih.  
**Ranzz Music — Made for pure music lovers.**

</div>

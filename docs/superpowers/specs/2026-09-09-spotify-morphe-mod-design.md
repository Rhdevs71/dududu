# Spesifikasi Desain: Modul Ekstensi Spotify Piko untuk Morphe CLI

- **Tanggal**: 2026-09-09
- **Target Aplikasi**: Spotify: Music and Podcasts
- **Versi Target**: `9.1.82.1596` (VersionCode: `146025380`, Arsitektur: `arm64-v8a`)
- **Package Asli**: `com.spotify.music`
- **Package Kloningan (Piko)**: `com.spotify.music.pikoo`
- **Target OS**: Android 10 – 15 (SDK 29 – 35)
- **Patch Engine**: Morphe CLI & Gradle Patcher Plugin (`app.morphe.patches`)

---

## 1. Latar Belakang & Tujuan
Menghadirkan modul modifikasi resmi Piko untuk Spotify berbasis penambalan bytecode Dalvik (Morphe CLI) yang dapat digunakan baik sebagai aplikasi mandiri maupun kloning berdampingan. Seluruh batasan akun gratis (iklan audio/video, limit 6x skip per jam, forced-shuffle, batasan kuota lirik bulanan, pembatasan kualitas audio) dibuka secara permanen dan terpusat melalui arsitektur dua lapis (Kotlin Patcher + Java Runtime Extension).

---

## 2. Arsitektur Sistem

### 2.1 Layer 1: Kotlin Bytecode Patches (`patches/src/main/kotlin/app/crimera/patches/spotify/`)
- Menggunakan engine Morphe AST dan Dalvik instruction transformers.
- Bertanggung jawab memodifikasi instruksi Smali di dalam DEX sebelum APK ditandatangani.
- Menangani dekonstruksi dan restrukturisasi `AndroidManifest.xml` untuk kloning package.

### 2.2 Layer 2: Java Runtime Extension (`extensions/spotify/src/main/java/app/morphe/extension/spotify/`)
- Kode Java modular yang di-bundle dan disuntikkan ke dalam APK target.
- Mengelola state runtime, preferensi pengguna (`SpotifyPref`), dan jembatan pemanggilan JNI/native.
- Logging sentral ke `/sdcard/Download/Piko/piko_debug.log`.

---

## 3. Rincian Fitur & Titik Injeksi Smali

### 3.1 Core Playback Restrictions & On-Demand Playback
- **Komponen Patcher**: `SpotifyPlaybackRestrictionsPatch.kt` & `SpotifyOnDemandPlaybackPatch.kt`
- **Target Injeksi**:
  1. `com.spotify.player.model.AutoValue_PlayerState`
     - Method `restrictions()` & `contextRestrictions()`: mengembalikan `Restrictions.EMPTY`.
  2. `com.spotify.player.model.AutoValue_Restrictions`
     - Seluruh method `disallow*Reasons()`: mengembalikan himpunan kosong via delegasi ke `Restrictions.EMPTY`.
     - Method `can*()`: dipaksa mengembalikan `true`.
- **Hasil**: Bebas skip lagu tanpa batas, seekbar aktif dan bisa digeser, mode acak bisa dinonaktifkan, dan lagu apa pun dapat diputar langsung.

### 3.2 Zero-Ad Audio & Video Blocker
- **Komponen Patcher**: `SpotifyAdblockPatch.kt`
- **Runtime Helper**: `SpotifyAdManager.java`
- **Target Injeksi**:
  1. `com.spotify.ads.esperanto.proto.SubInStreamResponse`
     - Method `n()` (return `Ad`): mengembalikan `Ad.getDefaultInstance()`.
     - Method boolean slot iklan: mengembalikan `false` via `SpotifyAdManager.neutralizeAdFlag`.
  2. `com.spotify.ads.esperanto.proto.SubSlotResponse` & `GetSlotResponse`
     - Boolean checks dinetralkan ke `false`.
  3. `AutoValue_PlayerState->adBreakContext()`:
     - Mengembalikan singleton *Absent* (`p.p5.a`), memotong antrean iklan interupsi.

### 3.3 ProductState Premium Spoofing & Anti-Upsell
- **Komponen Patcher**: `SpotifyProductStatePatch.kt` & `SpotifyAntiUpsellPatch.kt`
- **Runtime Helper**: `SpotifyProductStateSpoofer.java`
- **Target Injeksi**:
  1. Protokol Esperanto `EsSession$ProductStateMap` & wrapper `p.i6z`:
     - Intersepsi atribut: `type="premium"`, `can_play_on_demand="1"`, `interruption-free="1"`, `ads="0"`, `streaming-rules=""`, `unlimited-skips="1"`.
  2. `com.spotify.upsells.v1.proto.ShouldUpsellRequest` & `GetUpsellRequest`:
     - Seluruh method boolean eligibility dinetralkan ke `false`.

### 3.4 Unlimited Live Lyrics
- **Komponen Patcher**: `SpotifyLyricsPatch.kt`
- **Target Injeksi**:
  1. `LyricsFullscreenPageActivity`: method `isLyricsCapped` dialihkan mengembalikan `false`.
  2. Model `LyricsResponse`: `capStatus_` disetel ke 0 (`NONE`).

### 3.5 Very High Audio Quality (320 kbps)
- **Komponen Patcher**: `SpotifyAudioQualityPatch.kt`
- **Target Injeksi**:
  1. `com.spotify.player.model.AutoValue_PlaybackQuality`
     - Method `bitrateLevel()`, `highestAvailableQuality()`, `targetBitrateLevel()`: mengembalikan `BitrateLevel.VERY_HIGH`.

### 3.6 Piko Settings Menu Injection
- **Komponen Patcher**: `SpotifySettingsPatch.kt`
- **Runtime UI**: `SpotifySettingsDialog.java`
- **Target Injeksi**: Injeksi trigger menu pada toolbar halaman utama (`MainActivity`).

### 3.7 Clone Patch (Dual Installation)
- **Komponen Patcher**: `SpotifyClonePatch.kt`
- **Runtime Helper**: `SpotifyPackageSpoofer.java`
- **Target Injeksi**:
  1. `AndroidManifest.xml`: package `com.spotify.music.pikoo`, 14 authorities provider diubah, permission unik, label `Piko Spotify`.
  2. String konstanta bytecode: penggantian string package dan authority.
  3. `Context.getPackageName()`: dispoof ke `com.spotify.music` pada panggilan internal stack trace Spotify.

---

## 4. Rencana Verifikasi & Pengujian
1. **Kompilasi Gradle & Patcher**:
   - Memastikan `compileKotlin` pada `:patches` dan kompilasi `:extensions:spotify` sukses tanpa error lint/type.
2. **Patching APK via Morphe CLI**:
   - Memproses `C:\Users\Rhdevs\Downloads\spt.apkm` (atau `base.apk`) menggunakan Morphe CLI.
3. **Audit Dalvik Smali & Tanda Tangan**:
   - Memastikan 0 Dalvik VerifyError pada Android 15 (SDK 35).
   - Menandatangani APK kloning dengan Android SDK 35 `apksigner`.
4. **Verifikasi Fungsional**:
   - Pemutaran musik on-demand, skip tanpa batas, ketiadaan iklan, dan lirik aktif.

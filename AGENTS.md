# AGENTS.md — Piko Project Memory & Technical Knowledge Base

> **PERINGATAN UNTUK SEMUA AGENT / ASSISTANT**:
> File ini adalah memori permanen dan panduan teknis proyek **Piko (Instagram & Twitter Mod)**.
> Jika percakapan baru dimulai atau memori percakapan sebelumnya terpotong/hilang, **BACA DAN PATUHI SELURUH DOKUMEN INI SEBELUM MENGAMBIL TINDAKAN APAPUN**.

---

## 1. Identitas Proyek & Lingkungan Teknis

* **Nama Proyek**: Piko (Modifikasi/Ekstensi Kustom untuk Instagram & Twitter berbasis Bytecode Patching)
* **Repository Git**: `Rhdevs71/dududu` (Branch: `main`)
* **Workspace Lokal**: `c:\Users\Rhdevs\Downloads\Compressed\piko-mainbaru\piko-main`
* **Target APK Utama Pengguna**: `c:\Users\Rhdevs\Downloads\apknya.apkm` (Instagram Android versi `444.0.0.46.85`)
* **Package Name Target (Kloningan)**: `com.instagram.android.pikoo`
* **Target Device Pengguna**: Smartphone TECNO, OS Version: **Android 15 (SDK 35)**
* **Patching Engine**: Morphe CLI (`C:\Users\Rhdevs\Downloads\morphe-cli.jar`)
* **CI/CD Pipeline**: GitHub Actions Release Workflow (`.github/workflows/release.yml`)
* **Token GitHub**: Digunakan untuk API monitoring/download rilis (tersimpan di script scratch lokal)

---

## 2. Riwayat Perkembangan & Milestone (Sudah Sampai Tahap Mana?)

1. **Tahap 1: Mengatasi Dalvik/ART VerifyError pada Android 15 (Rilis v1.0.10 - v1.0.12)**
   - *Masalah*: APK Instagram v444 mengalami crash fatal saat startup:
     `java.lang.VerifyError: Verifier rejected class X.04aT: java.lang.Object X.04aT.unsafeParseFromJson(X.03Ie): [0x146] invalid use of move-result-object as branch target at 0x14b`.
   - *Penyebab*: Obfuscator Instagram memecah method parser JSON di mana instruksi `move-result-object` menjadi sasaran loncatan (*branch target*), yang secara ketat ditolak oleh ART Verifier Android 15 (SDK 35).
   - *Solusi*: Patching bytecode disesuaikan agar target branch tidak mendarat pada `move-result-object` serta memperbaiki register offset.

2. **Tahap 2: Perbaikan Fitur Friendship Status Indicator (Rilis v1.0.13 - v1.0.14)**
   - *Masalah*: Fitur indikator status pertemanan (follow-back badge) di profil Instagram tidak muncul atau gagal memuat status akun.
   - *Penyebab*: Perubahan fingerprint UI pada v444 di mana layout badge profil dipindahkan ke controller baru.
   - *Solusi*: Memperbarui hook pada `ProfileInfoFingerprint` dan merapikan inject badge di `FriendshipStatusIndicator.java`.

3. **Tahap 3: Penerapan Sistem Debug Log Terpusat & Terinci di SEMUA Fitur (Rilis v1.0.15 - Terkini)**
   - *Permintaan Pengguna*: Pengguna meminta **SEMUA** fitur, patch, tambalan, dan error handler mencatat kegagalan ke satu file log terpadu secara rinci (bukan hanya fitur download, dan tidak hanya menampilkan toast singkat yang cepat hilang).
   - *Solusi*:
     - Membuat bridge logger sentral `PikoLog.java`.
     - Mengalihkan semua `catch` block di 40+ file ke `PikoLog.e` / `PikoUtils.logger`.
     - Mengarahkan fatal uncaught crash pada thread mana pun ke `CustomCrashHandler.java`.
     - Mengarahkan semua toast ke file log melalui `PikoUtils.toast`.
     - File log disimpan di: **`/sdcard/Download/Piko/piko_debug.log`**.
   - *Status Saat Ini*: Rilis **`v1.0.15`** (`patches-1.0.15.mpp`) berhasil dirilis via CI Run #27. APK `C:\Users\Rhdevs\Downloads\instagram_v1015_59patches.apk` telah dipatch dan diaudit (102 calls checked, **0 warnings / 0 VerifyError**).

---

## 3. Arsitektur Sistem Debug Logging (`piko_debug.log`)

File log berada di penyimpanan internal perangkat:
📁 **`/sdcard/Download/Piko/piko_debug.log`**

### Format Catatan Log:
```text
================================================================================
[DEBUG_LOG] 2026-09-06 00:25:30.123 [main] TAG: DownloadUtils
Message: Error at downloadPost
Exception: java.lang.ClassCastException: java.lang.Long cannot be cast to java.lang.String
Stack trace:
	at app.morphe.extension.instagram.entity.MediaData.getPostID(MediaData.java:82)
	at app.morphe.extension.instagram.patches.download.DownloadUtils.downloadPost(DownloadUtils.java:213)
	...
================================================================================
```

### Komponen Kunci:
1. **`PikoUtils.java` (`extensions/shared/library`)**:
   - `logToFile(String tag, Object e)`: Menulis log blok lengkap (timestamp ms, thread name, tag, exception, stack trace).
   - `toast(String msg)`: Mencatat `[TOAST] <pesan>` ke file sebelum menampilkan pop-up toast.
   - `logger(String tag, String msg, Throwable t)`: Menerima exception dan mencetak stack trace lengkap.
2. **`CustomCrashHandler.java` (`extensions/shared/library`)**:
   - Mencegat uncaught exception fatal pada thread aplikasi dan menulis stack trace lengkap ke `piko_debug.log` sebelum aplikasi mati.
3. **`PikoLog.java` (`extensions/instagram`)**:
   - Helper sentral untuk mempermudah pemanggilan log pada seluruh patch Instagram (`PikoLog.e(tag, msg, throwable)`).

---

## 4. Katalog Lengkap Seluruh Fitur (59 Patches Piko Instagram)

Berikut adalah daftar lengkap seluruh patch dan fitur yang aktif pada aplikasi Piko Instagram:

### A. Fitur Pengunduhan Media (Downloader)
1. **Feed Post Downloader**: Mengunduh foto dan video dari beranda/feed dalam resolusi tertinggi.
2. **Carousel Downloader**: Memilih untuk mengunduh satu media spesifik atau mengunduh seluruh isi carousel ("Download all").
3. **Reels Video Downloader**: Mengunduh video Reels langsung dari tombol kustom atau menu overflow.
4. **Story & Highlight Downloader**: Mengunduh story dan arsip highlight akun lain dengan nama file bersih (disertai username).
5. **Direct Message (DM) Voice Note Downloader**: Mengunduh pesan suara (voice memo) dari ruang obrolan DM sebagai file `.mp3`.
6. **High-Res Profile Picture Downloader**: Menekan/membuka foto profil akun mana pun dalam resolusi penuh dan menyimpannya.
7. **Media Quality Variants Picker**: Membuka dialog untuk memilih resolusi dan varian bitrate video/gambar sebelum mengunduh.
8. **External Downloader Integration**: Mengirim tautan media langsung ke aplikasi pengunduh eksternal (seperti Seal, 1DM, IDM, dll.) yang nama packagenya bisa diatur di Pengaturan Piko.

### B. Privasi & Ghost Mode (Mode Siluman)
9. **Ghost View Stories (Lihat Story Anonim)**: Melihat story pengguna lain tanpa memicu status terbaca (nama kita tidak muncul di daftar *viewers*).
10. **Ghost View Direct Messages (Baca DM Tanpa Centang 'Seen')**: Membaca pesan obrolan DM tanpa memicu tanda terbaca bagi pengirim.
11. **Ghost View Live Streams (Tonton Live Anonim)**: Masuk dan menonton siaran langsung Instagram tanpa nama akun kita tampil di daftar penonton.
12. **Anti-Disappearing / Ephemeral Media**: Mencegah media sekali lihat (view-once photo/video) di DM menghilang setelah dibuka; media dapat dibuka berkali-kali dan disimpan.
13. **Deleted Messages Vault**: Mencatat dan menyimpan riwayat DM yang ditarik/dihapus oleh lawan bicara ke dalam database lokal SQLite (`PikoMessageDb`), dapat dilihat kembali di menu khusus.

### C. Personalisasi Feed & Story
14. **Limit Feed to Following Profiles**: Opsi untuk membatasi tampilan feed beranda hanya menampilkan postingan akun yang difollow, membersihkan feed dari postingan algoritma 'feed_recs'.
15. **Custom Like Animation**: Mengubah efek animasi ikon like/hati saat double tap (mendukung beragam opsi animasi kustom).
16. **Custom Story Ring Size**: Mengubah ukuran diameter cincin story di tray beranda sesuai selera pengguna.
17. **Story Timestamp Customization**: Menampilkan tanggal dan jam upload story secara presisi dan akurat.
18. **View Hidden Story Mentions**: Menampilkan daftar seluruh akun yang di-tag/di-mention dalam story meskipun tag tersebut disembunyikan di luar layar oleh pemilik story.
19. **Story Looping**: Mengulang pemutaran story secara otomatis tanpa berpindah ke story berikutnya.
20. **Disable Story Auto-Flipping**: Mencegah story berganti halaman otomatis saat pengguna sedang melihat story.
21. **Hide Navigation Buttons**: Menyembunyikan tombol navigasi bawah yang tidak diinginkan (misalnya tab Reels, Belanja/Shop, atau Pencarian).

### D. Interaksi Profil & Teks
22. **Friendship Status Indicator**: Menampilkan lencana/teks penanda di profil apakah akun tersebut mengikuti Anda kembali (*Follows You / Mutual*) atau tidak (*Not Following Back*).
23. **One-Click Copy Bio**: Menyalin seluruh teks biografi pengguna di profil ke clipboard hanya dengan satu sentuhan.
24. **Clean URLs & Tracking Stripper**: Menghapus parameter pelacak Meta (seperti `?igsh=...`, `fbclid`, dll.) saat menyalin tautan, serta opsi mengubah domain tautan.
25. **Custom Action Bar Buttons**: Menyematkan tombol cepat (ghost icon toggle, direct download button, Piko Settings gear) pada action bar feed, profil, dan obrolan.
26. **Comment Action Buttons**: Tombol khusus pada baris komentar untuk menyalin teks komentar secara instan.

### E. Tema & Tampilan Visual
27. **Material You Monet Theming (Android 12+)**: Mengintegrasikan warna palet wallpaper dinamis perangkat ke seluruh komponen antarmuka Instagram.
28. **AMOLED Pure Black Dark Mode**: Mode gelap hitam murni (true #000000) untuk menghemat baterai layar OLED/AMOLED dan tampilan visual yang kontras.

### F. Developer & Fitur Lanjutan
29. **Unlock Employee Options**: Membuka tab tersembunyi *Meta Internal Developer Options* pada menu pengaturan Instagram.
30. **MobileConfig Flags & Recommended Flags**: Mengubah ribuan konfigurasi flag eksperimen Meta secara langsung atau menerapkan konfigurasi rekomendasi Piko (menonaktifkan iklan sponsor, menyetel fitur baru, dll.).
31. **Disable Double Tap Gestures**: Opsi mematikan gestur double tap like secara independen pada postingan feed, reels, atau komentar untuk mencegah like yang tidak disengaja.
32. **Backup & Restore Preferences**: Mengekspor seluruh konfigurasi pengaturan Piko ke file JSON di penyimpanan dan mengimpornya kembali kapan saja.
33. **Object Browser**: Tool runtime debugging untuk menginspeksi hierarki objek JVM internal Instagram saat mengembangkan patch baru.

---

## 5. Struktur Direktori Kode Penting

```text
piko-main/
├── patches/                                  # Logika patch Morphe (Kotlin bytecode transformers & Smali)
│   └── src/main/kotlin/app/morphe/patches/
│       └── instagram/                        # Definisi 59 patch Instagram
├── extensions/
│   ├── shared/library/                       # Library inti Piko bersama
│   │   └── .../crimera/
│   │       ├── PikoUtils.java                # Engine utilitas, file logger, toast logger
│   │       ├── CustomCrashHandler.java       # Uncaught exception crash catcher
│   │       ├── ObjectBrowser.java            # Runtime JVM inspector
│   │       └── sharedPreference/             # Manajemen preferensi bersama
│   └── instagram/                            # Ekstensi khusus Instagram
│       └── .../instagram/
│           ├── utils/
│           │   ├── PikoLog.java              # Helper sentral logging Instagram -> piko_debug.log
│           │   ├── Pref.java                 # Akses ke switch preferensi pengguna
│           │   └── InstaUtils.java           # Utilitas umum Instagram
│           ├── patches/
│           │   ├── download/DownloadUtils.java # Dialog unduhan & router download media
│           │   ├── userprofile/              # Friendship indicator, profil picture, bio
│           │   ├── feed/                     # Limit feed, like animation, more options
│           │   ├── story/                    # Story button, timestamp, mentions, loop
│           │   ├── dm/                       # Deleted messages, ephemeral media
│           │   └── devFlags/                 # HookFlags, RecommendedFlags, EmployeeOptions
│           └── theme/                        # MaterialYouTheme & AMOLED loader
```

---

## 6. Prosedur Kerja: Kompilasi, Rilis, Patch, dan Verifikasi

### A. Alur Kompilasi & Rilis (CI/CD)
Karena dependensi Morphe berada di GitHub Packages berotentikasi, kompilasi patch bundle (`.mpp`) dilakukan melalui GitHub Actions:
1. Buat commit perubahan: `git add extensions/ ; git commit -m "..."`
2. Push ke remote: `git push origin main`
3. GitHub Actions workflow (`Release`) akan berjalan dan otomatis membuat rilis baru (misal `v1.0.15`) yang memuat `patches-x.x.x.mpp`.

### B. Alur Patching APK Lokal
Setelah file `.mpp` diunduh dari rilis GitHub:
```powershell
java -jar "C:\Users\Rhdevs\Downloads\morphe-cli.jar" patch `
  -p "C:\Users\Rhdevs\Downloads\patches-1.0.15.mpp" `
  -e Clone `
  -O packageName=com.instagram.android.pikoo `
  -o "C:\Users\Rhdevs\Downloads\instagram_v1015_59patches.apk" `
  -r "C:\Users\Rhdevs\Downloads\patch_result_59_v1015.json" `
  --continue-on-error `
  --unsigned `
  "c:\Users\Rhdevs\Downloads\apknya.apkm"
```

### C. Alur Audit Bytecode (Wajib Dijalankan!)
Sebelum menyatakan APK aman ke pengguna, jalankan audit bytecode untuk memastikan tidak ada `VerifyError` pada Android 15:
```powershell
java -cp "C:\Users\Rhdevs\.gemini\antigravity-ide\brain\1d333b8e-d15c-4ed1-ba84-247ab0ce728e\scratch;C:\Users\Rhdevs\Downloads\morphe-cli.jar" AuditPatchedApk "C:\Users\Rhdevs\Downloads\instagram_v1015_59patches.apk"
```
*Syarat lulus*: `Warnings found: 0` dan seluruh pemanggilan method ekstensi cocok dengan register serta signature class target.

---

## 7. Aturan Khusus & Batasan untuk Agent (User Rules)

1. **Rule 5.1 & 5.2 (Scope Lock & Larangan Perubahan Diam-Diam)**:
   - DILARANG mengubah file di luar rencana (*plan*) yang telah disetujui pengguna.
   - Selalu sampaikan rencana perubahan sebelum memodifikasi file.
2. **Rule 6.1 & 6.2 (Larangan Command Palsu / Tidak Berguna)**:
   - Dilarang menjalankan sleep/timer dummy. Semua proses harus memiliki tujuan teknis nyata dengan verifikasi exit code.
3. **Standar Penanganan Error**:
   - DILARANG membiarkan catch block kosong (`catch (Exception ignored) {}`) tanpa alasan yang sangat kuat.
   - Semua error baru WAJIB menggunakan `PikoLog.e(TAG, message, exception)` agar tercatat di `/sdcard/Download/Piko/piko_debug.log`.

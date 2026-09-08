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

3. **Tahap 3: Penerapan Sistem Debug Log Terpusat & Terinci di SEMUA Fitur (Rilis v1.0.15)**
   - *Permintaan Pengguna*: Pengguna meminta **SEMUA** fitur, patch, tambalan, dan error handler mencatat kegagalan ke satu file log terpadu secara rinci (bukan hanya fitur download, dan tidak hanya menampilkan toast singkat yang cepat hilang).
   - *Solusi*:
     - Membuat bridge logger sentral `PikoLog.java`.
     - Mengalihkan semua `catch` block di 40+ file ke `PikoLog.e` / `PikoUtils.logger`.
     - Mengarahkan fatal uncaught crash pada thread mana pun ke `CustomCrashHandler.java`.
     - Mengarahkan semua toast ke file log melalui `PikoUtils.toast`.
     - File log disimpan di: **`/sdcard/Download/Piko/piko_debug.log`**.
   - *Status*: Rilis **`v1.0.15`** (`patches-1.0.15.mpp`) berhasil dirilis via CI Run #27.

4. **Tahap 4: Perbaikan 4 Crash & Bug Krusial Berdasarkan Analisis `piko_debug.log` (Rilis v1.0.16)**
   - *Analisis Log Perangkat Pengguna (`piko_debug.log`)*:
     1. `FriendshipStatusIndicator & ProfileMoreOption`: `Method not found in class java.lang.Long`. Pada v444 field `A04` bertipe `long` sehingga `getAdditionalUserInfo()` memuat `Long`. Diperbaiki dengan mengabaikan primitive `Long` dan memanggil method langsung pada model `User`.
     2. `Video Downloader`: `NoClassDefFoundError: VideoVersionIntf`. Interface dipindah ke `com.instagram.api.schemas`. Diperbaiki dengan dynamic reflection `super.getMethod(this.obj, "getUrl")` dan deteksi aman Pando video.
     3. `Image Downloader`: `Method Cmh not found in class ImageInfoImpl`. Fingerprint mengambil method yang salah. Diperbaiki dengan mengekstrak method return `List` dari interface `ImageInfo` (`BaG`) serta dynamic discovery fallback.
     4. `Story Bottom Sheet`: `VerifyError: [0x38] register v0 has type Boolean but expected Reference: java.util.ArrayList`. Hook `SelfStoryAddStoryButtonFingerprint` salah mengambil register `boolean` dari `iget-boolean ... A06:Z`. Diperbaiki dengan hook presisi sebelum `toArray` pada register `ArrayList` `v5`.
   - *Status*: Rilis **`v1.0.16`** (`patches-1.0.16.mpp`) berhasil dirilis via CI Run #29. APK **`C:\Users\Rhdevs\Downloads\instagram_v1.0.16_59patches.apk`** telah dipatch dan diaudit (102 calls checked, **0 warnings / 0 VerifyError**).

5. **Tahap 5: Perbaikan FB Icon Crash di Friendship Badge & Shifting Sentinel UserData (Rilis v1.0.17)**
   - *Analisis Log Perangkat Pengguna (`piko_debug.log`)*:
     1. `FriendshipStatusIndicator`: `IllegalStateException: FB icon drawables are not supported in IG!` saat memuat `fb_ic_friend_*` (`AppFbIconDrawable`). Instagram menolak pemakaian drawable FB icon secara tegas. Diperbaiki dengan mengganti drawable ke icon resmi Instagram (`instagram_user_following_pano_outline_24`, `instagram_user_follow_pano_outline_24`, `instagram_user_unfollow_outline_24`) dan menambahkan try-catch guard.
     2. `UserData & UserDataEntity`: `Method Bvt not found in class com.instagram.user.model.User`, `Method BCu not found`, dan `MalformedURLException: no protocol: A1B`. Terjadi karena `changeFirstString` menimpa string konstanta pertama yang bergeser posisinya akibat penambahan string baru di method. Diperbaiki dengan mengganti `changeFirstString` ke `changeString("sentinel", value)` secara presisi dan menambahkan method direct-call fallback di `UserData.java`.
   - *Status*: Rilis **`v1.0.17`** (`patches-1.0.17.mpp`) berhasil dirilis via CI Run #31. APK **`C:\Users\Rhdevs\Downloads\instagram_v1.0.17_59patches.apk`** telah dipatch dan diaudit (102 calls checked, **0 warnings / 0 VerifyError**).

6. **Tahap 6: Perbaikan Fitur Ganti Icon Aplikasi (Custom App Icon) IG Plus (Rilis v1.0.18 - Terkini)**
   - *Analisis Masalah Reverse Engineering*:
     1. Fitur ganti icon aplikasi pada IG Plus tidak bereaksi saat diklik oleh pengguna.
     2. Controller asli Instagram `LX/07qq;` mendaftarkan `LX/0KNu;` ke lifecycle listener yang menunda pergantian icon sampai aplikasi masuk ke background (`onStop()`).
     3. Pada method `LX/0KNu;->onStop()`, terdapat cacat logika Dalvik: register `v11` (hasil pengecekan kesamaan icon saat ini dengan target) bernilai `0` (`false`). Instruksi `pm.setComponentEnabledSetting(component, v11, v11)` memanggil setting dengan state `0` (`COMPONENT_ENABLED_STATE_DEFAULT`). Karena di `AndroidManifest.xml` ke-13 `activity-alias` memiliki nilai default `android:enabled="false"`, maka state `0` mengembalikan status komponen ke kondisi **DISABLED**! Icon tidak pernah aktif dan memicu log kegagalan internal.
   - *Solusi & Implementasi*:
     1. Membuat `InstaAppIconManager.java` (`app.morphe.extension.instagram.patches.appicon`):
        - Mengambil nama activity alias dari field `A02` pada instance enum `LX/0ClA;` (didukung name-matching fallback ke 14 launcher alias manifest).
        - Mengeksekusi aktivasi seketika: `pm.setComponentEnabledSetting(targetComponent, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)`.
        - Menonaktifkan ke-13 launcher alias lainnya: `pm.setComponentEnabledSetting(otherComp, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP)`.
        - Menampilkan Toast konfirmasi langsung: `"Icon aplikasi berhasil diubah ke: [Nama Icon]!\n(Jika belum berubah di beranda, muat ulang launcher Anda)"`.
        - Mencatat proses secara komprehensif ke `/sdcard/Download/Piko/piko_debug.log`.
     2. Menginjeksi hook di awal method `LX/07qq;->A02(Context, LX/0ClA, UserSession, String, boolean)V` via `UnlockPlusBenefitsPatch.kt` dengan `invoke-static {p1, p2}, InstaAppIconManager;->applyIcon(Context, Object)V`.
   - *Status*: Rilis **`v1.0.18`** (`patches-1.0.18.mpp`) berhasil dirilis via CI Run #35.

7. **Tahap 7: Perbaikan Pop-up Dialog Paywall ("Belum Plus") & Unconditional Unlock Custom App Icon (Rilis v1.0.19)**
   - *Analisis Masalah Reverse Engineering*:
     1. Layar *Ubah Ikon Aplikasi Anda* (`LX/0EGZ;` / `AuraAppIconPickerFragment`) memeriksa hak akses benefit Plus melalui `LX/01oH;->A00(LX/07pc;->A05, UserSession)` -> `LX/07pt;->A0D(String)Z` dengan parameter `"CUSTOM_APP_ICON"`.
     2. Pada `Settings.java`, pengaturan `UNLOCK_PLUS_BENEFITS` sebelumnya disetel `false` secara default.
     3. Hook pada `UnlockPlusBenefitsPatch.kt` hanya memeriksa `Pref.unlockPlusBenefits()`. Karena default bernilai `false`, bytecode melompat ke logika asli Instagram yang mengembalikan `false`.
     4. Akibatnya, icon non-default ditandai dengan status **LOCKED (0)** oleh ViewModel `LX/0EKv;` (`A00`). Saat tombol **"Pilih ikon"** diklik, `LX/0EKv;->A0w` mendeteksi icon masih berstatus terkunci (`state == A00`) lalu meluncurkan coroutine `LX/0Oqd` yang memunculkan **dialog paywall/upsell ("belum plus")** dan membatalkan aktivasi icon.
   - *Solusi & Implementasi*:
     1. Menambahkan method pembantu di `Pref.java`: `public static boolean isBenefitAllowed(String benefit)` yang mengembalikan `true` seketika tanpa syarat untuk `"CUSTOM_APP_ICON"` / `"custom_app_icon"` dan mendelegasikan ke `unlockPlusBenefits()` untuk benefit lainnya.
     2. Mengubah nilai default `Settings.UNLOCK_PLUS_BENEFITS` dari `false` menjadi `true` agar seluruh benefit Plus lainnya (font cerita, preview, dll.) juga aktif out-of-the-box.
     3. Memperbarui hook pada `ActiveBenefitCheckerFingerprint.method` (`LX/07pt;->A0D(String)Z`) via `UnlockPlusBenefitsPatch.kt` menjadi `invoke-static {p1}, Pref;->isBenefitAllowed(String)Z`.
     4. Menambahkan method logger `d(String, Object)` pada `PikoLog.java`.
   - *Status*: Rilis **`v1.0.19`** (`patches-1.0.19.mpp`) berhasil dirilis via CI Run #38. APK **`C:\Users\Rhdevs\Downloads\instagram_v1.0.19_59patches.apk`** telah dipatch dan diaudit.

8. **Tahap 8: Perbaikan Custom App Icon IG Plus (Rilis v1.0.20 - v1.0.21)**
   - Perbaikan awal ViewModel `LX/0EKv;->A00` dan pembungkaman Bloks paywall `LX/0Hff;->A00`.
   - Perbaikan register range smali di `MakeEphemeralPermanentPatch.kt`.

9. **Tahap 9: Penonaktifan Permanen Dialog Upsell Paywall Custom App Icon di Layer Jetpack Compose & Eliminasi Warning Register v23 (Rilis v1.0.22 - Terkini)**
   - *Analisis Masalah Reverse Engineering*:
     1. Pengguna melaporkan bahwa saat ikon di grid disentuh/diklik, langsung muncul popup dialog upsell paywall: *"Buka ikon aplikasi kustom. Pilih ikon yang cocok dengan gaya Anda..."*. Ikon tidak terpilih dan tombol tidak bekerja.
     2. Melalui reverse engineering Jetpack Compose (`classes9.dex` & `classes14.dex`), ditemukan bahwa cell ikon dibangun oleh Composable `LX/0Wn3;->A01` (`AuraAppIconCell`) dan callback klik ditangani oleh lambda `LX/0RAH;->invoke`.
     3. Pada `LX/0RAH;->invoke` baris [287]-[297], Dalvik membaca status ikon `LX/0CJd;->A01` (`LX/0GuK`) dan membandingkannya dengan `sget-object v0, LX/0GuK;->A06:LX/0GuK;` (`IG_PLUS_LOCKED`).
     4. Karena akun non-subscriber, status ikon cocok dengan `A06`. Handler klik **langsung memicu coroutine Case 42** yang memunculkan dialog upsell paywall Bloks/Compose lalu melompat keluar (`goto/16`), **tanpa pernah memanggil** `LX/0EKv;->A00` (method pemilihan ikon)!
     5. Di `HookReelOverflowMenuButton.kt`, register `freeRegisterTwo` terdeteksi bernilai `23`. Smali 22c (`iget-object`) dan 35c (`invoke-static`) hanya mendukung register 4-bit (`v0..v15`), memicu 2 peringatan `[WARN] [STDIO]: Invalid register: v23`.
   - *Solusi & Implementasi*:
     1. Menetralkan field static enum `LX/0GuK;->A06` di method `<clinit>` tepat sebelum `return-void` dengan menyetel `const/4 v0, 0` lalu `sput-object v0, LX/0GuK;->A06`. Karena seluruh objek ikon berstatus enum non-null, maka perbandingan `icon.status == LX/0GuK.A06` di SEMUA tempat otomatis bernilai **FALSE**!
     2. Menetralkan instruksi perbandingan `sget-object v0, LX/0GuK;->A06` langsung di lambda klik `LX/0RAH;->invoke` menjadi `const/4 v0, 0`. Perbandingan `if-ne` selalu lolos ke pemilihan ikon (`LX/0EKv;->A00`) dan melewatkan seluruh blok popup upsell Case 42.
     3. Membatasi register di `HookReelOverflowMenuButton.kt` ke 4-bit safe registers (`safeRegisterOne`, `safeRegisterTwo`) untuk memusnahkan warning STDIO invalid register `v23`.
   - *Status*: Rilis **`v1.0.22`** (`patches-1.0.22.mpp`) berhasil dirilis via CI Run #45. APK **`C:\Users\Rhdevs\Downloads\instagram_v1.0.22_59patches.apk`** telah dipatch dan diaudit (**104 calls checked, 0 warnings / 0 VerifyError**, verifikasi Dalvik menunjukkan `sput-object v0, LX/0GuK;->A06` di `<clinit>`, `const/4 v0, 0` di `LX/0RAH;->invoke`, dan pemanggilan `AddReelButton` bebas dari warning `v23`).

10. **Tahap 10: Perbaikan Dalvik VerifyError pada Reels Controller `X.09qJ.A09` (Rilis v1.1.1 / v1.0.23 - Terkini)**
    - *Analisis Masalah Reverse Engineering*:
      1. Pengguna melaporkan bahwa fitur ganti icon IG Plus telah bekerja dengan sempurna, namun terjadi crash fatal saat membuka Reels:
         `java.lang.VerifyError: Verifier rejected class X.09qJ: void X.09qJ.A09(...) [0x16F] register v1 has type Reference: androidx.fragment.app.FragmentActivity but expected Reference: X.0CJF`.
      2. Pada perubahan sebelumnya di `HookReelOverflowMenuButton.kt`, logika fallback register memilih `v1`. Register `v1` memegang objek instansiasi button adder `LX/0F1s;` (`LX/0CJF`).
      3. Injeksi `iget-object v1, v0, LX/09rS;->A05:Landroidx/fragment/app/FragmentActivity;` menimpa register `v1` dengan `FragmentActivity`. Saat kode asli Instagram membaca `v1` di baris [0x16F] dan mengharapkan `LX/0CJF`, ART Verifier Android 15 menolak class tersebut.
    - *Solusi & Implementasi*:
      1. Melindungi register aktif `v0`, `v1`, `v4`, dan `v2` dalam `reservedRegisters`.
      2. Mengalokasikan `safeRegisterTwo` ke register 4-bit aman yang belum digunakan sebelum titik injeksi (memilih `v5` yang belum diinisialisasi hingga baris [33] di mana ia ditimpa oleh `move-object/from16 v5, v23`).
      3. Register `v1` tetap utuh memegang `LX/0CJF`.
    - *Status Saat Ini*: Rilis **`v1.1.1`** (`patches-1.1.1.mpp` / `patches-1.0.23.mpp`) berhasil dirilis via CI Run #46. APK **`C:\Users\Rhdevs\Downloads\instagram_v1.0.23_59patches.apk`** telah dipatch dan diaudit (**104 calls checked, 0 warnings / 0 VerifyError**, verifikasi Dalvik menunjukkan `v1` utuh dan pemanggilan menggunakan `v5, v1, v4, v2`).

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

---

## 8. Basis Analisis & Pengetahuan Teknis Reverse Engineering (Disassembly v444)

Bab ini mencatat seluruh **sumber acuan (base)**, hasil audit disassembled smali, dan temuan runtime agar agen berikutnya tidak perlu mengulang investigasi dari nol.

### A. Sumber Daya & Path Basis Analisis
1. **Decompiled Base Smali (Acuan Utama Struktur APK Target)**:
   - Path Folder: `C:\Users\Rhdevs\Downloads\apktools\unknown\base`
   - Keterangan: Berisi seluruh hasil disassembly apktool terhadap file APK Instagram v444 (`classes.dex` sampai `classes15.dex` dan seluruh resources XML).
2. **Log Nyata Perangkat Pengguna (Device Truth)**:
   - Path Lokal: `C:\Users\Rhdevs\Downloads\piko_debug.log`
   - Path di Perangkat: `/sdcard/Download/Piko/piko_debug.log`
   - Keterangan: Dicatat langsung oleh smartphone pengguna (TECNO, Android 15 SDK 35) melalui sistem logging terpusat Piko.
3. **Master Target APK**:
   - Path: `c:\Users\Rhdevs\Downloads\apknya.apkm` (Instagram Android versi `444.0.0.46.85`)
4. **Patched APK Terkini**:
   - Path: `C:\Users\Rhdevs\Downloads\instagram_v1.0.20_59patches.apk` (151,525,892 bytes)
5. **Patching Engine & Release MPP**:
   - CLI: `C:\Users\Rhdevs\Downloads\morphe-cli.jar`
   - Bundle Terkini: `C:\Users\Rhdevs\Downloads\patches-1.0.20.mpp` (Rilis GitHub Actions tag `v1.0.20`)

---

### B. Hasil Analisis & Peta Obfuscation Instagram v444

#### 1. Model Akun Pengguna (`com.instagram.user.model.User`)
* **File Smali Acuan**: Disassembly pada `unknown/base/smali/com/instagram/user/model/User.smali`
* **Pemetaan Method Obfuscated v444**:
  - `A1B()` -> `getBio()` (mengembalikan biografi pengguna sebagai `String`)
  - `A6y()` -> `getUsername()` (mengembalikan username pengguna sebagai `String`)
  - `A86()` / `A7N()` -> `getFullName()` / `getFollowersCount()`
  - `A1d()` -> `getFollowingCount()`
  - `A1i()` -> `getProfilePicUrl()` (mengembalikan URL foto profil / objek `ImageUrl`)
  - `A5s()` -> `isVerified()` (mengembalikan boolean status verifikasi centang biru)
* **Temuan Gotcha Primitive `Long`**:
  - Pada Instagram v444, field `A04` pada controller profil bertipe primitif `long`. Jika diekstrak via reflection generic, menghasilkan objek `java.lang.Long` yang tidak memiliki method profil user.
  - *Solusi Paten*: Guarding `!(obj instanceof Long)` dan memanggil method langsung pada instance `com.instagram.user.model.User`.

#### 2. Mekanisme Penggantian String Morphe (`UserDataEntity.kt`)
* **Temuan Masalah Bytecode**:
  - Fungsi Morphe `changeFirstString("val")` hanya mengganti instruksi `const-string` pertama pada sebuah method (index 0).
  - Jika developer menambahkan instruksi/konstanta string baru di baris awal method (misal `return ""` atau log tag), urutan index string bergeser. Akibatnya string nama method yang dituju tidak terganti dan tetap menjadi string sentinel mentah (`"Bvt"`, `"BCu"`, `"methodName"`, `"methodname"`, dll.), memicu `NoSuchMethodException` dan `MalformedURLException`.
* **Solusi Paten**:
  - Selalu gunakan `changeString("sentinel_token", actualMethodName)` secara eksplisit dan presisi.

#### 3. Bottom Sheet Story Controller (`LX/0Tlr;`)
* **File Smali Acuan**: `unknown/base/smali/X/0Tlr.smali` (method `A0o` dan `A0q`)
* **Temuan Masalah Bytecode**:
  - Hook lama menduga instruksi sebelum percabangan `if-eqz` adalah list hasil `move-result-object`.
  - Pada v444, instruksi tersebut telah berubah menjadi `iget-boolean ... A06:Z` (register `v0` boolean).
  - Menginjeksi `v0` ke method `StoryButton.addButtons(ArrayList)` menyebabkan **`java.lang.VerifyError: register v0 has type Boolean but expected Reference`** pada ART Verifier Android 15.
* **Solusi Paten**:
  - Pindahkan titik injeksi tepat sebelum pemanggilan `toArray` di mana register `v5` terbukti menyimpan referensi `java.util.ArrayList` yang valid.

#### 4. Media Downloader (Gambar & Video)
* **File Smali Acuan Gambar**: `unknown/base` (interface `ImageInfo` dan implementasi `ImageInfoImpl`)
  - Fingerprint lama mengambil method internal `Cmh()` yang tidak ada pada `ImageInfoImpl` v444.
  - Method publik interface yang mengembalikan list URL resolusi gambar adalah method dengan return type `Ljava/util/List;` (method interface `BaG`). Ekstraksi kandidat dilakukan dari method ini disertai dynamic fallback.
* **File Smali Acuan Video**: `com/instagram/api/schemas/VideoVersionIntf.smali`
  - Meta memindahkan package `VideoVersionIntf` dari `com.instagram.model.mediasize.*` ke `com.instagram.api.schemas.*`.
  - Penggunaan static cast menyebabkan `NoClassDefFoundError`.
  - *Solusi Paten*: Akses dinamis via reflection `super.getMethod(this.obj, "getUrl")` dan fallback deteksi Pando video model.

#### 5. Larangan Drawable Facebook Icon pada Instagram (`AppFbIconDrawable`)
* **Temuan Runtime Log**:
  - Pemanggilan `ResourceUtils.getDrawable("fb_ic_friend_*")` memicu fatal exception:
    `java.lang.IllegalStateException: FB icon drawables are not supported in IG!`
  - Class internal Instagram `AppFbIconDrawable` sengaja memblokir penggunaan drawable turunan FBUI.
* **Solusi Paten**:
  - Wajib hanya menggunakan drawable resmi Instagram, antara lain:
    * Follows You: `instagram_user_following_pano_outline_24`
    * Mutual: `instagram_user_follow_pano_outline_24`
    * Not Following: `instagram_user_unfollow_outline_24`
  - Seluruh pemanggilan resource icon dibungkus `try-catch` agar kegagalan resource tidak pernah merusak render layout badge status.

#### 6. Arsitektur Ganti Icon Aplikasi (Custom App Icon) & Activity Aliases
* **File Smali Acuan**:
  - `unknown/base/smali/X/07qq.smali` (Controller: `AuraAppIconSwitchManager`)
  - `unknown/base/smali/X/0ClA.smali` (Enum Icon: `DEFAULT`, `CANNES_NEON`, `CANNES_FIRE`, dll.)
  - `unknown/base/smali/X/0KNu.smali` (Lifecycle Listener `onStop()` switch handler)
  - `unknown/base/AndroidManifest.xml` (14 `activity-alias` launcher)
* **Daftar Lengkap 14 Launcher Activity-Alias di Manifest**:
  1. `com.instagram.android.activity.MainTabActivity` (Default)
  2. `com.instagram.android.activity.MainTabActivity.neon` (Cannes Neon)
  3. `com.instagram.android.activity.MainTabActivity.flame` (Cannes Fire)
  4. `com.instagram.android.activity.MainTabActivity.floral` (Cannes Floral)
  5. `com.instagram.android.activity.MainTabActivity.slime` (Cannes Slime)
  6. `com.instagram.android.activity.MainTabActivity.metal` (Cannes Metal)
  7. `com.instagram.android.activity.MainTabActivity.kpop` (Cannes K-Pop)
  8. `com.instagram.android.activity.MainTabActivity.haruko` (Haruko)
  9. `com.instagram.android.activity.MainTabActivity.felipe` (Felipe)
  10. `com.instagram.android.activity.MainTabActivity.humberto` (Humberto)
  11. `com.instagram.android.activity.MainTabActivity.zipeng` (Zipeng)
  12. `com.instagram.android.activity.MainTabActivity.uzo` (Uzo)
  13. `com.instagram.android.activity.MainTabActivity.ricky` (Ricky)
  14. `com.instagram.android.activity.MainTabActivity.throwback` (Throwback)
* **Temuan Masalah Bytecode**:
  - Method bawaan `LX/07qq;->A02` menunda penggantian icon dengan me-register runnable `LX/0NsU;` ke `LX/0KNu;` yang hanya dieksekusi saat activity `onStop()` (aplikasi diminimalkan).
  - Pada `LX/0KNu;->onStop()`, perbandingan `areEqual(source, target)` menghasilkan boolean `0` yang disimpan di register `v11`.
  - Pemanggilan `pm.setComponentEnabledSetting(targetComponent, v11, v11)` memanggil setting dengan state `0` (`COMPONENT_ENABLED_STATE_DEFAULT`). Karena di manifest semua alias bernilai `android:enabled="false"`, nilai 0 justru mematikan alias tersebut kembali!
* **Solusi Paten Piko (`InstaAppIconManager.java`)**:
  - Injeksi langsung di awal method `LX/07qq;->A02(Context, LX/0ClA, UserSession, String, boolean)V`.
  - Mengambil alias target dari field `A02` pada enum `LX/0ClA;`.
  - Memanggil `pm.setComponentEnabledSetting(targetComponent, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP)` seketika.
  - Menonaktifkan 13 alias lainnya (`COMPONENT_ENABLED_STATE_DISABLED`).
  - Menampilkan konfirmasi instan via `PikoUtils.toast` dan mencatat debug log ke `piko_debug.log`.

#### 7. Arsitektur UI Icon Picker & Bypass Paywall Dialog ("Belum Plus")
* **File Smali Acuan**:
  - `unknown/base/smali_classes9/X/0EGZ.smali` (`AuraAppIconPickerFragment`)
  - `unknown/base/smali_classes9/X/0EKv.smali` (ViewModel: `AuraAppIconPickerViewModel`)
  - `unknown/base/smali_classes2/X/01oH.smali` (Benefit Checker Helper: `A00(LX/07pc, UserSession)Z`)
  - `unknown/base/smali/X/07pt.smali` (Benefit Evaluator: `A0D(String)Z`)
  - `unknown/base/smali_classes3/X/06Pb.smali` (Service Layer: `A02`)
* **Alur Logika Pemeriksaan Benefit**:
  - UI memanggil `LX/01oH;->A00(LX/07pc;->A05, UserSession)` dengan enum `LX/07pc;->A05` (`CUSTOM_APP_ICON`).
  - `A00` mengambil field `A00` dari enum (string `"CUSTOM_APP_ICON"`) dan memanggil instance method `LX/07pt;->A0D(Ljava/lang/String;)Z`.
  - Jika `A0D` mengembalikan `false`:
    1. Di `LX/0EKv;->A00`: icon non-default ditandai dengan state `LX/0008;->A00` (integer 0, alias **LOCKED**).
    2. Di `LX/0EKv;->A0w`: saat event `LX/0dcX` (klik tombol "Pilih ikon") diterima, jika `state == A00`, ViewModel memeriksa kembali `LX/01oH;->A00`. Karena `false`, sistem meluncurkan coroutine `LX/0Oqd` yang menampilkan **dialog paywall/upsell ("belum plus")**.
    3. Di `LX/06Pb;->A02`: jika benefit `false`, Instagram menyimpan `"has_custom_app_icon_benefit_" + userId = false` di preferensi `"aura_app_icon_benefit"` dan mereset icon kembali ke default (`MainTabActivity`).
  - Jika `A0D` mengembalikan `true`:
    1. Icon ditandai dengan state `LX/0008;->A01` / `A0C` (**UNLOCKED & SELECTED**).
    2. Tombol "Pilih ikon" langsung meluncurkan `LX/0Oqc` (case 42) yang memanggil `LX/06Pb;->A02` -> `LX/07qq;->A02`.
    3. Hook Piko pada `LX/07qq;->A02` seketika memicu `InstaAppIconManager.applyIcon`, mengaktifkan launcher alias yang dipilih tanpa jeda dan tanpa dialog pop-up paywall.
    4. `LX/06Pb;->A02` menyimpan status kepemilikan benefit aktif di shared preferences, memastikan icon tidak pernah di-reset oleh sistem bawaan.


/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.settings.BooleanSetting;
import app.morphe.extension.crimera.sharedPreference.SharedPref;
import app.morphe.extension.instagram.entity.UserData;
import app.morphe.extension.instagram.patches.download.DownloadUtils;
import app.morphe.extension.instagram.patches.userprofile.ProfileMoreOption;
import app.morphe.extension.instagram.patches.userprofile.ProfilePictureViewer;
import app.morphe.extension.instagram.patches.userprofile.UserProfileButton;
import app.morphe.extension.instagram.patches.video.PlaybackSpeedController;
import app.morphe.extension.instagram.settings.Settings;
import app.morphe.extension.instagram.settings.preference.fragments.FragmentHook;
import app.morphe.extension.instagram.theme.RhpatchTextColorManager;
import app.morphe.extension.instagram.utils.PikoLog;

public class RhpatchInstagramDialog {

    private static final String ACCENT_COLOR = "#E1306C"; // Neon Sunset Magenta
    private static final String CARD_BG_COLOR = "#181818";
    private static final String CARD_BORDER_COLOR = "#262626";

    private static class SettingItem {
        final String title;
        final String description;
        final BooleanSetting setting;

        SettingItem(String title, String description, BooleanSetting setting) {
            this.title = title;
            this.description = description;
            this.setting = setting;
        }
    }

    private static class CategoryGroup {
        final String name;
        final String summary;
        final SettingItem[] items;

        CategoryGroup(String name, String summary, SettingItem[] items) {
            this.name = name;
            this.summary = summary;
            this.items = items;
        }
    }

    private static final CategoryGroup[] CATEGORIES = new CategoryGroup[] {
        new CategoryGroup(
            "1. Privasi & Keamanan Akun",
            "Kunci App, Anti-Revoke DM, Anti-Screenshot, Stealth Mode",
            new SettingItem[] {
                new SettingItem(
                    "Kunci Aplikasi (Biometrik / PIN HP)",
                    "Kunci Instagram dengan autentikasi biometrik sidik jari atau PIN/pola perangkat saat aplikasi dibuka.",
                    Settings.APP_LOCK
                ),
                new SettingItem(
                    "Blokir Notifikasi Screenshot DM",
                    "Cegah Instagram memberi tahu lawan bicara saat Anda mengambil tangkapan layar di obrolan DM.",
                    Settings.DISABLE_SCREENSHOT_DETECTION
                ),
                new SettingItem(
                    "Anti-Hilang Media Sekali Lihat",
                    "Buka dan lihat foto serta video sekali lihat (view-once) berkali-kali tanpa batasan waktu.",
                    Settings.UNLIMITED_REPLAYS
                ),
                new SettingItem(
                    "Penyelamat Pesan DM yang Dihapus",
                    "Catat dan simpan otomatis pesan teks yang ditarik atau dihapus lawan bicara ke database lokal.",
                    Settings.SAVE_DELETED_MESSAGES
                ),
                new SettingItem(
                    "Lacak Pesan DM yang Diedit",
                    "Simpan riwayat teks asli pesan sebelum diedit oleh lawan bicara di ruang obrolan.",
                    Settings.SAVE_EDITED_MESSAGES
                ),
                new SettingItem(
                    "Mode Siluman Status Online (Ghost Presence)",
                    "Blokir pelaporan status online Anda ke Meta sambil tetap melihat status aktif pengguna lain.",
                    Settings.HIDE_ONLINE_STATUS
                ),
                new SettingItem(
                    "Sembunyikan Status Sedang Mengetik",
                    "Lawan bicara tidak akan melihat indikator sedang mengetik saat Anda menulis pesan di DM.",
                    Settings.DISABLE_TYPING_STATUS
                )
            }
        ),

        new CategoryGroup(
            "2. Mode Siluman (Ghost Mode)",
            "Lihat Cerita & Live Anonim, Tanda Dibaca Manual",
            new SettingItem[] {
                new SettingItem(
                    "Lihat Cerita Anonim (Ghost Stories)",
                    "Lihat cerita atau story akun lain tanpa nama Anda muncul di daftar pemirsa (viewers).",
                    Settings.VIEW_STORIES_ANONYMOUSLY
                ),
                new SettingItem(
                    "Tonton Siaran Langsung Anonim (Ghost Live)",
                    "Tonton siaran langsung (Live) tanpa nama akun Anda masuk ke daftar penonton.",
                    Settings.VIEW_LIVE_ANONYMOUSLY
                ),
                new SettingItem(
                    "Baca Pesan DM Anonim (Ghost DM)",
                    "Baca pesan direct message tanpa mengirim tanda centang terbaca (Seen) ke pengirim.",
                    Settings.VIEW_DM_ANONYMOUSLY
                ),
                new SettingItem(
                    "Tombol Tandai Pesan Dibaca Manual",
                    "Tampilkan tombol khusus di obrolan untuk menandai pesan terbaca hanya saat Anda inginkan.",
                    Settings.ENABLE_MARK_CHAT_AS_READ
                )
            }
        ),

        new CategoryGroup(
            "3. Iklan & Bebas Gangguan",
            "Blokir Iklan Feed, Hilangkan Konten Saran & Tray",
            new SettingItem[] {
                new SettingItem(
                    "Blokir Semua Iklan Feed & Cerita",
                    "Hilangkan seluruh iklan komersial, postingan sponsor, dan penawaran belanja di feed dan reels.",
                    Settings.DISABLE_ADS
                ),
                new SettingItem(
                    "Sembunyikan Konten yang Disarankan",
                    "Hilangkan postingan rekomendasi pihak ketiga dan akun saran dari beranda Anda.",
                    Settings.HIDE_SUGGESTED_CONTENT
                ),
                new SettingItem(
                    "Hilangkan Ruang Bawah Kosong",
                    "Rapikan celah ruang kosong di bilah navigasi bawah untuk tampilan layar yang lebih luas.",
                    Settings.REMOVE_EMPTY_BOTTOM_SPACE
                ),
                new SettingItem(
                    "Kunci Gulir Otomatis Reels",
                    "Cegah video reels beralih secara tidak sengaja saat sedang ditonton.",
                    Settings.DISABLE_REELS_SCROLLING
                ),
                new SettingItem(
                    "Sembunyikan Bilah Catatan (Notes Tray)",
                    "Hilangkan tray gelembung catatan (notes) di bagian atas halaman direct message.",
                    Settings.HIDE_NOTES_TRAY
                ),
                new SettingItem(
                    "Sembunyikan Bilah Cerita di Feed",
                    "Sembunyikan deretan lingkaran cerita (stories tray) di bagian atas beranda utama.",
                    Settings.HIDE_STORIES_TRAY
                ),
                new SettingItem(
                    "Nonaktifkan Geser Layar Buat Cerita",
                    "Cegah kamera postingan terbuka tanpa sengaja saat menggeser layar beranda ke kanan.",
                    Settings.DISABLE_SWIPE_TO_CREATE
                ),
                new SettingItem(
                    "Matikan Putar Otomatis Video Feed",
                    "Hemat kuota data dengan mematikan putar otomatis video saat menjelajahi feed.",
                    Settings.DISABLE_VIDEO_AUTOPLAY
                )
            }
        ),

        new CategoryGroup(
            "4. Pusat Unduhan (Downloader)",
            "Unduh Media Resolusi Asli & Folder Username",
            new SettingItem[] {
                new SettingItem(
                    "Aktifkan Pengunduh Media Resolusi Penuh",
                    "Unduh foto, video reels, story, dan carousel dalam kualitas asli resolusi maksimal.",
                    Settings.ENABLE_DOWNLOAD
                ),
                new SettingItem(
                    "Unduh Cepat Sekali Sentuh (Direct Download)",
                    "Mulai pengunduhan media secara instan tanpa dialog konfirmasi tambahan.",
                    Settings.ENABLE_DIRECT_DOWNLOAD
                ),
                new SettingItem(
                    "Kelompokkan ke Folder Username",
                    "Simpan file unduhan ke sub-folder tersendiri berdasarkan nama pengguna pembuat konten.",
                    Settings.DOWNLOAD_USERNAME_FOLDER
                )
            }
        ),

        new CategoryGroup(
            "5. Fitur Eksklusif IG Plus & Profil",
            "Follow-Back Badge, Opsi Postingan, Salin Komentar, Loop Story",
            new SettingItem[] {
                new SettingItem(
                    "Buka Seluruh Manfaat IG Plus",
                    "Buka fitur ganti icon aplikasi kustom, font cerita khusus, font bio, dan preview Plus.",
                    Settings.UNLOCK_PLUS_BENEFITS
                ),
                new SettingItem(
                    "Lencana Status Pertemanan (Follow-Back)",
                    "Tampilkan lencana visual penanda apakah akun yang Anda kunjungi mengikuti Anda kembali.",
                    Settings.FOLLOW_BACK_INDICATOR
                ),
                new SettingItem(
                    "Warna Khusus Indikator Pertemanan",
                    "Warnai tombol ikuti dengan indikator warna sesuai status hubungan pertemanan.",
                    Settings.FOLLOW_BACK_COLOR_INDICATOR
                ),
                new SettingItem(
                    "Menu Opsi Tambahan pada Postingan",
                    "Tambahkan tombol cepat kecepatan putar, unduh media, dan salin teks pada setiap postingan.",
                    Settings.ENABLE_MORE_OPTIONS_ON_POST
                ),
                new SettingItem(
                    "Tombol Salin Teks Komentar",
                    "Salin teks komentar pengguna lain langsung dengan sekali sentuh.",
                    Settings.COMMENT_COPY_BUTTON
                ),
                new SettingItem(
                    "Tombol Unduh Media di Komentar",
                    "Simpan stiker gambar dan media dari kolom komentar ke galeri perangkat Anda.",
                    Settings.COMMENT_SAVE_MEDIA_BUTTON
                ),
                new SettingItem(
                    "Tingkatkan Kualitas Penampil Foto",
                    "Buka penampil foto dengan resolusi tinggi tanpa penurunan kualitas kompresi.",
                    Settings.IMPROVE_IMAGE_VIEWING
                ),
                new SettingItem(
                    "Lihat Sebutan Tersembunyi di Cerita",
                    "Tampilkan nama pengguna yang dimention di cerita meskipun teksnya disembunyikan.",
                    Settings.VIEW_STORY_MENTIONS
                ),
                new SettingItem(
                    "Putar Ulang Cerita Tanpa Henti (Loop Story)",
                    "Ulangi pemutaran story secara otomatis tanpa langsung berpindah ke story berikutnya.",
                    Settings.LOOP_STORY
                )
            }
        ),

        new CategoryGroup(
            "6. Pengaturan Lanjutan & Developer",
            "MetaConfig, Employee Options, AMOLED Murni, Clean URL",
            new SettingItem[] {
                new SettingItem(
                    "Aktifkan Opsi Pengembang (Developer Options)",
                    "Buka menu pengaturan internal Meta dengan menekan lama ikon Beranda (Home).",
                    Settings.DEVELOPER_OPTIONS
                ),
                new SettingItem(
                    "Langsung Buka Penggantian MetaConfig",
                    "Tekan lama ikon Beranda langsung membuka layar MetaConfig Overrides internal.",
                    Settings.DIRECTLY_OPEN_METACONFIG
                ),
                new SettingItem(
                    "Aktifkan Opsi Karyawan Internal Meta",
                    "Buka seluruh fitur pengujian eksperimental internal pengembang Meta.",
                    Settings.ENABLE_EMP_OPTIONS
                ),
                new SettingItem(
                    "Izinkan Sertifikat Jaringan Pengguna (Whitehat)",
                    "Dukung penggunaan sertifikat CA kustom untuk inspeksi lalu lintas jaringan.",
                    Settings.ALLOW_USER_NETWORK_CERTIFICATE
                ),
                new SettingItem(
                    "Hilangkan Peringatan Kedaluwarsa Build",
                    "Blokir jendela popup peringatan versi lama yang muncul otomatis dari Meta.",
                    Settings.REMOVE_BUILD_EXPIRE_POPUP
                ),
                new SettingItem(
                    "Tema AMOLED Hitam Pekat Murni",
                    "Terapkan latar belakang hitam murni #000000 untuk penghematan baterai layar OLED.",
                    Settings.AMOLED_THEME
                ),
                new SettingItem(
                    "Buka Tautan di Browser Eksternal",
                    "Gunakan peramban bawaan ponsel (Chrome/Brave) alih-alih webview internal Instagram.",
                    Settings.OPEN_LINKS_EXTERNALLY
                ),
                new SettingItem(
                    "Bersihkan Pelacak Parameter URL Berbagi",
                    "Hapus parameter pelacak seperti igsh, utm_source, dan tracking token saat menyalin tautan.",
                    Settings.SANITIZE_SHARE_LINKS
                ),
                new SettingItem(
                    "Nonaktifkan Pelaporan Analitik Meta",
                    "Hentikan pengiriman log telemetri dan analitik pemakaian ke server Meta.",
                    Settings.DISABLE_ANALYTICS
                ),
                new SettingItem(
                    "Sembunyikan Rekomendasi Teman",
                    "Hilangkan bilah rekomendasi akun pengguna baru yang mungkin Anda kenal di profil.",
                    Settings.DISABLE_DISCOVER_PEOPLE
                ),
                new SettingItem(
                    "Pencatatan Log Diagnostik RHpatch",
                    "Simpan catatan diagnostik lengkap ke /sdcard/Download/Piko/piko_debug.log.",
                    Settings.PIKO_DEBUG
                )
            }
        )
    };

    /**
     * Menampilkan dialog utama RHpatch Instagram Studio yang rapi, terstruktur per kategori,
     * dan bersih tanpa emoji.
     */
    public static void show(final Context context) {
        if (context == null) return;

        try {
            final Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            final float density = dm.density;

            // Root Card Container (Deep Dark Black: #121212)
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable rootBg = new GradientDrawable();
            rootBg.setColor(Color.parseColor("#121212"));
            rootBg.setCornerRadius(22 * density);
            rootBg.setStroke((int) (1 * density), Color.parseColor("#282828"));
            root.setBackground(rootBg);

            int rootPadH = (int) (18 * density);
            int rootPadV = (int) (18 * density);
            root.setPadding(rootPadH, rootPadV, rootPadH, rootPadV);

            // ==========================================
            // 1. HEADER SECTION
            // ==========================================
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            // RHpatch Badge
            TextView badge = new TextView(context);
            badge.setText(" RHPATCH ");
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(11f);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.parseColor(ACCENT_COLOR));
            badgeBg.setCornerRadius(6 * density);
            badge.setBackground(badgeBg);
            int badgePadH = (int) (6 * density);
            int badgePadV = (int) (2 * density);
            badge.setPadding(badgePadH, badgePadV, badgePadH, badgePadV);
            header.addView(badge);

            // Title
            TextView title = new TextView(context);
            title.setText(" Instagram Studio");
            title.setTextColor(Color.WHITE);
            title.setTextSize(17f);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleLp.leftMargin = (int) (8 * density);
            title.setLayoutParams(titleLp);
            header.addView(title);

            // Close button (X)
            TextView closeBtn = new TextView(context);
            closeBtn.setText("X");
            closeBtn.setTextColor(Color.parseColor("#A7A7A7"));
            closeBtn.setTextSize(16f);
            closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
            closeBtn.setGravity(Gravity.CENTER);
            int xPad = (int) (6 * density);
            closeBtn.setPadding(xPad, xPad, xPad, xPad);
            closeBtn.setOnClickListener(v -> dialog.dismiss());
            header.addView(closeBtn);

            root.addView(header);

            // Subtitle
            TextView subTitle = new TextView(context);
            subTitle.setText("Pusat Kustomisasi & Fitur Eksklusif Instagram");
            subTitle.setTextColor(Color.parseColor("#8E8E93"));
            subTitle.setTextSize(12f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subLp.topMargin = (int) (4 * density);
            subLp.bottomMargin = (int) (14 * density);
            subTitle.setLayoutParams(subLp);
            root.addView(subTitle);

            // ==========================================
            // 2. SCROLLABLE CONTENT
            // ==========================================
            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            );
            scrollView.setLayoutParams(scrollLp);

            LinearLayout contentContainer = new LinearLayout(context);
            contentContainer.setOrientation(LinearLayout.VERTICAL);

            // ------------------------------------------
            // A. TARGET PROFILE QUICK ACTION CARD (if viewing a profile)
            // ------------------------------------------
            final UserData targetUser = UserProfileButton.getCurrentUserData();
            if (targetUser != null && targetUser.getUsername() != null && !targetUser.getUsername().isEmpty()) {
                LinearLayout profileCard = new LinearLayout(context);
                profileCard.setOrientation(LinearLayout.VERTICAL);
                GradientDrawable pBg = new GradientDrawable();
                pBg.setColor(Color.parseColor("#1B1220"));
                pBg.setCornerRadius(14 * density);
                pBg.setStroke((int) (1.2f * density), Color.parseColor("#E1306C"));
                profileCard.setBackground(pBg);
                int pPad = (int) (14 * density);
                profileCard.setPadding(pPad, pPad, pPad, pPad);

                LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                pLp.bottomMargin = (int) (14 * density);
                profileCard.setLayoutParams(pLp);

                TextView pTitle = new TextView(context);
                pTitle.setText("Aksi Profil @" + targetUser.getUsername());
                pTitle.setTextColor(Color.WHITE);
                pTitle.setTextSize(14f);
                pTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                profileCard.addView(pTitle);

                TextView pDesc = new TextView(context);
                pDesc.setText("Pilihan aksi eksklusif profil yang sedang Anda lihat:");
                pDesc.setTextColor(Color.parseColor("#B3B3B3"));
                pDesc.setTextSize(11f);
                LinearLayout.LayoutParams pDescLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                pDescLp.topMargin = (int) (2 * density);
                pDescLp.bottomMargin = (int) (10 * density);
                pDesc.setLayoutParams(pDescLp);
                profileCard.addView(pDesc);

                // Button: Buka Menu 9 Pilihan Lengkap
                TextView open9Btn = new TextView(context);
                open9Btn.setText("Buka Menu Pilihan Profil Lengkap (9 Opsi) ->");
                open9Btn.setTextColor(Color.WHITE);
                open9Btn.setTextSize(12.5f);
                open9Btn.setTypeface(Typeface.DEFAULT_BOLD);
                open9Btn.setGravity(Gravity.CENTER);
                GradientDrawable open9Bg = new GradientDrawable();
                open9Bg.setColor(Color.parseColor("#E1306C"));
                open9Bg.setCornerRadius(18 * density);
                open9Btn.setBackground(open9Bg);
                int bPad = (int) (9 * density);
                open9Btn.setPadding(bPad, bPad, bPad, bPad);
                open9Btn.setOnClickListener(v -> {
                    dialog.dismiss();
                    ProfileMoreOption.moreOptionsDailogueBox(context, targetUser);
                });
                profileCard.addView(open9Btn);

                // Quick Row: Lihat Foto Profil & Unduh Foto Profil
                LinearLayout pRow = new LinearLayout(context);
                pRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams pRowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                pRowLp.topMargin = (int) (8 * density);
                pRow.setLayoutParams(pRowLp);

                TextView viewDpBtn = new TextView(context);
                viewDpBtn.setText("Lihat Foto Profil");
                viewDpBtn.setTextColor(Color.WHITE);
                viewDpBtn.setTextSize(11.5f);
                viewDpBtn.setGravity(Gravity.CENTER);
                GradientDrawable btn1Bg = new GradientDrawable();
                btn1Bg.setColor(Color.parseColor("#262626"));
                btn1Bg.setCornerRadius(12 * density);
                viewDpBtn.setBackground(btn1Bg);
                viewDpBtn.setPadding(bPad, bPad, bPad, bPad);
                LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                b1Lp.rightMargin = (int) (4 * density);
                viewDpBtn.setLayoutParams(b1Lp);
                viewDpBtn.setOnClickListener(v -> {
                    ProfilePictureViewer.show(context, targetUser);
                });
                pRow.addView(viewDpBtn);

                TextView dlDpBtn = new TextView(context);
                dlDpBtn.setText("Unduh Foto Profil");
                dlDpBtn.setTextColor(Color.WHITE);
                dlDpBtn.setTextSize(11.5f);
                dlDpBtn.setGravity(Gravity.CENTER);
                GradientDrawable btn2Bg = new GradientDrawable();
                btn2Bg.setColor(Color.parseColor("#262626"));
                btn2Bg.setCornerRadius(12 * density);
                dlDpBtn.setBackground(btn2Bg);
                dlDpBtn.setPadding(bPad, bPad, bPad, bPad);
                LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                b2Lp.leftMargin = (int) (4 * density);
                dlDpBtn.setLayoutParams(b2Lp);
                dlDpBtn.setOnClickListener(v -> {
                    try {
                        String url = targetUser.getProfilePictureUrl();
                        String username = targetUser.getUsername();
                        String sub = DownloadUtils.getSubfolderName(username);
                        DownloadUtils.downloadMediaUrl(context, url, sub, username + "_dp.jpg");
                        Toast.makeText(context, "Mengunduh foto profil @" + username, Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        Toast.makeText(context, "Gagal mengunduh foto profil", Toast.LENGTH_SHORT).show();
                    }
                });
                pRow.addView(dlDpBtn);

                profileCard.addView(pRow);
                contentContainer.addView(profileCard);
            }

            // ------------------------------------------
            // B. STUDIO WARNA TEKS KUSTOM CARD
            // ------------------------------------------
            int tPad = (int) (14 * density);
            LinearLayout themeCard = new LinearLayout(context);
            themeCard.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable tBg = new GradientDrawable();
            tBg.setColor(Color.parseColor(CARD_BG_COLOR));
            tBg.setCornerRadius(14 * density);
            tBg.setStroke((int) (1 * density), Color.parseColor(CARD_BORDER_COLOR));
            themeCard.setBackground(tBg);
            themeCard.setPadding(tPad, tPad, tPad, tPad);

            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tLp.bottomMargin = (int) (14 * density);
            themeCard.setLayoutParams(tLp);

            // Theme Header + Switch
            LinearLayout themeHeader = new LinearLayout(context);
            themeHeader.setOrientation(LinearLayout.HORIZONTAL);
            themeHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView themeTitle = new TextView(context);
            themeTitle.setText("Studio Warna Teks Kustom");
            themeTitle.setTextColor(Color.WHITE);
            themeTitle.setTextSize(13.5f);
            themeTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams ttLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            themeTitle.setLayoutParams(ttLp);
            themeHeader.addView(themeTitle);

            Switch themeSwitch = createSwitch(context, RhpatchTextColorManager.isEnabled());
            themeHeader.addView(themeSwitch);
            themeCard.addView(themeHeader);

            TextView themeDesc = new TextView(context);
            themeDesc.setText("Ubah semua teks Instagram (feed, profil, pesan, komentar) secara dinamis.");
            themeDesc.setTextColor(Color.parseColor("#A7A7A7"));
            themeDesc.setTextSize(11f);
            LinearLayout.LayoutParams tdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tdLp.topMargin = (int) (3 * density);
            tdLp.bottomMargin = (int) (10 * density);
            themeDesc.setLayoutParams(tdLp);
            themeCard.addView(themeDesc);

            // Live Preview Box
            final TextView previewBox = new TextView(context);
            previewBox.setText("Contoh Teks: Instagram RHpatch Studio Theme");
            previewBox.setTextSize(13f);
            previewBox.setTypeface(Typeface.DEFAULT_BOLD);
            previewBox.setGravity(Gravity.CENTER);
            previewBox.setTextColor(RhpatchTextColorManager.getParsedColor());
            GradientDrawable previewBg = new GradientDrawable();
            previewBg.setColor(Color.parseColor("#0C0C0C"));
            previewBg.setCornerRadius(10 * density);
            previewBg.setStroke((int) (1 * density), Color.parseColor("#2D2D2D"));
            previewBox.setBackground(previewBg);
            int prevPad = (int) (10 * density);
            previewBox.setPadding(prevPad, prevPad, prevPad, prevPad);
            LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            prevLp.bottomMargin = (int) (10 * density);
            previewBox.setLayoutParams(prevLp);
            themeCard.addView(previewBox);

            // Horizontal Color Chips ScrollView
            HorizontalScrollView colorScrollView = new HorizontalScrollView(context);
            colorScrollView.setHorizontalScrollBarEnabled(false);
            LinearLayout colorRow = new LinearLayout(context);
            colorRow.setOrientation(LinearLayout.HORIZONTAL);

            for (int p = 0; p < RhpatchTextColorManager.PRESET_NAMES.length; p++) {
                final String pName = RhpatchTextColorManager.PRESET_NAMES[p];
                final String pHex = RhpatchTextColorManager.PRESET_COLORS[p];

                LinearLayout chip = new LinearLayout(context);
                chip.setOrientation(LinearLayout.HORIZONTAL);
                chip.setGravity(Gravity.CENTER_VERTICAL);
                GradientDrawable chipBg = new GradientDrawable();
                chipBg.setColor(Color.parseColor("#222222"));
                chipBg.setCornerRadius(14 * density);
                chipBg.setStroke((int) (1 * density), Color.parseColor(pHex));
                chip.setBackground(chipBg);
                int cPadH = (int) (10 * density);
                int cPadV = (int) (6 * density);
                chip.setPadding(cPadH, cPadV, cPadH, cPadV);

                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cLp.rightMargin = (int) (6 * density);
                chip.setLayoutParams(cLp);

                View dot = new View(context);
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(Color.parseColor(pHex));
                dot.setBackground(dotBg);
                int dotSize = (int) (10 * density);
                LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
                dotLp.rightMargin = (int) (6 * density);
                dot.setLayoutParams(dotLp);
                chip.addView(dot);

                TextView chipText = new TextView(context);
                chipText.setText(pName);
                chipText.setTextColor(Color.WHITE);
                chipText.setTextSize(11f);
                chipText.setTypeface(Typeface.DEFAULT_BOLD);
                chip.addView(chipText);

                chip.setOnClickListener(v -> {
                    RhpatchTextColorManager.setColorHex(pHex);
                    previewBox.setTextColor(Color.parseColor(pHex));
                    if (context instanceof Activity) {
                        RhpatchTextColorManager.refreshNow((Activity) context);
                    }
                    Toast.makeText(context, "Warna diubah: " + pName, Toast.LENGTH_SHORT).show();
                });

                colorRow.addView(chip);
            }

            // Custom Hex Button Chip
            TextView customHexBtn = new TextView(context);
            customHexBtn.setText("+ Custom Hex");
            customHexBtn.setTextColor(Color.parseColor(ACCENT_COLOR));
            customHexBtn.setTextSize(11f);
            customHexBtn.setTypeface(Typeface.DEFAULT_BOLD);
            customHexBtn.setGravity(Gravity.CENTER);
            GradientDrawable customBg = new GradientDrawable();
            customBg.setColor(Color.parseColor("#222222"));
            customBg.setCornerRadius(14 * density);
            customBg.setStroke((int) (1 * density), Color.parseColor(ACCENT_COLOR));
            customHexBtn.setBackground(customBg);
            int custPadH = (int) (12 * density);
            int custPadV = (int) (6 * density);
            customHexBtn.setPadding(custPadH, custPadV, custPadH, custPadV);
            customHexBtn.setOnClickListener(v -> {
                showCustomHexDialog(context, previewBox);
            });
            colorRow.addView(customHexBtn);

            colorScrollView.addView(colorRow);
            themeCard.addView(colorScrollView);

            themeSwitch.setOnCheckedChangeListener((btn, checked) -> {
                RhpatchTextColorManager.setEnabled(checked);
                if (context instanceof Activity) {
                    RhpatchTextColorManager.refreshNow((Activity) context);
                }
                Toast.makeText(context, checked ? "Warna teks kustom diaktifkan" : "Warna teks default dipulihkan", Toast.LENGTH_SHORT).show();
            });

            contentContainer.addView(themeCard);

            // ------------------------------------------
            // D. MENU KATEGORI PENGATURAN (Clean Categorized Navigation)
            // ------------------------------------------
            TextView catSectionHeader = new TextView(context);
            catSectionHeader.setText("KATEGORI PENGATURAN");
            catSectionHeader.setTextColor(Color.parseColor(ACCENT_COLOR));
            catSectionHeader.setTextSize(12f);
            catSectionHeader.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams cshLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cshLp.topMargin = (int) (6 * density);
            cshLp.bottomMargin = (int) (10 * density);
            catSectionHeader.setLayoutParams(cshLp);
            contentContainer.addView(catSectionHeader);

            // 6 Clean Category Navigation Cards
            for (final CategoryGroup group : CATEGORIES) {
                LinearLayout catCard = new LinearLayout(context);
                catCard.setOrientation(LinearLayout.HORIZONTAL);
                catCard.setGravity(Gravity.CENTER_VERTICAL);
                catCard.setClickable(true);
                catCard.setFocusable(true);

                GradientDrawable catCardBg = new GradientDrawable();
                catCardBg.setColor(Color.parseColor(CARD_BG_COLOR));
                catCardBg.setCornerRadius(14 * density);
                catCardBg.setStroke((int) (1 * density), Color.parseColor(CARD_BORDER_COLOR));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    catCard.setBackground(new RippleDrawable(
                        ColorStateList.valueOf(Color.parseColor("#33FFFFFF")),
                        catCardBg,
                        null
                    ));
                } else {
                    catCard.setBackground(catCardBg);
                }

                int cPadH = (int) (15 * density);
                int cPadV = (int) (14 * density);
                catCard.setPadding(cPadH, cPadV, cPadH, cPadV);

                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cLp.bottomMargin = (int) (8 * density);
                catCard.setLayoutParams(cLp);

                // Text Column
                LinearLayout catTextCol = new LinearLayout(context);
                catTextCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams catTextLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                catTextCol.setLayoutParams(catTextLp);

                TextView catName = new TextView(context);
                catName.setText(group.name);
                catName.setTextColor(Color.WHITE);
                catName.setTextSize(14f);
                catName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                catTextCol.addView(catName);

                TextView catSum = new TextView(context);
                catSum.setText(group.summary + " (" + group.items.length + " Opsi)");
                catSum.setTextColor(Color.parseColor("#A7A7A7"));
                catSum.setTextSize(11f);
                LinearLayout.LayoutParams cslp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cslp.topMargin = (int) (3 * density);
                catSum.setLayoutParams(cslp);
                catTextCol.addView(catSum);

                catCard.addView(catTextCol);

                // Arrow indicator ->
                TextView arrow = new TextView(context);
                arrow.setText("->");
                arrow.setTextColor(Color.parseColor(ACCENT_COLOR));
                arrow.setTextSize(14f);
                arrow.setTypeface(Typeface.DEFAULT_BOLD);
                catCard.addView(arrow);

                catCard.setOnClickListener(v -> {
                    showCategorySubDialog(context, group, density);
                });

                contentContainer.addView(catCard);
            }

            scrollView.addView(contentContainer);
            root.addView(scrollView);

            // ==========================================
            // 3. QUICK ACTION: MENU PENGATURAN LENGKAP
            // ==========================================
            TextView advancedBtn = new TextView(context);
            advancedBtn.setText("Buka Pengaturan Lanjutan (Developer & MobileConfig) ->");
            advancedBtn.setTextColor(Color.parseColor(ACCENT_COLOR));
            advancedBtn.setTextSize(11.5f);
            advancedBtn.setTypeface(Typeface.DEFAULT_BOLD);
            advancedBtn.setGravity(Gravity.CENTER);
            int advPad = (int) (8 * density);
            advancedBtn.setPadding(advPad, advPad, advPad, advPad);
            advancedBtn.setOnClickListener(v -> {
                dialog.dismiss();
                FragmentHook.startSettings();
            });
            root.addView(advancedBtn);

            // ==========================================
            // 4. ACTION BUTTON: TUTUP & TERAPKAN
            // ==========================================
            TextView applyBtn = new TextView(context);
            applyBtn.setText("Tutup & Terapkan");
            applyBtn.setTextColor(Color.WHITE);
            applyBtn.setTextSize(14f);
            applyBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            applyBtn.setGravity(Gravity.CENTER);

            GradientDrawable applyBg = new GradientDrawable();
            applyBg.setColor(Color.parseColor(ACCENT_COLOR));
            applyBg.setCornerRadius(24 * density);
            applyBtn.setBackground(applyBg);

            int btnPadV = (int) (13 * density);
            applyBtn.setPadding(0, btnPadV, 0, btnPadV);

            LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            applyLp.topMargin = (int) (6 * density);
            applyBtn.setLayoutParams(applyLp);

            applyBtn.setOnClickListener(v -> dialog.dismiss());
            root.addView(applyBtn);

            // ==========================================
            // 5. RESET KE DEFAULT
            // ==========================================
            TextView resetBtn = new TextView(context);
            resetBtn.setText("Reset Semua Pengaturan ke Default");
            resetBtn.setTextColor(Color.parseColor("#777777"));
            resetBtn.setTextSize(11f);
            resetBtn.setGravity(Gravity.CENTER);
            int resetPad = (int) (6 * density);
            resetBtn.setPadding(resetPad, resetPad, resetPad, resetPad);
            resetBtn.setOnClickListener(v -> {
                for (CategoryGroup g : CATEGORIES) {
                    for (SettingItem s : g.items) {
                        SharedPref.setBooleanPref(s.setting.key, s.setting.defaultValue);
                    }
                }
                RhpatchTextColorManager.setEnabled(false);
                RhpatchTextColorManager.setColorHex(RhpatchTextColorManager.DEFAULT_COLOR_HEX);
                PlaybackSpeedController.setSpeedLockEnabled(false);
                PlaybackSpeedController.applySpeed(1.0f);
                Toast.makeText(context, "Pengaturan RHpatch direset ke default!", Toast.LENGTH_SHORT).show();
            });
            root.addView(resetBtn);

            // Setup Dialog Window Properties
            dialog.setContentView(root);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wlp = window.getAttributes();
                int screenW = dm.widthPixels;
                int screenH = dm.heightPixels;

                int targetW = Math.min((int) (screenW * 0.94f), (int) (480 * density));
                int targetH = (int) (screenH * 0.88f);

                wlp.width = targetW;
                wlp.height = targetH;
                wlp.gravity = Gravity.CENTER;
                window.setAttributes(wlp);
            }

            dialog.show();
        } catch (Throwable t) {
            PikoUtils.logger("RhpatchInstagramDialog", "Gagal menampilkan dialog pengaturan: " + t.getMessage(), t);
        }
    }

    /**
     * Menampilkan sub-dialog untuk kategori tertentu dengan tampilan kartu yang bersih dan fokus.
     */
    private static void showCategorySubDialog(final Context context, final CategoryGroup group, final float density) {
        if (context == null || group == null) return;

        try {
            final Dialog subDialog = new Dialog(context);
            subDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable rootBg = new GradientDrawable();
            rootBg.setColor(Color.parseColor("#121212"));
            rootBg.setCornerRadius(22 * density);
            rootBg.setStroke((int) (1 * density), Color.parseColor("#282828"));
            root.setBackground(rootBg);

            int rootPadH = (int) (18 * density);
            int rootPadV = (int) (18 * density);
            root.setPadding(rootPadH, rootPadV, rootPadH, rootPadV);

            // Header: Back Button + Category Title
            LinearLayout subHeader = new LinearLayout(context);
            subHeader.setOrientation(LinearLayout.HORIZONTAL);
            subHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView backBtn = new TextView(context);
            backBtn.setText("<- Kembali");
            backBtn.setTextColor(Color.parseColor(ACCENT_COLOR));
            backBtn.setTextSize(13f);
            backBtn.setTypeface(Typeface.DEFAULT_BOLD);
            int bPad = (int) (6 * density);
            backBtn.setPadding(0, bPad, (int) (10 * density), bPad);
            backBtn.setOnClickListener(v -> subDialog.dismiss());
            subHeader.addView(backBtn);

            TextView subTitle = new TextView(context);
            subTitle.setText(group.name);
            subTitle.setTextColor(Color.WHITE);
            subTitle.setTextSize(15f);
            subTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            subTitle.setLayoutParams(stLp);
            subHeader.addView(subTitle);

            TextView closeBtn = new TextView(context);
            closeBtn.setText("X");
            closeBtn.setTextColor(Color.parseColor("#A7A7A7"));
            closeBtn.setTextSize(16f);
            closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
            closeBtn.setGravity(Gravity.CENTER);
            closeBtn.setPadding(bPad, bPad, bPad, bPad);
            closeBtn.setOnClickListener(v -> subDialog.dismiss());
            subHeader.addView(closeBtn);

            root.addView(subHeader);

            // Summary text
            TextView summaryView = new TextView(context);
            summaryView.setText(group.summary);
            summaryView.setTextColor(Color.parseColor("#8E8E93"));
            summaryView.setTextSize(11.5f);
            LinearLayout.LayoutParams sumLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            sumLp.topMargin = (int) (4 * density);
            sumLp.bottomMargin = (int) (14 * density);
            summaryView.setLayoutParams(sumLp);
            root.addView(summaryView);

            // ScrollView for category items
            ScrollView subScroll = new ScrollView(context);
            subScroll.setVerticalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            );
            subScroll.setLayoutParams(scrollLp);

            LinearLayout itemsContainer = new LinearLayout(context);
            itemsContainer.setOrientation(LinearLayout.VERTICAL);

            for (final SettingItem item : group.items) {
                final BooleanSetting setting = item.setting;
                boolean isEnabled = SharedPref.getBooleanPref(setting);

                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setClickable(true);
                card.setFocusable(true);

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(Color.parseColor(CARD_BG_COLOR));
                cardBg.setCornerRadius(14 * density);
                cardBg.setStroke((int) (1 * density), Color.parseColor(CARD_BORDER_COLOR));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    card.setBackground(new RippleDrawable(
                        ColorStateList.valueOf(Color.parseColor("#33FFFFFF")),
                        cardBg,
                        null
                    ));
                } else {
                    card.setBackground(cardBg);
                }

                int cardPadH = (int) (14 * density);
                int cardPadV = (int) (12 * density);
                card.setPadding(cardPadH, cardPadV, cardPadH, cardPadV);

                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardLp.bottomMargin = (int) (8 * density);
                card.setLayoutParams(cardLp);

                // Text Column
                LinearLayout textCol = new LinearLayout(context);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                textCol.setLayoutParams(textLp);

                TextView itemTitle = new TextView(context);
                itemTitle.setText(item.title);
                itemTitle.setTextColor(Color.WHITE);
                itemTitle.setTextSize(13.5f);
                itemTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                textCol.addView(itemTitle);

                TextView itemDesc = new TextView(context);
                itemDesc.setText(item.description);
                itemDesc.setTextColor(Color.parseColor("#A7A7A7"));
                itemDesc.setTextSize(11f);
                LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                descLp.topMargin = (int) (2 * density);
                itemDesc.setLayoutParams(descLp);
                textCol.addView(itemDesc);

                card.addView(textCol);

                // Switch
                final Switch toggle = createSwitch(context, isEnabled);
                card.addView(toggle);

                toggle.setOnCheckedChangeListener((btn, checked) -> {
                    SharedPref.setBooleanPref(setting.key, checked);
                });

                card.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));

                itemsContainer.addView(card);
            }

            subScroll.addView(itemsContainer);
            root.addView(subScroll);

            // Bottom Back Button
            TextView doneBtn = new TextView(context);
            doneBtn.setText("Simpan & Kembali");
            doneBtn.setTextColor(Color.WHITE);
            doneBtn.setTextSize(13.5f);
            doneBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            doneBtn.setGravity(Gravity.CENTER);

            GradientDrawable doneBg = new GradientDrawable();
            doneBg.setColor(Color.parseColor(ACCENT_COLOR));
            doneBg.setCornerRadius(24 * density);
            doneBtn.setBackground(doneBg);

            int donePadV = (int) (12 * density);
            doneBtn.setPadding(0, donePadV, 0, donePadV);

            LinearLayout.LayoutParams doneLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            doneLp.topMargin = (int) (10 * density);
            doneBtn.setLayoutParams(doneLp);
            doneBtn.setOnClickListener(v -> subDialog.dismiss());
            root.addView(doneBtn);

            subDialog.setContentView(root);
            Window window = subDialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wlp = window.getAttributes();
                int screenW = dm.widthPixels;
                int screenH = dm.heightPixels;

                int targetW = Math.min((int) (screenW * 0.94f), (int) (480 * density));
                int targetH = (int) (screenH * 0.88f);

                wlp.width = targetW;
                wlp.height = targetH;
                wlp.gravity = Gravity.CENTER;
                window.setAttributes(wlp);
            }

            subDialog.show();
        } catch (Throwable t) {
            PikoLog.e("RhpatchInstagramDialog", "Gagal menampilkan sub-dialog kategori: " + t.getMessage(), t);
        }
    }

    private static Switch createSwitch(Context context, boolean checked) {
        Switch toggle = new Switch(context);
        toggle.setChecked(checked);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
            };
            int[] thumbColors = new int[] {
                Color.parseColor(ACCENT_COLOR),
                Color.parseColor("#B3B3B3")
            };
            int[] trackColors = new int[] {
                Color.parseColor("#66E1306C"),
                Color.parseColor("#3E3E3E")
            };
            toggle.setThumbTintList(new ColorStateList(states, thumbColors));
            toggle.setTrackTintList(new ColorStateList(states, trackColors));
        }

        return toggle;
    }


    private static void showCustomHexDialog(final Context context, final TextView previewBox) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            builder.setTitle("Input Warna Hex Kustom");

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("#FFD700 atau FFD700");
            input.setText(RhpatchTextColorManager.getColorHex());
            input.setTextColor(Color.WHITE);
            builder.setView(input);

            builder.setPositiveButton("Terapkan", (d, which) -> {
                String hex = input.getText().toString().trim();
                if (!hex.startsWith("#")) {
                    hex = "#" + hex;
                }
                try {
                    int parsed = Color.parseColor(hex);
                    RhpatchTextColorManager.setColorHex(hex);
                    if (previewBox != null) {
                        previewBox.setTextColor(parsed);
                    }
                    if (context instanceof Activity) {
                        RhpatchTextColorManager.refreshNow((Activity) context);
                    }
                    Toast.makeText(context, "Warna kustom diterapkan: " + hex, Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(context, "Format hex tidak valid! Contoh: #FFD700", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Batal", (d, which) -> d.cancel());
            builder.show();
        } catch (Throwable t) {
            PikoLog.e("RhpatchInstagramDialog", "Error showing custom hex dialog", t);
        }
    }
}

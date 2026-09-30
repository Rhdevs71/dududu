/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
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
import app.morphe.extension.facebook.patches.FacebookMediaDownloader;
import app.morphe.extension.facebook.patches.FacebookPostMenuHook;
import app.morphe.extension.facebook.settings.FacebookSettings;

public class RhpatchFacebookDialog {

    private static final String ACCENT_COLOR = "#1877F2"; // Facebook Royal Blue
    private static final String CARD_BG_COLOR = "#181818"; // Dark charcoal 
    private static final String CARD_BORDER_COLOR = "#2A2A2A";

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
        final String id;
        final String name;
        final String summary;
        final SettingItem[] items;

        CategoryGroup(String id, String name, String summary, SettingItem[] items) {
            this.id = id;
            this.name = name;
            this.summary = summary;
            this.items = items;
        }
    }

    private static final CategoryGroup[] CATEGORIES = new CategoryGroup[] {
        new CategoryGroup(
            "privacy",
            "🛡️ Privasi & Ghost",
            "Ghost Story, Ghost Messenger, Ghost Typing, App Lock",
            new SettingItem[] {
                new SettingItem(
                    "Ghost Story Viewer (Nonton Cerita Senyap)",
                    "Lihat semua Story Facebook teman tanpa nama Anda tercatat di daftar pemirsa cerita mereka.",
                    FacebookSettings.GHOST_STORY
                ),
                new SettingItem(
                    "Ghost Read Receipt (Messenger)",
                    "Buka dan baca pesan obrolan tanpa memicu centang biru / bulatan foto terbaca ke lawan bicara.",
                    FacebookSettings.GHOST_READ_RECEIPT
                ),
                new SettingItem(
                    "Sembunyikan Status 'Sedang Mengetik...'",
                    "Lawan bicara tidak akan melihat indikator saat Anda sedang mengetik pesan di Messenger.",
                    FacebookSettings.GHOST_TYPING
                ),
                new SettingItem(
                    "Kunci Aplikasi (Biometrik / PIN HP)",
                    "Kunci Facebook dengan autentikasi sidik jari atau PIN/pola perangkat saat aplikasi dibuka.",
                    FacebookSettings.APP_LOCK
                ),
                new SettingItem(
                    "Anti-Screenshot Obrolan Rahasia",
                    "Cegah deteksi screenshot pada percakapan rahasia (Secret Conversations) Messenger.",
                    FacebookSettings.ANTI_SCREENSHOT
                ),
            }
        ),

        new CategoryGroup(
            "downloader",
            "📥 Downloader",
            "Unduh Reels, Video Feed, Watch, dan Story Kualitas HD",
            new SettingItem[] {
                new SettingItem(
                    "Reels Downloader",
                    "Tambahkan opsi tombol unduh langsung di menu postingan Facebook Reels.",
                    FacebookSettings.DOWNLOAD_REELS
                ),
                new SettingItem(
                    "Video Feed & Watch Downloader",
                    "Unduh video beranda dan Facebook Watch dalam resolusi tertinggi (1080p/720p MP4).",
                    FacebookSettings.DOWNLOAD_VIDEO
                ),
                new SettingItem(
                    "Story Media Downloader",
                    "Simpan foto dan video cerita teman langsung ke galeri perangkat Anda.",
                    FacebookSettings.DOWNLOAD_STORY
                ),
                new SettingItem(
                    "Foto Profil & Sampul HD",
                    "Buka dan simpan foto profil atau foto sampul dalam resolusi penuh asli CDN Graph.",
                    FacebookSettings.DOWNLOAD_PHOTO
                ),
            }
        ),

        new CategoryGroup(
            "adblock",
            "🚫 Bebas Iklan",
            "Blokir Postingan Bersponsor dan Iklan Video",
            new SettingItem[] {
                new SettingItem(
                    "Blokir Postingan Bersponsor (Sponsored Ads)",
                    "Sembunyikan postingan iklan bersponsor dari linimasa beranda Facebook Anda.",
                    FacebookSettings.BLOCK_SPONSORED_ADS
                ),
                new SettingItem(
                    "Lewati Iklan Video (In-Stream Ads)",
                    "Putar video Watch dan Reels tanpa terputus oleh jeda iklan sponsor di tengah video.",
                    FacebookSettings.BLOCK_INSTREAM_ADS
                ),
                new SettingItem(
                    "Sembunyikan 'Orang yang Mungkin Anda Kenal'",
                    "Bersihkan beranda dari rekomendasi pertemanan acak (People You May Know).",
                    FacebookSettings.HIDE_PYMK
                ),
                new SettingItem(
                    "Sembunyikan Carousel Reels di Beranda",
                    "Sembunyikan baris video Reels di linimasa beranda bagi yang ingin fokus hanya pada postingan teks & foto.",
                    FacebookSettings.HIDE_REELS_IN_FEED
                ),
            }
        ),

        new CategoryGroup(
            "playback",
            "⚡ Video & Audio",
            "Kontrol Kecepatan Putar dan Pemutaran Latar Belakang",
            new SettingItem[] {
                new SettingItem(
                    "Pengatur Kecepatan Putar Video",
                    "Pilihan kecepatan putar video (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x) pada video Facebook.",
                    FacebookSettings.PLAYBACK_SPEED_CONTROLLER
                ),
                new SettingItem(
                    "Putar Video di Latar Belakang (Background Play)",
                    "Tetap dengarkan audio video Facebook saat aplikasi diminimalkan atau layar mati.",
                    FacebookSettings.BACKGROUND_AUDIO_PLAY
                ),
                new SettingItem(
                    "Mute Video Bawaan Saat Scroll",
                    "Putar video secara senyap tanpa suara saat pertama kali muncul di linimasa.",
                    FacebookSettings.DEFAULT_MUTE_VIDEO
                ),
            }
        ),

        new CategoryGroup(
            "ui",
            "🎨 Kustomisasi",
            "Tema Amoled Black dan Tombol Mengambang",
            new SettingItem[] {
                new SettingItem(
                    "Dark Mode Hitam Pekat (Amoled Black)",
                    "Ubah warna latar belakang tema gelap abu-abu Facebook menjadi hitam pekat #000000 murni.",
                    FacebookSettings.AMOLED_DARK_MODE
                ),
                new SettingItem(
                    "Tampilkan Tombol Mengambang [RHpatch]",
                    "Tampilkan tombol mengambang [RHpatch] di layar untuk membuka menu mod ini.",
                    FacebookSettings.SHOW_PROFILE_CAPSULE
                ),
            }
        ),

        new CategoryGroup(
            "utils",
            "🔗 Utilitas & Bypass",
            "Bypass Link Shim, Browser Eksternal, Text Selection",
            new SettingItem[] {
                new SettingItem(
                    "Bypass Pelacakan Link Shim Facebook",
                    "Hapus parameter pelacak 'l.facebook.com' dan 'fbclid' saat membuka tautan.",
                    FacebookSettings.BYPASS_LINK_SHIM
                ),
                new SettingItem(
                    "Buka Tautan Langsung di Browser Eksternal",
                    "Lewati in-app browser bawaan Facebook dan buka langsung tautan ke browser pilihan Anda (Chrome/Firefox/Brave).",
                    FacebookSettings.OPEN_EXTERNAL_BROWSER
                ),
                new SettingItem(
                    "Izinkan Salin Teks di Semua Bagian",
                    "Bebaskan pemilihan teks (text selection) pada postingan, komentar, dan status.",
                    FacebookSettings.ENABLE_TEXT_SELECTION
                ),
            }
        ),
    };

    private static int sSelectedCategoryIndex = 0;

    public static void show(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        try {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            final float density = dm.density;
            int screenWidth = dm.widthPixels;
            int screenHeight = dm.heightPixels;

            // Root Container
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(Color.parseColor("#0F172A")); // Deep Slate / Obsidian Dark
            root.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));

            GradientDrawable rootBg = new GradientDrawable();
            rootBg.setColor(Color.parseColor("#0F172A"));
            rootBg.setCornerRadius(20 * density);
            rootBg.setStroke((int) (1.5f * density), Color.parseColor("#1E293B"));
            root.setBackground(rootBg);

            // Header: Title & Close Button
            LinearLayout header = new LinearLayout(activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, 0, 0, (int) (10 * density));

            LinearLayout titleCol = new LinearLayout(activity);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleCol.setLayoutParams(titleColLp);

            TextView titleTv = new TextView(activity);
            titleTv.setText("📘 Facebook Mod Suite");
            titleTv.setTextColor(Color.WHITE);
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            titleTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            titleCol.addView(titleTv);

            TextView subtitleTv = new TextView(activity);
            subtitleTv.setText("RHpatch • Pengaturan Mod & Privasi Akun");
            subtitleTv.setTextColor(Color.parseColor("#94A3B8")); // Slate 400
            subtitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            titleCol.addView(subtitleTv);

            header.addView(titleCol);

            TextView closeBtn = new TextView(activity);
            closeBtn.setText("✕");
            closeBtn.setTextColor(Color.parseColor("#EF4444")); // Red-500
            closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
            closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
            closeBtn.setPadding((int) (8 * density), (int) (4 * density), (int) (8 * density), (int) (4 * density));
            closeBtn.setOnClickListener(v -> dialog.dismiss());
            header.addView(closeBtn);

            root.addView(header);

            // Divider
            View divider = new View(activity);
            divider.setBackgroundColor(Color.parseColor("#1E293B"));
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density));
            divLp.bottomMargin = (int) (10 * density);
            divider.setLayoutParams(divLp);
            root.addView(divider);

            // Horizontal Category Tab Bar
            HorizontalScrollView categoryScroll = new HorizontalScrollView(activity);
            categoryScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout.LayoutParams catScrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            catScrollLp.bottomMargin = (int) (12 * density);
            categoryScroll.setLayoutParams(catScrollLp);

            final LinearLayout tabContainer = new LinearLayout(activity);
            tabContainer.setOrientation(LinearLayout.HORIZONTAL);
            categoryScroll.addView(tabContainer);
            root.addView(categoryScroll);

            // Vertical Content Area
            ScrollView contentScroll = new ScrollView(activity);
            contentScroll.setVerticalScrollBarEnabled(true);
            LinearLayout.LayoutParams contentScrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            );
            contentScroll.setLayoutParams(contentScrollLp);

            final LinearLayout itemsContainer = new LinearLayout(activity);
            itemsContainer.setOrientation(LinearLayout.VERTICAL);
            contentScroll.addView(itemsContainer);
            root.addView(contentScroll);

            // Render categories tabs & items
            final List<TextView> tabViews = new ArrayList<>();
            final Runnable renderItemsRunnable = () -> {
                itemsContainer.removeAllViews();
                CategoryGroup group = CATEGORIES[sSelectedCategoryIndex];

                // Category Summary Banner
                TextView banner = new TextView(activity);
                banner.setText("📌 " + group.summary);
                banner.setTextColor(Color.parseColor("#38BDF8")); // Cyan
                banner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                banner.setTypeface(Typeface.DEFAULT_BOLD);
                banner.setPadding((int) (10 * density), (int) (6 * density), (int) (10 * density), (int) (6 * density));
                GradientDrawable bannerBg = new GradientDrawable();
                bannerBg.setColor(Color.parseColor("#140284C7"));
                bannerBg.setCornerRadius(8 * density);
                bannerBg.setStroke((int) (1 * density), Color.parseColor("#330284C7"));
                banner.setBackground(bannerBg);
                LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bannerLp.bottomMargin = (int) (10 * density);
                banner.setLayoutParams(bannerLp);
                itemsContainer.addView(banner);

                // Khusus kategori Downloader: Pasang Interactive Downloader Card!
                if ("downloader".equals(group.id)) {
                    LinearLayout dlCard = new LinearLayout(activity);
                    dlCard.setOrientation(LinearLayout.VERTICAL);
                    dlCard.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));

                    GradientDrawable dlCardBg = new GradientDrawable();
                    dlCardBg.setColor(Color.parseColor("#1E293B"));
                    dlCardBg.setCornerRadius(12 * density);
                    dlCardBg.setStroke((int) (1.5f * density), Color.parseColor(ACCENT_COLOR));
                    dlCard.setBackground(dlCardBg);

                    LinearLayout.LayoutParams dlCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    dlCardLp.bottomMargin = (int) (14 * density);
                    dlCard.setLayoutParams(dlCardLp);

                    TextView dlCardTitle = new TextView(activity);
                    dlCardTitle.setText("⚡ Pusat Unduh Video & Reels (HD)");
                    dlCardTitle.setTextColor(Color.WHITE);
                    dlCardTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                    dlCardTitle.setTypeface(Typeface.DEFAULT_BOLD);
                    dlCard.addView(dlCardTitle);

                    TextView dlCardSub = new TextView(activity);
                    dlCardSub.setText("Tempel tautan video/reels publik Facebook untuk mengunduh langsung format MP4 HD ke galeri.");
                    dlCardSub.setTextColor(Color.parseColor("#94A3B8"));
                    dlCardSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                    dlCardSub.setPadding(0, (int) (2 * density), 0, (int) (8 * density));
                    dlCard.addView(dlCardSub);

                    final EditText urlInput = new EditText(activity);
                    urlInput.setHint("https://www.facebook.com/reel/...");
                    urlInput.setHintTextColor(Color.parseColor("#64748B"));
                    urlInput.setTextColor(Color.WHITE);
                    urlInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                    urlInput.setSingleLine(true);
                    urlInput.setPadding((int) (10 * density), (int) (8 * density), (int) (10 * density), (int) (8 * density));

                    GradientDrawable inputBg = new GradientDrawable();
                    inputBg.setColor(Color.parseColor("#0F172A"));
                    inputBg.setCornerRadius(8 * density);
                    inputBg.setStroke((int) (1 * density), Color.parseColor("#334155"));
                    urlInput.setBackground(inputBg);

                    // Auto-fill jika ada link Facebook di papan klip
                    String clipUrl = FacebookMediaDownloader.getClipboardFacebookUrl(activity);
                    if (clipUrl != null) {
                        urlInput.setText(clipUrl);
                    }
                    dlCard.addView(urlInput);

                    // Action buttons row
                    LinearLayout btnRow = new LinearLayout(activity);
                    btnRow.setOrientation(LinearLayout.HORIZONTAL);
                    btnRow.setPadding(0, (int) (8 * density), 0, 0);

                    // Tombol Tempel
                    Button pasteBtn = new Button(activity);
                    pasteBtn.setText("📋 Tempel");
                    pasteBtn.setTextColor(Color.parseColor("#38BDF8"));
                    pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                    pasteBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    GradientDrawable pasteBg = new GradientDrawable();
                    pasteBg.setColor(Color.parseColor("#140284C7"));
                    pasteBg.setCornerRadius(8 * density);
                    pasteBtn.setBackground(pasteBg);
                    LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(0, (int) (38 * density), 1.0f);
                    pasteLp.rightMargin = (int) (6 * density);
                    pasteBtn.setLayoutParams(pasteLp);
                    pasteBtn.setOnClickListener(v -> {
                        String clip = FacebookMediaDownloader.getClipboardFacebookUrl(activity);
                        if (clip != null) {
                            urlInput.setText(clip);
                            Toast.makeText(activity, "Tautan berhasil ditempel!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "Tidak ada tautan Facebook di papan klip.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    btnRow.addView(pasteBtn);

                    // Tombol Unduh HD
                    Button dlBtn = new Button(activity);
                    dlBtn.setText("📥 Unduh Video HD");
                    dlBtn.setTextColor(Color.WHITE);
                    dlBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                    dlBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    GradientDrawable dlBtnBg = new GradientDrawable();
                    dlBtnBg.setColor(Color.parseColor(ACCENT_COLOR));
                    dlBtnBg.setCornerRadius(8 * density);
                    dlBtn.setBackground(dlBtnBg);
                    LinearLayout.LayoutParams dlLp = new LinearLayout.LayoutParams(0, (int) (38 * density), 1.6f);
                    dlBtn.setLayoutParams(dlLp);
                    dlBtn.setOnClickListener(v -> {
                        String targetUrl = urlInput.getText().toString().trim();
                        if (targetUrl.isEmpty()) {
                            Toast.makeText(activity, "Silakan masukkan atau tempel link video terlebih dahulu!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FacebookMediaDownloader.startDownload(activity, targetUrl);
                    });
                    btnRow.addView(dlBtn);

                    dlCard.addView(btnRow);

                    // 2. Tombol Unduh Foto Layar Penuh (Profil / Sampul / Story)
                    Button screenPhotoBtn = new Button(activity);
                    screenPhotoBtn.setText("🖼️ Simpan Foto Layar Ini (Profil / Sampul / Story HD)");
                    screenPhotoBtn.setTextColor(Color.WHITE);
                    screenPhotoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                    screenPhotoBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    GradientDrawable photoBg = new GradientDrawable();
                    photoBg.setColor(Color.parseColor("#4C1D95")); // Deep Purple
                    photoBg.setCornerRadius(8 * density);
                    photoBg.setStroke((int) (1 * density), Color.parseColor("#8B5CF6"));
                    screenPhotoBtn.setBackground(photoBg);
                    LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (38 * density));
                    photoLp.topMargin = (int) (8 * density);
                    screenPhotoBtn.setLayoutParams(photoLp);
                    screenPhotoBtn.setOnClickListener(v -> {
                        FacebookMediaDownloader.downloadActiveScreenPhoto(activity);
                    });
                    dlCard.addView(screenPhotoBtn);

                    // 3. Tombol Media Terakhir Terdeteksi (jika ada post yang diklik titik 3)
                    if (FacebookMediaDownloader.sLastActiveMedia != null) {
                        final FacebookPostMenuHook.PostMediaInfo info = FacebookMediaDownloader.sLastActiveMedia;
                        final String vUrl = (info.hdVideoUrl != null) ? info.hdVideoUrl : info.sdVideoUrl;
                        
                        if (vUrl != null || info.imageUrl != null) {
                            TextView lastHeader = new TextView(activity);
                            lastHeader.setText("🎬 Media Postingan Terakhir Terdeteksi:");
                            lastHeader.setTextColor(Color.parseColor("#38BDF8"));
                            lastHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                            lastHeader.setTypeface(Typeface.DEFAULT_BOLD);
                            lastHeader.setPadding(0, (int) (10 * density), 0, (int) (4 * density));
                            dlCard.addView(lastHeader);

                            if (vUrl != null) {
                                Button vBtn = new Button(activity);
                                vBtn.setText("⚡ Unduh Video HD Terdeteksi (.mp4)");
                                vBtn.setTextColor(Color.WHITE);
                                vBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                                vBtn.setTypeface(Typeface.DEFAULT_BOLD);
                                GradientDrawable vBg = new GradientDrawable();
                                vBg.setColor(Color.parseColor("#047857")); // Emerald
                                vBg.setCornerRadius(8 * density);
                                vBtn.setBackground(vBg);
                                LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (36 * density));
                                vLp.bottomMargin = (int) (4 * density);
                                vBtn.setLayoutParams(vLp);
                                vBtn.setOnClickListener(v -> {
                                    FacebookMediaDownloader.downloadDirectStream(activity, vUrl, "Facebook_Detected_Video", true);
                                });
                                dlCard.addView(vBtn);
                            }

                            if (info.imageUrl != null) {
                                Button pBtn = new Button(activity);
                                pBtn.setText("⚡ Unduh Foto HD Terdeteksi (.jpg)");
                                pBtn.setTextColor(Color.WHITE);
                                pBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                                pBtn.setTypeface(Typeface.DEFAULT_BOLD);
                                GradientDrawable pBg = new GradientDrawable();
                                pBg.setColor(Color.parseColor("#6D28D9"));
                                pBg.setCornerRadius(8 * density);
                                pBtn.setBackground(pBg);
                                LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (36 * density));
                                pBtn.setLayoutParams(pLp);
                                pBtn.setOnClickListener(v -> {
                                    FacebookMediaDownloader.downloadDirectStream(activity, info.imageUrl, "Facebook_Detected_Photo", false);
                                });
                                dlCard.addView(pBtn);
                            }
                        }
                    }

                    // 4. Banner petunjuk menu titik 3
                    TextView guideText = new TextView(activity);
                    guideText.setText("💡 CARA TERCEPAT: Anda juga bisa mengunduh langsung dari postingan atau Reels mana pun dengan menekan menu titik 3 (...) di sudut kanan atas postingan!");
                    guideText.setTextColor(Color.parseColor("#F59E0B")); // Amber
                    guideText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                    guideText.setPadding((int) (4 * density), (int) (8 * density), (int) (4 * density), (int) (2 * density));
                    dlCard.addView(guideText);

                    itemsContainer.addView(dlCard);
                }

                // Render each setting item card
                for (final SettingItem item : group.items) {
                    LinearLayout card = new LinearLayout(activity);
                    card.setOrientation(LinearLayout.HORIZONTAL);
                    card.setGravity(Gravity.CENTER_VERTICAL);
                    card.setPadding((int) (12 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));

                    GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setColor(Color.parseColor(CARD_BG_COLOR));
                    cardBg.setCornerRadius(10 * density);
                    cardBg.setStroke((int) (1 * density), Color.parseColor(CARD_BORDER_COLOR));
                    card.setBackground(cardBg);

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    cardLp.bottomMargin = (int) (8 * density);
                    card.setLayoutParams(cardLp);

                    // Text column
                    LinearLayout textCol = new LinearLayout(activity);
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    textLp.rightMargin = (int) (8 * density);
                    textCol.setLayoutParams(textLp);

                    TextView itemTitle = new TextView(activity);
                    itemTitle.setText(item.title);
                    itemTitle.setTextColor(Color.WHITE);
                    itemTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    itemTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    textCol.addView(itemTitle);

                    TextView itemDesc = new TextView(activity);
                    itemDesc.setText(item.description);
                    itemDesc.setTextColor(Color.parseColor("#94A3B8")); // Slate-400
                    itemDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                    itemDesc.setPadding(0, (int) (2 * density), 0, 0);
                    textCol.addView(itemDesc);

                    card.addView(textCol);

                    // Switch toggle
                    Switch sw = new Switch(activity);
                    boolean currentVal = SharedPref.getBooleanPref(item.setting);
                    sw.setChecked(currentVal);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int[][] states = new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{-android.R.attr.state_checked}
                        };
                        int[] thumbColors = new int[]{Color.parseColor(ACCENT_COLOR), Color.parseColor("#64748B")};
                        sw.setThumbTintList(new ColorStateList(states, thumbColors));
                    }

                    sw.setOnCheckedChangeListener((buttonView, checked) -> {
                        SharedPref.setBooleanPref(item.setting.key, checked);
                        String statusStr = checked ? "Diaktifkan" : "Dinonaktifkan";
                        Toast.makeText(activity, item.title + " " + statusStr, Toast.LENGTH_SHORT).show();
                    });

                    card.addView(sw);

                    card.setOnClickListener(v -> sw.toggle());
                    itemsContainer.addView(card);
                }
            };

            // Build category chips
            for (int i = 0; i < CATEGORIES.length; i++) {
                final int idx = i;
                final CategoryGroup cat = CATEGORIES[i];

                final TextView tab = new TextView(activity);
                tab.setText(cat.name);
                tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tab.setPadding((int) (12 * density), (int) (6 * density), (int) (12 * density), (int) (6 * density));

                LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                tabLp.rightMargin = (int) (6 * density);
                tab.setLayoutParams(tabLp);

                tab.setOnClickListener(v -> {
                    sSelectedCategoryIndex = idx;
                    for (int j = 0; j < tabViews.size(); j++) {
                        updateTabAppearance(tabViews.get(j), j == sSelectedCategoryIndex, density);
                    }
                    renderItemsRunnable.run();
                });

                tabViews.add(tab);
                updateTabAppearance(tab, i == sSelectedCategoryIndex, density);
                tabContainer.addView(tab);
            }

            renderItemsRunnable.run();

            dialog.setContentView(root);

            Window win = dialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int dialogWidth = Math.min((int) (screenWidth * 0.94f), (int) (480 * density));
                int dialogHeight = Math.min((int) (screenHeight * 0.88f), (int) (680 * density));
                win.setLayout(dialogWidth, dialogHeight);
                win.setGravity(Gravity.CENTER);
            }

            dialog.show();
        } catch (Throwable t) {
            PikoUtils.logger("RhpatchFacebookDialog", "Gagal menampilkan menu mod Facebook: " + t.getMessage(), t);
        }
    }

    private static void updateTabAppearance(TextView tab, boolean isSelected, float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16 * density);
        if (isSelected) {
            tab.setTextColor(Color.WHITE);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            bg.setColor(Color.parseColor(ACCENT_COLOR));
        } else {
            tab.setTextColor(Color.parseColor("#94A3B8"));
            tab.setTypeface(Typeface.DEFAULT);
            bg.setColor(Color.parseColor("#1E293B"));
        }
        tab.setBackground(bg);
    }
}

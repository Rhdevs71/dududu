/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.settings.BooleanSetting;
import app.morphe.extension.crimera.sharedPreference.SharedPref;

public class SpotifySettingsDialog {

    private static final String[][] SETTINGS_METADATA = new String[][] {
        { "Blokir Iklan Audio & Video", "Hilangkan semua iklan komersial, audio, dan video sponsor." },
        { "Bebas Batasan Skip & Seekbar", "Lewati batas 6x skip/jam & scrubbing player bar bebas." },
        { "Pemutaran Langsung (On-Demand)", "Bebas pilih dan putar lagu mana saja langsung dari album/playlist." },
        { "Nonaktifkan Mode Acak Paksa", "Putar lagu sesuai urutan tanpa dipaksa shuffle oleh Spotify." },
        { "Buka Lirik Berjalan Tanpa Batas", "Bypass pembatasan kuota bulanan lirik lagu (unlimited lyrics)." },
        { "Kualitas Audio 320kbps (Extreme)", "Buka bitrate streaming audio kualitas tertinggi Very High." },
        { "Blokir Pop-up Promosi (Anti-Upsell)", "Hilangkan semua popup tawaran langganan 'Dapatkan Premium'." },
        { "Status Akun Platinum (HiFi)", "Simulasi akun Spotify Platinum & kapabilitas tertinggi player." },
        { "Pencatatan Log Diagnostik RHpatch", "Simpan catatan diagnostik rinci ke /sdcard/Download/Rhpatch/rhpatch_debug.log." }
    };

    private static final BooleanSetting[] SETTING_KEYS = new BooleanSetting[] {
        SpotifySettings.BLOCK_ADS,
        SpotifySettings.UNLIMITED_SKIPS,
        SpotifySettings.ON_DEMAND_PLAYBACK,
        SpotifySettings.FORCE_SHUFFLE_DISABLED,
        SpotifySettings.UNLIMITED_LYRICS,
        SpotifySettings.VERY_HIGH_AUDIO_QUALITY,
        SpotifySettings.ANTI_UPSELL,
        SpotifySettings.PRODUCT_STATE_SPOOF,
        SpotifySettings.PIKO_DEBUG
    };

    /**
     * Menampilkan dialog pengaturan modern bergaya Spotify Dark Glassmorphic.
     */
    public static void show(Context context) {
        if (context == null) return;

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            float density = dm.density;

            // Root Card Container (Deep Spotify Black: #121212)
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

            // 1. Header Section
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            // RHpatch Badge
            TextView badge = new TextView(context);
            badge.setText(" RHPATCH ");
            badge.setTextColor(Color.BLACK);
            badge.setTextSize(11f);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.parseColor("#1DB954")); // Spotify Green
            badgeBg.setCornerRadius(6 * density);
            badge.setBackground(badgeBg);
            int badgePadH = (int) (6 * density);
            int badgePadV = (int) (2 * density);
            badge.setPadding(badgePadH, badgePadV, badgePadH, badgePadV);
            header.addView(badge);

            // Title
            TextView title = new TextView(context);
            title.setText(" Spotify Studio");
            title.setTextColor(Color.WHITE);
            title.setTextSize(17f);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleLp.leftMargin = (int) (8 * density);
            title.setLayoutParams(titleLp);
            header.addView(title);

            // Close button (X)
            TextView closeBtn = new TextView(context);
            closeBtn.setText("✕");
            closeBtn.setTextColor(Color.parseColor("#888888"));
            closeBtn.setTextSize(18f);
            closeBtn.setGravity(Gravity.CENTER);
            int closePad = (int) (6 * density);
            closeBtn.setPadding(closePad, closePad, closePad, closePad);
            closeBtn.setOnClickListener(v -> dialog.dismiss());
            header.addView(closeBtn);

            root.addView(header);

            // Subtitle
            TextView subtitle = new TextView(context);
            subtitle.setText("Pengaturan Fitur & Kustomisasi Player");
            subtitle.setTextColor(Color.parseColor("#888888"));
            subtitle.setTextSize(12f);
            subtitle.setPadding(0, (int) (4 * density), 0, (int) (12 * density));
            root.addView(subtitle);

            // Divider
            View divider = new View(context);
            divider.setBackgroundColor(Color.parseColor("#242424"));
            root.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density)));

            // 2. Scrollable Content Section
            ScrollView scroll = new ScrollView(context);
            scroll.setVerticalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            );
            scrollLp.topMargin = (int) (10 * density);
            scrollLp.bottomMargin = (int) (10 * density);
            scroll.setLayoutParams(scrollLp);

            LinearLayout listContainer = new LinearLayout(context);
            listContainer.setOrientation(LinearLayout.VERTICAL);

            Switch[] switchArray = new Switch[SETTING_KEYS.length];

            // ColorStateList for Spotify Green Switches
            int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
            };
            int[] thumbColors = new int[] {
                Color.parseColor("#1DB954"), // Spotify Green
                Color.parseColor("#666666")  // Muted gray
            };
            int[] trackColors = new int[] {
                Color.parseColor("#53B978"), // Translucent green
                Color.parseColor("#383838")  // Dark track
            };
            ColorStateList thumbCsl = new ColorStateList(states, thumbColors);
            ColorStateList trackCsl = new ColorStateList(states, trackColors);

            for (int i = 0; i < SETTING_KEYS.length; i++) {
                final int idx = i;
                BooleanSetting setting = SETTING_KEYS[i];
                boolean isCurrentChecked = SharedPref.getBooleanPref(setting);

                // Card container for each row
                LinearLayout itemCard = new LinearLayout(context);
                itemCard.setOrientation(LinearLayout.HORIZONTAL);
                itemCard.setGravity(Gravity.CENTER_VERTICAL);

                GradientDrawable itemBg = new GradientDrawable();
                itemBg.setColor(Color.parseColor("#181818"));
                itemBg.setCornerRadius(12 * density);
                itemCard.setBackground(itemBg);

                int cardPad = (int) (14 * density);
                itemCard.setPadding(cardPad, cardPad, cardPad, cardPad);

                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardLp.bottomMargin = (int) (8 * density);
                itemCard.setLayoutParams(cardLp);

                // Left Texts (Title + Subtitle)
                LinearLayout textCol = new LinearLayout(context);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                textLp.rightMargin = (int) (10 * density);
                textCol.setLayoutParams(textLp);

                TextView itemTitle = new TextView(context);
                itemTitle.setText(SETTINGS_METADATA[i][0]);
                itemTitle.setTextColor(Color.WHITE);
                itemTitle.setTextSize(14f);
                itemTitle.setTypeface(Typeface.DEFAULT_BOLD);
                textCol.addView(itemTitle);

                TextView itemDesc = new TextView(context);
                itemDesc.setText(SETTINGS_METADATA[i][1]);
                itemDesc.setTextColor(Color.parseColor("#A7A7A7"));
                itemDesc.setTextSize(11.5f);
                itemDesc.setPadding(0, (int) (2 * density), 0, 0);
                textCol.addView(itemDesc);

                itemCard.addView(textCol);

                // Right Switch
                Switch sw = new Switch(context);
                sw.setChecked(isCurrentChecked);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    sw.setThumbTintList(thumbCsl);
                    sw.setTrackTintList(trackCsl);
                }
                switchArray[i] = sw;

                sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    SharedPref.setBooleanPref(setting.key, isChecked);
                    if (setting == SpotifySettings.UNLIMITED_SKIPS) {
                        SharedPref.setBooleanPref(SpotifySettings.ENABLE_SEEK.key, isChecked);
                    }
                });

                itemCard.setOnClickListener(v -> sw.toggle());

                itemCard.addView(sw);
                listContainer.addView(itemCard);
            }

            scroll.addView(listContainer);
            root.addView(scroll);

            // 3. Footer Section
            View dividerBottom = new View(context);
            dividerBottom.setBackgroundColor(Color.parseColor("#242424"));
            root.addView(dividerBottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density)));

            // Close / Save Button: Spotify Green Capsule
            TextView doneBtn = new TextView(context);
            doneBtn.setText("Tutup & Terapkan");
            doneBtn.setTextColor(Color.BLACK);
            doneBtn.setTextSize(14f);
            doneBtn.setTypeface(Typeface.DEFAULT_BOLD);
            doneBtn.setGravity(Gravity.CENTER);

            GradientDrawable doneBg = new GradientDrawable();
            doneBg.setColor(Color.parseColor("#1DB954"));
            doneBg.setCornerRadius(24 * density);
            doneBtn.setBackground(doneBg);

            int btnHeight = (int) (46 * density);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, btnHeight);
            btnLp.topMargin = (int) (12 * density);
            doneBtn.setLayoutParams(btnLp);

            doneBtn.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Toast.makeText(context, "Pengaturan Piko Spotify berhasil disimpan!", Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            });
            root.addView(doneBtn);

            // Reset Button
            TextView resetBtn = new TextView(context);
            resetBtn.setText("Reset ke Pengaturan Default");
            resetBtn.setTextColor(Color.parseColor("#888888"));
            resetBtn.setTextSize(12f);
            resetBtn.setGravity(Gravity.CENTER);
            int resetPad = (int) (8 * density);
            resetBtn.setPadding(0, resetPad, 0, (int) (2 * density));

            resetBtn.setOnClickListener(v -> {
                for (int i = 0; i < SETTING_KEYS.length; i++) {
                    BooleanSetting s = SETTING_KEYS[i];
                    SharedPref.setBooleanPref(s.key, s.defaultValue);
                    if (switchArray[i] != null) {
                        switchArray[i].setChecked(s.defaultValue);
                    }
                }
                Toast.makeText(context, "Pengaturan direset ke default!", Toast.LENGTH_SHORT).show();
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

                // Max width 92% of screen or 420dp
                int targetW = Math.min((int) (screenW * 0.92f), (int) (420 * density));
                // Max height 82% of screen
                int targetH = (int) (screenH * 0.82f);

                wlp.width = targetW;
                wlp.height = targetH;
                wlp.gravity = Gravity.CENTER;
                window.setAttributes(wlp);
            }

            dialog.show();
        } catch (Throwable t) {
            PikoUtils.logger("SpotifySettingsDialog", "Gagal menampilkan dialog pengaturan: " + t.getMessage(), t);
        }
    }
}

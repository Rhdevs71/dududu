/*
 * Copyright (C) 2026 RHpatch <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.ui;

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
import app.morphe.extension.instagram.settings.Settings;
import app.morphe.extension.instagram.settings.preference.fragments.FragmentHook;

public class RhpatchInstagramDialog {

    private static final String ACCENT_COLOR = "#E1306C"; // Instagram Sunset Neon Magenta

    private static final String[][] SETTINGS_METADATA = new String[][] {
        { "Blokir Iklan Feed & Stories", "Hilangkan semua iklan komersial, postingan sponsor, dan saran." },
        { "Mode Siluman Lihat Cerita", "Lihat story tanpa memicu tanda terbaca (nama Anda tidak muncul di viewer)." },
        { "Mode Siluman Baca Direct Message", "Baca pesan obrolan DM tanpa mengirim tanda centang 'Seen' ke pengirim." },
        { "Anti-Hilang Media Sekali Lihat", "Buka foto dan video sekali lihat (view-once) berkali-kali tanpa batas." },
        { "Penyelamat Pesan DM yang Ditarik", "Catat dan simpan otomatis pesan yang dihapus lawan bicara ke database lokal." },
        { "Pengunduh Media Resolusi Tertinggi", "Unduh foto, video, carousel, reels, dan story dalam kualitas asli." },
        { "Buka Seluruh Fitur IG Plus", "Buka icon aplikasi kustom, font bio khusus, font cerita, dan preview." },
        { "Blokir Notifikasi Screenshot DM", "Cegah Instagram memberitahu lawan bicara saat Anda screenshot layar obrolan." },
        { "Lencana Indikator Pertemanan", "Tampilkan lencana status apakah akun mengikuti Anda kembali di halaman profil." },
        { "Mode Gelap AMOLED Hitam Murni", "Terapkan warna hitam murni #000000 untuk hemat baterai layar OLED." },
        { "Pencatatan Log Diagnostik RHpatch", "Simpan catatan diagnostik rinci ke /sdcard/Download/Rhpatch/rhpatch_debug.log." }
    };

    private static final BooleanSetting[] SETTING_KEYS = new BooleanSetting[] {
        Settings.DISABLE_ADS,
        Settings.VIEW_STORIES_ANONYMOUSLY,
        Settings.VIEW_DM_ANONYMOUSLY,
        Settings.UNLIMITED_REPLAYS,
        Settings.SAVE_DELETED_MESSAGES,
        Settings.ENABLE_DOWNLOAD,
        Settings.UNLOCK_PLUS_BENEFITS,
        Settings.DISABLE_SCREENSHOT_DETECTION,
        Settings.FOLLOW_BACK_INDICATOR,
        Settings.AMOLED_THEME,
        Settings.PIKO_DEBUG
    };

    /**
     * Menampilkan dialog pengaturan modern bergaya Instagram Dark Glassmorphic (senada dengan Spotify).
     */
    public static void show(Context context) {
        if (context == null) return;

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            float density = dm.density;

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

            // 1. Header Section
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            // RHpatch Badge (Neon Sunset Magenta)
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
            closeBtn.setText("✕");
            closeBtn.setTextColor(Color.parseColor("#A7A7A7"));
            closeBtn.setTextSize(17f);
            closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
            closeBtn.setGravity(Gravity.CENTER);
            int xPad = (int) (6 * density);
            closeBtn.setPadding(xPad, xPad, xPad, xPad);
            closeBtn.setOnClickListener(v -> dialog.dismiss());
            header.addView(closeBtn);

            root.addView(header);

            // Subtitle
            TextView subTitle = new TextView(context);
            subTitle.setText("Pengaturan Fitur & Kustomisasi Instagram");
            subTitle.setTextColor(Color.parseColor("#A7A7A7"));
            subTitle.setTextSize(12f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subLp.topMargin = (int) (4 * density);
            subLp.bottomMargin = (int) (14 * density);
            subTitle.setLayoutParams(subLp);
            root.addView(subTitle);

            // 2. Scrollable Feature List
            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            );
            scrollView.setLayoutParams(scrollLp);

            LinearLayout listContainer = new LinearLayout(context);
            listContainer.setOrientation(LinearLayout.VERTICAL);

            Switch[] switchArray = new Switch[SETTING_KEYS.length];

            for (int i = 0; i < SETTING_KEYS.length; i++) {
                final int index = i;
                final BooleanSetting setting = SETTING_KEYS[i];
                boolean isEnabled = SharedPref.getBooleanPref(setting);

                // Feature Card
                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setClickable(true);
                card.setFocusable(true);

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(Color.parseColor("#181818"));
                cardBg.setCornerRadius(14 * density);
                cardBg.setStroke((int) (1 * density), Color.parseColor("#242424"));

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
                cardLp.bottomMargin = (int) (10 * density);
                card.setLayoutParams(cardLp);

                // Text Column
                LinearLayout textCol = new LinearLayout(context);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                textCol.setLayoutParams(textLp);

                TextView itemTitle = new TextView(context);
                itemTitle.setText(SETTINGS_METADATA[i][0]);
                itemTitle.setTextColor(Color.WHITE);
                itemTitle.setTextSize(13.5f);
                itemTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                textCol.addView(itemTitle);

                TextView itemDesc = new TextView(context);
                itemDesc.setText(SETTINGS_METADATA[i][1]);
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

                // Magenta Switch
                Switch toggle = new Switch(context);
                toggle.setChecked(isEnabled);

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

                switchArray[i] = toggle;

                toggle.setOnCheckedChangeListener((btn, checked) -> {
                    SharedPref.setBooleanPref(setting.key, checked);
                });

                card.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));

                card.addView(toggle);
                listContainer.addView(card);
            }

            scrollView.addView(listContainer);
            root.addView(scrollView);

            // 3. Quick Action: Menu Pengaturan Lengkap
            TextView advancedBtn = new TextView(context);
            advancedBtn.setText("Buka Pengaturan Lanjutan (Developer & MobileConfig) →");
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

            // 4. Action Button (Full-width Magenta Pill)
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

            // 5. Reset to default button
            TextView resetBtn = new TextView(context);
            resetBtn.setText("Reset ke Pengaturan Default");
            resetBtn.setTextColor(Color.parseColor("#777777"));
            resetBtn.setTextSize(11f);
            resetBtn.setGravity(Gravity.CENTER);
            int resetPad = (int) (6 * density);
            resetBtn.setPadding(resetPad, resetPad, resetPad, resetPad);
            resetBtn.setOnClickListener(v -> {
                for (int i = 0; i < SETTING_KEYS.length; i++) {
                    BooleanSetting s = SETTING_KEYS[i];
                    SharedPref.setBooleanPref(s.key, s.defaultValue);
                    if (switchArray[i] != null) {
                        switchArray[i].setChecked(s.defaultValue);
                    }
                }
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

                int targetW = Math.min((int) (screenW * 0.92f), (int) (420 * density));
                int targetH = (int) (screenH * 0.82f);

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
}

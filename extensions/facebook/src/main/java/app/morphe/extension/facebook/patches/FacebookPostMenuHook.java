/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.crimera.PikoUtils;

public class FacebookPostMenuHook {

    private static final String TAG = "FacebookPostMenuHook";
    private static final String TAG_MOD_MENU_CONTAINER = "rhpatch_post_menu_card";

    private static final Pattern PATTERN_HD_VIDEO = Pattern.compile("\"(?:playable_url_quality_hd|browser_native_hd_url)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_SD_VIDEO = Pattern.compile("\"(?:playable_url|browser_native_sd_url)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_IMAGE_URI = Pattern.compile("\"(?:large_share_image_uri|high_res_image_uri|uri)\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\"]+)\"");
    private static final Pattern PATTERN_PERMALINK = Pattern.compile("\"(?:permalink_url|url)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_MESSAGE = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");

    public static class PostMediaInfo {
        public String hdVideoUrl;
        public String sdVideoUrl;
        public String imageUrl;
        public String permalink;
        public String messageText;
        public String storyId;
    }

    /**
     * Diinjeksi di LX/Rbs;->F2r()Landroid/app/Dialog; saat menu titik 3 pada postingan / reels diklik.
     */
    public static void onPostMenuCreated(final Dialog dialog, final View anchorView, final Object feedUnit) {
        if (dialog == null) return;
        try {
            final PostMediaInfo mediaInfo = extractMediaInfo(feedUnit);
            if (mediaInfo != null) {
                // Simpan sebagai media aktif terakhir
                FacebookMediaDownloader.sLastActiveMedia = mediaInfo;
            }

            // Pasang kartu download di Dialog setelah view siap
            final View decorView = dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null;
            if (decorView != null) {
                decorView.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            attachDownloadCardToDialog(dialog, decorView, mediaInfo);
                        } catch (Throwable t) {
                            PikoUtils.logger(TAG, "Error attaching card to dialog", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Error in onPostMenuCreated", t);
        }
    }

    public static PostMediaInfo extractMediaInfo(Object feedUnit) {
        if (feedUnit == null) return null;
        try {
            Object story = feedUnit;
            // Jika feedUnit adalah wrapper (LX/2QD), ambil field A01
            try {
                java.lang.reflect.Field fieldA01 = feedUnit.getClass().getDeclaredField("A01");
                fieldA01.setAccessible(true);
                Object inner = fieldA01.get(feedUnit);
                if (inner != null) {
                    story = inner;
                }
            } catch (Throwable ignored) {}

            String jsonTree = null;
            // Coba ambil representasi JSON tree dari TreeJNI
            try {
                Method debugMethod = story.getClass().getMethod("toExpensiveHumanReadableDebugString");
                debugMethod.setAccessible(true);
                Object res = debugMethod.invoke(story);
                if (res instanceof String) {
                    jsonTree = (String) res;
                }
            } catch (Throwable ignored) {}

            if (jsonTree == null) {
                jsonTree = story.toString();
            }

            if (jsonTree == null || jsonTree.isEmpty()) {
                return null;
            }

            PostMediaInfo info = new PostMediaInfo();

            // 1. Ekstrak Video HD
            Matcher m = PATTERN_HD_VIDEO.matcher(jsonTree);
            if (m.find()) {
                info.hdVideoUrl = unescapeJson(m.group(1));
            }

            // 2. Ekstrak Video SD
            m = PATTERN_SD_VIDEO.matcher(jsonTree);
            if (m.find()) {
                info.sdVideoUrl = unescapeJson(m.group(1));
            }

            // 3. Ekstrak Gambar HD
            m = PATTERN_IMAGE_URI.matcher(jsonTree);
            while (m.find()) {
                String img = unescapeJson(m.group(1));
                if (img.contains("scontent") || img.contains("fbcdn.net")) {
                    info.imageUrl = img;
                    break;
                }
            }

            // 4. Ekstrak Permalink
            m = PATTERN_PERMALINK.matcher(jsonTree);
            if (m.find()) {
                info.permalink = unescapeJson(m.group(1));
            }

            // 5. Ekstrak Message
            m = PATTERN_MESSAGE.matcher(jsonTree);
            if (m.find()) {
                info.messageText = unescapeJson(m.group(1));
            }

            return info;
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "extractMediaInfo error", t);
            return null;
        }
    }

    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\/", "/").replace("\\\"", "\"").replace("\\u0025", "%").replace("\\u0026", "&");
    }

    private static void attachDownloadCardToDialog(final Dialog dialog, View decorView, final PostMediaInfo info) {
        if (decorView == null || !(decorView instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) decorView;

        // Cegah duplikasi view
        if (root.findViewWithTag(TAG_MOD_MENU_CONTAINER) != null) return;

        Context context = dialog.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout card = new LinearLayout(context);
        card.setTag(TAG_MOD_MENU_CONTAINER);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#EE111827")); // Dark charcoal
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) (1.5f * density), Color.parseColor("#1877F2")); // Facebook Royal Blue
        card.setBackground(bg);

        int pad = (int) (12 * density);
        card.setPadding(pad, pad, pad, pad);

        // Header Title
        TextView title = new TextView(context);
        title.setText("🛡️ [RHpatch] Opsi Unduh Media Postingan");
        title.setTextColor(Color.parseColor("#1877F2"));
        title.setTextSize(13.5f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (38 * density)
        );
        btnLp.topMargin = (int) (8 * density);

        // 1. Tombol Video HD jika ada video
        final String videoUrl = (info != null && info.hdVideoUrl != null) ? info.hdVideoUrl : (info != null ? info.sdVideoUrl : null);
        if (videoUrl != null && !videoUrl.isEmpty()) {
            TextView btnVideo = createButton(context, "🎬 Unduh Video (HD MP4)", "#1877F2", Color.WHITE, density);
            btnVideo.setLayoutParams(btnLp);
            btnVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    FacebookMediaDownloader.downloadDirectStream(v.getContext(), videoUrl, "Facebook_Video", true);
                }
            });
            card.addView(btnVideo);
        }

        // 2. Tombol Foto HD jika ada foto
        if (info != null && info.imageUrl != null && !info.imageUrl.isEmpty()) {
            TextView btnPhoto = createButton(context, "🖼️ Unduh Foto (Full HD JPG)", "#8B5CF6", Color.WHITE, density);
            btnPhoto.setLayoutParams(btnLp);
            btnPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    FacebookMediaDownloader.downloadDirectStream(v.getContext(), info.imageUrl, "Facebook_Photo", false);
                }
            });
            card.addView(btnPhoto);
        }

        // Row untuk Salin Teks & Salin Tautan
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(btnLp);

        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);

        // 3. Tombol Salin Teks
        TextView btnCopyText = createButton(context, "📋 Salin Teks", "#1F2937", Color.WHITE, density);
        btnCopyText.setLayoutParams(subLp);
        btnCopyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (info != null && info.messageText != null && !info.messageText.isEmpty()) {
                    copyToClipboard(v.getContext(), "Facebook Text", info.messageText);
                    Toast.makeText(v.getContext(), "📋 Teks postingan berhasil disalin!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "⚠️ Postingan ini tidak memiliki teks/caption.", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        row.addView(btnCopyText);

        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams((int) (8 * density), ViewGroup.LayoutParams.MATCH_PARENT));
        row.addView(divider);

        // 4. Tombol Salin Tautan
        TextView btnCopyLink = createButton(context, "🔗 Salin Tautan", "#1F2937", Color.WHITE, density);
        btnCopyLink.setLayoutParams(subLp);
        btnCopyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String link = (info != null && info.permalink != null) ? info.permalink : null;
                if (link != null) {
                    copyToClipboard(v.getContext(), "Facebook Link", link);
                    Toast.makeText(v.getContext(), "🔗 Tautan postingan berhasil disalin!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "⚠️ Tautan postingan tidak ditemukan.", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        row.addView(btnCopyLink);

        card.addView(row);

        // Masukkan card ke bagian paling atas decorView dialog
        ViewGroup.MarginLayoutParams cardLp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (10 * density);
        cardLp.setMargins(margin, margin, margin, margin);
        card.setLayoutParams(cardLp);

        root.addView(card, 0);
    }

    private static TextView createButton(Context context, String text, String bgColor, int textColor, float density) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgColor));
        bg.setCornerRadius(10 * density);
        btn.setBackground(bg);

        return btn;
    }

    private static void copyToClipboard(Context context, String label, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(label, text));
            }
        } catch (Throwable ignored) {}
    }
}

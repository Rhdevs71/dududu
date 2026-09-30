/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.crimera.PikoUtils;

public class FacebookMediaDownloader {

    private static final String TAG = "FacebookMediaDownloader";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final Pattern PATTERN_HD_JSON = Pattern.compile("\"playable_url_quality_hd\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_SD_JSON = Pattern.compile("\"playable_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_BROWSER_HD = Pattern.compile("\"browser_native_hd_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_BROWSER_SD = Pattern.compile("\"browser_native_sd_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_OG_VIDEO = Pattern.compile("<meta\\s+property=\"og:video(?::secure_url)?\"\\s+content=\"([^\"]+)\"");
    private static final Pattern PATTERN_HD_SRC = Pattern.compile("hd_src\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_SD_SRC = Pattern.compile("sd_src\\s*:\\s*\"([^\"]+)\"");

    public static String getClipboardFacebookUrl(Context context) {
        if (context == null) return null;
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        String str = text.toString().trim();
                        if (str.contains("facebook.com") || str.contains("fb.watch")) {
                            return str;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Error reading clipboard", t);
        }
        return null;
    }

    public static void startDownload(final Context context, final String rawUrl) {
        if (context == null || rawUrl == null || rawUrl.trim().isEmpty()) {
            toast(context, "⚠️ Tautan video tidak boleh kosong!");
            return;
        }

        final String cleanUrl = rawUrl.trim();
        toast(context, "🔍 Memeriksa dan menganalisis video Facebook...");

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Jika URL adalah link CDN langsung (.mp4 atau mengandung fbcdn.net)
                    if (cleanUrl.contains(".mp4") || (cleanUrl.contains("fbcdn.net") && cleanUrl.contains("/v/"))) {
                        enqueueDownload(context, cleanUrl, "Facebook_Direct_Video");
                        return;
                    }

                    // Fetch halaman Facebook untuk resolusi CDN URL
                    String cdnUrl = resolveFacebookVideoUrl(cleanUrl);
                    if (cdnUrl != null && !cdnUrl.isEmpty()) {
                        enqueueDownload(context, cdnUrl, "Facebook_Video");
                    } else {
                        MAIN_HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                                toast(context, "❌ Gagal mengekstrak video. Pastikan postingan bersifat publik atau salin tautan video langsung.");
                            }
                        });
                    }
                } catch (final Throwable t) {
                    PikoUtils.logger(TAG, "Download extraction failed", t);
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            toast(context, "❌ Gagal mengunduh: " + t.getMessage());
                        }
                    });
                }
            }
        });
    }

    private static String resolveFacebookVideoUrl(String postUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(postUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");

            int responseCode = conn.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl != null) {
                    return resolveFacebookVideoUrl(redirectUrl);
                }
            }

            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                if (sb.length() > 2_000_000) break; // Limit 2MB max
            }
            reader.close();

            String html = sb.toString();

            // 1. Coba HD JSON URL
            Matcher m = PATTERN_HD_JSON.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 2. Coba Browser Native HD URL
            m = PATTERN_BROWSER_HD.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 3. Coba HD src
            m = PATTERN_HD_SRC.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 4. Coba SD JSON URL
            m = PATTERN_SD_JSON.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 5. Coba Browser Native SD URL
            m = PATTERN_BROWSER_SD.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 6. Coba SD src
            m = PATTERN_SD_SRC.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 7. Coba OpenGraph og:video meta
            m = PATTERN_OG_VIDEO.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Error resolving FB video: " + t.getMessage(), t);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static String unescapeUrl(String raw) {
        if (raw == null) return null;
        String s = raw.replace("\\/", "/")
                      .replace("\\u0025", "%")
                      .replace("\\u0026", "&")
                      .replace("&amp;", "&");
        try {
            s = URLDecoder.decode(s, "UTF-8");
        } catch (Throwable ignored) {}
        return s;
    }

    private static void enqueueDownload(final Context context, final String directUrl, final String prefix) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm == null) {
                        toast(context, "❌ Layanan DownloadManager sistem tidak tersedia!");
                        return;
                    }

                    Uri downloadUri = Uri.parse(directUrl);
                    String fileName = prefix + "_" + System.currentTimeMillis() + ".mp4";

                    File fbDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Facebook");
                    if (!fbDir.exists()) {
                        fbDir.mkdirs();
                    }

                    DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                    request.setTitle("📥 Facebook HD Video: " + fileName);
                    request.setDescription("Mengunduh video Facebook kualitas tinggi...");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Facebook/" + fileName);
                    request.setMimeType("video/mp4");
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(true);

                    dm.enqueue(request);

                    toast(context, "🚀 Unduhan dimulai: " + fileName + "\n(Cek bilah notifikasi perangkat Anda)");

                    // Daftarkan ke MediaScanner agar muncul di Galeri
                    File finalFile = new File(fbDir, fileName);
                    MediaScannerConnection.scanFile(context, new String[]{finalFile.getAbsolutePath()}, new String[]{"video/mp4"}, null);

                } catch (Throwable t) {
                    PikoUtils.logger(TAG, "Failed enqueueing download", t);
                    toast(context, "❌ Gagal memproses unduhan: " + t.getMessage());
                }
            }
        });
    }

    private static void toast(final Context context, final String msg) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                } catch (Throwable ignored) {}
            }
        });
    }
}

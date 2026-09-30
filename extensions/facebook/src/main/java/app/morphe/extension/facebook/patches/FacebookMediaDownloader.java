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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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

    public static FacebookPostMenuHook.PostMediaInfo sLastActiveMedia = null;

    private static final Pattern PATTERN_HD_JSON = Pattern.compile("\"(?:playable_url_quality_hd|browser_native_hd_url)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_SD_JSON = Pattern.compile("\"(?:playable_url|browser_native_sd_url)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATTERN_OG_VIDEO = Pattern.compile("<meta\\s+property=\"og:video(?::secure_url)?\"\\s+content=\"([^\"]+)\"");
    private static final Pattern PATTERN_OG_IMAGE = Pattern.compile("<meta\\s+property=\"og:image\"\\s+content=\"([^\"]+)\"");
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
        toast(context, "🔍 Memeriksa dan mengekstrak media Facebook...");

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Jika URL adalah link CDN langsung (.mp4 atau mengandung fbcdn.net)
                    if (cleanUrl.contains(".mp4") || (cleanUrl.contains("fbcdn.net") && cleanUrl.contains("/v/"))) {
                        enqueueDownload(context, cleanUrl, "Facebook_Direct_Video", true);
                        return;
                    }

                    // Fetch halaman Facebook dengan scraper crawler bypass
                    String cdnUrl = resolveFacebookVideoUrl(cleanUrl);
                    if (cdnUrl != null && !cdnUrl.isEmpty()) {
                        enqueueDownload(context, cdnUrl, "Facebook_Video", true);
                    } else {
                        MAIN_HANDLER.post(new Runnable() {
                            @Override
                            public void run() {
                                toast(context, "❌ Gagal mengekstrak media. Anda juga bisa mengunduh langsung lewat menu titik 3 (...) di postingan!");
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

    public static void downloadDirectStream(Context context, String directUrl, String prefix, boolean isVideo) {
        if (context == null || directUrl == null || directUrl.isEmpty()) {
            toast(context, "⚠️ Tautan media kosong!");
            return;
        }
        enqueueDownload(context, directUrl, prefix, isVideo);
    }

    public static void downloadActiveScreenPhoto(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    View decorView = activity.getWindow().getDecorView();
                    ImageView largestIv = findLargestImageView(decorView);

                    if (largestIv == null || largestIv.getDrawable() == null) {
                        toast(activity, "⚠️ Tidak ditemukan foto aktif di layar ini.");
                        return;
                    }

                    Drawable drawable = largestIv.getDrawable();
                    Bitmap bitmap = null;
                    if (drawable instanceof BitmapDrawable) {
                        bitmap = ((BitmapDrawable) drawable).getBitmap();
                    }

                    if (bitmap == null) {
                        toast(activity, "⚠️ Gagal mengekstrak data gambar.");
                        return;
                    }

                    File fbDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Facebook");
                    if (!fbDir.exists()) fbDir.mkdirs();

                    String fileName = "Facebook_Photo_" + System.currentTimeMillis() + ".jpg";
                    File photoFile = new File(fbDir, fileName);

                    FileOutputStream fos = new FileOutputStream(photoFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.flush();
                    fos.close();

                    MediaScannerConnection.scanFile(activity, new String[]{photoFile.getAbsolutePath()}, new String[]{"image/jpeg"}, null);

                    toast(activity, "✅ Foto berhasil disimpan ke Galeri:\n" + fileName);

                } catch (Throwable t) {
                    PikoUtils.logger(TAG, "Failed downloading active photo", t);
                    toast(activity, "❌ Gagal menyimpan foto: " + t.getMessage());
                }
            }
        });
    }

    private static ImageView findLargestImageView(View view) {
        if (view == null) return null;
        if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            if (iv.getDrawable() != null && iv.getWidth() > 100 && iv.getHeight() > 100) {
                return iv;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            ImageView best = null;
            long maxArea = 0;
            for (int i = 0; i < vg.getChildCount(); i++) {
                ImageView candidate = findLargestImageView(vg.getChildAt(i));
                if (candidate != null) {
                    long area = (long) candidate.getWidth() * candidate.getHeight();
                    if (area > maxArea) {
                        maxArea = area;
                        best = candidate;
                    }
                }
            }
            return best;
        }
        return null;
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
            // Gunakan User-Agent crawler Facebook agar tidak dialihkan ke halaman login
            conn.setRequestProperty("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)");
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
                if (sb.length() > 2_000_000) break;
            }
            reader.close();

            String html = sb.toString();

            // 1. Coba HD JSON URL
            Matcher m = PATTERN_HD_JSON.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 2. Coba HD src
            m = PATTERN_HD_SRC.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 3. Coba SD JSON URL
            m = PATTERN_SD_JSON.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 4. Coba SD src
            m = PATTERN_SD_SRC.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 5. Coba OpenGraph og:video meta
            m = PATTERN_OG_VIDEO.matcher(html);
            if (m.find()) return unescapeUrl(m.group(1));

            // 6. Coba OpenGraph og:image meta
            m = PATTERN_OG_IMAGE.matcher(html);
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

    private static void enqueueDownload(final Context context, final String directUrl, final String prefix, final boolean isVideo) {
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
                    String ext = isVideo ? ".mp4" : ".jpg";
                    String mime = isVideo ? "video/mp4" : "image/jpeg";
                    String fileName = prefix + "_" + System.currentTimeMillis() + ext;

                    File fbDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Facebook");
                    if (!fbDir.exists()) {
                        fbDir.mkdirs();
                    }

                    DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                    request.setTitle("📥 Facebook HD " + (isVideo ? "Video" : "Foto") + ": " + fileName);
                    request.setDescription("Mengunduh media Facebook kualitas tinggi...");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Facebook/" + fileName);
                    request.setMimeType(mime);
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(true);

                    dm.enqueue(request);

                    toast(context, "🚀 Unduhan dimulai: " + fileName + "\n(Cek bilah notifikasi perangkat Anda)");

                    // Daftarkan ke MediaScanner agar muncul di Galeri
                    File finalFile = new File(fbDir, fileName);
                    MediaScannerConnection.scanFile(context, new String[]{finalFile.getAbsolutePath()}, new String[]{mime}, null);

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

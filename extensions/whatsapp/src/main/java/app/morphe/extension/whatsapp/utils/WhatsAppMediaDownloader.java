/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WhatsAppMediaDownloader {
    private static final String TAG = "WAMediaDownloader";
    private static final String PIKO_WA_DIR = "Download/Piko/WhatsApp";

    public static File getOutputDirectory(String subFolder) {
        File sdcard = Environment.getExternalStorageDirectory();
        File baseDir = new File(sdcard, PIKO_WA_DIR);
        if (subFolder != null && !subFolder.isEmpty()) {
            baseDir = new File(baseDir, subFolder);
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return baseDir;
    }

    public static String generateFileName(String prefix, String ext) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return prefix + "_" + timestamp + "." + (ext != null ? ext.replace(".", "") : "bin");
    }

    public static boolean saveMediaFile(File sourceFile, String subFolder, String targetFileName) {
        if (sourceFile == null || !sourceFile.exists()) {
            WhatsAppLog.w(TAG, "Source file does not exist: " + sourceFile);
            return false;
        }

        try {
            File outDir = getOutputDirectory(subFolder);
            File destFile = new File(outDir, targetFileName);

            InputStream in = new FileInputStream(sourceFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            // Scan to gallery
            Context ctx = Utils.getContext();
            if (ctx != null) {
                MediaScannerConnection.scanFile(
                    ctx,
                    new String[]{destFile.getAbsolutePath()},
                    null,
                    (path, uri) -> WhatsAppLog.d(TAG, "Media scanned: " + path)
                );
            }

            WhatsAppLog.i(TAG, "Media saved successfully to: " + destFile.getAbsolutePath());
            PikoUtils.toast("Media tersimpan ke: " + destFile.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            WhatsAppLog.e(TAG, "Error saving media file", t);
            PikoUtils.toast("Gagal menyimpan media: " + t.getMessage());
            return false;
        }
    }
}

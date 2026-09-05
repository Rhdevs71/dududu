/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.crimera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.Logger;

@SuppressWarnings("unused")
public class PikoUtils {
    private static final Context ctx = Utils.getContext();

    public static Context getContext() {
        return ctx;
    }

    // Credits to Morphe:
    // https://github.com/MorpheApp/morphe-patches/blob/d6a88edcfba71f9b630314c4c8b56347a10c8b2a/extensions/youtube/src/main/java/app/morphe/extension/youtube/settings/preference/ExternalDownloaderPreference.java#L128-L138
    public static boolean isAppInstalledAndEnabled(String packageName) {
        try {
            return ctx.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void launchIntent(Intent intent){
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
         } catch (Exception e) {
            Logger.printException(() -> "launchIntent failure", e);
            logger(e);
        }
    }

    public static void shareTextToPackageName(String url, String packageName) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.setPackage(packageName);
        launchIntent(intent);
    }

    public static void shareText(String txt) {
        final String appPackageName = ctx.getPackageName();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, txt);
        sendIntent.setType("text/plain");
        launchIntent(sendIntent);
    }

    public static void openUrl(String url, boolean currentPackageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if(currentPackageName){
             intent.setPackage(ctx.getPackageName());
        }
        launchIntent(intent);

    }

    public static void openDefaultLinks() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS);
        intent.setData(Uri.parse("package:" + ctx.getPackageName()));
        launchIntent(intent);
    }

    public static boolean pikoWriteFile(String fileName,String data,boolean append){
        return pikoWriteFile(fileName,"Piko",data,append);
    }

    public static boolean pikoWriteFile(String fileName,String subFolder, String data,boolean append){
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pikoDir = new File(downloadsDir, subFolder);

        if (!pikoDir.exists()) {
            pikoDir.mkdirs();
        }

        File outputFile = new File(pikoDir, fileName);
        return writeFile(outputFile,data.getBytes(),append);
    }

    public static boolean writeFile(File fileName, byte[] data, boolean append) {
        try {
            FileOutputStream outputStream = new FileOutputStream(fileName, append);
            outputStream.write(data);
            outputStream.close();
            return true;
        } catch (Exception e) {
            logger(e.toString());
        }
        return false;
    }

    public static String readFile(File fileName) {
        try {
            if (!fileName.exists())
                return null;

            StringBuilder content = new StringBuilder();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(fileName));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            return content.toString();
        } catch (Exception e) {
            logger(e.toString());
        }
        return null;
    }

    public static void toast(String msg) {
        logToFile("TOAST", msg);
        app.morphe.extension.shared.Utils.showToastShort(msg);
    }

    public static void logger(Object e) {
        logger("Piko", e);
    }

    public static void logger(String tag, Object e) {
        String logName = "piko";
        Log.e(logName, "[" + tag + "] " + e);
        if (e instanceof Throwable) {
            Log.e(logName, "[" + tag + "] StackTrace: ", (Throwable) e);
        }
        logToFile(tag, e);
    }

    public static void logger(String tag, String msg, Throwable t) {
        String logName = "piko";
        Log.e(logName, "[" + tag + "] " + msg, t);
        if (t != null) {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                t.printStackTrace(new java.io.PrintWriter(sw));
                logToFile(tag, (msg != null ? msg + "\n" : "") + "Exception: " + t.getClass().getName() + ": " + t.getMessage() + "\nStack trace:\n" + sw.toString());
            } catch (Exception ex) {
                logToFile(tag, (msg != null ? msg + ": " : "") + t);
            }
        } else {
            logToFile(tag, msg);
        }
    }

    public static synchronized void logToFile(String tag, Object e) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US);
            String timestamp = sdf.format(new java.util.Date());
            StringBuilder sb = new StringBuilder();
            sb.append("\n================================================================\n");
            sb.append("[").append(timestamp).append("] [").append(tag).append("] [Thread: ").append(Thread.currentThread().getName()).append("]\n");

            if (e instanceof Throwable) {
                Throwable t = (Throwable) e;
                sb.append("Exception: ").append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                t.printStackTrace(pw);
                sb.append("Stack trace:\n").append(sw.toString());
            } else {
                sb.append("Message: ").append(e).append("\n");
            }

            byte[] logBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 1. Try public Downloads/Piko/piko_debug.log
            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File pikoDir = new File(downloadsDir, "Piko");
                if (!pikoDir.exists()) {
                    pikoDir.mkdirs();
                }
                File logFile = new File(pikoDir, "piko_debug.log");
                writeRawFile(logFile, logBytes, true);
            } catch (Exception ignored) {
            }

            // 2. Fallback to app external and internal files dir
            Context context = ctx != null ? ctx : Utils.getContext();
            if (context != null) {
                try {
                    File extDir = context.getExternalFilesDir("logs");
                    if (extDir != null) {
                        if (!extDir.exists()) extDir.mkdirs();
                        File logFile = new File(extDir, "piko_debug.log");
                        writeRawFile(logFile, logBytes, true);
                    }
                } catch (Exception ignored) {
                }
                try {
                    File intDir = new File(context.getFilesDir(), "logs");
                    if (!intDir.exists()) intDir.mkdirs();
                    File logFile = new File(intDir, "piko_debug.log");
                    writeRawFile(logFile, logBytes, true);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            Log.e("piko", "Error in logToFile: " + ex);
        }
    }

    private static boolean writeRawFile(File file, byte[] data, boolean append) {
        try {
            FileOutputStream fos = new FileOutputStream(file, append);
            fos.write(data);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Converts sp value to actual device pixels.
     *
     * @return The device pixel value.
     */
    public static int spToPixels(float sp) {
        return  (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                Resources.getSystem().getDisplayMetrics()
        );
    }
}

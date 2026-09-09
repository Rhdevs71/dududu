/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.patches.clone;

import android.content.Context;

import app.morphe.extension.spotify.utils.SpotifyLog;

public class SpotifyPackageSpoofer {
    private static final String TAG = "SpotifyPackageSpoofer";
    private static final String ORIGINAL_PACKAGE = "com.spotify.music";

    /**
     * Intercepts calls to Context.getPackageName() in Spotify bytecode.
     * When internal Spotify native components, telemetry, or login checks verify the package name,
     * returns the original "com.spotify.music".
     */
    public static String getSpoofedPackageName(Context context, String realPackageName) {
        if (realPackageName == null || realPackageName.equals(ORIGINAL_PACKAGE)) {
            return ORIGINAL_PACKAGE;
        }

        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 2; i < Math.min(stack.length, 8); i++) {
                String className = stack[i].getClassName();
                if (className.startsWith("com.spotify") ||
                    className.contains("spotify") ||
                    className.startsWith("p.")) {
                    return ORIGINAL_PACKAGE;
                }
            }
        } catch (Throwable t) {
            SpotifyLog.w(TAG, "Error checking stack trace: " + t.getMessage());
        }

        return realPackageName;
    }
}

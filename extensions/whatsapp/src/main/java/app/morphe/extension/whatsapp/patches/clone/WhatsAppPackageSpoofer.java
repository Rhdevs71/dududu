/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.patches.clone;

import android.content.Context;

import app.morphe.extension.whatsapp.utils.WhatsAppLog;

public class WhatsAppPackageSpoofer {
    private static final String TAG = "WhatsAppPackageSpoofer";
    private static final String ORIGINAL_PACKAGE = "com.whatsapp";

    /**
     * Intercepts calls to Context.getPackageName() in WhatsApp bytecode.
     * When internal WhatsApp/Meta components check the package name for signature/integrity
     * or crypto identity verification, returns the original "com.whatsapp".
     */
    public static String getSpoofedPackageName(Context context, String realPackageName) {
        if (realPackageName == null || realPackageName.equals(ORIGINAL_PACKAGE)) {
            return ORIGINAL_PACKAGE;
        }

        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 2; i < Math.min(stack.length, 8); i++) {
                String className = stack[i].getClassName();
                if (className.startsWith("com.whatsapp") ||
                    className.startsWith("org.whispersystems") ||
                    className.startsWith("com.facebook") ||
                    className.contains("Signal")) {
                    return ORIGINAL_PACKAGE;
                }
            }
        } catch (Throwable t) {
            WhatsAppLog.w(TAG, "Error checking stack trace: " + t.getMessage());
        }

        return realPackageName;
    }
}

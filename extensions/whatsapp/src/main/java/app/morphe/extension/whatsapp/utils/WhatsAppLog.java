/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.utils;

import app.morphe.extension.crimera.PikoUtils;

public class WhatsAppLog {
    private static final String DEFAULT_TAG = "WhatsAppPiko";

    public static void d(String tag, Object message) {
        PikoUtils.logger(tag != null ? tag : DEFAULT_TAG, message != null ? message.toString() : "null");
    }

    public static void i(String tag, Object message) {
        PikoUtils.logger(tag != null ? tag : DEFAULT_TAG, message != null ? message.toString() : "null");
    }

    public static void w(String tag, Object message) {
        PikoUtils.logger(tag != null ? tag : DEFAULT_TAG, "[WARN] " + (message != null ? message.toString() : "null"));
    }

    public static void e(String tag, String message, Throwable t) {
        PikoUtils.logger(tag != null ? tag : DEFAULT_TAG, message, t);
    }

    public static void e(String tag, Throwable t) {
        PikoUtils.logger(tag != null ? tag : DEFAULT_TAG, t);
    }
}

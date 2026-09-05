/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.utils;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.shared.Logger;

public class PikoLog {
    public static void e(String tag, Object error) {
        PikoUtils.logger(tag, error);
    }

    public static void e(String tag, String message, Throwable t) {
        PikoUtils.logger(tag, message, t);
        if (t != null) {
            try {
                Logger.printException(() -> tag + ": " + (message != null ? message : ""), t);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void e(String tag, Throwable t) {
        PikoUtils.logger(tag, t);
        if (t != null) {
            try {
                Logger.printException(() -> tag + " error", t);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void toast(String tag, String message) {
        PikoUtils.logger(tag, "[TOAST] " + message);
        PikoUtils.toast(message);
    }
}

/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.theme;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import app.morphe.extension.crimera.settings.BooleanSetting;
import app.morphe.extension.crimera.sharedPreference.SharedPref;
import app.morphe.extension.instagram.utils.PikoLog;

public class RhpatchTextColorManager {
    private static final String TAG = "RhpatchTextColorManager";
    public static final BooleanSetting CUSTOM_TEXT_COLOR_ENABLED = new BooleanSetting("rhpatch_custom_text_color_enabled", false);
    public static final String PREF_COLOR_HEX = "rhpatch_custom_text_color_hex";

    public static final String DEFAULT_COLOR_HEX = "#FFD700"; // Luxury Gold
    private static final int TAG_ORIG_COLOR = 0x7e090001;

    public static final String[] PRESET_NAMES = new String[] {
        "Luxury Gold", "Emerald Green", "Sunset Magenta", "Cyber Cyan", "Royal Purple",
        "Vibrant Orange", "Rose Gold", "Electric Blue", "Neon Lime", "Pure White"
    };

    public static final String[] PRESET_COLORS = new String[] {
        "#FFD700", "#00E676", "#FF2E93", "#00E5FF", "#A855F7",
        "#FF7A00", "#F48FB1", "#2979FF", "#EEFF41", "#FFFFFF"
    };

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static boolean isScheduled = false;

    public static boolean isEnabled() {
        return Boolean.TRUE.equals(SharedPref.getBooleanPref(CUSTOM_TEXT_COLOR_ENABLED));
    }

    public static void setEnabled(boolean enabled) {
        SharedPref.setBooleanPref(CUSTOM_TEXT_COLOR_ENABLED.key, enabled);
    }

    public static String getColorHex() {
        return SharedPref.getStringPref(PREF_COLOR_HEX, DEFAULT_COLOR_HEX);
    }

    public static void setColorHex(String hex) {
        SharedPref.setStringPref(PREF_COLOR_HEX, hex);
    }

    public static int getParsedColor() {
        try {
            String hex = getColorHex();
            if (hex != null && !hex.isEmpty()) {
                if (!hex.startsWith("#")) {
                    hex = "#" + hex;
                }
                return Color.parseColor(hex);
            }
        } catch (Throwable t) {
            PikoLog.e(TAG, "Invalid color hex: " + getColorHex(), t);
        }
        return Color.parseColor(DEFAULT_COLOR_HEX);
    }

    public static void initActivity(final Activity activity) {
        if (activity == null) return;
        try {
            final View decorView = activity.getWindow().getDecorView();
            decorView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (isEnabled() && !isScheduled) {
                        isScheduled = true;
                        mainHandler.postDelayed(() -> {
                            isScheduled = false;
                            try {
                                applyToViewTree(decorView, isEnabled(), getParsedColor());
                            } catch (Throwable ignored) {}
                        }, 60);
                    }
                    return true;
                }
            });
            applyToViewTree(decorView, isEnabled(), getParsedColor());
        } catch (Throwable t) {
            PikoLog.e(TAG, "initActivity failed", t);
        }
    }

    public static void refreshNow(Activity activity) {
        if (activity == null) return;
        try {
            View decorView = activity.getWindow().getDecorView();
            applyToViewTree(decorView, isEnabled(), getParsedColor());
        } catch (Throwable t) {
            PikoLog.e(TAG, "refreshNow failed", t);
        }
    }

    public static void applyToViewTree(View view, boolean enabled, int targetColor) {
        if (view == null) return;

        // Skip our own floating capsule and dialog so they stay readable with their native themes
        Object tag = view.getTag();
        if (tag != null && tag.toString().startsWith("rhpatch_")) {
            return;
        }

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (enabled) {
                if (tv.getTag(TAG_ORIG_COLOR) == null) {
                    tv.setTag(TAG_ORIG_COLOR, tv.getTextColors());
                }
                tv.setTextColor(targetColor);
            } else {
                Object orig = tv.getTag(TAG_ORIG_COLOR);
                if (orig instanceof ColorStateList) {
                    tv.setTextColor((ColorStateList) orig);
                }
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                applyToViewTree(vg.getChildAt(i), enabled, targetColor);
            }
        }
    }
}

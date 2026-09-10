/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.theme;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import app.morphe.extension.crimera.settings.BooleanSetting;
import app.morphe.extension.crimera.sharedPreference.SharedPref;
import app.morphe.extension.instagram.utils.PikoLog;

public class RhpatchTextColorManager {
    private static final String TAG = "RhpatchTextColorManager";
    public static final BooleanSetting CUSTOM_TEXT_COLOR_ENABLED = new BooleanSetting("rhpatch_custom_text_color_enabled", false);
    public static final String PREF_COLOR_HEX = "rhpatch_custom_text_color_hex";

    public static final String DEFAULT_COLOR_HEX = "#FFD700"; // Luxury Gold
    private static final int TAG_ORIG_COLOR = 0x7e090001;
    private static final int TAG_LISTENER_ATTACHED = 0x7e090002;
    private static final int TAG_APPLIED_COLOR = 0x7e090003;

    public static final String[] PRESET_NAMES = new String[] {
        "Luxury Gold", "Emerald Green", "Sunset Magenta", "Cyber Cyan", "Royal Purple",
        "Vibrant Orange", "Rose Gold", "Electric Blue", "Neon Lime", "Pure White"
    };

    public static final String[] PRESET_COLORS = new String[] {
        "#FFD700", "#00E676", "#FF2E93", "#00E5FF", "#A855F7",
        "#FF7A00", "#F48FB1", "#2979FF", "#EEFF41", "#FFFFFF"
    };

    // Cache refleksi statis untuk performa instan tanpa melempar exception di UI Thread
    private static Field fgColorField = null;
    private static boolean fgColorFieldInit = false;

    private static Method mGetTextLayout = null;
    private static boolean mGetTextLayoutInit = false;

    private static Method mGetLayout = null;
    private static boolean mGetLayoutInit = false;

    private static Field fLayoutA02 = null;
    private static boolean fLayoutA02Init = false;

    private static Field fLayoutA0B = null;
    private static boolean fLayoutA0BInit = false;

    private static Field fRcA0Q = null;
    private static boolean fRcA0QInit = false;

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
            if (decorView.getTag(TAG_LISTENER_ATTACHED) == null) {
                decorView.setTag(TAG_LISTENER_ATTACHED, Boolean.TRUE);

                ViewTreeObserver vto = decorView.getViewTreeObserver();
                // Gunakan OnPreDrawListener terukur (200ms debounce) tanpa OnGlobalLayoutListener
                // untuk menjamin kestabilan 120 FPS tanpa drop frame / lag saat scroll
                vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    private long lastRun = 0;
                    @Override
                    public boolean onPreDraw() {
                        if (isEnabled()) {
                            long now = SystemClock.uptimeMillis();
                            if (now - lastRun > 200) {
                                lastRun = now;
                                try {
                                    applyToViewTree(decorView, true, getParsedColor());
                                } catch (Throwable ignored) {}
                            }
                        }
                        return true;
                    }
                });
            }

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

        // Lewatkan komponen internal RHpatch agar tetap terbaca
        Object tag = view.getTag();
        if (tag != null && tag.toString().startsWith("rhpatch_")) {
            return;
        }

        // 1. Tangani TextView standar (IgTextView, EditText, ActionTextView, dll.)
        if (view instanceof TextView) {
            processTextView((TextView) view, enabled, targetColor);
            return;
        }

        // 2. Strict Type Guard untuk ViewGroup:
        // Kontainer tata letak (FrameLayout, LinearLayout, RecyclerView, dll.) HANYA menelusuri anaknya.
        // SAMA SEKALI TIDAK menjalankan refleksi pada ViewGroup!
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                applyToViewTree(vg.getChildAt(i), enabled, targetColor);
            }
            return;
        }

        // 3. View Daun Non-TextView (hanya proses jika merupakan IgTextLayoutView atau RCTextView)
        String className = view.getClass().getName();
        if (className.contains("IgTextLayoutView") || className.contains("RCTextView")) {
            processCustomTextView(view, enabled, targetColor, className);
        }
    }

    private static void processTextView(TextView tv, boolean enabled, int targetColor) {
        try {
            if (enabled) {
                // Periksa apakah view ini sudah terwarnai dengan warna yang sama (skip instan 0ms)
                Object applied = tv.getTag(TAG_APPLIED_COLOR);
                if (applied instanceof Integer && (Integer) applied == targetColor && tv.getCurrentTextColor() == targetColor) {
                    return;
                }

                if (tv.getTag(TAG_ORIG_COLOR) == null) {
                    tv.setTag(TAG_ORIG_COLOR, tv.getTextColors());
                }

                if (tv.getCurrentTextColor() != targetColor) {
                    tv.setTextColor(targetColor);
                }

                try {
                    tv.setLinkTextColor(targetColor);
                } catch (Throwable ignored) {}

                try {
                    int hintColor = Color.argb(140, Color.red(targetColor), Color.green(targetColor), Color.blue(targetColor));
                    tv.setHintTextColor(hintColor);
                } catch (Throwable ignored) {}

                // Menangani teks berformat Spanned (username di komentar, mention @, hashtag #)
                CharSequence text = tv.getText();
                if (text instanceof Spanned && text.length() > 0) {
                    recolorSpanned((Spanned) text, targetColor);
                }

                tv.setTag(TAG_APPLIED_COLOR, targetColor);
            } else {
                tv.setTag(TAG_APPLIED_COLOR, null);
                Object orig = tv.getTag(TAG_ORIG_COLOR);
                if (orig instanceof ColorStateList) {
                    tv.setTextColor((ColorStateList) orig);
                    tv.setTag(TAG_ORIG_COLOR, null);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void processCustomTextView(View view, boolean enabled, int targetColor, String className) {
        try {
            if (enabled) {
                // Periksa apakah view ini sudah terwarnai dengan warna yang sama (skip instan 0ms)
                Object applied = view.getTag(TAG_APPLIED_COLOR);
                if (applied instanceof Integer && (Integer) applied == targetColor) {
                    return;
                }

                Layout layout = extractLayoutCached(view, className);
                if (layout != null) {
                    TextPaint paint = layout.getPaint();
                    if (paint != null) {
                        boolean modified = false;
                        if (paint.getColor() != targetColor) {
                            if (view.getTag(TAG_ORIG_COLOR) == null) {
                                view.setTag(TAG_ORIG_COLOR, paint.getColor());
                            }
                            paint.setColor(targetColor);
                            modified = true;
                        }
                        if (paint.linkColor != targetColor) {
                            paint.linkColor = targetColor;
                            modified = true;
                        }

                        CharSequence text = layout.getText();
                        if (text instanceof Spanned && text.length() > 0) {
                            recolorSpanned((Spanned) text, targetColor);
                        }

                        if (modified) {
                            view.invalidate();
                        }
                    }
                }

                // Tangani field ColorStateList A0Q pada RCTextView
                if (className.contains("RCTextView")) {
                    try {
                        if (!fRcA0QInit) {
                            fRcA0QInit = true;
                            fRcA0Q = view.getClass().getDeclaredField("A0Q");
                            fRcA0Q.setAccessible(true);
                        }
                        if (fRcA0Q != null) {
                            fRcA0Q.set(view, ColorStateList.valueOf(targetColor));
                        }
                    } catch (Throwable ignored) {}
                }

                view.setTag(TAG_APPLIED_COLOR, targetColor);
            } else {
                view.setTag(TAG_APPLIED_COLOR, null);
                Object orig = view.getTag(TAG_ORIG_COLOR);
                if (orig instanceof Integer) {
                    Layout layout = extractLayoutCached(view, className);
                    if (layout != null && layout.getPaint() != null) {
                        layout.getPaint().setColor((Integer) orig);
                        view.setTag(TAG_ORIG_COLOR, null);
                        view.invalidate();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Layout extractLayoutCached(View view, String className) {
        if (view == null) return null;

        Class<?> cls = view.getClass();

        // 1. IgTextLayoutView: coba getTextLayout() atau field A02
        if (className.contains("IgTextLayoutView")) {
            try {
                if (!mGetTextLayoutInit) {
                    mGetTextLayoutInit = true;
                    mGetTextLayout = cls.getMethod("getTextLayout");
                }
                if (mGetTextLayout != null) {
                    Object res = mGetTextLayout.invoke(view);
                    if (res instanceof Layout) return (Layout) res;
                }
            } catch (Throwable ignored) {}

            try {
                if (!fLayoutA02Init) {
                    fLayoutA02Init = true;
                    fLayoutA02 = cls.getDeclaredField("A02");
                    fLayoutA02.setAccessible(true);
                }
                if (fLayoutA02 != null) {
                    Object res = fLayoutA02.get(view);
                    if (res instanceof Layout) return (Layout) res;
                }
            } catch (Throwable ignored) {}
        }

        // 2. RCTextView: coba getLayout() atau field A0B
        if (className.contains("RCTextView")) {
            try {
                if (!mGetLayoutInit) {
                    mGetLayoutInit = true;
                    mGetLayout = cls.getMethod("getLayout");
                }
                if (mGetLayout != null) {
                    Object res = mGetLayout.invoke(view);
                    if (res instanceof Layout) return (Layout) res;
                }
            } catch (Throwable ignored) {}

            try {
                if (!fLayoutA0BInit) {
                    fLayoutA0BInit = true;
                    fLayoutA0B = cls.getDeclaredField("A0B");
                    fLayoutA0B.setAccessible(true);
                }
                if (fLayoutA0B != null) {
                    Object res = fLayoutA0B.get(view);
                    if (res instanceof Layout) return (Layout) res;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private static void recolorSpanned(Spanned spanned, int targetColor) {
        if (spanned == null || spanned.length() == 0) return;
        try {
            // Hanya ambil ForegroundColorSpan secara terarah tanpa Object.class generik
            ForegroundColorSpan[] spans = spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class);
            if (spans != null && spans.length > 0) {
                if (!fgColorFieldInit) {
                    fgColorFieldInit = true;
                    try {
                        fgColorField = ForegroundColorSpan.class.getDeclaredField("mColor");
                        fgColorField.setAccessible(true);
                    } catch (Throwable ignored) {}
                }

                if (fgColorField != null) {
                    for (ForegroundColorSpan span : spans) {
                        try {
                            fgColorField.setInt(span, targetColor);
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}

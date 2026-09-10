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
import android.text.style.ClickableSpan;
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

    public static final String[] PRESET_NAMES = new String[] {
        "Luxury Gold", "Emerald Green", "Sunset Magenta", "Cyber Cyan", "Royal Purple",
        "Vibrant Orange", "Rose Gold", "Electric Blue", "Neon Lime", "Pure White"
    };

    public static final String[] PRESET_COLORS = new String[] {
        "#FFD700", "#00E676", "#FF2E93", "#00E5FF", "#A855F7",
        "#FF7A00", "#F48FB1", "#2979FF", "#EEFF41", "#FFFFFF"
    };

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Field fgColorField = null;
    private static boolean fgColorFieldInit = false;

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
                vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    private long lastRun = 0;
                    @Override
                    public boolean onPreDraw() {
                        if (isEnabled()) {
                            long now = SystemClock.uptimeMillis();
                            if (now - lastRun > 100) {
                                lastRun = now;
                                try {
                                    applyToViewTree(decorView, true, getParsedColor());
                                } catch (Throwable ignored) {}
                            }
                        }
                        return true;
                    }
                });

                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (isEnabled()) {
                            try {
                                applyToViewTree(decorView, true, getParsedColor());
                            } catch (Throwable ignored) {}
                        }
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

        // 1. Tangani TextView standar (dan subclass-nya: IgTextView, EditText, dll.)
        if (view instanceof TextView) {
            processTextView((TextView) view, enabled, targetColor);
        } else {
            // 2. Tangani RCTextView (RenderCore/Litho bio), IgTextLayoutView (caption feed/komentar),
            //    dan view kustom lainnya yang menggambar Layout / TextPaint secara mandiri
            processLayoutOrCustomView(view, enabled, targetColor);
        }

        // 3. Telusuri anak-anak jika merupakan ViewGroup
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                applyToViewTree(vg.getChildAt(i), enabled, targetColor);
            }
        }
    }

    private static void processTextView(TextView tv, boolean enabled, int targetColor) {
        try {
            if (enabled) {
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
            } else {
                Object orig = tv.getTag(TAG_ORIG_COLOR);
                if (orig instanceof ColorStateList) {
                    tv.setTextColor((ColorStateList) orig);
                    tv.setTag(TAG_ORIG_COLOR, null);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void processLayoutOrCustomView(View view, boolean enabled, int targetColor) {
        try {
            String className = view.getClass().getName();

            Layout layout = extractLayout(view);
            if (layout != null) {
                TextPaint paint = layout.getPaint();
                if (paint != null) {
                    if (enabled) {
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
                    } else {
                        Object orig = view.getTag(TAG_ORIG_COLOR);
                        if (orig instanceof Integer) {
                            paint.setColor((Integer) orig);
                            view.setTag(TAG_ORIG_COLOR, null);
                            view.invalidate();
                        }
                    }
                }
            }

            // Tangani Paint kustom atau getPaint() pada view jika ada
            try {
                Method getPaintMethod = view.getClass().getMethod("getPaint");
                Object p = getPaintMethod.invoke(view);
                if (p instanceof Paint) {
                    Paint paint = (Paint) p;
                    if (enabled) {
                        if (paint.getColor() != targetColor) {
                            paint.setColor(targetColor);
                            if (paint instanceof TextPaint) {
                                ((TextPaint) paint).linkColor = targetColor;
                            }
                            view.invalidate();
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Tangani field ColorStateList A0Q pada RCTextView
            if (className.contains("RCTextView")) {
                try {
                    Field f = view.getClass().getDeclaredField("A0Q");
                    f.setAccessible(true);
                    f.set(view, ColorStateList.valueOf(targetColor));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static Layout extractLayout(View view) {
        if (view == null) return null;

        // 1. Coba panggil method publik / declared
        try {
            Method m = view.getClass().getMethod("getTextLayout");
            Object res = m.invoke(view);
            if (res instanceof Layout) return (Layout) res;
        } catch (Throwable ignored) {}

        try {
            Method m = view.getClass().getMethod("getLayout");
            Object res = m.invoke(view);
            if (res instanceof Layout) return (Layout) res;
        } catch (Throwable ignored) {}

        // 2. Coba field yang sering digunakan Instagram (A02 di IgTextLayoutView, A0B di RCTextView, mLayout)
        try {
            Field f = view.getClass().getDeclaredField("A02");
            f.setAccessible(true);
            Object res = f.get(view);
            if (res instanceof Layout) return (Layout) res;
        } catch (Throwable ignored) {}

        try {
            Field f = view.getClass().getDeclaredField("A0B");
            f.setAccessible(true);
            Object res = f.get(view);
            if (res instanceof Layout) return (Layout) res;
        } catch (Throwable ignored) {}

        try {
            Field f = view.getClass().getDeclaredField("mLayout");
            f.setAccessible(true);
            Object res = f.get(view);
            if (res instanceof Layout) return (Layout) res;
        } catch (Throwable ignored) {}

        // 3. Fallback: telusuri field yang bertipe Layout
        try {
            Class<?> curr = view.getClass();
            while (curr != null && curr != View.class && curr != Object.class) {
                for (Field f : curr.getDeclaredFields()) {
                    if (Layout.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object res = f.get(view);
                        if (res instanceof Layout) return (Layout) res;
                    }
                }
                curr = curr.getSuperclass();
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static void recolorSpanned(Spanned spanned, int targetColor) {
        if (spanned == null || spanned.length() == 0) return;
        try {
            Object[] spans = spanned.getSpans(0, spanned.length(), Object.class);
            if (spans != null && spans.length > 0) {
                for (Object span : spans) {
                    recolorSpan(span, targetColor);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void recolorSpan(Object span, int targetColor) {
        if (span == null) return;

        // Inisialisasi cache field mColor pada ForegroundColorSpan
        if (!fgColorFieldInit) {
            fgColorFieldInit = true;
            try {
                fgColorField = ForegroundColorSpan.class.getDeclaredField("mColor");
                fgColorField.setAccessible(true);
            } catch (Throwable ignored) {}
        }

        // 1. Jika merupakan ForegroundColorSpan atau turunannya
        if (span instanceof ForegroundColorSpan && fgColorField != null) {
            try {
                fgColorField.setInt(span, targetColor);
            } catch (Throwable ignored) {}
        }

        // 2. Periksa field int warna kustom pada span obfuscated Instagram
        try {
            Class<?> cls = span.getClass();
            if (cls != ForegroundColorSpan.class) {
                for (Field f : cls.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        f.setAccessible(true);
                        int val = f.getInt(span);
                        // Nilai warna ARGB memiliki alpha 0xFF (val < -1 atau val > 0x00FFFFFF)
                        if ((val & 0xFF000000) != 0 && (val < -1 || val > 0x00FFFFFF)) {
                            f.setInt(span, targetColor);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}

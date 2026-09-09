/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.settings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import app.morphe.extension.crimera.PikoUtils;

public class SpotifySettingsInjector {
    private static final String TAG_PIKO_BTN = "piko_spotify_settings_btn";

    /**
     * Injects the floating Piko settings pill onto the Spotify main activity decor view.
     */
    public static void onActivityResume(Activity activity) {
        if (activity == null) return;

        try {
            activity.getWindow().getDecorView().post(() -> {
                try {
                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    if (decorView.findViewWithTag(TAG_PIKO_BTN) != null) {
                        return; // Already present
                    }

                    TextView btn = new TextView(activity);
                    btn.setTag(TAG_PIKO_BTN);
                    btn.setText("Piko");
                    btn.setTextColor(Color.WHITE);
                    btn.setTextSize(11f);
                    btn.setTypeface(Typeface.DEFAULT_BOLD);
                    btn.setGravity(Gravity.CENTER);

                    // Spotify Green capsule (#1DB954)
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor("#1DB954"));
                    bg.setCornerRadius(30f);
                    bg.setStroke(2, Color.parseColor("#15883e"));
                    btn.setBackground(bg);
                    btn.setElevation(16f);

                    float density = activity.getResources().getDisplayMetrics().density;
                    int width = (int) (52 * density);
                    int height = (int) (26 * density);
                    int margin = (int) (14 * density);
                    int topOffset = (int) (36 * density); // Positioned comfortably below system status bar

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
                    lp.gravity = Gravity.TOP | Gravity.END;
                    lp.topMargin = topOffset;
                    lp.rightMargin = margin;
                    btn.setLayoutParams(lp);

                    btn.setOnClickListener(v -> SpotifySettingsDialog.show(activity));

                    decorView.addView(btn);
                } catch (Throwable t) {
                    PikoUtils.logger("SpotifySettingsInjector", "Error attaching Piko button: " + t.getMessage(), t);
                }
            });
        } catch (Throwable ignored) {}
    }
}

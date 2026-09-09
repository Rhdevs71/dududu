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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import app.morphe.extension.crimera.PikoUtils;

public class SpotifySettingsInjector {
    private static final String TAG_PIKO_BTN = "rhpatch_spotify_settings_btn";

    /**
     * Injects the sleek, draggable floating RHpatch pill onto the Spotify main activity decor view.
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

                    float density = activity.getResources().getDisplayMetrics().density;

                    TextView btn = new TextView(activity);
                    btn.setTag(TAG_PIKO_BTN);
                    btn.setText("RHpatch");
                    btn.setTextColor(Color.WHITE);
                    btn.setTextSize(12f);
                    btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    btn.setGravity(Gravity.CENTER);

                    // Premium Dark Capsule: #181818 with Spotify Green accent border
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor("#E6181818")); // Translucent charcoal
                    bg.setCornerRadius(24 * density);
                    bg.setStroke((int) (1.5f * density), Color.parseColor("#1DB954")); // Spotify Green accent border
                    btn.setBackground(bg);
                    btn.setElevation(16f * density);

                    int hPad = (int) (14 * density);
                    int vPad = (int) (6 * density);
                    btn.setPadding(hPad, vPad, hPad, vPad);

                    int topOffset = (int) (52 * density); // Positioned comfortably below system status bar
                    int rightMargin = (int) (12 * density);

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    lp.gravity = Gravity.TOP | Gravity.END;
                    lp.topMargin = topOffset;
                    lp.rightMargin = rightMargin;
                    btn.setLayoutParams(lp);

                    // Touch drag listener: allows user to freely drag the pill vertically along the screen edge
                    btn.setOnTouchListener(new View.OnTouchListener() {
                        private float dY = 0f;
                        private float startY = 0f;
                        private boolean isDragging = false;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getActionMasked()) {
                                case MotionEvent.ACTION_DOWN:
                                    dY = v.getY() - event.getRawY();
                                    startY = event.getRawY();
                                    isDragging = false;
                                    return true;

                                case MotionEvent.ACTION_MOVE:
                                    if (Math.abs(event.getRawY() - startY) > 8 * density) {
                                        isDragging = true;
                                    }
                                    if (isDragging) {
                                        float newY = event.getRawY() + dY;
                                        int screenH = decorView.getHeight();
                                        int btnH = v.getHeight();
                                        if (newY >= (30 * density) && newY <= (screenH - btnH - (int) (70 * density))) {
                                            v.setY(newY);
                                        }
                                    }
                                    return true;

                                case MotionEvent.ACTION_UP:
                                    if (!isDragging) {
                                        v.performClick();
                                    }
                                    return true;
                            }
                            return false;
                        }
                    });

                    btn.setOnClickListener(v -> SpotifySettingsDialog.show(activity));

                    decorView.addView(btn);
                } catch (Throwable t) {
                    PikoUtils.logger("SpotifySettingsInjector", "Error attaching Piko button: " + t.getMessage(), t);
                }
            });
        } catch (Throwable ignored) {}
    }
}

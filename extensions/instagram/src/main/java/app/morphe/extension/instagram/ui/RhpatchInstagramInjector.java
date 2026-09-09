/*
 * Copyright (C) 2026 RHpatch <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.instagram.theme.RhpatchTextColorManager;

public class RhpatchInstagramInjector {
    public static final String TAG_RHPATCH_BTN = "rhpatch_instagram_settings_btn";
    private static boolean lifecycleRegistered = false;

    /**
     * Mengatur visibilitas tombol kapsul ● RHpatch secara real-time.
     */
    public static void setCapsuleVisibility(Context context, final int visibility) {
        if (context == null) return;
        Activity activity = getActivity(context);
        if (activity == null) return;

        try {
            activity.runOnUiThread(() -> {
                try {
                    View decorView = activity.getWindow().getDecorView();
                    View btn = decorView.findViewWithTag(TAG_RHPATCH_BTN);
                    if (btn != null) {
                        btn.setVisibility(visibility);
                    }
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
    }

    private static Activity getActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    /**
     * Menyematkan tombol kapsul mengambang draggable ● RHpatch pada DecorView activity Instagram.
     * Default: GONE (hanya muncul saat berada di halaman/tab Profil).
     */
    public static void onActivityResume(final Activity activity) {
        if (activity == null) return;

        try {
            // Inisialisasi mesin warna teks kustom jika aktif
            RhpatchTextColorManager.initActivity(activity);

            // Pasang lifecycle monitor untuk mendeteksi kapan pengguna masuk / keluar dari halaman profil
            if (activity instanceof FragmentActivity && !lifecycleRegistered) {
                try {
                    FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
                    fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                        @Override
                        public void onFragmentResumed(FragmentManager fm, Fragment f) {
                            String name = f.getClass().getName();
                            if (name.contains("UserDetailFragment") || name.contains("Profile")) {
                                setCapsuleVisibility(activity, View.VISIBLE);
                            } else {
                                setCapsuleVisibility(activity, View.GONE);
                            }
                        }
                    }, true);
                    lifecycleRegistered = true;
                } catch (Throwable ignored) {}
            }

            activity.getWindow().getDecorView().post(() -> {
                try {
                    final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    if (decorView.findViewWithTag(TAG_RHPATCH_BTN) != null) {
                        return; // Sudah terpasang
                    }

                    float density = activity.getResources().getDisplayMetrics().density;

                    TextView btn = new TextView(activity);
                    btn.setTag(TAG_RHPATCH_BTN);
                    btn.setText("● RHpatch");
                    btn.setTextColor(Color.WHITE);
                    btn.setTextSize(12f);
                    btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    btn.setGravity(Gravity.CENTER);

                    // PENTING: Awal mula GONE agar tab Beranda, Reels, Explore, DM 100% bersih!
                    btn.setVisibility(View.GONE);

                    // Premium Dark Capsule: #181818 dengan border Neon Sunset Magenta (#E1306C)
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor("#E6181818")); // Translucent charcoal
                    bg.setCornerRadius(24 * density);
                    bg.setStroke((int) (1.5f * density), Color.parseColor("#E1306C")); // Instagram Sunset Neon Magenta
                    btn.setBackground(bg);
                    btn.setElevation(16f * density);

                    int hPad = (int) (14 * density);
                    int vPad = (int) (6 * density);
                    btn.setPadding(hPad, vPad, hPad, vPad);

                    int topOffset = (int) (60 * density);
                    int rightMargin = (int) (12 * density);

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    lp.gravity = Gravity.TOP | Gravity.END;
                    lp.topMargin = topOffset;
                    lp.rightMargin = rightMargin;
                    btn.setLayoutParams(lp);

                    // Touch drag listener
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

                    btn.setOnClickListener(v -> RhpatchInstagramDialog.show(activity));

                    decorView.addView(btn);
                } catch (Throwable t) {
                    PikoUtils.logger("RhpatchInstagramInjector", "Error attaching RHpatch button: " + t.getMessage(), t);
                }
            });
        } catch (Throwable ignored) {}
    }
}

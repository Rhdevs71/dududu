/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.facebook.patches.FacebookAppLockManager;
import app.morphe.extension.facebook.patches.FacebookLinkManager;
import app.morphe.extension.facebook.settings.FacebookPref;
import app.morphe.extension.shared.Utils;

public class RhpatchFacebookInjector {

    public static final String TAG_RHPATCH_BTN = "rhpatch_facebook_settings_btn";
    private static boolean sAppLifecycleRegistered = false;

    /**
     * Diinjeksi di FacebookApplication.onCreate() untuk memantau SEMUA Activity Facebook secara global.
     */
    public static void initApplication(Application application) {
        if (application == null || sAppLifecycleRegistered) return;
        try {
            Utils.setContext(application);
            PikoUtils.setContext(application);

            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    Utils.setActivity(activity);
                    FacebookLinkManager.checkAndInterceptBrowser(activity);
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    Utils.setActivity(activity);
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    onActivityResume(activity);
                }

                @Override
                public void onActivityPaused(Activity activity) {}

                @Override
                public void onActivityStopped(Activity activity) {
                    onActivityStop(activity);
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

                @Override
                public void onActivityDestroyed(Activity activity) {}
            });
            sAppLifecycleRegistered = true;
            PikoUtils.logger("RhpatchFacebookInjector", "ActivityLifecycleCallbacks successfully registered on FacebookApplication!");
        } catch (Throwable t) {
            PikoUtils.logger("RhpatchFacebookInjector", "Failed to register ActivityLifecycleCallbacks", t);
        }
    }

    public static void setCapsuleVisibility(Context context, final int visibility) {
        if (context == null) return;
        final Activity activity = getActivity(context);
        if (activity == null) return;

        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        View decorView = activity.getWindow().getDecorView();
                        View btn = decorView.findViewWithTag(TAG_RHPATCH_BTN);
                        if (btn != null) {
                            btn.setVisibility(visibility);
                        }
                    } catch (Throwable ignored) {}
                }
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

    public static void onActivityResume(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        try {
            // Update Activity dan Context global
            Utils.setActivity(activity);
            PikoUtils.setContext(activity.getApplicationContext());

            // 1. Eksekusi App Lock Biometrik jika aktif
            FacebookAppLockManager.onActivityResumed(activity);

            // 2. Intercept External Browser jika ini adalah BrowserLiteActivity
            FacebookLinkManager.checkAndInterceptBrowser(activity);

            // 3. Tampilkan tombol kapsul overlay [RHpatch]
            if (!FacebookPref.isShowProfileCapsule()) {
                setCapsuleVisibility(activity, View.GONE);
                return;
            }

            activity.getWindow().getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (activity.isFinishing()) return;

                        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                        if (decorView == null) return;

                        View existing = decorView.findViewWithTag(TAG_RHPATCH_BTN);
                        if (existing != null) {
                            existing.setVisibility(View.VISIBLE);
                            return; // Sudah terpasang, pastikan terlihat
                        }

                        final float density = activity.getResources().getDisplayMetrics().density;

                        final TextView btn = new TextView(activity);
                        btn.setTag(TAG_RHPATCH_BTN);
                        btn.setText("🛡️ [RHpatch]");
                        btn.setTextColor(Color.WHITE);
                        btn.setTextSize(12.5f);
                        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                        btn.setGravity(Gravity.CENTER);
                        btn.setVisibility(View.VISIBLE);

                        // Styling Sleek Dark Glass dengan aksen Facebook Royal Blue (#1877F2)
                        GradientDrawable bg = new GradientDrawable();
                        bg.setColor(Color.parseColor("#E6111827")); // Deep dark charcoal
                        bg.setCornerRadius(24 * density);
                        bg.setStroke((int) (1.8f * density), Color.parseColor("#1877F2")); // Facebook Royal Blue
                        btn.setBackground(bg);
                        btn.setElevation(16f * density);

                        int hPad = (int) (14 * density);
                        int vPad = (int) (8 * density);
                        btn.setPadding(hPad, vPad, hPad, vPad);

                        int topOffset = (int) (120 * density);
                        int rightMargin = (int) (12 * density);

                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        lp.gravity = Gravity.TOP | Gravity.END;
                        lp.topMargin = topOffset;
                        lp.rightMargin = rightMargin;
                        btn.setLayoutParams(lp);

                        // Vertical touch drag listener
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
                                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
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
                                    case MotionEvent.ACTION_CANCEL:
                                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                                        if (!isDragging) {
                                            showMenu(activity);
                                        }
                                        return true;
                                }
                                return false;
                            }
                        });

                        decorView.addView(btn);
                    } catch (Throwable t) {
                        PikoUtils.logger("RhpatchFacebookInjector", "Error attaching RHpatch button: " + t.getMessage(), t);
                    }
                }
            });
        } catch (Throwable t) {
            PikoUtils.logger("RhpatchFacebookInjector", "Failed onActivityResume", t);
        }
    }

    public static void onActivityStop(Activity activity) {
        FacebookAppLockManager.onActivityStopped(activity);
    }

    public static void showMenu(Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            try {
                RhpatchFacebookDialog.show(activity);
            } catch (Throwable t) {
                PikoUtils.logger("RhpatchFacebookInjector", "Failed to show RhpatchFacebookDialog", t);
            }
        }
    }
}

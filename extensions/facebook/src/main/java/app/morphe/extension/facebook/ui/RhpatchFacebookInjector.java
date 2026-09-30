/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.facebook.settings.FacebookPref;

public class RhpatchFacebookInjector {

    private static final String TAG = "RhpatchFacebookInjector";
    private static final String TAG_CAPSULE_BUTTON = "rhpatch_facebook_capsule_btn";
    private static final Map<Activity, Boolean> sRegisteredActivities = new WeakHashMap<>();

    private static float sSavedButtonY = -1f;

    public static void onActivityResume(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    onActivityResume(activity);
                }
            });
            return;
        }

        try {
            ensureCapsuleButtonAttached(activity);
            ensureFragmentLifecycleRegistered(activity);
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Failed onActivityResume", t);
        }
    }

    public static void showMenu(Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            try {
                RhpatchFacebookDialog.show(activity);
            } catch (Throwable t) {
                PikoUtils.logger(TAG, "Failed to show RhpatchFacebookDialog", t);
            }
        }
    }

    private static void ensureCapsuleButtonAttached(final Activity activity) {
        try {
            View decorView = activity.getWindow().getDecorView();
            if (!(decorView instanceof ViewGroup)) {
                return;
            }

            ViewGroup root = (ViewGroup) decorView;
            View existing = root.findViewWithTag(TAG_CAPSULE_BUTTON);
            if (existing != null) {
                updateCapsuleVisibility(activity, existing);
                return;
            }

            final View capsule = createCapsuleButton(activity);
            capsule.setTag(TAG_CAPSULE_BUTTON);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            int marginEnd = dpToPx(activity, 16);
            int marginBottom = dpToPx(activity, 96); // Above bottom nav tab bar
            params.setMargins(0, 0, marginEnd, marginBottom);

            root.addView(capsule, params);

            if (sSavedButtonY > 0) {
                capsule.post(new Runnable() {
                    @Override
                    public void run() {
                        capsule.setY(sSavedButtonY);
                    }
                });
            }

            updateCapsuleVisibility(activity, capsule);
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Error attaching capsule button", t);
        }
    }

    private static View createCapsuleButton(final Activity activity) {
        final Context context = activity;

        final LinearLayout capsule = new LinearLayout(context);
        capsule.setOrientation(LinearLayout.HORIZONTAL);
        capsule.setGravity(Gravity.CENTER_VERTICAL);
        capsule.setPadding(dpToPx(context, 14), dpToPx(context, 8), dpToPx(context, 14), dpToPx(context, 8));

        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { Color.parseColor("#E60B1015"), Color.parseColor("#E6182438") }
        );
        bg.setCornerRadius(dpToPx(context, 20));
        bg.setStroke(dpToPx(context, 1.2f), Color.parseColor("#4D1877F2"));
        capsule.setBackground(bg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            capsule.setElevation(dpToPx(context, 8));
        }

        TextView icon = new TextView(context);
        icon.setText("🛡️");
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        capsule.addView(icon);

        TextView label = new TextView(context);
        label.setText(" RHpatch");
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        capsule.addView(label);

        final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        capsule.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX, downRawY;
            private float startX, startY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = v.getX();
                        startY = v.getY();
                        isDragging = false;
                        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(100).start();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - downRawX;
                        float deltaY = event.getRawY() - downRawY;

                        if (!isDragging && Math.hypot(deltaX, deltaY) > touchSlop) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            ViewGroup parent = (ViewGroup) v.getParent();
                            if (parent != null) {
                                float newX = startX + deltaX;
                                float newY = startY + deltaY;

                                float maxX = parent.getWidth() - v.getWidth();
                                float maxY = parent.getHeight() - v.getHeight();

                                newX = Math.max(0, Math.min(newX, maxX));
                                newY = Math.max(0, Math.min(newY, maxY));

                                v.setX(newX);
                                v.setY(newY);
                                sSavedButtonY = newY;
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                        if (!isDragging) {
                            showMenu(activity);
                        }
                        return true;
                }
                return false;
            }
        });

        return capsule;
    }

    private static void ensureFragmentLifecycleRegistered(final Activity activity) {
        if (!(activity instanceof FragmentActivity)) {
            return;
        }

        if (sRegisteredActivities.containsKey(activity)) {
            return;
        }

        try {
            FragmentActivity fa = (FragmentActivity) activity;
            fa.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(FragmentManager fm, Fragment f) {
                        handleFragmentChange(activity);
                    }

                    @Override
                    public void onFragmentStarted(FragmentManager fm, Fragment f) {
                        handleFragmentChange(activity);
                    }

                    @Override
                    public void onFragmentPaused(FragmentManager fm, Fragment f) {
                        handleFragmentChange(activity);
                    }
                },
                true
            );
            sRegisteredActivities.put(activity, Boolean.TRUE);
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Failed to register FragmentLifecycleCallbacks", t);
        }
    }

    private static void handleFragmentChange(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View decorView = activity.getWindow().getDecorView();
                    if (decorView instanceof ViewGroup) {
                        View capsule = ((ViewGroup) decorView).findViewWithTag(TAG_CAPSULE_BUTTON);
                        if (capsule != null) {
                            updateCapsuleVisibility(activity, capsule);
                        }
                    }
                } catch (Throwable t) {
                    PikoUtils.logger(TAG, "Error updating capsule on fragment change", t);
                }
            }
        });
    }

    private static void updateCapsuleVisibility(Activity activity, View capsule) {
        if (capsule == null) {
            return;
        }

        try {
            if (!FacebookPref.isShowProfileCapsule()) {
                capsule.setVisibility(View.GONE);
                return;
            }

            if (FacebookPref.alwaysShowFloatingButton()) {
                capsule.setVisibility(View.VISIBLE);
                return;
            }

            boolean isProfileActive = isProfileScreenActive(activity);
            capsule.setVisibility(isProfileActive ? View.VISIBLE : View.GONE);
        } catch (Throwable t) {
            capsule.setVisibility(View.VISIBLE);
        }
    }

    private static boolean isProfileScreenActive(Activity activity) {
        if (activity == null) {
            return false;
        }

        String actName = activity.getClass().getName();
        if (actName.contains("Profile") || actName.contains("Timeline")) {
            return true;
        }

        if (activity instanceof FragmentActivity) {
            try {
                FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
                List<Fragment> fragments = fm.getFragments();
                if (fragments != null) {
                    for (Fragment f : fragments) {
                        if (f != null && f.isResumed() && !f.isHidden()) {
                            String fragName = f.getClass().getName();
                            if (fragName.contains("Profile") || fragName.contains("Timeline")) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private static int dpToPx(Context context, float dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }
}

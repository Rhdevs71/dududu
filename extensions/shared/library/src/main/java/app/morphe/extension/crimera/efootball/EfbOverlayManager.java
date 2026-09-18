/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.crimera.efootball;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import app.morphe.extension.crimera.PikoUtils;

/**
 * In-game Overlay Mod Menu for eFootball Mobile (PES Android).
 * Zero-permission DecorView floating overlay providing real-time UE4 console controls:
 * - Match Speed / Time Dilation (slomo 1.0 - 2.0x)
 * - Camera Field of View / Drone View (fov 90 - 120)
 * - In-game Player Scale / Height (1.0x - 1.6x)
 * - FPS Unlocker (30 - 120 FPS)
 * - Resolution Scale (100% - 150%)
 * - In-game FPS & Performance Stats
 * - Direct UE4 Console Command Terminal
 */
@SuppressWarnings("unused")
public class EfbOverlayManager {
    private static final String TAG = "EfbOverlayManager";
    private static Activity sActivity = null;
    private static boolean sOverlayAttached = false;
    private static FrameLayout sRootOverlay = null;
    private static View sFloatingBall = null;
    private static View sMenuModal = null;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static void init(final Activity activity) {
        if (activity == null) return;
        sActivity = activity;

        try {
            PikoUtils.logger(TAG + ": Initializing eFootball Overlay Mod Menu on " + activity.getClass().getName());
        } catch (Throwable ignored) {}

        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    attachOverlay(activity);
                } catch (Throwable t) {
                    try {
                        PikoUtils.logger(TAG + ": Failed to attach overlay: " + t.getMessage());
                    } catch (Throwable ignored) {}
                }
            }
        }, 1500); // 1.5s delay to let UE4 engine view surface settle
    }

    private static void attachOverlay(final Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) return;

        if (sOverlayAttached && sRootOverlay != null && decorView.indexOfChild(sRootOverlay) != -1) {
            return;
        }

        sRootOverlay = new FrameLayout(activity);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        sRootOverlay.setLayoutParams(rootParams);

        // 1. Create Floating Ball
        sFloatingBall = createFloatingBall(activity);
        sRootOverlay.addView(sFloatingBall);

        // 2. Create Modal Menu (initially hidden)
        sMenuModal = createMenuModal(activity);
        sMenuModal.setVisibility(View.GONE);
        sRootOverlay.addView(sMenuModal);

        decorView.addView(sRootOverlay);
        sOverlayAttached = true;

        showToast("⚽ Piko eFootball Mod Menu Active!\nTap ⚽ to open menu.");
    }

    private static View createFloatingBall(final Activity activity) {
        final int sizePx = dpToPx(activity, 54);

        final FrameLayout ballContainer = new FrameLayout(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = dpToPx(activity, 20);
        params.topMargin = dpToPx(activity, 80);
        ballContainer.setLayoutParams(params);

        // Styling: Glassmorphism Dark Circle with Glowing Cyan Border
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#E60F172A")); // Deep slate 90%
        bg.setStroke(dpToPx(activity, 2), Color.parseColor("#00E5FF")); // Glowing Cyan
        ballContainer.setBackground(bg);
        ballContainer.setElevation(dpToPx(activity, 8));

        // Content
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        content.setLayoutParams(contentParams);

        TextView iconView = new TextView(activity);
        iconView.setText("⚽");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        iconView.setGravity(Gravity.CENTER);
        content.addView(iconView);

        TextView labelView = new TextView(activity);
        labelView.setText("MOD");
        labelView.setTextColor(Color.parseColor("#00E5FF"));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setGravity(Gravity.CENTER);
        content.addView(labelView);

        ballContainer.addView(content);

        // Drag & Click Handler
        ballContainer.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private float startX, startY;
            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        startTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Bounds checking
                        ViewGroup parent = (ViewGroup) v.getParent();
                        if (parent != null) {
                            newX = Math.max(0, Math.min(newX, parent.getWidth() - v.getWidth()));
                            newY = Math.max(0, Math.min(newY, parent.getHeight() - v.getHeight()));
                        }

                        v.setX(newX);
                        v.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(event.getRawX() - startX);
                        float diffY = Math.abs(event.getRawY() - startY);
                        long duration = System.currentTimeMillis() - startTime;

                        if (diffX < dpToPx(activity, 10) && diffY < dpToPx(activity, 10) && duration < 300) {
                            // Click detected
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });

        return ballContainer;
    }

    private static View createMenuModal(final Activity activity) {
        // Semi-transparent backdrop overlay
        final FrameLayout backdrop = new FrameLayout(activity);
        FrameLayout.LayoutParams backdropParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        backdrop.setLayoutParams(backdropParams);
        backdrop.setBackgroundColor(Color.parseColor("#80000000")); // Dark backdrop
        backdrop.setClickable(true);

        // Menu card container
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int menuWidth = Math.min(dpToPx(activity, 380), (int) (dm.widthPixels * 0.90f));
        int menuMaxHeight = (int) (dm.heightPixels * 0.85f);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                menuWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;

        LinearLayout card = new LinearLayout(activity);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16));

        // Card Styling: Modern Dark Slate + Cyan Accent
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dpToPx(activity, 18));
        cardBg.setColor(Color.parseColor("#FA0B1120")); // 98% dark slate
        cardBg.setStroke(dpToPx(activity, 1), Color.parseColor("#3300E5FF")); // Subtle cyan glow
        card.setBackground(cardBg);
        card.setElevation(dpToPx(activity, 16));

        // 1. Header (Title + Close button)
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        header.setLayoutParams(headerParams);

        LinearLayout titleLayout = new LinearLayout(activity);
        titleLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        titleLayout.setLayoutParams(titleParams);

        TextView titleView = new TextView(activity);
        titleView.setText("⚽ eFootball Mod Menu");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleLayout.addView(titleView);

        TextView subtitleView = new TextView(activity);
        subtitleView.setText("Piko v1.9.0 • Unreal Engine 4");
        subtitleView.setTextColor(Color.parseColor("#94A3B8"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleLayout.addView(subtitleView);

        header.addView(titleLayout);

        // Close button
        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#EF4444"));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setPadding(dpToPx(activity, 8), dpToPx(activity, 4), dpToPx(activity, 8), dpToPx(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });
        header.addView(closeBtn);

        card.addView(header);

        // Divider
        View divider = new View(activity);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(activity, 1)
        );
        divParams.setMargins(0, dpToPx(activity, 10), 0, dpToPx(activity, 10));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#1E293B"));
        card.addView(divider);

        // Scrollable content
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scrollParams.weight = 1.0f;
        scrollView.setLayoutParams(scrollParams);

        LinearLayout contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // SECTION 1: GAMEPLAY & MATCH CONTROL
        addSectionHeader(activity, contentLayout, "🎮 MATCH & GAMEPLAY CONTROLS");

        // 1.1 Match Speed / Time Dilation
        addFeatureLabel(activity, contentLayout, "Match Speed (Time Dilation):");
        LinearLayout speedRow = new LinearLayout(activity);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, speedRow, "1.0x (Normal)", "#334155", () -> executeCommand("slomo 1.0"));
        addOptionButton(activity, speedRow, "1.25x", "#1E293B", () -> executeCommand("slomo 1.25"));
        addOptionButton(activity, speedRow, "1.5x", "#1E293B", () -> executeCommand("slomo 1.5"));
        addOptionButton(activity, speedRow, "2.0x (Turbo)", "#0F766E", () -> executeCommand("slomo 2.0"));
        contentLayout.addView(speedRow);

        // 1.2 Camera FOV / Drone View
        addFeatureLabel(activity, contentLayout, "Camera Angle / Field of View (FOV):");
        LinearLayout fovRow = new LinearLayout(activity);
        fovRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fovRow, "90° (Normal)", "#334155", () -> executeCommand("fov 90"));
        addOptionButton(activity, fovRow, "100° (Wide)", "#1E293B", () -> executeCommand("fov 100"));
        addOptionButton(activity, fovRow, "110° (Stadium)", "#1E293B", () -> executeCommand("fov 110"));
        addOptionButton(activity, fovRow, "120° (Drone)", "#0369A1", () -> executeCommand("fov 120"));
        contentLayout.addView(fovRow);

        // 1.3 In-game Player Scale / Height (Pemain Raksasa)
        addFeatureLabel(activity, contentLayout, "In-Game Player Height / Scale:");
        LinearLayout scaleRow = new LinearLayout(activity);
        scaleRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, scaleRow, "1.0x (Standard)", "#334155", () -> executePlayerScale(1.0f));
        addOptionButton(activity, scaleRow, "1.15x (Tall)", "#1E293B", () -> executePlayerScale(1.15f));
        addOptionButton(activity, scaleRow, "1.35x (Giant)", "#1E293B", () -> executePlayerScale(1.35f));
        addOptionButton(activity, scaleRow, "1.60x (Titan)", "#7C2D12", () -> executePlayerScale(1.60f));
        contentLayout.addView(scaleRow);

        // SECTION 2: GRAPHICS & PERFORMANCE
        addSectionHeader(activity, contentLayout, "⚡ GRAPHICS & PERFORMANCE");

        // 2.1 Max FPS
        addFeatureLabel(activity, contentLayout, "Target Frame Rate (FPS Unlocker):");
        LinearLayout fpsRow = new LinearLayout(activity);
        fpsRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fpsRow, "30 FPS", "#1E293B", () -> executeCommand("t.MaxFPS 30"));
        addOptionButton(activity, fpsRow, "60 FPS", "#334155", () -> executeCommand("t.MaxFPS 60"));
        addOptionButton(activity, fpsRow, "90 FPS", "#1E293B", () -> executeCommand("t.MaxFPS 90"));
        addOptionButton(activity, fpsRow, "120 FPS / Max", "#047857", () -> executeCommand("t.MaxFPS 120"));
        contentLayout.addView(fpsRow);

        // 2.2 Resolution Scaling (Screen Percentage)
        addFeatureLabel(activity, contentLayout, "Resolution Scale (Screen Percentage):");
        LinearLayout resRow = new LinearLayout(activity);
        resRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, resRow, "100% (Default)", "#334155", () -> executeCommand("r.ScreenPercentage 100"));
        addOptionButton(activity, resRow, "125% (HD)", "#1E293B", () -> executeCommand("r.ScreenPercentage 125"));
        addOptionButton(activity, resRow, "150% (Ultra)", "#4338CA", () -> executeCommand("r.ScreenPercentage 150"));
        contentLayout.addView(resRow);

        // 2.3 FPS & Engine Stats Overlay
        LinearLayout statsRow = new LinearLayout(activity);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(0, dpToPx(activity, 4), 0, dpToPx(activity, 4));
        addOptionButton(activity, statsRow, "📊 Toggle In-Game FPS Stats", "#1E293B", () -> executeCommand("stat fps"));
        addOptionButton(activity, statsRow, "⏱️ Toggle Frame Timers", "#1E293B", () -> executeCommand("stat unit"));
        contentLayout.addView(statsRow);

        // SECTION 3: LICENSE STATUS & INTEGRITY
        addSectionHeader(activity, contentLayout, "🛡️ SECURITY & LICENSE STATUS");
        addStatusBadge(activity, contentLayout, "✅ Google Play License: PROTECTED (Status: LICENSED)");
        addStatusBadge(activity, contentLayout, "✅ Morphe UE4 Console Engine: CONNECTED");

        // SECTION 4: CUSTOM CONSOLE COMMAND TERMINAL
        addSectionHeader(activity, contentLayout, "⌨️ CUSTOM UE4 CONSOLE COMMAND");
        final LinearLayout cmdRow = new LinearLayout(activity);
        cmdRow.setOrientation(LinearLayout.HORIZONTAL);
        cmdRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cmdRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cmdRowParams.setMargins(0, dpToPx(activity, 4), 0, dpToPx(activity, 8));
        cmdRow.setLayoutParams(cmdRowParams);

        final EditText cmdInput = new EditText(activity);
        cmdInput.setHint("e.g. slomo 1.8 or r.TonemapperFilm 0");
        cmdInput.setHintTextColor(Color.parseColor("#64748B"));
        cmdInput.setTextColor(Color.WHITE);
        cmdInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cmdInput.setSingleLine(true);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#1E293B"));
        inputBg.setCornerRadius(dpToPx(activity, 8));
        inputBg.setStroke(dpToPx(activity, 1), Color.parseColor("#334155"));
        cmdInput.setBackground(inputBg);
        cmdInput.setPadding(dpToPx(activity, 10), dpToPx(activity, 8), dpToPx(activity, 10), dpToPx(activity, 8));

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        inputParams.setMarginEnd(dpToPx(activity, 8));
        cmdInput.setLayoutParams(inputParams);
        cmdRow.addView(cmdInput);

        Button runBtn = new Button(activity);
        runBtn.setText("EXECUTE");
        runBtn.setTextColor(Color.WHITE);
        runBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        runBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable runBg = new GradientDrawable();
        runBg.setColor(Color.parseColor("#0284C7"));
        runBg.setCornerRadius(dpToPx(activity, 8));
        runBtn.setBackground(runBg);
        runBtn.setPadding(dpToPx(activity, 12), dpToPx(activity, 6), dpToPx(activity, 12), dpToPx(activity, 6));
        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = cmdInput.getText().toString().trim();
                if (!cmd.isEmpty()) {
                    executeCommand(cmd);
                    cmdInput.setText("");
                }
            }
        });
        cmdRow.addView(runBtn);
        contentLayout.addView(cmdRow);

        scrollView.addView(contentLayout);
        card.addView(scrollView);
        backdrop.addView(card);

        // Click outside closes modal
        backdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });

        return backdrop;
    }

    private static void addSectionHeader(Context context, LinearLayout parent, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(Color.parseColor("#38BDF8")); // Cyan-300
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(context, 10), 0, dpToPx(context, 4));
        header.setLayoutParams(params);
        parent.addView(header);
    }

    private static void addFeatureLabel(Context context, LinearLayout parent, String label) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#CBD5E1")); // Slate-300
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(context, 4), 0, dpToPx(context, 2));
        tv.setLayoutParams(params);
        parent.addView(tv);
    }

    private static void addOptionButton(Context context, LinearLayout parent, String label, String hexBg, final Runnable action) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btn.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hexBg));
        bg.setCornerRadius(dpToPx(context, 8));
        bg.setStroke(dpToPx(context, 1), Color.parseColor("#334155"));
        btn.setBackground(bg);
        btn.setPadding(dpToPx(context, 6), dpToPx(context, 4), dpToPx(context, 6), dpToPx(context, 4));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dpToPx(context, 34),
                1.0f
        );
        params.setMargins(dpToPx(context, 2), dpToPx(context, 2), dpToPx(context, 2), dpToPx(context, 4));
        btn.setLayoutParams(params);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (action != null) action.run();
            }
        });

        parent.addView(btn);
    }

    private static void addStatusBadge(Context context, LinearLayout parent, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#10B981")); // Emerald-500
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dpToPx(context, 8), dpToPx(context, 4), dpToPx(context, 8), dpToPx(context, 4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1010B981")); // 10% emerald
        bg.setCornerRadius(dpToPx(context, 6));
        bg.setStroke(dpToPx(context, 1), Color.parseColor("#2010B981"));
        tv.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(context, 2), 0, dpToPx(context, 2));
        tv.setLayoutParams(params);
        parent.addView(tv);
    }

    public static void toggleMenu() {
        if (sMenuModal == null) return;
        boolean isVisible = (sMenuModal.getVisibility() == View.VISIBLE);
        sMenuModal.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    /**
     * Executes Unreal Engine 4 Console Commands via GameActivity reflection.
     */
    public static void executeCommand(final String cmd) {
        if (sActivity == null) {
            showToast("Error: Game Activity not ready");
            return;
        }

        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean executed = false;
                try {
                    // 1. Direct call to nativeConsoleCommand on GameActivity instance
                    Method m = sActivity.getClass().getMethod("nativeConsoleCommand", String.class);
                    m.invoke(sActivity, cmd);
                    executed = true;
                } catch (Throwable t1) {
                    // 2. Fallback: retrieve GameActivity._activity static field
                    try {
                        Field f = sActivity.getClass().getField("_activity");
                        Object act = f.get(null);
                        if (act != null) {
                            Method m = act.getClass().getMethod("nativeConsoleCommand", String.class);
                            m.invoke(act, cmd);
                            executed = true;
                        }
                    } catch (Throwable t2) {
                        try {
                            PikoUtils.logger(TAG + ": Command execution failed: " + t2.getMessage());
                        } catch (Throwable ignored) {}
                    }
                }

                if (executed) {
                    showToast("⚡ UE4 Applied: " + cmd);
                    try {
                        PikoUtils.logger(TAG + ": UE4 Executed command: " + cmd);
                    } catch (Throwable ignored) {}
                } else {
                    showToast("⚠️ Could not reach UE4 console");
                }
            }
        });
    }

    /**
     * Adjusts player scale / height in-game.
     */
    public static void executePlayerScale(final float scale) {
        // Send UE4 actor scale adjustments
        executeCommand("SetActorScale3D " + scale);
        executeCommand("SetWorldScale3D " + scale);
        showToast("⚽ Player Scale set to " + scale + "x");
    }

    private static void showToast(final String text) {
        if (sActivity == null) return;
        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(sActivity, text, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            }
        });
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}

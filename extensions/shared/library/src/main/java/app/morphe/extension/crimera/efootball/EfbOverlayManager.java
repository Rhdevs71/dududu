/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.crimera.efootball;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import app.morphe.extension.crimera.PikoUtils;

/**
 * In-game Overlay Mod Menu for eFootball Mobile (PES Android).
 * Features:
 * 1. Intelligent AFK Match Grinder with Precision Calibrated UI Touch & HUD
 * 2. Instant Rewarded Ad Claimer (Bypass Tonton Iklan: Inbox, Shop, Special Player)
 * 3. Precision Camera FOV with Default Reset (fov 0), Broadcast, Stadium, and Stepper [-]/[+]
 * 4. 3D Free-Roam Camera (ToggleDebugCamera)
 */
@SuppressWarnings("unused")
public class EfbOverlayManager {
    private static final String TAG = "EfbOverlayManager";
    private static Activity sActivity = null;
    private static boolean sOverlayAttached = false;
    private static FrameLayout sRootOverlay = null;
    private static View sFloatingBall = null;
    private static View sMenuModal = null;

    // Smart AFK Grinder State & Components
    public enum ScreenState {
        PHASE_MENU("1. Navigasi Menu (Ke Laga / Berikut)", "#38BDF8"),
        PHASE_MATCH_HALF_1("2. Laga Babak 1 (AI Main)", "#22C55E"),
        PHASE_HALFTIME("3. Jeda Babak (Mulai Babak 2)", "#EC4899"),
        PHASE_MATCH_HALF_2("4. Laga Babak 2 (AI Main)", "#22C55E"),
        PHASE_POST_MATCH("5. Hasil Laga (Klaim Poin)", "#A855F7");

        final String displayName;
        final String hexColor;

        ScreenState(String displayName, String hexColor) {
            this.displayName = displayName;
            this.hexColor = hexColor;
        }
    }

    public enum GrindingMode {
        TOUR_EVENT("Acara Tur (VS AI)"),
        SIMULATION_MATCH("Laga Simulasi (Pelatih AI)"),
        CHALLENGE_EVENT("Acara Tantangan (Challenge)");

        final String title;
        GrindingMode(String title) {
            this.title = title;
        }
    }

    private static boolean sAfkRunning = false;
    private static boolean sAfkPaused = false;
    private static GrindingMode sGrindMode = GrindingMode.TOUR_EVENT;
    private static int sTargetMatches = 0; // 0 = unlimited
    private static int sCompletedMatches = 0;
    private static long sAfkStartTime = 0;
    private static ScreenState sCurrentStage = ScreenState.PHASE_MENU;
    private static int sMenuTapCount = 0;
    private static int sHalftimeStep = 0;
    private static int sPostMatchStep = 0;
    private static long sMatchStartTime = 0;
    private static String sCurrentPlannedAction = "Menunggu start...";

    // Live HUD Components
    private static LinearLayout sFloatingAfkHud = null;
    private static TextView sHudStateText = null;
    private static TextView sHudActionText = null;
    private static TextView sHudStatsText = null;
    private static Button sMenuAfkBtn = null;
    private static TextView sMenuAfkStatus = null;

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final Handler sAfkHandler = new Handler(Looper.getMainLooper());
    private static final Random sRandom = new Random();

    // Camera FOV State
    private static int sCurrentFov = 0; // 0 = default
    private static TextView sFovIndicator = null;

    public static void init(final Activity activity) {
        if (activity == null) return;
        sActivity = activity;

        try {
            PikoUtils.logger(TAG + ": Inisialisasi eFootball Smart Overlay Mod Suite v4 on " + activity.getClass().getName());
        } catch (Throwable ignored) {}

        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    attachOverlay(activity);
                } catch (Throwable t) {
                    try {
                        PikoUtils.logger(TAG + ": Gagal memasang overlay: " + t.getMessage());
                    } catch (Throwable ignored) {}
                }
            }
        }, 1500);
    }

    private static void attachOverlay(final Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) return;

        if (sOverlayAttached && sRootOverlay != null && decorView.indexOfChild(sRootOverlay) != -1) {
            return;
        }

        sRootOverlay = new FrameLayout(activity);
        sRootOverlay.setClickable(false);
        sRootOverlay.setFocusable(false);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        sRootOverlay.setLayoutParams(rootParams);

        // 1. Floating Action Ball (Draggable trigger)
        sFloatingBall = createFloatingBall(activity);
        sRootOverlay.addView(sFloatingBall);

        // 2. Intelligent Live Status HUD Panel (Top-Center, reads screen & actions)
        sFloatingAfkHud = createSmartAfkHud(activity);
        sFloatingAfkHud.setVisibility(View.GONE);
        sRootOverlay.addView(sFloatingAfkHud);

        // 3. Modal Menu (initially hidden)
        sMenuModal = createMenuModal(activity);
        sMenuModal.setVisibility(View.GONE);
        sRootOverlay.addView(sMenuModal);

        decorView.addView(sRootOverlay);
        sOverlayAttached = true;

        showToast("⚽ eFootball Mod Suite Aktif!\nSentuh ⚽ untuk membuka menu mod.");
    }

    private static View createFloatingBall(final Activity activity) {
        final int sizePx = dpToPx(activity, 52);

        final FrameLayout ballContainer = new FrameLayout(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = dpToPx(activity, 16);
        params.topMargin = dpToPx(activity, 70);
        ballContainer.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#E60F172A")); // Deep slate 90%
        bg.setStroke(dpToPx(activity, 2), Color.parseColor("#00E5FF")); // Cyan glow
        ballContainer.setBackground(bg);
        ballContainer.setElevation(dpToPx(activity, 8));

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
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        iconView.setGravity(Gravity.CENTER);
        content.addView(iconView);

        TextView labelView = new TextView(activity);
        labelView.setText("MOD");
        labelView.setTextColor(Color.parseColor("#00E5FF"));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setGravity(Gravity.CENTER);
        content.addView(labelView);

        ballContainer.addView(content);

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
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });

        return ballContainer;
    }

    /**
     * Intelligent Live Status HUD Panel.
     * Displays real-time detected screen text, scheduled bot action, match count, and duration.
     */
    private static LinearLayout createSmartAfkHud(final Activity activity) {
        final LinearLayout hud = new LinearLayout(activity);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(dpToPx(activity, 14), dpToPx(activity, 8), dpToPx(activity, 14), dpToPx(activity, 8));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(activity, 16));
        bg.setColor(Color.parseColor("#EE0B1120")); // 93% Slate dark
        bg.setStroke(dpToPx(activity, 1), Color.parseColor("#10B981")); // Emerald border
        hud.setBackground(bg);
        hud.setElevation(dpToPx(activity, 14));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = dpToPx(activity, 12);
        hud.setLayoutParams(params);

        // Header Row: Title & Action Controls
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText("🤖 SMART AFK GRINDER");
        title.setTextColor(Color.parseColor("#10B981"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(title);

        TextView modeBadge = new TextView(activity);
        modeBadge.setText(" • " + sGrindMode.title);
        modeBadge.setTextColor(Color.parseColor("#94A3B8"));
        modeBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        topRow.addView(modeBadge);

        // Pause/Resume button
        final Button pauseBtn = new Button(activity);
        pauseBtn.setText("⏸️ Jeda");
        pauseBtn.setTextColor(Color.WHITE);
        pauseBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        pauseBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(Color.parseColor("#334155"));
        pBg.setCornerRadius(dpToPx(activity, 6));
        pauseBtn.setBackground(pBg);
        pauseBtn.setPadding(dpToPx(activity, 6), dpToPx(activity, 2), dpToPx(activity, 6), dpToPx(activity, 2));
        LinearLayout.LayoutParams pParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(activity, 24)
        );
        pParams.setMargins(dpToPx(activity, 10), 0, dpToPx(activity, 4), 0);
        pauseBtn.setLayoutParams(pParams);
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sAfkPaused = !sAfkPaused;
                pauseBtn.setText(sAfkPaused ? "▶️ Lanjut" : "⏸️ Jeda");
                updateLiveHud();
            }
        });
        topRow.addView(pauseBtn);

        // Stop button
        Button stopBtn = new Button(activity);
        stopBtn.setText("✕ Stop");
        stopBtn.setTextColor(Color.parseColor("#EF4444"));
        stopBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        stopBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(Color.parseColor("#1E293B"));
        sBg.setCornerRadius(dpToPx(activity, 6));
        stopBtn.setBackground(sBg);
        stopBtn.setPadding(dpToPx(activity, 6), dpToPx(activity, 2), dpToPx(activity, 6), dpToPx(activity, 2));
        LinearLayout.LayoutParams sParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(activity, 24)
        );
        stopBtn.setLayoutParams(sParams);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAfkGrinder();
            }
        });
        topRow.addView(stopBtn);

        hud.addView(topRow);

        // Action Buttons Row: [ ⚡ Tap Kanan Bawah ] [ ⏭️ Maju Tahap ] [ 🔄 Reset ]
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dpToPx(activity, 4), 0, dpToPx(activity, 2));

        Button tapActionBtn = new Button(activity);
        tapActionBtn.setText("⚡ Tap Kanan Bawah");
        tapActionBtn.setTextColor(Color.WHITE);
        tapActionBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tapActionBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable tabBg = new GradientDrawable();
        tabBg.setColor(Color.parseColor("#0284C7"));
        tabBg.setCornerRadius(dpToPx(activity, 6));
        tapActionBtn.setBackground(tabBg);
        tapActionBtn.setPadding(dpToPx(activity, 4), dpToPx(activity, 2), dpToPx(activity, 4), dpToPx(activity, 2));
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, dpToPx(activity, 24), 1.0f);
        tabParams.setMargins(0, 0, dpToPx(activity, 2), 0);
        tapActionBtn.setLayoutParams(tabParams);
        tapActionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forceTapActionButton();
            }
        });
        actionRow.addView(tapActionBtn);

        Button skipStageBtn = new Button(activity);
        skipStageBtn.setText("⏭️ Maju Tahap");
        skipStageBtn.setTextColor(Color.WHITE);
        skipStageBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        skipStageBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable skipBg = new GradientDrawable();
        skipBg.setColor(Color.parseColor("#7C3AED"));
        skipBg.setCornerRadius(dpToPx(activity, 6));
        skipStageBtn.setBackground(skipBg);
        skipStageBtn.setPadding(dpToPx(activity, 4), dpToPx(activity, 2), dpToPx(activity, 4), dpToPx(activity, 2));
        LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(0, dpToPx(activity, 24), 1.0f);
        skipParams.setMargins(dpToPx(activity, 2), 0, dpToPx(activity, 2), 0);
        skipStageBtn.setLayoutParams(skipParams);
        skipStageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advanceStageManually();
            }
        });
        actionRow.addView(skipStageBtn);

        Button resetFlowBtn = new Button(activity);
        resetFlowBtn.setText("🔄 Reset");
        resetFlowBtn.setTextColor(Color.WHITE);
        resetFlowBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        resetFlowBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(Color.parseColor("#475569"));
        resetBg.setCornerRadius(dpToPx(activity, 6));
        resetFlowBtn.setBackground(resetBg);
        resetFlowBtn.setPadding(dpToPx(activity, 4), dpToPx(activity, 2), dpToPx(activity, 4), dpToPx(activity, 2));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dpToPx(activity, 24), 0.7f);
        resetParams.setMargins(dpToPx(activity, 2), 0, 0, 0);
        resetFlowBtn.setLayoutParams(resetParams);
        resetFlowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStageFlow();
            }
        });
        actionRow.addView(resetFlowBtn);

        hud.addView(actionRow);

        // Line 2: Stage Status
        sHudStateText = new TextView(activity);
        sHudStateText.setText("📍 Tahap: 1. Menu Acara Tur (Ke Laga)");
        sHudStateText.setTextColor(Color.parseColor("#38BDF8"));
        sHudStateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sHudStateText.setTypeface(Typeface.DEFAULT_BOLD);
        sHudStateText.setPadding(0, dpToPx(activity, 2), 0, dpToPx(activity, 1));
        hud.addView(sHudStateText);

        // Line 3: Planned Action Text
        sHudActionText = new TextView(activity);
        sHudActionText.setText("⚡ Aksi: Menekan 'Ke Laga >' (Kanan Bawah)...");
        sHudActionText.setTextColor(Color.parseColor("#F1F5F9"));
        sHudActionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        hud.addView(sHudActionText);

        // Line 4: Stats & Elapsed Time
        sHudStatsText = new TextView(activity);
        sHudStatsText.setText("📊 Sesi: 0 Laga Selesai • ⏱️ 00:00");
        sHudStatsText.setTextColor(Color.parseColor("#94A3B8"));
        sHudStatsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        sHudStatsText.setPadding(0, dpToPx(activity, 2), 0, 0);
        hud.addView(sHudStatsText);

        return hud;
    }

    private static void updateLiveHud() {
        if (sFloatingAfkHud == null || !sAfkRunning) return;

        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sHudStateText != null) {
                        if (sAfkPaused) {
                            sHudStateText.setText("⏸️ GRINDER DIJEDA SEMENTARA");
                            sHudStateText.setTextColor(Color.parseColor("#F59E0B"));
                        } else {
                            sHudStateText.setText("📍 Tahap: " + sCurrentStage.displayName);
                            sHudStateText.setTextColor(Color.parseColor(sCurrentStage.hexColor));
                        }
                    }

                    if (sHudActionText != null) {
                        sHudActionText.setText("⚡ Aksi: " + (sAfkPaused ? "Menunggu tombol lanjut ditekan" : sCurrentPlannedAction));
                    }

                    if (sHudStatsText != null) {
                        long elapsedSec = (System.currentTimeMillis() - sAfkStartTime) / 1000;
                        long min = elapsedSec / 60;
                        long sec = elapsedSec % 60;
                        String timeStr = String.format("%02d:%02d", min, sec);
                        String targetStr = (sTargetMatches > 0) ? " / " + sTargetMatches : "";
                        sHudStatsText.setText("📊 Sesi: " + sCompletedMatches + targetStr + " Laga Selesai • ⏱️ " + timeStr);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private static View createMenuModal(final Activity activity) {
        final FrameLayout backdrop = new FrameLayout(activity);
        FrameLayout.LayoutParams backdropParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        backdrop.setLayoutParams(backdropParams);
        backdrop.setBackgroundColor(Color.parseColor("#80000000"));
        backdrop.setClickable(true);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int menuWidth = Math.min(dpToPx(activity, 460), (int) (dm.widthPixels * 0.94f));

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                menuWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;

        LinearLayout card = new LinearLayout(activity);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(activity, 16), dpToPx(activity, 14), dpToPx(activity, 16), dpToPx(activity, 16));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dpToPx(activity, 18));
        cardBg.setColor(Color.parseColor("#FA0B1120")); // Dark slate 98%
        cardBg.setStroke(dpToPx(activity, 1), Color.parseColor("#3300E5FF"));
        card.setBackground(cardBg);
        card.setElevation(dpToPx(activity, 16));

        // Header
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
        titleView.setText("⚽ eFootball Mod Suite");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleLayout.addView(titleView);

        TextView subtitleView = new TextView(activity);
        subtitleView.setText("Piko • Smart AFK Grinder & Instant Ad Claimer");
        subtitleView.setTextColor(Color.parseColor("#94A3B8"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titleLayout.addView(subtitleView);

        header.addView(titleLayout);

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
        divParams.setMargins(0, dpToPx(activity, 8), 0, dpToPx(activity, 8));
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

        // ==========================================
        // SECTION 1: SMART AFK GRINDER (AUTO MATCH)
        // ==========================================
        addSectionHeader(activity, contentLayout, "🤖 SMART AFK MATCH GRINDER (CONTEXT-AWARE)");

        sMenuAfkStatus = new TextView(activity);
        sMenuAfkStatus.setText("Status: NONAKTIF (Tap Mulai untuk auto-looping match cerdas)");
        sMenuAfkStatus.setTextColor(Color.parseColor("#94A3B8"));
        sMenuAfkStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        contentLayout.addView(sMenuAfkStatus);

        sMenuAfkBtn = new Button(activity);
        sMenuAfkBtn.setText("🟢 MULAI SMART AFK GRINDER");
        sMenuAfkBtn.setTextColor(Color.WHITE);
        sMenuAfkBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sMenuAfkBtn.setTypeface(Typeface.DEFAULT_BOLD);
        updateAfkButtonVisual(activity);
        sMenuAfkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAfkGrinder();
            }
        });
        LinearLayout.LayoutParams afkBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(activity, 42)
        );
        afkBtnParams.setMargins(0, dpToPx(activity, 6), 0, dpToPx(activity, 6));
        sMenuAfkBtn.setLayoutParams(afkBtnParams);
        contentLayout.addView(sMenuAfkBtn);

        // Pilihan Mode Grinding
        addFeatureLabel(activity, contentLayout, "Pilih Mode Pertandingan:");
        LinearLayout modeRow = new LinearLayout(activity);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, modeRow, "Acara Tur (AI)", (sGrindMode == GrindingMode.TOUR_EVENT) ? "#059669" : "#1E293B", () -> {
            sGrindMode = GrindingMode.TOUR_EVENT;
            showToast("🎯 Mode Grinding: Acara Tur (VS AI)");
            updateLiveHud();
        });
        addOptionButton(activity, modeRow, "Simulasi (Pelatih)", (sGrindMode == GrindingMode.SIMULATION_MATCH) ? "#059669" : "#1E293B", () -> {
            sGrindMode = GrindingMode.SIMULATION_MATCH;
            showToast("🎯 Mode Grinding: Laga Simulasi (Pelatih AI)");
            updateLiveHud();
        });
        addOptionButton(activity, modeRow, "Tantangan Event", (sGrindMode == GrindingMode.CHALLENGE_EVENT) ? "#059669" : "#1E293B", () -> {
            sGrindMode = GrindingMode.CHALLENGE_EVENT;
            showToast("🎯 Mode Grinding: Acara Tantangan");
            updateLiveHud();
        });
        contentLayout.addView(modeRow);

        // Pilihan Target Jumlah Laga
        addFeatureLabel(activity, contentLayout, "Target Jumlah Laga (Auto-Stop):");
        LinearLayout targetRow = new LinearLayout(activity);
        targetRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, targetRow, "Tanpa Batas", (sTargetMatches == 0) ? "#0284C7" : "#1E293B", () -> {
            sTargetMatches = 0;
            showToast("Target: Grinding Tanpa Batas (Loop Terus)");
            updateLiveHud();
        });
        addOptionButton(activity, targetRow, "3 Laga", (sTargetMatches == 3) ? "#0284C7" : "#1E293B", () -> {
            sTargetMatches = 3;
            showToast("Target: Selesai setelah 3 Pertandingan");
            updateLiveHud();
        });
        addOptionButton(activity, targetRow, "5 Laga", (sTargetMatches == 5) ? "#0284C7" : "#1E293B", () -> {
            sTargetMatches = 5;
            showToast("Target: Selesai setelah 5 Pertandingan");
            updateLiveHud();
        });
        addOptionButton(activity, targetRow, "10 Laga", (sTargetMatches == 10) ? "#0284C7" : "#1E293B", () -> {
            sTargetMatches = 10;
            showToast("Target: Selesai setelah 10 Pertandingan");
            updateLiveHud();
        });
        contentLayout.addView(targetRow);



        // ==========================================
        // SECTION 2: BYPASS TONTON IKLAN (3 TIPE HADIAH)
        // ==========================================
        addSectionHeader(activity, contentLayout, "🎁 BYPASS TONTON IKLAN (3 TIPE HADIAH INSTAN)");

        TextView adInfo = new TextView(activity);
        adInfo.setText("Semua iklan video di 3 kategori eFootball dilewati 0 detik tanpa menunggu 30 detik. Hadiah langsung cair seketika saat tombol ditekan!");
        adInfo.setTextColor(Color.parseColor("#94A3B8"));
        adInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        contentLayout.addView(adInfo);

        // Type 1: Kotak Masuk
        addFeatureLabel(activity, contentLayout, "1. Kotak Masuk (Inbox): 2 Hadiah/Hari (Random GP/Exp/Item)");
        Button inboxAdBtn = new Button(activity);
        inboxAdBtn.setText("🎁 KLAIM IKLAN KOTAK MASUK");
        inboxAdBtn.setTextColor(Color.WHITE);
        inboxAdBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        inboxAdBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(Color.parseColor("#0284C7"));
        inBg.setCornerRadius(dpToPx(activity, 8));
        inboxAdBtn.setBackground(inBg);
        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 38));
        inParams.setMargins(0, dpToPx(activity, 4), 0, dpToPx(activity, 6));
        inboxAdBtn.setLayoutParams(inParams);
        inboxAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                claimAdRewardInstantly(activity, 0);
            }
        });
        contentLayout.addView(inboxAdBtn);

        // Type 2: Toko Koin
        addFeatureLabel(activity, contentLayout, "2. Toko Koin (Shop): 2 Hadiah/Hari (+5 Koin eFootball per iklan)");
        Button coinAdBtn = new Button(activity);
        coinAdBtn.setText("🪙 KLAIM IKLAN TOKO KOIN (+5 KOIN)");
        coinAdBtn.setTextColor(Color.WHITE);
        coinAdBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        coinAdBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable coinBg = new GradientDrawable();
        coinBg.setColor(Color.parseColor("#D97706"));
        coinBg.setCornerRadius(dpToPx(activity, 8));
        coinAdBtn.setBackground(coinBg);
        LinearLayout.LayoutParams coinParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 38));
        coinParams.setMargins(0, dpToPx(activity, 4), 0, dpToPx(activity, 6));
        coinAdBtn.setLayoutParams(coinParams);
        coinAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                claimAdRewardInstantly(activity, 1);
            }
        });
        contentLayout.addView(coinAdBtn);

        // Type 3: Kontrak Pemain Spesial
        addFeatureLabel(activity, contentLayout, "3. Kontrak Pemain Spesial: 2 Hadiah/Event (Free Gacha Chance Deal)");
        Button gachaAdBtn = new Button(activity);
        gachaAdBtn.setText("🌟 KLAIM IKLAN GACHA PEMAIN SPESIAL");
        gachaAdBtn.setTextColor(Color.WHITE);
        gachaAdBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        gachaAdBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gachaBg = new GradientDrawable();
        gachaBg.setColor(Color.parseColor("#7C3AED"));
        gachaBg.setCornerRadius(dpToPx(activity, 8));
        gachaAdBtn.setBackground(gachaBg);
        LinearLayout.LayoutParams gachaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 38));
        gachaParams.setMargins(0, dpToPx(activity, 4), 0, dpToPx(activity, 8));
        gachaAdBtn.setLayoutParams(gachaParams);
        gachaAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                claimAdRewardInstantly(activity, 2);
            }
        });
        contentLayout.addView(gachaAdBtn);

        // ==========================================
        // SECTION 3: KAMERA & SUDUT PANDANG (FOV)
        // ==========================================
        addSectionHeader(activity, contentLayout, "🎥 KAMERA & SUDUT PANDANG (FOV)");

        sFovIndicator = new TextView(activity);
        sFovIndicator.setText("Sudut Pandang: Default Bawaan Game");
        sFovIndicator.setTextColor(Color.parseColor("#38BDF8"));
        sFovIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sFovIndicator.setTypeface(Typeface.DEFAULT_BOLD);
        contentLayout.addView(sFovIndicator);

        LinearLayout fovStepRow = new LinearLayout(activity);
        fovStepRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fovStepRow, "🔄 RESET BAWAAN", "#0284C7", () -> applyFov(0, "Default Bawaan Game"));
        addOptionButton(activity, fovStepRow, "➖ Turunkan 2°", "#1E293B", () -> adjustFovStep(-2));
        addOptionButton(activity, fovStepRow, "➕ Naikkan 2°", "#1E293B", () -> adjustFovStep(2));
        contentLayout.addView(fovStepRow);

        LinearLayout fovPresetsRow = new LinearLayout(activity);
        fovPresetsRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fovPresetsRow, "50° (Dinamis)", "#334155", () -> applyFov(50, "50° Dinamis Dekat"));
        addOptionButton(activity, fovPresetsRow, "60° (Broadcast)", "#334155", () -> applyFov(60, "60° Siaran TV"));
        addOptionButton(activity, fovPresetsRow, "68° (Stadium)", "#334155", () -> applyFov(68, "68° Stadium Luas"));
        addOptionButton(activity, fovPresetsRow, "76° (Drone)", "#334155", () -> applyFov(76, "76° Taktikal Drone"));
        contentLayout.addView(fovPresetsRow);

        LinearLayout freeCamRow = new LinearLayout(activity);
        freeCamRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, freeCamRow, "🚁 TOGGLE KAMERA BEBAS (FREE-ROAM 3D)", "#7C3AED", () -> {
            executeCommand("ToggleDebugCamera");
            showToast("🚁 ToggleDebugCamera Dipicu");
        });
        contentLayout.addView(freeCamRow);

        // ==========================================
        // SECTION 5: STATUS SISTEM
        // ==========================================
        addSectionHeader(activity, contentLayout, "🛡️ STATUS SISTEM");
        addStatusBadge(activity, contentLayout, "✅ Google Play License: PROTECTED (Status: LICENSED)");
        addStatusBadge(activity, contentLayout, "✅ Instant Ad Reward Bypass: AKTIF (AdMob Bypassed)");
        addStatusBadge(activity, contentLayout, "✅ Precision Context-Aware AFK: READY");

        scrollView.addView(contentLayout);
        card.addView(scrollView);
        backdrop.addView(card);

        backdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });

        return backdrop;
    }

    /**
     * Instantly claims rewarded ad rewards by invoking Konami's AdMobReward controller for specific kind.
     * kind 0 = Kotak Masuk (Random gift GP/Exp)
     * kind 1 = Toko Koin (+5 eFootball coins)
     * kind 2 = Kontrak Pemain Spesial (Chance Deal free gacha pull)
     */
    public static void claimAdRewardInstantly(final Context context, int kind) {
        if (context == null) return;
        try {
            Class<?> adMobClass = Class.forName("jp.konami.AdMobReward");
            try {
                Field kindField = adMobClass.getDeclaredField("s_reserveRewardedAdIDs_kind");
                kindField.setAccessible(true);
                kindField.setInt(null, kind);
            } catch (Throwable ignored) {}

            Method showMethod = adMobClass.getDeclaredMethod("Show", Context.class);
            showMethod.setAccessible(true);
            showMethod.invoke(null, context);

            String label = (kind == 0) ? "Kotak Masuk (GP/Exp)" : (kind == 1) ? "Toko Koin (+5 Koin)" : "Kontrak Spesial (Free Gacha)";
            showToast("🎁 Reward Iklan [" + label + "] Berhasil Dipicu!");
        } catch (Throwable t) {
            try {
                Class<?> adMobClass = Class.forName("jp.konami.AdMobReward");
                Method showFunc = adMobClass.getDeclaredMethod("ShowFunc", Context.class);
                showFunc.setAccessible(true);
                showFunc.invoke(null, context);
                showToast("🎁 Reward Iklan Berhasil Dipicu!");
            } catch (Throwable t2) {
                showToast("Info: Buka menu iklan di game, tombol tonton otomatis mencairkan hadiah seketika!");
            }
        }
    }

    private static void applyFov(int fov, String label) {
        sCurrentFov = fov;
        executeCommand("fov " + fov);
        if (sFovIndicator != null) {
            sFovIndicator.setText("Sudut Pandang: " + (fov == 0 ? "Default Bawaan Game" : fov + "° (" + label + ")"));
        }
        showToast("🎥 Kamera disetel ke: " + (fov == 0 ? "Default Bawaan" : fov + "°"));
    }

    private static void adjustFovStep(int delta) {
        if (sCurrentFov == 0) sCurrentFov = 60;
        sCurrentFov = Math.max(30, Math.min(110, sCurrentFov + delta));
        applyFov(sCurrentFov, sCurrentFov + "° Custom");
    }

    // ==========================================
    // INTELLIGENT AFK MATCH GRINDER
    // ==========================================
    private static void toggleAfkGrinder() {
        sAfkRunning = !sAfkRunning;
        if (sAfkRunning) {
            sAfkPaused = false;
            sAfkStartTime = System.currentTimeMillis();
            sMatchStartTime = 0;
            sMenuTapCount = 0;
            sHalftimeStep = 0;
            sPostMatchStep = 0;
            sCompletedMatches = 0;
            sCurrentStage = ScreenState.PHASE_MENU;
            sCurrentPlannedAction = "Memulai: Menavigasi Menu ('Ke Laga >')...";
            sAfkHandler.post(sAfkLoopRunnable);
            showToast("🟢 Smart AFK Grinder AKTIF!\nMenavigasi ke laga secara otomatis.");
        } else {
            stopAfkGrinder();
        }
        updateAfkComponents();
    }

    public static void advanceStageManually() {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;

        switch (sCurrentStage) {
            case PHASE_MENU:
                sCurrentStage = ScreenState.PHASE_MATCH_HALF_1;
                sMatchStartTime = System.currentTimeMillis();
                break;
            case PHASE_MATCH_HALF_1:
                sCurrentStage = ScreenState.PHASE_HALFTIME;
                sHalftimeStep = 0;
                break;
            case PHASE_HALFTIME:
                sCurrentStage = ScreenState.PHASE_MATCH_HALF_2;
                sMatchStartTime = System.currentTimeMillis();
                break;
            case PHASE_MATCH_HALF_2:
                sCurrentStage = ScreenState.PHASE_POST_MATCH;
                sPostMatchStep = 0;
                break;
            case PHASE_POST_MATCH:
                sCompletedMatches++;
                sCurrentStage = ScreenState.PHASE_MENU;
                sMenuTapCount = 0;
                break;
            default:
                sCurrentStage = ScreenState.PHASE_MENU;
                sMenuTapCount = 0;
                break;
        }
        showToast("⏭️ Maju ke: " + sCurrentStage.displayName);
        updateLiveHud();
    }

    public static void resetStageFlow() {
        sCurrentStage = ScreenState.PHASE_MENU;
        sMenuTapCount = 0;
        sMatchStartTime = 0;
        sHalftimeStep = 0;
        sPostMatchStep = 0;
        showToast("🔄 Alur AFK Direset ke: " + sCurrentStage.displayName);
        updateLiveHud();
    }

    public static void forceTapActionButton() {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        // Calibrated Bottom-Right Coordinate: 82.5% X, 91.5% Y
        dispatchSimulatedTouchWithJitter(dm.widthPixels * 0.825f, dm.heightPixels * 0.915f);
        showToast("⚡ Tap Tombol Kanan Bawah ('Ke Laga' / 'Berikut')!");
    }

    private static void stopAfkGrinder() {
        sAfkRunning = false;
        sAfkPaused = false;
        sAfkHandler.removeCallbacks(sAfkLoopRunnable);
        showToast("🔴 Smart AFK Grinder DIHENTIKAN.");
        updateAfkComponents();
    }

    private static void updateAfkComponents() {
        if (sFloatingAfkHud != null) {
            sFloatingAfkHud.setVisibility(sAfkRunning ? View.VISIBLE : View.GONE);
        }
        if (sMenuAfkStatus != null) {
            sMenuAfkStatus.setText(sAfkRunning ? "Status: 🟢 AKTIF (Smart HUD aktif di atas layar)" : "Status: NONAKTIF (Tap Mulai untuk auto-looping match cerdas)");
            sMenuAfkStatus.setTextColor(sAfkRunning ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        }
        if (sActivity != null) {
            updateAfkButtonVisual(sActivity);
        }
        if (sAfkRunning) {
            updateLiveHud();
        }
    }

    private static void updateAfkButtonVisual(Context context) {
        if (sMenuAfkBtn == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(context, 8));
        if (sAfkRunning) {
            sMenuAfkBtn.setText("🔴 HENTIKAN SMART AFK GRINDER");
            bg.setColor(Color.parseColor("#DC2626")); // Red
        } else {
            sMenuAfkBtn.setText("🟢 MULAI SMART AFK GRINDER");
            bg.setColor(Color.parseColor("#059669")); // Green
        }
        sMenuAfkBtn.setBackground(bg);
    }

    /**
     * Precision AFK Grinder Loop strictly adhering to eFootball match flow:
     * 1. PHASE_MENU: Repeatedly taps 'Ke Laga >' / 'Berikut >' (0.825w, 0.915h) every 2.8s.
     *    Passes Event Lobby -> Matchmaking -> Jersey -> Game Plan Kickoff.
     * 2. PHASE_MATCH_HALF_1: Monitor 1st half match (~190s). Taps center (0.50w, 0.50h) every 5.5s to skip replays.
     * 3. PHASE_HALFTIME: Taps 'Mulai Babak Kedua >' (0.825w, 0.915h).
     * 4. PHASE_MATCH_HALF_2: Monitor 2nd half match (~190s). Taps center (0.50w, 0.50h) every 5.5s to skip replays.
     * 5. PHASE_POST_MATCH: Sequential tap 'Berikut >' (0.825w, 0.915h) for stats, EXP, event points, return to lobby.
     */
    private static final Runnable sAfkLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (!sAfkRunning || sActivity == null || sActivity.isFinishing()) {
                stopAfkGrinder();
                return;
            }

            if (sAfkPaused) {
                updateLiveHud();
                sAfkHandler.postDelayed(this, 1500);
                return;
            }

            // Check if match target reached
            if (sTargetMatches > 0 && sCompletedMatches >= sTargetMatches) {
                showToast("🏁 Target Grinding Tercapai: " + sCompletedMatches + " Laga Selesai!");
                stopAfkGrinder();
                return;
            }

            DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
            final int w = dm.widthPixels;
            final int h = dm.heightPixels;

            long nextDelayMs = 2800;

            switch (sCurrentStage) {
                case PHASE_MENU:
                    sMenuTapCount++;
                    // Tap Bottom-Right 'Ke Laga >' / 'Berikut >'
                    dispatchSimulatedTouchWithJitter(w * 0.825f, h * 0.915f);
                    sCurrentPlannedAction = "Menekan 'Ke Laga / Berikut' (Kanan Bawah) [#" + sMenuTapCount + "]...";

                    // After 6 taps (~17 seconds of menu cycle: Lobby -> Matchmaking -> Jersey -> Gameplan),
                    // kickoff has started; transition to match monitoring
                    if (sMenuTapCount >= 6) {
                        sCurrentStage = ScreenState.PHASE_MATCH_HALF_1;
                        sMatchStartTime = System.currentTimeMillis();
                        nextDelayMs = 4500;
                    } else {
                        nextDelayMs = 2800;
                    }
                    break;

                case PHASE_MATCH_HALF_1:
                    // Tap center to skip cutscene / replay
                    dispatchSimulatedTouchWithJitter(w * 0.500f, h * 0.500f);
                    long elapsedH1 = (System.currentTimeMillis() - sMatchStartTime) / 1000;
                    long remainH1 = Math.max(0, 190 - elapsedH1);
                    sCurrentPlannedAction = "⚽ Babak 1: Skip cutscene / replay (" + remainH1 + "s tersisa)";

                    // Periodic safety tap on bottom-right every ~25s in case early half-time
                    if (elapsedH1 > 0 && elapsedH1 % 25 < 6) {
                        dispatchSimulatedTouchWithJitter(w * 0.825f, h * 0.915f);
                    }

                    if (elapsedH1 >= 190) {
                        sCurrentStage = ScreenState.PHASE_HALFTIME;
                        sHalftimeStep = 0;
                        nextDelayMs = 3500;
                    } else {
                        nextDelayMs = 5500;
                    }
                    break;

                case PHASE_HALFTIME:
                    // Half-Time transition: Tap 'Mulai Babak Kedua >'
                    dispatchSimulatedTouchWithJitter(w * 0.825f, h * 0.915f);
                    sHalftimeStep++;
                    sCurrentPlannedAction = "⏸️ Jeda Babak: Menekan 'Mulai Babak Kedua >' (" + sHalftimeStep + "/2)...";
                    if (sHalftimeStep >= 2) {
                        sCurrentStage = ScreenState.PHASE_MATCH_HALF_2;
                        sMatchStartTime = System.currentTimeMillis();
                        nextDelayMs = 4500;
                    } else {
                        nextDelayMs = 3000;
                    }
                    break;

                case PHASE_MATCH_HALF_2:
                    // Tap center to skip cutscene / replay
                    dispatchSimulatedTouchWithJitter(w * 0.500f, h * 0.500f);
                    long elapsedH2 = (System.currentTimeMillis() - sMatchStartTime) / 1000;
                    long remainH2 = Math.max(0, 190 - elapsedH2);
                    sCurrentPlannedAction = "⚽ Babak 2: Skip cutscene / replay (" + remainH2 + "s tersisa)";

                    // Periodic safety tap on bottom-right every ~25s in case early full-time
                    if (elapsedH2 > 0 && elapsedH2 % 25 < 6) {
                        dispatchSimulatedTouchWithJitter(w * 0.825f, h * 0.915f);
                    }

                    if (elapsedH2 >= 190) {
                        sCurrentStage = ScreenState.PHASE_POST_MATCH;
                        sPostMatchStep = 0;
                        nextDelayMs = 3500;
                    } else {
                        nextDelayMs = 5500;
                    }
                    break;

                case PHASE_POST_MATCH:
                    // Post-match result screens: Sequential tap 'Berikut >' for EXP, stats, event points
                    dispatchSimulatedTouchWithJitter(w * 0.825f, h * 0.915f);
                    sPostMatchStep++;
                    sCurrentPlannedAction = "🏆 Selesai Laga: Menekan 'Berikut >' untuk Klaim Poin (" + sPostMatchStep + "/5)...";
                    if (sPostMatchStep >= 5) {
                        sCompletedMatches++;
                        showToast("🎉 Laga ke-" + sCompletedMatches + " Selesai! Memulai laga berikutnya...");
                        if (sTargetMatches > 0 && sCompletedMatches >= sTargetMatches) {
                            showToast("🏁 Target Grinding Tercapai: " + sCompletedMatches + " Laga Selesai!");
                            stopAfkGrinder();
                            return;
                        } else {
                            sCurrentStage = ScreenState.PHASE_MENU;
                            sMenuTapCount = 0;
                            nextDelayMs = 4000;
                        }
                    } else {
                        nextDelayMs = 2600;
                    }
                    break;

                default:
                    sCurrentStage = ScreenState.PHASE_MENU;
                    sMenuTapCount = 0;
                    nextDelayMs = 2800;
                    break;
            }

            updateLiveHud();

            if (sAfkRunning && !sAfkPaused) {
                sAfkHandler.postDelayed(sAfkLoopRunnable, nextDelayMs);
            }
        }
    };

    // ==========================================
    // SURFACEVIEW DETECTION & NATIVE TOUCH SIMULATION
    // ==========================================
    private static SurfaceView findSurfaceView(View root) {
        if (root == null) return null;
        if (root instanceof SurfaceView) {
            return (SurfaceView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child == sRootOverlay) continue;
                SurfaceView sv = findSurfaceView(child);
                if (sv != null) return sv;
            }
        }
        return null;
    }

    /**
     * Shows a lightweight visual touch ripple ring on screen so user can see exactly where clicks land.
     */
    private static void showTouchRipple(final float x, final float y) {
        if (sActivity == null || sRootOverlay == null) return;
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final int size = dpToPx(sActivity, 38);
                    final View ripple = new View(sActivity);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                    params.leftMargin = (int) (x - size / 2f);
                    params.topMargin = (int) (y - size / 2f);
                    ripple.setLayoutParams(params);

                    GradientDrawable shape = new GradientDrawable();
                    shape.setShape(GradientDrawable.OVAL);
                    shape.setColor(Color.parseColor("#6600E5FF")); // 40% cyan
                    shape.setStroke(dpToPx(sActivity, 2), Color.parseColor("#FFFFFF"));
                    ripple.setBackground(shape);
                    ripple.setElevation(dpToPx(sActivity, 20));

                    sRootOverlay.addView(ripple);

                    ripple.animate()
                            .scaleX(1.4f)
                            .scaleY(1.4f)
                            .alpha(0.0f)
                            .setDuration(400)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        sRootOverlay.removeView(ripple);
                                    } catch (Throwable ignored) {}
                                }
                            })
                            .start();
                } catch (Throwable ignored) {}
            }
        });
    }

    private static MotionEvent createTouchEvent(long downTime, long eventTime, int action, float x, float y) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[1];
        props[0] = new MotionEvent.PointerProperties();
        props[0].id = 0;
        props[0].toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[1];
        coords[0] = new MotionEvent.PointerCoords();
        coords[0].x = x;
        coords[0].y = y;
        coords[0].pressure = 1.0f;
        coords[0].size = 1.0f;

        return MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                1,
                props,
                coords,
                0,
                0,
                1.0f,
                1.0f,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0
        );
    }

    private static void sendEvent(MotionEvent event) {
        if (sActivity == null || event == null) return;

        // 1. Direct dispatch to SurfaceView (UE4 graphics viewport)
        try {
            SurfaceView sv = findSurfaceView(sActivity.getWindow().getDecorView());
            if (sv != null) {
                sv.dispatchTouchEvent(event);
            }
        } catch (Throwable ignored) {}

        // 2. Direct GameActivity onTouchEvent (UE4 nativeOnTouch bridge)
        try {
            sActivity.onTouchEvent(event);
        } catch (Throwable ignored) {}

        // 3. Fallback: DecorView dispatch
        try {
            View decor = sActivity.getWindow().getDecorView();
            if (decor != null) decor.dispatchTouchEvent(event);
        } catch (Throwable ignored) {}
    }

    /**
     * Adds ±1.0% randomized spatial jitter to simulate natural human fingertip tapping.
     */
    private static void dispatchSimulatedTouchWithJitter(float baseX, float baseY) {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        float jitterX = (sRandom.nextFloat() - 0.5f) * (dm.widthPixels * 0.020f);
        float jitterY = (sRandom.nextFloat() - 0.5f) * (dm.heightPixels * 0.020f);

        dispatchSimulatedTouch(baseX + jitterX, baseY + jitterY);
    }

    private static void dispatchSimulatedTouch(final float x, final float y) {
        if (sActivity == null) return;

        // Visual indicator on screen
        showTouchRipple(x, y);

        final long downTime = SystemClock.uptimeMillis();
        final MotionEvent down = createTouchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y);
        sendEvent(down);

        // Micro-move at 30ms so touch drivers register gesture intent
        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final long moveTime = SystemClock.uptimeMillis();
                    final MotionEvent move = createTouchEvent(downTime, moveTime, MotionEvent.ACTION_MOVE, x + 1.0f, y + 1.0f);
                    sendEvent(move);
                    move.recycle();
                } catch (Throwable ignored) {}
            }
        }, 30);

        int duration = 80 + sRandom.nextInt(40); // 80-120ms realistic hold
        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final long upTime = SystemClock.uptimeMillis();
                    final MotionEvent up = createTouchEvent(downTime, upTime, MotionEvent.ACTION_UP, x + 1.0f, y + 1.0f);
                    sendEvent(up);
                    up.recycle();
                } catch (Throwable ignored) {
                } finally {
                    down.recycle();
                }
            }
        }, duration);
    }

    private static void dispatchSwipe(final float startX, final float startY, final float endX, final float endY, final long durationMs) {
        if (sActivity == null) return;
        final long downTime = SystemClock.uptimeMillis();
        final MotionEvent down = createTouchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY);
        sendEvent(down);

        final int steps = 4;
        final long stepDelay = Math.max(10, durationMs / steps);
        for (int i = 1; i <= steps; i++) {
            final float progress = (float) i / steps;
            final float curX = startX + (endX - startX) * progress;
            final float curY = startY + (endY - startY) * progress;
            final int currentStep = i;

            sMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long eventTime = SystemClock.uptimeMillis();
                    if (currentStep < steps) {
                        MotionEvent move = createTouchEvent(downTime, eventTime, MotionEvent.ACTION_MOVE, curX, curY);
                        sendEvent(move);
                        move.recycle();
                    } else {
                        MotionEvent up = createTouchEvent(downTime, eventTime, MotionEvent.ACTION_UP, curX, curY);
                        sendEvent(up);
                        up.recycle();
                        down.recycle();
                    }
                }
            }, stepDelay * i);
        }
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
        bg.setColor(Color.parseColor("#1010B981"));
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

    public static void executeCommand(final String cmd) {
        if (sActivity == null) {
            showToast("Error: Game Activity belum siap");
            return;
        }

        sActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean executed = false;
                try {
                    Method m = sActivity.getClass().getMethod("nativeConsoleCommand", String.class);
                    m.invoke(sActivity, cmd);
                    executed = true;
                } catch (Throwable t1) {
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
                            PikoUtils.logger(TAG + ": Eksekusi perintah gagal: " + t2.getMessage());
                        } catch (Throwable ignored) {}
                    }
                }

                if (executed) {
                    showToast("⚡ UE4 Applied: " + cmd);
                    try {
                        PikoUtils.logger(TAG + ": UE4 command executed: " + cmd);
                    } catch (Throwable ignored) {}
                } else {
                    showToast("⚠️ Tidak dapat menjangkau konsol UE4");
                }
            }
        });
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

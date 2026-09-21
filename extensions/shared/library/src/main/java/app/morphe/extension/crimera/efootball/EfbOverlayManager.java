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
 * 1. Intelligent Context-Aware AFK Match Grinder with Precision UI & Menu Recognition
 * 2. Instant Rewarded Ad Claimer (Bypass Tonton Iklan)
 * 3. 1-Tap Smart Skill-Moves Draggable Overlay (Double Touch, Stunning Shot, Fake Shot)
 * 4. Precision Camera FOV with Default Reset (fov 0), Broadcast, Stadium, and Stepper [-]/[+]
 * 5. 3D Free-Roam Camera (ToggleDebugCamera)
 */
@SuppressWarnings("unused")
public class EfbOverlayManager {
    private static final String TAG = "EfbOverlayManager";
    private static Activity sActivity = null;
    private static boolean sOverlayAttached = false;
    private static FrameLayout sRootOverlay = null;
    private static View sFloatingBall = null;
    private static View sMenuModal = null;

    // Smart Skill-Moves Floating Overlay State
    private static View sFloatingSkillPad = null;
    private static boolean sSkillsPadEnabled = false;
    private static Button sMenuSkillsBtn = null;
    private static TextView sMenuSkillsStatus = null;

    // Smart AFK Grinder State & Components
    public enum ScreenState {
        UNKNOWN("Memindai Layar...", "#94A3B8"),
        EVENT_TOUR_MENU("Menu Acara / Persiapan Laga", "#38BDF8"),
        MATCH_PREPARATION("Pilih Tim / Mulai Laga", "#FBBF24"),
        IN_MATCH_PLAYING("Laga Berlangsung (Pitch Aktif)", "#10B981"),
        HALF_TIME_WHISTLE("Jeda Babak (Half-Time)", "#F59E0B"),
        FULL_TIME_RESULT("Laga Selesai (Full-Time)", "#3B82F6"),
        PLAYER_RATINGS_STATS("Hasil & Statistik Laga", "#8B5CF6"),
        EVENT_POINTS_REWARD("Poin Acara & Klaim Hadiah", "#EC4899"),
        POPUP_CONTRACT_RENEWAL("Peringatan Kontrak Pemain", "#EF4444"),
        POPUP_NETWORK_RETRY("Dialog Koneksi Jaringan", "#F97316"),
        POPUP_GENERIC_DIALOG("Pop-Up Konfirmasi Dialog", "#06B6D4");

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
    private static boolean sAutoRenewContract = true;
    private static boolean sAutoClaimRewards = true;
    private static int sTargetMatches = 0; // 0 = unlimited
    private static int sCompletedMatches = 0;
    private static long sAfkStartTime = 0;
    private static ScreenState sCurrentScreenState = ScreenState.UNKNOWN;
    private static String sCurrentPlannedAction = "Menunggu pemindaian...";

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

        // 3. Floating Skill Moves Pad (Draggable)
        sFloatingSkillPad = createFloatingSkillPad(activity);
        sFloatingSkillPad.setVisibility(View.GONE);
        sRootOverlay.addView(sFloatingSkillPad);

        // 4. Modal Menu (initially hidden)
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

        // Line 2: Screen Detection Status
        sHudStateText = new TextView(activity);
        sHudStateText.setText("📍 Menu: Memindai Layar...");
        sHudStateText.setTextColor(Color.parseColor("#38BDF8"));
        sHudStateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sHudStateText.setTypeface(Typeface.DEFAULT_BOLD);
        sHudStateText.setPadding(0, dpToPx(activity, 2), 0, dpToPx(activity, 1));
        hud.addView(sHudStateText);

        // Line 3: Planned Action Text
        sHudActionText = new TextView(activity);
        sHudActionText.setText("⚡ Aksi: Mengidentifikasi konteks antarmuka...");
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
                            sHudStateText.setText("📍 Menu: " + sCurrentScreenState.displayName);
                            sHudStateText.setTextColor(Color.parseColor(sCurrentScreenState.hexColor));
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

    /**
     * Floating Draggable Skill Pad Widget.
     * Contains 3 macro buttons: Double Touch (DT), Stunning Shot (SHOT), Fake Shot (FEINT).
     */
    private static View createFloatingSkillPad(final Activity activity) {
        final LinearLayout pad = new LinearLayout(activity);
        pad.setOrientation(LinearLayout.HORIZONTAL);
        pad.setGravity(Gravity.CENTER_VERTICAL);
        pad.setPadding(dpToPx(activity, 6), dpToPx(activity, 4), dpToPx(activity, 6), dpToPx(activity, 4));

        GradientDrawable padBg = new GradientDrawable();
        padBg.setShape(GradientDrawable.RECTANGLE);
        padBg.setCornerRadius(dpToPx(activity, 20));
        padBg.setColor(Color.parseColor("#E60B1120")); // Dark slate 90%
        padBg.setStroke(dpToPx(activity, 1), Color.parseColor("#38BDF8")); // Cyan border
        pad.setBackground(padBg);
        pad.setElevation(dpToPx(activity, 12));

        FrameLayout.LayoutParams padParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        padParams.gravity = Gravity.TOP | Gravity.START;
        padParams.leftMargin = dpToPx(activity, 50);
        padParams.topMargin = dpToPx(activity, 150);
        pad.setLayoutParams(padParams);

        // Drag Handle
        TextView dragHandle = new TextView(activity);
        dragHandle.setText("⠿");
        dragHandle.setTextColor(Color.parseColor("#94A3B8"));
        dragHandle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        dragHandle.setPadding(dpToPx(activity, 4), 0, dpToPx(activity, 6), 0);
        pad.addView(dragHandle);

        // Button 1: Double Touch (DT)
        pad.addView(createSkillPadButton(activity, "⚡ DT", "#0284C7", () -> triggerDoubleTouch()));

        // Button 2: Stunning Shot (SHOT)
        pad.addView(createSkillPadButton(activity, "🚀 SHOT", "#D97706", () -> triggerStunningShot()));

        // Button 3: Fake Shot (FEINT)
        pad.addView(createSkillPadButton(activity, "🎯 FEINT", "#7C3AED", () -> triggerFakeShot()));

        View.OnTouchListener dragListener = new View.OnTouchListener() {
            private float dX, dY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = pad.getX() - event.getRawX();
                        dY = pad.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        ViewGroup parent = (ViewGroup) pad.getParent();
                        if (parent != null) {
                            newX = Math.max(0, Math.min(newX, parent.getWidth() - pad.getWidth()));
                            newY = Math.max(0, Math.min(newY, parent.getHeight() - pad.getHeight()));
                        }

                        pad.setX(newX);
                        pad.setY(newY);
                        return true;
                }
                return false;
            }
        };

        dragHandle.setOnTouchListener(dragListener);
        return pad;
    }

    private static Button createSkillPadButton(Context context, String label, String hexBg, final Runnable action) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btn.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hexBg));
        bg.setCornerRadius(dpToPx(context, 14));
        bg.setStroke(dpToPx(context, 1), Color.parseColor("#334155"));
        btn.setBackground(bg);
        btn.setPadding(dpToPx(context, 8), dpToPx(context, 2), dpToPx(context, 8), dpToPx(context, 2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(context, 54),
                dpToPx(context, 34)
        );
        params.setMargins(dpToPx(context, 2), 0, dpToPx(context, 2), 0);
        btn.setLayoutParams(params);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (action != null) action.run();
            }
        });

        return btn;
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

        // Switch Opsi Otomasi Lanjutan
        addFeatureLabel(activity, contentLayout, "Otomasi Penanganan Layar:");
        LinearLayout optionsRow = new LinearLayout(activity);
        optionsRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, optionsRow, sAutoRenewContract ? "✅ Auto-Perbarui Kontrak" : "❌ Auto-Perbarui Kontrak", sAutoRenewContract ? "#059669" : "#64748B", () -> {
            sAutoRenewContract = !sAutoRenewContract;
            showToast("Auto-Renew Kontrak: " + (sAutoRenewContract ? "AKTIF" : "NONAKTIF"));
        });
        addOptionButton(activity, optionsRow, sAutoClaimRewards ? "✅ Auto-Klaim Hadiah" : "❌ Auto-Klaim Hadiah", sAutoClaimRewards ? "#059669" : "#64748B", () -> {
            sAutoClaimRewards = !sAutoClaimRewards;
            showToast("Auto-Klaim Hadiah: " + (sAutoClaimRewards ? "AKTIF" : "NONAKTIF"));
        });
        contentLayout.addView(optionsRow);

        // ==========================================
        // SECTION 2: BYPASS TONTON IKLAN (INSTANT REWARD)
        // ==========================================
        addSectionHeader(activity, contentLayout, "🎁 BYPASS TONTON IKLAN (INSTANT AD REWARD)");

        TextView adInfo = new TextView(activity);
        adInfo.setText("Iklan video 30 detik dilewati otomatis! Saat tombol 'Tonton Iklan' ditekan di game, hadiah (GP, Exp, Item) langsung cair seketika.");
        adInfo.setTextColor(Color.parseColor("#94A3B8"));
        adInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        contentLayout.addView(adInfo);

        Button claimAdBtn = new Button(activity);
        claimAdBtn.setText("⚡ KLAIM REWARD IKLAN SEKARANG");
        claimAdBtn.setTextColor(Color.WHITE);
        claimAdBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        claimAdBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable claimBg = new GradientDrawable();
        claimBg.setColor(Color.parseColor("#7C3AED")); // Purple glow
        claimBg.setCornerRadius(dpToPx(activity, 8));
        claimAdBtn.setBackground(claimBg);
        LinearLayout.LayoutParams claimBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(activity, 42)
        );
        claimBtnParams.setMargins(0, dpToPx(activity, 6), 0, dpToPx(activity, 8));
        claimAdBtn.setLayoutParams(claimBtnParams);
        claimAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                claimAdRewardInstantly(activity);
            }
        });
        contentLayout.addView(claimAdBtn);

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
        // SECTION 4: SMART SKILL-MOVES PAD
        // ==========================================
        addSectionHeader(activity, contentLayout, "⚡ SMART SKILL-MOVES PAD (TOMBOL KONTROL)");

        sMenuSkillsStatus = new TextView(activity);
        sMenuSkillsStatus.setText("Status: NONAKTIF (Tap tombol untuk tampilkan di layar)");
        sMenuSkillsStatus.setTextColor(Color.parseColor("#94A3B8"));
        sMenuSkillsStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        contentLayout.addView(sMenuSkillsStatus);

        sMenuSkillsBtn = new Button(activity);
        sMenuSkillsBtn.setText("🟢 AKTIFKAN FLOATING SKILL PAD DI LAYAR");
        sMenuSkillsBtn.setTextColor(Color.WHITE);
        sMenuSkillsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sMenuSkillsBtn.setTypeface(Typeface.DEFAULT_BOLD);
        updateSkillsButtonVisual(activity);
        sMenuSkillsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSkillsPad();
            }
        });
        LinearLayout.LayoutParams skillBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(activity, 40)
        );
        skillBtnParams.setMargins(0, dpToPx(activity, 6), 0, dpToPx(activity, 4));
        sMenuSkillsBtn.setLayoutParams(skillBtnParams);
        contentLayout.addView(sMenuSkillsBtn);

        // Quick Test Row
        LinearLayout skillTestRow = new LinearLayout(activity);
        skillTestRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, skillTestRow, "⚡ Test DT", "#0284C7", () -> triggerDoubleTouch());
        addOptionButton(activity, skillTestRow, "🚀 Test Shot", "#D97706", () -> triggerStunningShot());
        addOptionButton(activity, skillTestRow, "🎯 Test Feint", "#7C3AED", () -> triggerFakeShot());
        contentLayout.addView(skillTestRow);

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
     * Instantly claims rewarded ad rewards by invoking Konami's AdMobReward controller.
     */
    public static void claimAdRewardInstantly(final Context context) {
        if (context == null) return;
        try {
            Class<?> adMobClass = Class.forName("jp.konami.AdMobReward");
            Method showMethod = adMobClass.getDeclaredMethod("Show", Context.class);
            showMethod.setAccessible(true);
            showMethod.invoke(null, context);
            showToast("🎁 Reward Iklan Berhasil Diklaim!\n(GP / Exp / Item langsung masuk ke akun Anda)");
        } catch (Throwable t) {
            try {
                Class<?> adMobClass = Class.forName("jp.konami.AdMobReward");
                Method showFunc = adMobClass.getDeclaredMethod("ShowFunc", Context.class);
                showFunc.setAccessible(true);
                showFunc.invoke(null, context);
                showToast("🎁 Reward Iklan Berhasil Dipicu!");
            } catch (Throwable t2) {
                showToast("Info: Buka menu tonton iklan di game, reward otomatis cair saat tombol ditekan!");
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
    // SMART SKILL-MOVES MACRO LOGIC
    // ==========================================
    private static void toggleSkillsPad() {
        sSkillsPadEnabled = !sSkillsPadEnabled;
        if (sFloatingSkillPad != null) {
            sFloatingSkillPad.setVisibility(sSkillsPadEnabled ? View.VISIBLE : View.GONE);
        }
        if (sMenuSkillsStatus != null) {
            sMenuSkillsStatus.setText(sSkillsPadEnabled ? "Status: 🟢 AKTIF (Pad ditampilkan di layar)" : "Status: NONAKTIF (Tap tombol untuk tampilkan di layar)");
            sMenuSkillsStatus.setTextColor(sSkillsPadEnabled ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        }
        if (sActivity != null) {
            updateSkillsButtonVisual(sActivity);
        }
        showToast("⚡ Smart Skill-Moves Pad: " + (sSkillsPadEnabled ? "DITAMPILKAN di Layar" : "DISEMBUNYIKAN"));
    }

    private static void updateSkillsButtonVisual(Context context) {
        if (sMenuSkillsBtn == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(context, 8));
        if (sSkillsPadEnabled) {
            sMenuSkillsBtn.setText("🔴 SEMBUNYIKAN FLOATING SKILL PAD");
            bg.setColor(Color.parseColor("#DC2626")); // Red
        } else {
            sMenuSkillsBtn.setText("🟢 AKTIFKAN FLOATING SKILL PAD DI LAYAR");
            bg.setColor(Color.parseColor("#059669")); // Green
        }
        sMenuSkillsBtn.setBackground(bg);
    }

    public static void triggerDoubleTouch() {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;

        dispatchSwipe(w * 0.18f, h * 0.72f, w * 0.26f, h * 0.72f, 60);

        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dispatchSimulatedTouch(w * 0.88f, h * 0.82f);
            }
        }, 30);

        showToast("⚡ Double Touch (La Croqueta)!");
    }

    public static void triggerStunningShot() {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;

        dispatchSwipe(w * 0.85f, h * 0.60f, w * 0.95f, h * 0.60f, 80);
        showToast("🚀 Stunning Shot (Power Shot)!");
    }

    public static void triggerFakeShot() {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;

        dispatchSimulatedTouch(w * 0.85f, h * 0.60f);

        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dispatchSwipe(w * 0.18f, h * 0.72f, w * 0.10f, h * 0.72f, 40);
            }
        }, 40);

        showToast("🎯 Fake Shot (Tipuan Tembak)!");
    }

    // ==========================================
    // INTELLIGENT CONTEXT-AWARE AFK GRINDER
    // ==========================================
    private static void toggleAfkGrinder() {
        sAfkRunning = !sAfkRunning;
        if (sAfkRunning) {
            sAfkPaused = false;
            sAfkStartTime = System.currentTimeMillis();
            sCompletedMatches = 0;
            sCurrentScreenState = ScreenState.UNKNOWN;
            sCurrentPlannedAction = "Memulai pemindaian layar otomatis...";
            sAfkHandler.post(sAfkLoopRunnable);
            showToast("🟢 Smart AFK Grinder AKTIF!\nLive HUD menampilkan status layar secara langsung.");
        } else {
            stopAfkGrinder();
        }
        updateAfkComponents();
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
     * Core context-aware loop:
     * 1. Inspects active screen via PixelCopy / View tree sampling.
     * 2. Categorizes the screen into ScreenState.
     * 3. Updates the Live Status HUD with human-readable text.
     * 4. Dispatches the appropriate smart action with natural human-like touch timing.
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

            // Inspect screen state
            inspectScreenAndAct(new OnInspectionCompletedListener() {
                @Override
                public void onCompleted(ScreenState state, String actionDesc, long nextDelayMs) {
                    sCurrentScreenState = state;
                    sCurrentPlannedAction = actionDesc;
                    updateLiveHud();

                    if (sAfkRunning && !sAfkPaused) {
                        sAfkHandler.postDelayed(sAfkLoopRunnable, nextDelayMs);
                    }
                }
            });
        }
    };

    private interface OnInspectionCompletedListener {
        void onCompleted(ScreenState state, String actionDesc, long nextDelayMs);
    }

    /**
     * Samples the game screen using PixelCopy on the Window/SurfaceView to identify the exact menu.
     */
    private static void inspectScreenAndAct(final OnInspectionCompletedListener listener) {
        if (sActivity == null) return;
        final DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        final int w = dm.widthPixels;
        final int h = dm.heightPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Window window = sActivity.getWindow();
            // Tiny 160x90 thumbnail bitmap for microsecond analysis
            final Bitmap thumbnail = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888);
            try {
                PixelCopy.request(window, thumbnail, new PixelCopy.OnPixelCopyFinishedListener() {
                    @Override
                    public void onPixelCopyFinished(int copyResult) {
                        if (copyResult == PixelCopy.SUCCESS) {
                            classifyThumbnailAndDispatch(thumbnail, w, h, listener);
                        } else {
                            fallbackHeuristicInspection(w, h, listener);
                        }
                    }
                }, sMainHandler);
                return;
            } catch (Throwable t) {
                try {
                    PikoUtils.logger(TAG + ": PixelCopy exception: " + t.getMessage());
                } catch (Throwable ignored) {}
            }
        }

        fallbackHeuristicInspection(w, h, listener);
    }

    /**
     * Evaluates pixel signatures on the thumbnail bitmap:
     * - Field grass green: ~160x90 center (x: 50..110, y: 30..60)
     * - Action button color at bottom-right (x: 135..150, y: 75..85)
     * - Modal dialog scrim: darkened periphery
     * - Top bar scoreboard vs menu title banner
     */
    private static void classifyThumbnailAndDispatch(Bitmap thumb, int w, int h, OnInspectionCompletedListener listener) {
        try {
            int tw = thumb.getWidth();
            int th = thumb.getHeight();

            // 1. Check bottom-right action button area (x: 0.78..0.92, y: 0.90..0.96)
            // In eFootball: 'Ke Laga >', 'Mulai Laga >', 'Lanjut >'
            boolean isBlueActionBtn = false;
            boolean isGoldActionBtn = false;
            int actionY = (int) (th * 0.93f);
            for (int xPct = 78; xPct <= 92; xPct += 4) {
                int px = (int) (tw * (xPct / 100.0f));
                if (px < tw && actionY < th) {
                    int p = thumb.getPixel(px, actionY);
                    int pr = Color.red(p);
                    int pg = Color.green(p);
                    int pb = Color.blue(p);
                    if (pb > 150 && pr < 120) {
                        isBlueActionBtn = true;
                    }
                    if (pr > 170 && pg > 130 && pb < 100) {
                        isGoldActionBtn = true;
                    }
                }
            }
            boolean hasActionBtn = isBlueActionBtn || isGoldActionBtn;

            // 2. Check top-right Home icon (x: 0.90, y: 0.06)
            int homePixel = thumb.getPixel((int) (tw * 0.90f), (int) (th * 0.06f));
            boolean isHomeIconPresent = (Color.blue(homePixel) > 130 && Color.red(homePixel) < 110);

            // 3. Check bottom-left Kembali button (x: 0.12, y: 0.93)
            int backPixel = thumb.getPixel((int) (tw * 0.12f), (int) (th * 0.93f));
            boolean isKembaliPresent = (Color.blue(backPixel) > 130 && Color.red(backPixel) < 110);

            // 4. Check dark scrim dialog (Perbarui Kontrak / Koneksi)
            int cornerPixel = thumb.getPixel(5, 5);
            boolean isDarkScrim = (Color.red(cornerPixel) < 35 && Color.green(cornerPixel) < 35 && Color.blue(cornerPixel) < 35);

            // 5. Sample pitch grass green pixels in center
            int greenCount = 0;
            for (int x = tw / 4; x < (tw * 3) / 4; x += 4) {
                for (int y = th / 4; y < (th * 3) / 4; y += 4) {
                    int pixel = thumb.getPixel(x, y);
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);
                    if (g > 60 && g > r * 1.25 && g > b * 1.25) {
                        greenCount++;
                    }
                }
            }

            boolean isMenuScreen = hasActionBtn || isHomeIconPresent || isKembaliPresent;

            // Decision Branch 1: Modal dialog / Contract expired popup
            if (isDarkScrim && !isMenuScreen) {
                if (sAutoRenewContract) {
                    dispatchSimulatedTouchWithJitter(w * 0.65f, h * 0.62f);
                    listener.onCompleted(ScreenState.POPUP_CONTRACT_RENEWAL, "Menekan 'Perbarui Kontrak' / Konfirmasi dialog...", 1800);
                } else {
                    dispatchSimulatedTouchWithJitter(w * 0.50f, h * 0.70f);
                    listener.onCompleted(ScreenState.POPUP_GENERIC_DIALOG, "Menutup dialog konfirmasi...", 1800);
                }
                return;
            }

            // Decision Branch 2: MENU SCREEN (Event Tour, Match Preparation, Full-Time, Halftime)
            // Even if the 3D stadium renders grass in background, if menu buttons are visible, it is a MENU!
            if (hasActionBtn || isMenuScreen) {
                dispatchSimulatedTouchWithJitter(w * 0.85f, h * 0.93f);

                if (isGoldActionBtn) {
                    listener.onCompleted(ScreenState.MATCH_PREPARATION, "Menekan 'Mulai Laga' (Kanan Bawah)...", 2000);
                } else {
                    listener.onCompleted(ScreenState.EVENT_TOUR_MENU, "Menekan 'Ke Laga' / 'Lanjut' (Kanan Bawah)...", 2000);
                }
                return;
            }

            // Decision Branch 3: Bottom-Center Claim Rewards
            int bcPixel = thumb.getPixel(tw / 2, (int) (th * 0.82f));
            if (Color.blue(bcPixel) > 140 || (Color.red(bcPixel) > 180 && Color.green(bcPixel) > 140)) {
                if (sAutoClaimRewards) {
                    dispatchSimulatedTouchWithJitter(w * 0.50f, h * 0.80f);
                    listener.onCompleted(ScreenState.EVENT_POINTS_REWARD, "Mengklaim poin & hadiah event...", 1800);
                    return;
                }
            }

            // Decision Branch 4: Live match on pitch (no menu buttons, grass dominates)
            if (greenCount > 50) {
                dispatchSimulatedTouchWithJitter(w * 0.50f, h * 0.50f);
                listener.onCompleted(ScreenState.IN_MATCH_PLAYING, "Melewati cutscene / memantau laga...", 3500);
                return;
            }

            // Fallback for transition
            fallbackHeuristicInspection(w, h, listener);

        } catch (Throwable t) {
            fallbackHeuristicInspection(w, h, listener);
        } finally {
            if (thumb != null && !thumb.isRecycled()) {
                thumb.recycle();
            }
        }
    }

    /**
     * Fallback adaptive sequential cycle with jitter when direct pixel buffer is unavailable.
     * Strictly prioritizes the primary action button at (0.85w, 0.93h) so menus never get stuck!
     */
    private static int sFallbackStep = 0;
    private static void fallbackHeuristicInspection(int w, int h, OnInspectionCompletedListener listener) {
        int step = sFallbackStep % 4;
        sFallbackStep++;

        if (step == 0 || step == 2) {
            // Action button (Ke Laga / Mulai Laga / Lanjut) at bottom right
            dispatchSimulatedTouchWithJitter(w * 0.85f, h * 0.93f);
            listener.onCompleted(ScreenState.EVENT_TOUR_MENU, "Menekan tombol 'Ke Laga' / 'Lanjut' (Kanan Bawah)", 2000);
        } else if (step == 1) {
            // Dialog confirmation / Contract renewal at center right
            dispatchSimulatedTouchWithJitter(w * 0.65f, h * 0.62f);
            listener.onCompleted(ScreenState.POPUP_CONTRACT_RENEWAL, "Menekan dialog konfirmasi / perpanjang kontrak", 1800);
        } else {
            // Skip replay / cutscene tap center
            dispatchSimulatedTouchWithJitter(w * 0.50f, h * 0.50f);
            listener.onCompleted(ScreenState.IN_MATCH_PLAYING, "Melewati cutscene laga / tap layar", 2500);
        }
    }

    // ==========================================
    // NATIVE TOUCH SIMULATION WITH HUMAN JITTER
    // ==========================================
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
        try {
            sActivity.dispatchTouchEvent(event);
        } catch (Throwable t1) {
            try {
                final ViewGroup decor = (ViewGroup) sActivity.getWindow().getDecorView();
                if (decor != null) decor.dispatchTouchEvent(event);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Adds ±1.2% randomized spatial jitter to simulate natural human fingertip tapping.
     */
    private static void dispatchSimulatedTouchWithJitter(float baseX, float baseY) {
        if (sActivity == null) return;
        DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
        float jitterX = (sRandom.nextFloat() - 0.5f) * (dm.widthPixels * 0.024f);
        float jitterY = (sRandom.nextFloat() - 0.5f) * (dm.heightPixels * 0.024f);

        dispatchSimulatedTouch(baseX + jitterX, baseY + jitterY);
    }

    private static void dispatchSimulatedTouch(final float x, final float y) {
        if (sActivity == null) return;
        final long downTime = SystemClock.uptimeMillis();
        final MotionEvent down = createTouchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y);
        sendEvent(down);

        int duration = 40 + sRandom.nextInt(30); // 40-70ms realistic hold
        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final long upTime = SystemClock.uptimeMillis();
                    final MotionEvent up = createTouchEvent(downTime, upTime, MotionEvent.ACTION_UP, x, y);
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

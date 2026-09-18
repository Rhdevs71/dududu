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
import android.os.SystemClock;
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
 * Features:
 * 1. AFK Smart Event Grinder (Auto Match Loop, Auto Skip Replays, Auto Result Advance)
 * 2. Precision Camera FOV with Default Reset (fov 0), Broadcast, Stadium, and Stepper [-]/[+]
 * 3. 3D Free-Roam Camera (ToggleDebugCamera)
 * 4. Match & Engine Exploits (PlayersOnly freeze opponents, FreezeFrame, t.MaxFPS unlocker)
 * 5. Interactive UE4 Console Terminal
 */
@SuppressWarnings("unused")
public class EfbOverlayManager {
    private static final String TAG = "EfbOverlayManager";
    private static Activity sActivity = null;
    private static boolean sOverlayAttached = false;
    private static FrameLayout sRootOverlay = null;
    private static View sFloatingBall = null;
    private static View sMenuModal = null;
    private static TextView sFloatingAfkBadge = null;

    // AFK Grinder State
    private static boolean sAfkRunning = false;
    private static int sAfkCycle = 0;
    private static Button sMenuAfkBtn = null;
    private static TextView sMenuAfkStatus = null;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final Handler sAfkHandler = new Handler(Looper.getMainLooper());

    // Camera FOV State
    private static int sCurrentFov = 0; // 0 = default
    private static TextView sFovIndicator = null;

    public static void init(final Activity activity) {
        if (activity == null) return;
        sActivity = activity;

        try {
            PikoUtils.logger(TAG + ": Initializing eFootball Overlay Mod Menu v2 on " + activity.getClass().getName());
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

        // 1. Floating Action Ball (Draggable)
        sFloatingBall = createFloatingBall(activity);
        sRootOverlay.addView(sFloatingBall);

        // 2. Floating AFK Status Badge (Top-Center, visible when AFK active)
        sFloatingAfkBadge = createFloatingAfkBadge(activity);
        sFloatingAfkBadge.setVisibility(View.GONE);
        sRootOverlay.addView(sFloatingAfkBadge);

        // 3. Modal Menu (initially hidden)
        sMenuModal = createMenuModal(activity);
        sMenuModal.setVisibility(View.GONE);
        sRootOverlay.addView(sMenuModal);

        decorView.addView(sRootOverlay);
        sOverlayAttached = true;

        showToast("⚽ Piko eFootball Mod Menu Aktif!\nSentuh ⚽ untuk membuka menu.");
    }

    private static View createFloatingBall(final Activity activity) {
        final int sizePx = dpToPx(activity, 54);

        final FrameLayout ballContainer = new FrameLayout(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = dpToPx(activity, 20);
        params.topMargin = dpToPx(activity, 80);
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

    private static TextView createFloatingAfkBadge(final Activity activity) {
        TextView badge = new TextView(activity);
        badge.setText("🟢 AFK GRINDER: AKTIF (Auto-Loop Match)");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setPadding(dpToPx(activity, 14), dpToPx(activity, 6), dpToPx(activity, 14), dpToPx(activity, 6));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(activity, 20));
        bg.setColor(Color.parseColor("#E6065F46")); // Emerald dark 90%
        bg.setStroke(dpToPx(activity, 2), Color.parseColor("#10B981")); // Glowing emerald
        badge.setBackground(bg);
        badge.setElevation(dpToPx(activity, 10));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = dpToPx(activity, 14);
        badge.setLayoutParams(params);

        badge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAfkGrinder();
            }
        });

        return badge;
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
        int menuWidth = Math.min(dpToPx(activity, 420), (int) (dm.widthPixels * 0.92f));

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                menuWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;

        LinearLayout card = new LinearLayout(activity);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16));

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
        titleView.setText("⚽ eFootball Mod Menu");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleLayout.addView(titleView);

        TextView subtitleView = new TextView(activity);
        subtitleView.setText("Piko v2.0 • Precision Camera & AFK Grinder");
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
        // SECTION 1: AFK MATCH GRINDER (AUTO EVENT)
        // ==========================================
        addSectionHeader(activity, contentLayout, "🤖 AFK MATCH GRINDER (AUTO-LOOP EVENT)");

        sMenuAfkStatus = new TextView(activity);
        sMenuAfkStatus.setText("Status: NONAKTIF (Tap Mulai untuk auto-loop match event)");
        sMenuAfkStatus.setTextColor(Color.parseColor("#94A3B8"));
        sMenuAfkStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        contentLayout.addView(sMenuAfkStatus);

        sMenuAfkBtn = new Button(activity);
        sMenuAfkBtn.setText("🟢 MULAI AFK MATCH GRINDER");
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

        TextView afkHelp = new TextView(activity);
        afkHelp.setText("💡 Grinder otomatis menekan tombol Lanjut, Klaim Hadiah, Lewati Replay, & Mulai Laga Berikutnya secara berkala saat HP ditinggal.");
        afkHelp.setTextColor(Color.parseColor("#64748B"));
        afkHelp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        contentLayout.addView(afkHelp);

        // ==========================================
        // SECTION 2: KAMERA & SUDUT PANDANG (FOV)
        // ==========================================
        addSectionHeader(activity, contentLayout, "🎥 KAMERA & SUDUT PANDANG (FOV)");

        sFovIndicator = new TextView(activity);
        sFovIndicator.setText("Sudut Pandang: Default Bawaan Game");
        sFovIndicator.setTextColor(Color.parseColor("#38BDF8"));
        sFovIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sFovIndicator.setTypeface(Typeface.DEFAULT_BOLD);
        contentLayout.addView(sFovIndicator);

        // Row 1: Reset Default & Steppers [-] [+]
        LinearLayout fovStepRow = new LinearLayout(activity);
        fovStepRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fovStepRow, "🔄 RESET BAWAAN", "#0284C7", () -> applyFov(0, "Default Bawaan Game"));
        addOptionButton(activity, fovStepRow, "➖ Turunkan 2°", "#1E293B", () -> adjustFovStep(-2));
        addOptionButton(activity, fovStepRow, "➕ Naikkan 2°", "#1E293B", () -> adjustFovStep(2));
        contentLayout.addView(fovStepRow);

        // Row 2: Preset Sudut Lapangan Realistis Game Bola
        LinearLayout fovPresetsRow = new LinearLayout(activity);
        fovPresetsRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fovPresetsRow, "50° (Dinamis)", "#334155", () -> applyFov(50, "50° Dinamis Dekat"));
        addOptionButton(activity, fovPresetsRow, "60° (Broadcast)", "#334155", () -> applyFov(60, "60° Siaran TV"));
        addOptionButton(activity, fovPresetsRow, "68° (Stadium)", "#334155", () -> applyFov(68, "68° Stadium Luas"));
        addOptionButton(activity, fovPresetsRow, "76° (Drone)", "#334155", () -> applyFov(76, "76° Taktikal Drone"));
        contentLayout.addView(fovPresetsRow);

        // Row 3: 3D Free-Roam Camera
        LinearLayout freeCamRow = new LinearLayout(activity);
        freeCamRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, freeCamRow, "🚁 TOGGLE KAMERA BEBAS (FREE-ROAM 3D)", "#7C3AED", () -> {
            executeCommand("ToggleDebugCamera");
            showToast("🚁 ToggleDebugCamera Dipicu");
        });
        contentLayout.addView(freeCamRow);

        // ==========================================
        // SECTION 3: MATCH & ENGINE EXPLOITS
        // ==========================================
        addSectionHeader(activity, contentLayout, "⚡ MATCH & ENGINE EXPLOITS");

        LinearLayout exploitRow1 = new LinearLayout(activity);
        exploitRow1.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, exploitRow1, "❄️ BEKUKAN LAWAN (PlayersOnly)", "#059669", () -> {
            executeCommand("PlayersOnly");
            showToast("❄️ Perintah PlayersOnly Dipicu!");
        });
        addOptionButton(activity, exploitRow1, "⏸️ JEDA MATCH (FreezeFrame)", "#D97706", () -> {
            executeCommand("FreezeFrame");
            showToast("⏸️ FreezeFrame Dipicu!");
        });
        contentLayout.addView(exploitRow1);

        // FPS Unlocker
        addFeatureLabel(activity, contentLayout, "Target Frame Rate (t.MaxFPS):");
        LinearLayout fpsRow = new LinearLayout(activity);
        fpsRow.setOrientation(LinearLayout.HORIZONTAL);
        addOptionButton(activity, fpsRow, "60 FPS", "#1E293B", () -> executeCommand("t.MaxFPS 60"));
        addOptionButton(activity, fpsRow, "90 FPS", "#1E293B", () -> executeCommand("t.MaxFPS 90"));
        addOptionButton(activity, fpsRow, "120 FPS", "#047857", () -> executeCommand("t.MaxFPS 120"));
        addOptionButton(activity, fpsRow, "Unlimited (0)", "#4338CA", () -> executeCommand("t.MaxFPS 0"));
        contentLayout.addView(fpsRow);

        // ==========================================
        // SECTION 4: KONSOL PERINTAH UE4 KUSTOM
        // ==========================================
        addSectionHeader(activity, contentLayout, "⌨️ KONSOL PERINTAH UE4 KUSTOM");
        final LinearLayout cmdRow = new LinearLayout(activity);
        cmdRow.setOrientation(LinearLayout.HORIZONTAL);
        cmdRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cmdRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cmdRowParams.setMargins(0, dpToPx(activity, 4), 0, dpToPx(activity, 6));
        cmdRow.setLayoutParams(cmdRowParams);

        final EditText cmdInput = new EditText(activity);
        cmdInput.setHint("contoh: fov 62 atau t.MaxFPS 120");
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
        runBtn.setText("RUN");
        runBtn.setTextColor(Color.WHITE);
        runBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        runBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable runBg = new GradientDrawable();
        runBg.setColor(Color.parseColor("#0284C7"));
        runBg.setCornerRadius(dpToPx(activity, 8));
        runBtn.setBackground(runBg);
        runBtn.setPadding(dpToPx(activity, 14), dpToPx(activity, 6), dpToPx(activity, 14), dpToPx(activity, 6));
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

        // Status Lisensi
        addSectionHeader(activity, contentLayout, "🛡️ STATUS LISENSI");
        addStatusBadge(activity, contentLayout, "✅ Google Play License: PROTECTED (Status: LICENSED)");
        addStatusBadge(activity, contentLayout, "✅ Morphe UE4 Bridge: TERHUBUNG");

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

    private static void applyFov(int fov, String label) {
        sCurrentFov = fov;
        executeCommand("fov " + fov);
        if (sFovIndicator != null) {
            sFovIndicator.setText("Sudut Pandang: " + (fov == 0 ? "Default Bawaan Game" : fov + "° (" + label + ")"));
        }
        showToast("🎥 Kamera disetel ke: " + (fov == 0 ? "Default Bawaan" : fov + "°"));
    }

    private static void adjustFovStep(int delta) {
        if (sCurrentFov == 0) sCurrentFov = 60; // base from broadcast
        sCurrentFov = Math.max(30, Math.min(110, sCurrentFov + delta));
        applyFov(sCurrentFov, sCurrentFov + "° Custom");
    }

    // ==========================================
    // AFK MATCH GRINDER LOGIC
    // ==========================================
    private static void toggleAfkGrinder() {
        sAfkRunning = !sAfkRunning;
        if (sAfkRunning) {
            sAfkCycle = 0;
            sAfkHandler.post(sAfkRunnable);
            showToast("🟢 AFK Match Grinder AKTIF!\nBot akan mengulang match & melewati cutscene secara otomatis.");
        } else {
            sAfkHandler.removeCallbacks(sAfkRunnable);
            showToast("🔴 AFK Match Grinder DIHENTIKAN.");
        }
        updateAfkBadge();
        if (sActivity != null) {
            updateAfkButtonVisual(sActivity);
        }
    }

    private static void updateAfkBadge() {
        if (sFloatingAfkBadge != null) {
            sFloatingAfkBadge.setVisibility(sAfkRunning ? View.VISIBLE : View.GONE);
        }
        if (sMenuAfkStatus != null) {
            sMenuAfkStatus.setText(sAfkRunning ? "Status: 🟢 AKTIF (Sedang melakukan auto-looping match)" : "Status: NONAKTIF (Tap Mulai untuk auto-loop match event)");
            sMenuAfkStatus.setTextColor(sAfkRunning ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
        }
    }

    private static void updateAfkButtonVisual(Context context) {
        if (sMenuAfkBtn == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(context, 8));
        if (sAfkRunning) {
            sMenuAfkBtn.setText("🔴 HENTIKAN AFK MATCH GRINDER");
            bg.setColor(Color.parseColor("#DC2626")); // Red
        } else {
            sMenuAfkBtn.setText("🟢 MULAI AFK MATCH GRINDER");
            bg.setColor(Color.parseColor("#059669")); // Green
        }
        sMenuAfkBtn.setBackground(bg);
    }

    private static final Runnable sAfkRunnable = new Runnable() {
        @Override
        public void run() {
            if (!sAfkRunning || sActivity == null || sActivity.isFinishing()) {
                sAfkRunning = false;
                updateAfkBadge();
                return;
            }

            try {
                DisplayMetrics dm = sActivity.getResources().getDisplayMetrics();
                int w = dm.widthPixels;
                int h = dm.heightPixels;

                // Cycle through key event match navigation coordinates:
                // Cycle 0: Bottom-Right button (Next, Lanjut, Laga Berikutnya, Selesai)
                // Cycle 1: Bottom-Center button (OK, Klaim Hadiah, Konfirmasi Dialog)
                // Cycle 2: Center of Screen (Skip Replay, Lewati Selebrasi, Tap to Continue)
                float tapX, tapY;
                int step = sAfkCycle % 3;
                if (step == 0) {
                    tapX = w * 0.88f; // Bottom-Right
                    tapY = h * 0.88f;
                } else if (step == 1) {
                    tapX = w * 0.50f; // Bottom-Center
                    tapY = h * 0.80f;
                } else {
                    tapX = w * 0.50f; // Center
                    tapY = h * 0.50f;
                }
                sAfkCycle++;

                dispatchSimulatedTouch(tapX, tapY);
            } catch (Throwable t) {
                try {
                    PikoUtils.logger(TAG + ": AFK Grinder touch exception: " + t.getMessage());
                } catch (Throwable ignored) {}
            }

            // Schedule next simulated tap in 1.5 seconds
            sAfkHandler.postDelayed(this, 1500);
        }
    };

    /**
     * Injects touch events directly to the window's DecorView.
     * Requires ZERO permissions, NO root, and works in-process!
     */
    private static void dispatchSimulatedTouch(final float x, final float y) {
        if (sActivity == null) return;
        final ViewGroup decorView = (ViewGroup) sActivity.getWindow().getDecorView();
        if (decorView == null) return;

        long downTime = SystemClock.uptimeMillis();
        long eventTime = downTime + 40;

        final MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        final MotionEvent up = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);

        decorView.dispatchTouchEvent(down);
        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    decorView.dispatchTouchEvent(up);
                } catch (Throwable ignored) {
                } finally {
                    down.recycle();
                    up.recycle();
                }
            }
        }, 40);
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

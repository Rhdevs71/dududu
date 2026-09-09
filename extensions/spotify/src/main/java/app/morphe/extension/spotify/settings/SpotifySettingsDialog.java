/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.settings.BooleanSetting;
import app.morphe.extension.crimera.sharedPreference.SharedPref;

public class SpotifySettingsDialog {

    private static final String[] SETTING_LABELS = new String[] {
        "Blokir Iklan Audio & Video",
        "Bebas Batasan Skip & Seekbar",
        "Pemutaran Langsung (On-Demand)",
        "Nonaktifkan Mode Acak Paksa",
        "Buka Lirik Berjalan Tanpa Batas",
        "Kualitas Audio 320kbps (Very High)",
        "Blokir Pop-up Promosi (Anti-Upsell)",
        "Simulasi Akun Premium (ProductState)",
        "Pencatatan Log Debug Piko"
    };

    private static final BooleanSetting[] SETTING_KEYS = new BooleanSetting[] {
        SpotifySettings.BLOCK_ADS,
        SpotifySettings.UNLIMITED_SKIPS,
        SpotifySettings.ON_DEMAND_PLAYBACK,
        SpotifySettings.FORCE_SHUFFLE_DISABLED,
        SpotifySettings.UNLIMITED_LYRICS,
        SpotifySettings.VERY_HIGH_AUDIO_QUALITY,
        SpotifySettings.ANTI_UPSELL,
        SpotifySettings.PRODUCT_STATE_SPOOF,
        SpotifySettings.PIKO_DEBUG
    };

    /**
     * Menampilkan dialog pengaturan multi-choice Piko Spotify.
     */
    public static void show(Context context) {
        if (context == null) return;

        try {
            boolean[] checkedStates = new boolean[SETTING_KEYS.length];
            for (int i = 0; i < SETTING_KEYS.length; i++) {
                checkedStates[i] = SharedPref.getBooleanPref(SETTING_KEYS[i]);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Pengaturan Piko Spotify");
            builder.setMultiChoiceItems(SETTING_LABELS, checkedStates, (dialog, which, isChecked) -> {
                if (which >= 0 && which < SETTING_KEYS.length) {
                    BooleanSetting setting = SETTING_KEYS[which];
                    SharedPref.setBooleanPref(setting.key, isChecked);

                    // Sync related settings
                    if (setting == SpotifySettings.UNLIMITED_SKIPS) {
                        SharedPref.setBooleanPref(SpotifySettings.ENABLE_SEEK.key, isChecked);
                    }
                }
            });

            builder.setPositiveButton("Selesai", (dialog, which) -> {
                dialog.dismiss();
                try {
                    Toast.makeText(context, "Pengaturan Piko Spotify berhasil disimpan!", Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            });

            builder.setNeutralButton("Reset Default", (dialog, which) -> {
                for (BooleanSetting setting : SETTING_KEYS) {
                    SharedPref.setBooleanPref(setting.key, setting.defaultValue);
                }
                Toast.makeText(context, "Pengaturan direset ke default!", Toast.LENGTH_SHORT).show();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Throwable t) {
            PikoUtils.logger("SpotifySettingsDialog", "Gagal menampilkan dialog pengaturan: " + t.getMessage(), t);
        }
    }
}

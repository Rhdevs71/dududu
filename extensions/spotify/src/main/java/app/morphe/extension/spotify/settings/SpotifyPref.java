/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.settings;

import app.morphe.extension.crimera.sharedPreference.SharedPref;

public class SpotifyPref {
    public static boolean pikoDebug() {
        return SharedPref.getBooleanPref(SpotifySettings.PIKO_DEBUG);
    }

    public static boolean blockAds() {
        return SharedPref.getBooleanPref(SpotifySettings.BLOCK_ADS);
    }

    public static boolean unlimitedSkips() {
        return SharedPref.getBooleanPref(SpotifySettings.UNLIMITED_SKIPS);
    }

    public static boolean enableSeek() {
        return SharedPref.getBooleanPref(SpotifySettings.ENABLE_SEEK);
    }

    public static boolean onDemandPlayback() {
        return SharedPref.getBooleanPref(SpotifySettings.ON_DEMAND_PLAYBACK);
    }

    public static boolean forceShuffleDisabled() {
        return SharedPref.getBooleanPref(SpotifySettings.FORCE_SHUFFLE_DISABLED);
    }

    public static boolean veryHighAudioQuality() {
        return SharedPref.getBooleanPref(SpotifySettings.VERY_HIGH_AUDIO_QUALITY);
    }

    public static boolean unlimitedLyrics() {
        return SharedPref.getBooleanPref(SpotifySettings.UNLIMITED_LYRICS);
    }

    public static boolean antiUpsell() {
        return SharedPref.getBooleanPref(SpotifySettings.ANTI_UPSELL);
    }

    public static boolean productStateSpoof() {
        return SharedPref.getBooleanPref(SpotifySettings.PRODUCT_STATE_SPOOF);
    }
}


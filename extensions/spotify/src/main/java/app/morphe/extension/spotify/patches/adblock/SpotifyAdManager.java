/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.patches.adblock;

import app.morphe.extension.spotify.settings.SpotifyPref;
import app.morphe.extension.spotify.utils.SpotifyLog;

public class SpotifyAdManager {
    private static final String TAG = "SpotifyAdManager";

    /**
     * Returns false if ad-blocking is enabled, effectively neutralizing ad flags in Spotify protos.
     */
    public static boolean neutralizeAdFlag(boolean originalHasAd) {
        if (SpotifyPref.blockAds()) {
            if (originalHasAd && SpotifyPref.pikoDebug()) {
                SpotifyLog.d(TAG, "Neutralized audio/video ad slot from Spotify response");
            }
            return false;
        }
        return originalHasAd;
    }

    /**
     * Checks whether ad blocking is active.
     */
    public static boolean isAdBlockActive() {
        return SpotifyPref.blockAds();
    }
}

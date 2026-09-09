/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.patches.upsell;

import app.morphe.extension.spotify.settings.SpotifyPref;
import app.morphe.extension.spotify.utils.SpotifyLog;

public class SpotifyAntiUpsell {
    private static final String TAG = "SpotifyAntiUpsell";

    /**
     * Intercepts upsell eligibility checks (e.g. ShouldUpsellRequest).
     * When Anti-Upsell is enabled, always returns false so paid promotional popups are muted.
     */
    public static boolean isUpsellEligible(boolean originalEligible) {
        if (SpotifyPref.antiUpsell()) {
            if (originalEligible && SpotifyPref.pikoDebug()) {
                SpotifyLog.d(TAG, "Suppressed Spotify premium upsell / paywall popup request");
            }
            return false;
        }
        return originalEligible;
    }

    /**
     * Intercepts banner / card visibility flags for premium promotions.
     */
    public static boolean shouldShowPromo(boolean originalShow) {
        if (SpotifyPref.antiUpsell()) {
            return false;
        }
        return originalShow;
    }
}

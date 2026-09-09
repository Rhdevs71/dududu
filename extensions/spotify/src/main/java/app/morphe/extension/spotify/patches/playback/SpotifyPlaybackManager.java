/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.patches.playback;

import app.morphe.extension.spotify.settings.SpotifyPref;
import app.morphe.extension.spotify.utils.SpotifyLog;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SpotifyPlaybackManager {
    private static final String TAG = "SpotifyPlaybackManager";

    /**
     * Replaces disallow reason sets with empty sets when skips/seek are unlocked.
     */
    public static Set<?> getDisallowReasons(Set<?> originalReasons) {
        if (SpotifyPref.unlimitedSkips() || SpotifyPref.enableSeek()) {
            return Collections.emptySet();
        }
        return originalReasons;
    }

    /**
     * Overrides ProductState attribute map queries.
     */
    public static String getProductStateAttribute(String key, String originalValue) {
        return app.morphe.extension.spotify.patches.productstate.SpotifyProductStateSpoofer.getProductStateAttribute(key, originalValue);
    }


    /**
     * Checks if lyrics capping check should be bypassed.
     */
    public static boolean isLyricsCapped(boolean originalCapped) {
        if (SpotifyPref.unlimitedLyrics()) {
            return false;
        }
        return originalCapped;
    }
}

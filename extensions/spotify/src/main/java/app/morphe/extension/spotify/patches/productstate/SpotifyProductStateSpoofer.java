/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.patches.productstate;

import app.morphe.extension.spotify.settings.SpotifyPref;
import app.morphe.extension.spotify.utils.SpotifyLog;

import java.util.HashMap;
import java.util.Map;

public class SpotifyProductStateSpoofer {
    private static final String TAG = "SpotifyProductState";

    /**
     * Intercepts single attribute lookups from ProductState models or Esperanto clients.
     */
    public static String getProductStateAttribute(String key, String originalValue) {
        if (key == null || !SpotifyPref.productStateSpoof()) {
            return originalValue;
        }

        switch (key.toLowerCase()) {
            case "type":
                return "premium";
            case "can_play_on_demand":
            case "interruption-free":
            case "unlimited-skips":
                return "1";
            case "ads":
            case "ad-rules":
            case "pause-after-every-track":
            case "shuffle":
                return "0";
            case "streaming-rules":
                return "";
            case "audio-quality":
                if (SpotifyPref.veryHighAudioQuality()) {
                    return "very_high";
                }
                break;
            case "lyrics_capping":
            case "lyrics_locked":
                if (SpotifyPref.unlimitedLyrics()) {
                    return "0";
                }
                break;
        }

        return originalValue;
    }

    /**
     * Intercepts boolean checks from ProductState models.
     */
    public static boolean getProductStateBoolean(String key, boolean originalValue) {
        if (key == null || !SpotifyPref.productStateSpoof()) {
            return originalValue;
        }

        switch (key.toLowerCase()) {
            case "can_play_on_demand":
            case "interruption-free":
            case "unlimited-skips":
            case "offline":
                return true;
            case "ads":
            case "ad-rules":
            case "pause-after-every-track":
            case "shuffle":
                return false;
        }

        return originalValue;
    }

    /**
     * Applies full spoofed attributes onto an existing map (e.g. EsSession$ProductStateMap).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void applySpoofedProductState(Map map) {
        if (map == null || !SpotifyPref.productStateSpoof()) {
            return;
        }

        try {
            map.put("type", "premium");
            map.put("can_play_on_demand", "1");
            map.put("interruption-free", "1");
            map.put("ads", "0");
            map.put("ad-rules", "0");
            map.put("pause-after-every-track", "0");
            map.put("streaming-rules", "");
            map.put("unlimited-skips", "1");
            if (SpotifyPref.veryHighAudioQuality()) {
                map.put("audio-quality", "very_high");
            }
            if (SpotifyPref.forceShuffleDisabled()) {
                map.put("shuffle", "0");
            }
            if (SpotifyPref.unlimitedLyrics()) {
                map.put("lyrics_capping", "0");
                map.put("lyrics_locked", "0");
            }

            if (SpotifyPref.pikoDebug()) {
                SpotifyLog.d(TAG, "Applied Premium attributes onto ProductState map");
            }
        } catch (Throwable t) {
            SpotifyLog.e(TAG, "Failed to apply spoofed attributes: " + t.getMessage(), t);
        }
    }
}

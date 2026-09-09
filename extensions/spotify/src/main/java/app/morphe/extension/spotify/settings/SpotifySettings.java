/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.spotify.settings;

import app.morphe.extension.crimera.settings.BooleanSetting;

public class SpotifySettings {
    public static final BooleanSetting PIKO_DEBUG = new BooleanSetting("spotify_piko_debug", false);
    public static final BooleanSetting BLOCK_ADS = new BooleanSetting("spotify_block_ads", true);
    public static final BooleanSetting UNLIMITED_SKIPS = new BooleanSetting("spotify_unlimited_skips", true);
    public static final BooleanSetting ENABLE_SEEK = new BooleanSetting("spotify_enable_seek", true);
    public static final BooleanSetting ON_DEMAND_PLAYBACK = new BooleanSetting("spotify_on_demand_playback", true);
    public static final BooleanSetting FORCE_SHUFFLE_DISABLED = new BooleanSetting("spotify_force_shuffle_disabled", true);
    public static final BooleanSetting VERY_HIGH_AUDIO_QUALITY = new BooleanSetting("spotify_very_high_audio_quality", true);
    public static final BooleanSetting UNLIMITED_LYRICS = new BooleanSetting("spotify_unlimited_lyrics", true);
    public static final BooleanSetting ANTI_UPSELL = new BooleanSetting("spotify_anti_upsell", true);
    public static final BooleanSetting PRODUCT_STATE_SPOOF = new BooleanSetting("spotify_product_state_spoof", true);
}


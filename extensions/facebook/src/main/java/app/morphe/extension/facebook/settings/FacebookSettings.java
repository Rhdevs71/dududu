/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.settings;

import app.morphe.extension.crimera.settings.BooleanSetting;

public class FacebookSettings {
    // Kategori 1: Privasi & Ghost Mode
    public static final BooleanSetting GHOST_STORY = new BooleanSetting("fb_ghost_story", true);
    public static final BooleanSetting GHOST_READ_RECEIPT = new BooleanSetting("fb_ghost_read_receipt", true);
    public static final BooleanSetting GHOST_TYPING = new BooleanSetting("fb_ghost_typing", true);
    public static final BooleanSetting APP_LOCK = new BooleanSetting("fb_app_lock", false);
    public static final BooleanSetting ANTI_SCREENSHOT = new BooleanSetting("fb_anti_screenshot", true);

    // Kategori 2: Media & Video Downloader
    public static final BooleanSetting DOWNLOAD_REELS = new BooleanSetting("fb_download_reels", true);
    public static final BooleanSetting DOWNLOAD_VIDEO = new BooleanSetting("fb_download_video", true);
    public static final BooleanSetting DOWNLOAD_STORY = new BooleanSetting("fb_download_story", true);
    public static final BooleanSetting DOWNLOAD_PHOTO = new BooleanSetting("fb_download_photo", true);

    // Kategori 3: Feed Bersih & Ad-Blocker
    public static final BooleanSetting BLOCK_SPONSORED_ADS = new BooleanSetting("fb_block_sponsored_ads", true);
    public static final BooleanSetting BLOCK_INSTREAM_ADS = new BooleanSetting("fb_block_instream_ads", true);
    public static final BooleanSetting HIDE_PYMK = new BooleanSetting("fb_hide_pymk", true);
    public static final BooleanSetting HIDE_REELS_IN_FEED = new BooleanSetting("fb_hide_reels_in_feed", false);

    // Kategori 4: Kontrol Video & Audio
    public static final BooleanSetting PLAYBACK_SPEED_CONTROLLER = new BooleanSetting("fb_playback_speed_controller", true);
    public static final BooleanSetting BACKGROUND_AUDIO_PLAY = new BooleanSetting("fb_background_audio_play", true);
    public static final BooleanSetting DEFAULT_MUTE_VIDEO = new BooleanSetting("fb_default_mute_video", false);

    // Kategori 5: Kustomisasi Tampilan
    public static final BooleanSetting AMOLED_DARK_MODE = new BooleanSetting("fb_amoled_dark_mode", false);
    public static final BooleanSetting SHOW_PROFILE_CAPSULE = new BooleanSetting("fb_show_profile_capsule", true);
    public static final BooleanSetting ALWAYS_SHOW_FLOATING_BUTTON = new BooleanSetting("fb_always_show_floating_button", false);

    // Kategori 6: Utilitas & Bypass
    public static final BooleanSetting BYPASS_LINK_SHIM = new BooleanSetting("fb_bypass_link_shim", true);
    public static final BooleanSetting OPEN_EXTERNAL_BROWSER = new BooleanSetting("fb_open_external_browser", true);
    public static final BooleanSetting ENABLE_TEXT_SELECTION = new BooleanSetting("fb_enable_text_selection", true);
}

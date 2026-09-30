/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.settings;

import app.morphe.extension.crimera.sharedPreference.SharedPref;

public class FacebookPref {
    public static boolean isGhostStory() {
        return SharedPref.getBooleanPref(FacebookSettings.GHOST_STORY);
    }

    public static boolean isGhostReadReceipt() {
        return SharedPref.getBooleanPref(FacebookSettings.GHOST_READ_RECEIPT);
    }

    public static boolean isGhostTyping() {
        return SharedPref.getBooleanPref(FacebookSettings.GHOST_TYPING);
    }

    public static boolean isAppLock() {
        return SharedPref.getBooleanPref(FacebookSettings.APP_LOCK);
    }

    public static boolean isAntiScreenshot() {
        return SharedPref.getBooleanPref(FacebookSettings.ANTI_SCREENSHOT);
    }

    public static boolean isDownloadReels() {
        return SharedPref.getBooleanPref(FacebookSettings.DOWNLOAD_REELS);
    }

    public static boolean isDownloadVideo() {
        return SharedPref.getBooleanPref(FacebookSettings.DOWNLOAD_VIDEO);
    }

    public static boolean isDownloadStory() {
        return SharedPref.getBooleanPref(FacebookSettings.DOWNLOAD_STORY);
    }

    public static boolean isDownloadPhoto() {
        return SharedPref.getBooleanPref(FacebookSettings.DOWNLOAD_PHOTO);
    }

    public static boolean isBlockSponsoredAds() {
        return SharedPref.getBooleanPref(FacebookSettings.BLOCK_SPONSORED_ADS);
    }

    public static boolean isBlockInstreamAds() {
        return SharedPref.getBooleanPref(FacebookSettings.BLOCK_INSTREAM_ADS);
    }

    public static boolean isHidePymk() {
        return SharedPref.getBooleanPref(FacebookSettings.HIDE_PYMK);
    }

    public static boolean isHideReelsInFeed() {
        return SharedPref.getBooleanPref(FacebookSettings.HIDE_REELS_IN_FEED);
    }

    public static boolean isPlaybackSpeedController() {
        return SharedPref.getBooleanPref(FacebookSettings.PLAYBACK_SPEED_CONTROLLER);
    }

    public static boolean isBackgroundAudioPlay() {
        return SharedPref.getBooleanPref(FacebookSettings.BACKGROUND_AUDIO_PLAY);
    }

    public static boolean isDefaultMuteVideo() {
        return SharedPref.getBooleanPref(FacebookSettings.DEFAULT_MUTE_VIDEO);
    }

    public static boolean isAmoledDarkMode() {
        return SharedPref.getBooleanPref(FacebookSettings.AMOLED_DARK_MODE);
    }

    public static boolean isShowProfileCapsule() {
        return SharedPref.getBooleanPref(FacebookSettings.SHOW_PROFILE_CAPSULE);
    }

    public static boolean alwaysShowFloatingButton() {
        return SharedPref.getBooleanPref(FacebookSettings.ALWAYS_SHOW_FLOATING_BUTTON);
    }

    public static boolean isBypassLinkShim() {
        return SharedPref.getBooleanPref(FacebookSettings.BYPASS_LINK_SHIM);
    }

    public static boolean isOpenExternalBrowser() {
        return SharedPref.getBooleanPref(FacebookSettings.OPEN_EXTERNAL_BROWSER);
    }

    public static boolean isEnableTextSelection() {
        return SharedPref.getBooleanPref(FacebookSettings.ENABLE_TEXT_SELECTION);
    }
}

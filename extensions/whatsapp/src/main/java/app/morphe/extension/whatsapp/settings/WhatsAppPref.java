/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.settings;

import app.morphe.extension.crimera.sharedPreference.SharedPref;

public class WhatsAppPref {
    public static boolean pikoDebug() {
        return SharedPref.getBooleanPref(WhatsAppSettings.PIKO_DEBUG);
    }

    public static boolean unlockNovaPlus() {
        return SharedPref.getBooleanPref(WhatsAppSettings.UNLOCK_NOVA_PLUS);
    }

    public static boolean antiRevoke() {
        return SharedPref.getBooleanPref(WhatsAppSettings.ANTI_REVOKE);
    }

    public static boolean antiViewOnce() {
        return SharedPref.getBooleanPref(WhatsAppSettings.ANTI_VIEW_ONCE);
    }

    public static boolean hideSeenReceipt() {
        return SharedPref.getBooleanPref(WhatsAppSettings.HIDE_SEEN_RECEIPT);
    }

    public static boolean seenOnReply() {
        return SharedPref.getBooleanPref(WhatsAppSettings.SEEN_ON_REPLY);
    }

    public static boolean freezeLastSeen() {
        return SharedPref.getBooleanPref(WhatsAppSettings.FREEZE_LAST_SEEN);
    }

    public static boolean hideTypingStatus() {
        return SharedPref.getBooleanPref(WhatsAppSettings.HIDE_TYPING_STATUS);
    }

    public static boolean statusDownloader() {
        return SharedPref.getBooleanPref(WhatsAppSettings.STATUS_DOWNLOADER);
    }

    public static boolean mediaQualityHd() {
        return SharedPref.getBooleanPref(WhatsAppSettings.MEDIA_QUALITY_HD);
    }

    public static boolean separateGroupChats() {
        return SharedPref.getBooleanPref(WhatsAppSettings.SEPARATE_GROUP_CHATS);
    }

    public static boolean appLock() {
        return SharedPref.getBooleanPref(WhatsAppSettings.APP_LOCK);
    }
}

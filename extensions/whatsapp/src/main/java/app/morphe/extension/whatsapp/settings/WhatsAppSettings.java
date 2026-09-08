/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.settings;

import app.morphe.extension.crimera.settings.BooleanSetting;
import app.morphe.extension.crimera.settings.StringSetting;

public class WhatsAppSettings {
    public static final BooleanSetting PIKO_DEBUG = new BooleanSetting("wa_piko_debug", false);
    public static final BooleanSetting UNLOCK_NOVA_PLUS = new BooleanSetting("wa_unlock_nova_plus", true);
    public static final BooleanSetting ANTI_REVOKE = new BooleanSetting("wa_anti_revoke", true);
    public static final BooleanSetting ANTI_VIEW_ONCE = new BooleanSetting("wa_anti_view_once", true);
    public static final BooleanSetting HIDE_SEEN_RECEIPT = new BooleanSetting("wa_hide_seen_receipt", false);
    public static final BooleanSetting SEEN_ON_REPLY = new BooleanSetting("wa_seen_on_reply", true);
    public static final BooleanSetting FREEZE_LAST_SEEN = new BooleanSetting("wa_freeze_last_seen", false);
    public static final BooleanSetting HIDE_TYPING_STATUS = new BooleanSetting("wa_hide_typing_status", false);
    public static final BooleanSetting STATUS_DOWNLOADER = new BooleanSetting("wa_status_downloader", true);
    public static final BooleanSetting MEDIA_QUALITY_HD = new BooleanSetting("wa_media_quality_hd", true);
    public static final BooleanSetting SEPARATE_GROUP_CHATS = new BooleanSetting("wa_separate_group_chats", false);
    public static final BooleanSetting APP_LOCK = new BooleanSetting("wa_app_lock", false);
}

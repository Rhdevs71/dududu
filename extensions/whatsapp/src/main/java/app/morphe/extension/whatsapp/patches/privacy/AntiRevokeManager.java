/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.patches.privacy;

import app.morphe.extension.whatsapp.settings.WhatsAppPref;
import app.morphe.extension.whatsapp.utils.WhatsAppLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AntiRevokeManager {
    private static final String TAG = "AntiRevokeManager";

    // Set of message IDs that sender attempted to revoke
    private static final Set<String> REVOKED_MESSAGE_IDS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, Long> REVOKED_TIMESTAMPS = new ConcurrentHashMap<>();

    /**
     * Called before WhatsApp marks or deletes a message due to sender revocation.
     * Returns true if revocation should be BLOCKED (message preserved).
     */
    public static boolean shouldPreventRevocation(String messageId, boolean isFromMe) {
        if (!WhatsAppPref.antiRevoke()) {
            return false;
        }

        // Do not prevent user from revoking their own messages
        if (isFromMe) {
            return false;
        }

        if (messageId != null && !messageId.isEmpty()) {
            REVOKED_MESSAGE_IDS.add(messageId);
            REVOKED_TIMESTAMPS.put(messageId, System.currentTimeMillis());
            WhatsAppLog.i(TAG, "Anti-Revoke intercepted revocation for message: " + messageId);
        }

        return true;
    }

    /**
     * Checks if a given message ID was previously revoked by the sender.
     */
    public static boolean isRevoked(String messageId) {
        return messageId != null && REVOKED_MESSAGE_IDS.contains(messageId);
    }

    /**
     * Formats revoked message text with an anti-revoke badge.
     */
    public static String getRevokedNotice(String messageId) {
        Long time = REVOKED_TIMESTAMPS.get(messageId);
        String timeStr = "";
        if (time != null) {
            timeStr = " (" + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(time)) + ")";
        }
        return " 🚫 [Dihapus" + timeStr + "]";
    }
}

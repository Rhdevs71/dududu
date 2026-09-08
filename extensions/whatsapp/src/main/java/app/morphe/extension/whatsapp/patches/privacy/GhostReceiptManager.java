/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.patches.privacy;

import app.morphe.extension.whatsapp.settings.WhatsAppPref;
import app.morphe.extension.whatsapp.utils.WhatsAppLog;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GhostReceiptManager {
    private static final String TAG = "GhostReceiptManager";

    // JIDs that user has replied to
    private static final Set<String> REPLIED_JIDS = ConcurrentHashMap.newKeySet();

    /**
     * Called when a read receipt job / packet is about to be queued or sent.
     * Returns true if sending receipt should be BLOCKED (hide blue tick).
     */
    public static boolean shouldBlockReadReceipt(String jid) {
        if (!WhatsAppPref.hideSeenReceipt()) {
            return false;
        }

        if (WhatsAppPref.seenOnReply()) {
            // Allow read receipt if user has sent a reply to this contact
            if (jid != null && REPLIED_JIDS.contains(jid)) {
                WhatsAppLog.d(TAG, "Allowing read receipt because user replied to: " + jid);
                return false;
            }
        }

        WhatsAppLog.d(TAG, "Blocking read receipt for: " + jid);
        return true;
    }

    /**
     * Called when user sends a message in a conversation.
     * Records that user replied to this JID, unlocking blue ticks if seenOnReply is enabled.
     */
    public static void onUserSendMessage(String jid) {
        if (jid != null && !jid.isEmpty()) {
            REPLIED_JIDS.add(jid);
            WhatsAppLog.d(TAG, "User replied to: " + jid + ", unlocked receipts");
        }
    }

    /**
     * Called when typing / recording presence is about to be transmitted.
     * Returns true if typing status should be BLOCKED (stealth typing).
     */
    public static boolean shouldBlockTypingStatus() {
        if (WhatsAppPref.hideTypingStatus()) {
            WhatsAppLog.d(TAG, "Suppressed typing presence");
            return true;
        }
        return false;
    }
}

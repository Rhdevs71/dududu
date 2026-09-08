/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.ghost;

import app.morphe.extension.instagram.utils.PikoLog;
import app.morphe.extension.instagram.utils.Pref;

public class GhostPresenceHook {
    private static final String TAG = "GhostPresenceHook";

    /**
     * Checks if online presence / active status reporting should be suppressed.
     * When true, presence packets to Meta servers are blocked, keeping the user offline/invisible.
     */
    public static boolean shouldBlockPresence() {
        try {
            boolean block = Pref.hideOnlineStatus();
            if (block) {
                PikoLog.d(TAG, "Online presence reporting blocked (stealth mode active)");
            }
            return block;
        } catch (Throwable t) {
            PikoLog.e(TAG, "shouldBlockPresence check failed", t);
            return false;
        }
    }

    /**
     * Hook point for method returns or branch bypass.
     * If hide online status is enabled, returns false or empty state so that user activity is not sent.
     */
    public static boolean isUserActiveBypass(boolean originalActive) {
        if (shouldBlockPresence()) {
            return false; // Force user state to inactive/offline
        }
        return originalActive;
    }
}

/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.whatsapp.patches.nova;

import app.morphe.extension.whatsapp.settings.WhatsAppPref;
import app.morphe.extension.whatsapp.utils.WhatsAppLog;

public class NovaBenefitsManager {
    private static final String TAG = "NovaBenefitsManager";

    /**
     * Called by PromoEligibilityManager and aura benefit checkers.
     * Always returns true for WhatsApp Plus (Nova) benefits when enabled.
     */
    public static boolean isBenefitActive(String benefitName, boolean originalResult) {
        if (WhatsAppPref.unlockNovaPlus()) {
            WhatsAppLog.d(TAG, "Unlocking Nova Benefit: " + benefitName);
            return true;
        }
        return originalResult;
    }

    public static boolean isBenefitAllowed(String benefitName) {
        if (WhatsAppPref.unlockNovaPlus()) {
            WhatsAppLog.d(TAG, "Unlocking Nova Benefit: " + benefitName);
            return true;
        }
        return true; // Default allow for Plus
    }

    /**
     * Intercepts subscription check flags.
     */
    public static boolean isSubscriber(boolean originalResult) {
        if (WhatsAppPref.unlockNovaPlus()) {
            return true;
        }
        return originalResult;
    }

    /**
     * Intercepts maximum pinned chats check.
     */
    public static int getPinnedChatsLimit(int originalLimit) {
        if (WhatsAppPref.unlockNovaPlus()) {
            return 999; // Unlimited pinned chats
        }
        return originalLimit;
    }
}

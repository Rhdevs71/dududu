/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.nova

import app.crimera.patches.whatsapp.misc.extension.whatsAppExtensionPatch
import app.crimera.patches.whatsapp.utils.Constants.COMPATIBILITY_WHATSAPP
import app.crimera.patches.whatsapp.utils.Constants.NOVA_MANAGER_CLASS
import app.crimera.patches.whatsapp.utils.Constants.PROMO_ELIGIBILITY_MANAGER
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object PromoEligibilityFingerprint : Fingerprint(
    definingClass = PROMO_ELIGIBILITY_MANAGER,
)

@Suppress("unused")
val unlockNovaPatch =
    bytecodePatch(
        name = "Unlock WhatsApp Plus (Nova)",
        description = "Unlocks official Meta Nova / WhatsApp Plus features including Custom App Themes, Custom App Icons, Pinned Chats Limit increase, and exclusive stickers.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            PromoEligibilityFingerprint.classDef.methods.forEach { method ->
                if (method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 1
                        invoke-static {v0}, $NOVA_MANAGER_CLASS->isSubscriber(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }

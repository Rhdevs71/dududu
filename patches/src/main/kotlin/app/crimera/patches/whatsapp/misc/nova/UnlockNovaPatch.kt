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

internal object NovaBenefitEnumFingerprint : Fingerprint(
    strings = listOf("CUSTOM_APP_THEME", "CUSTOM_APP_ICON", "PIN_MORE_CHATS"),
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
            // 1. PromoEligibilityManager Hook
            PromoEligibilityFingerprint.classDefOrNull?.methods?.forEach { method ->
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

            // 2. Nova Benefit Enums Hook (returns true for benefit checks)
            NovaBenefitEnumFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const-string v0, "nova"
                        invoke-static {v0}, $NOVA_MANAGER_CLASS->isBenefitAllowed(Ljava/lang/String;)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }

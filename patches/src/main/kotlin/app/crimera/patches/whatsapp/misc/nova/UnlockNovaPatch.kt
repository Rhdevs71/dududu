/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.nova

import app.crimera.patches.whatsapp.misc.extension.whatsAppExtensionPatch
import app.crimera.patches.whatsapp.utils.Constants.COMPATIBILITY_WHATSAPP
import app.crimera.patches.whatsapp.utils.Constants.NOVA_MANAGER_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockNovaPatch =
    bytecodePatch(
        name = "Unlock WhatsApp Plus (Nova)",
        description = "Unlocks official Meta Nova / WhatsApp Plus features including Custom App Themes, Custom App Icons, Pinned Chats Limit increase, and exclusive stickers.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            // Find and hook PromoEligibilityManager
            classes.forEach { classDef ->
                if (classDef.type == "Lcom/whatsapp/nova/manager/PromoEligibilityManager;") {
                    val mutableClass = mutableClassDefBy(classDef)
                    mutableClass.methods.forEach { method ->
                        // Hook eligibility checkers
                        if (method.returnType == "Z") {
                            method.addInstructions(
                                0,
                                """
                                invoke-static {v0}, $NOVA_MANAGER_CLASS->isSubscriber(Z)Z
                                move-result v0
                                return v0
                                """,
                            )
                        }
                    }
                }
            }
        }
    }

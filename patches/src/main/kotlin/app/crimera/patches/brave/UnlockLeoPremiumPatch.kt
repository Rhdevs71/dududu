/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.brave

import app.crimera.patches.brave.Constants.BRAVE_COMPATIBILITY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string

object BraveLeoSubscriptionFingerprint :
    Fingerprint(
        returnType = "Z",
        filters =
            listOf(
                string("brave.ai_chat.subscription_active_android"),
            ),
    )

@Suppress("unused")
val unlockLeoPremiumPatch =
    bytecodePatch(
        name = "Unlock Brave Leo AI Premium",
        description = "Unlocks Brave Leo AI Premium subscription status and premium models without a paywall.",
        default = true,
    ) {
        compatibleWith(BRAVE_COMPATIBILITY)

        execute {
            BraveLeoSubscriptionFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    const/4 v0, 0x1
                    return v0
                    """.trimIndent(),
                )
            }
        }
    }

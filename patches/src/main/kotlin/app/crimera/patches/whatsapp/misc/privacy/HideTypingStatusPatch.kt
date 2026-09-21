/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.privacy

import app.crimera.patches.whatsapp.misc.extension.whatsAppExtensionPatch
import app.crimera.patches.whatsapp.utils.Constants.COMPATIBILITY_WHATSAPP
import app.crimera.patches.whatsapp.utils.Constants.GHOST_RECEIPT_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object HandleMeComposingFingerprint : Fingerprint(
    strings = listOf("HandleMeComposing/sendComposing"),
)

@Suppress("unused")
val hideTypingStatusPatch =
    bytecodePatch(
        name = "Hide Typing & Recording Status",
        description = "Prevents broadcasting typing and audio recording indicators to chat recipients.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            HandleMeComposingFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {}, $GHOST_RECEIPT_CLASS->shouldBlockTypingStatus()Z
                    move-result v0
                    if-nez v0, :send_typing_normally
                    return-void
                    :send_typing_normally
                    """.trimIndent(),
                )
            }
        }
    }

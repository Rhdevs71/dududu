/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.privacy

import app.crimera.patches.whatsapp.misc.extension.whatsAppExtensionPatch
import app.crimera.patches.whatsapp.utils.Constants.COMPATIBILITY_WHATSAPP
import app.crimera.patches.whatsapp.utils.Constants.PREF_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object PresenceStateManagerFingerprint : Fingerprint(
    strings = listOf("presencestatemanager/setAvailable/new-state"),
)

@Suppress("unused")
val freezeLastSeenPatch =
    bytecodePatch(
        name = "Freeze Last Seen",
        description = "Prevents sending your active presence status to Meta servers, keeping your last seen timestamp frozen in time while using the app.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            PresenceStateManagerFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {}, $PREF_CLASS->freezeLastSeen()Z
                    move-result v0
                    if-nez v0, :send_presence_normally
                    return-void
                    :send_presence_normally
                    """.trimIndent(),
                )
            }
        }
    }

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

internal object ViewOnceFingerprint : Fingerprint(
    strings = listOf("INSERT_VIEW_ONCE_SQL"),
)

@Suppress("unused")
val antiViewOncePatch =
    bytecodePatch(
        name = "Anti-View Once",
        description = "Allows viewing view-once photos and videos indefinitely and enables saving them.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            ViewOnceFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {}, $PREF_CLASS->antiViewOnce()Z
                    move-result v0
                    if-eqz v0, :keep_view_once
                    return-void
                    :keep_view_once
                    """.trimIndent(),
                )
            }
        }
    }

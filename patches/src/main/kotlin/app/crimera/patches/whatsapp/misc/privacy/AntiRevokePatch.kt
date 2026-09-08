/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.privacy

import app.crimera.patches.whatsapp.misc.extension.whatsAppExtensionPatch
import app.crimera.patches.whatsapp.utils.Constants.ANTI_REVOKE_CLASS
import app.crimera.patches.whatsapp.utils.Constants.COMPATIBILITY_WHATSAPP
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object AntiRevokeFingerprint : Fingerprint(
    strings = listOf("msgstore/edit/revoke"),
)

@Suppress("unused")
val antiRevokePatch =
    bytecodePatch(
        name = "Anti-Revoke Messages",
        description = "Prevents senders from deleting messages for you in chats and groups. Revoked messages remain visible and are tagged with a deleted icon.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            AntiRevokeFingerprint.result?.let { result ->
                val method = result.mutableMethod
                // Inject at the beginning of revocation handler:
                // If shouldPreventRevocation returns true, return early without deleting
                method.addInstructions(
                    0,
                    """
                    const/4 v0, 0
                    invoke-static {v0, v0}, $ANTI_REVOKE_CLASS->shouldPreventRevocation(Ljava/lang/String;Z)Z
                    move-result v0
                    if-eqz v0, :continue_normal_flow
                    return-void
                    :continue_normal_flow
                    """,
                )
            }
        }
    }

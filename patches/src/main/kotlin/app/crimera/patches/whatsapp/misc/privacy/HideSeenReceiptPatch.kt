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

internal object SendReadReceiptJobFingerprint : Fingerprint(
    strings = listOf("SendReadReceiptJob"),
)

@Suppress("unused")
val hideSeenReceiptPatch =
    bytecodePatch(
        name = "Hide Read Receipts (Blue Ticks)",
        description = "Prevents sending read receipts (blue ticks) when viewing messages. Optionally sends blue ticks only when you reply.",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            SendReadReceiptJobFingerprint.result?.let { result ->
                val method = result.mutableMethod
                method.addInstructions(
                    0,
                    """
                    const/4 v0, 0
                    invoke-static {v0}, $GHOST_RECEIPT_CLASS->shouldBlockReadReceipt(Ljava/lang/String;)Z
                    move-result v0
                    if-nez v0, :send_receipt_normally
                    return-void
                    :send_receipt_normally
                    """,
                )
            }
        }
    }

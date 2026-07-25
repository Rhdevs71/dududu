/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.actionBar.inboxActionBarButton

import app.crimera.patches.instagram.utils.Constants.ACTIONBAR_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode

object InboxActionBarBuilderFingerprint : Fingerprint(
    filters =
        listOf(
            resourceLiteral(ResourceType.ID, "direct_inbox_action_bar"),
            opcode(Opcode.CHECK_CAST),
        ),
)

val inboxActionBarButtonPatch =
    bytecodePatch(
        description = "This patch is adds support for adding buttons on Inbox action bar.",
    ) {
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        dependsOn(resourceMappingPatch)

        execute {
            InboxActionBarBuilderFingerprint.apply {
                val idIndex = instructionMatches.first().index

                method.apply {
                    val checkCastIndex = indexOfFirstInstruction(idIndex, Opcode.CHECK_CAST)
                    val viewGroupRegister = getInstruction(checkCastIndex).registersUsed[0]
                    addInstruction(
                        checkCastIndex + 1,
                        """
                        invoke-static {v$viewGroupRegister}, $ACTIONBAR_DESCRIPTOR->inboxActionBarButton(Landroid/view/ViewGroup;)V
                        """.trimIndent(),
                    )
                }
            }
        }
    }

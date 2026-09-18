/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.efootball.overlay

import app.crimera.patches.efootball.misc.extension.efootballExtensionPatch
import app.crimera.patches.efootball.utils.Constants.COMPATIBILITY_EFOOTBALL
import app.crimera.patches.efootball.utils.Constants.EFB_OVERLAY_MANAGER_CLASS
import app.crimera.patches.efootball.utils.Constants.GAME_ACTIVITY_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val overlayMenuPatch =
    bytecodePatch(
        name = "In-Game Mod Menu Overlay",
        description = "Adds draggable in-game mod menu overlay with match speed, camera drone FOV, player scale, and graphics controls.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_EFOOTBALL)
        dependsOn(efootballExtensionPatch)

        execute {
            try {
                val gameActivityClass = mutableClassDefBy(GAME_ACTIVITY_CLASS)
                val onCreateMethod = gameActivityClass.methods.firstOrNull { it.name == "onCreate" }

                if (onCreateMethod != null) {
                    val returnIndex = onCreateMethod.instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
                    if (returnIndex >= 0) {
                        onCreateMethod.addInstructions(
                            returnIndex,
                            """
                            sget-object v0, $GAME_ACTIVITY_CLASS->_activity:$GAME_ACTIVITY_CLASS
                            invoke-static {v0}, $EFB_OVERLAY_MANAGER_CLASS->init(Landroid/app/Activity;)V
                            """.trimIndent(),
                        )
                    }
                }
            } catch (e: Exception) {
                println("[OverlayMenuPatch] Error hooking GameActivity.onCreate: ${e.message}")
            }
        }
    }

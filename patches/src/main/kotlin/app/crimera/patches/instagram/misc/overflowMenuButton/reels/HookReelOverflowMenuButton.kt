/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.overflowMenuButton.reels

import app.crimera.patches.instagram.misc.download.AddReelButtonFingerprint
import app.crimera.patches.instagram.utils.Constants.ADD_REEL_BTN_OVERFLOW_MENU_BUTTON_CLASS
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.Constants.FRAGMENT_ACTIVITY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val hookReelOverflowMenuButton =
    bytecodePatch(
        description = "This patch hooks reel overflow button list adder",
    ) {
        dependsOn(reelsOverflowMenuButtonEntity)
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        execute {
            AddReelButtonFingerprint.method.apply {
                val classDef = AddReelButtonFingerprint.classDef
                val className = classDef.type
                val classFields = classDef.fields

                val appActivityField = classFields.first { it.type == FRAGMENT_ACTIVITY }

                val selfClassRegister = instructions[indexOfFirstInstruction(Opcode.MOVE_OBJECT_FROM16)].registersUsed[0]
                val buttonAdderInstanceRegister = instructions[indexOfFirstInstruction(Opcode.NEW_INSTANCE)].registersUsed[0]

                val sPutIndex = indexOfFirstInstruction(Opcode.SPUT)
                val mediaObjectFromParameterIndex = indexOfFirstInstruction(sPutIndex, Opcode.MOVE_OBJECT_FROM16)
                val mediaObjectRegister = instructions[mediaObjectFromParameterIndex].registersUsed[0]

                val tempReg1 = findFreeRegister(mediaObjectFromParameterIndex + 1, selfClassRegister, buttonAdderInstanceRegister, mediaObjectRegister)
                val tempReg2 = findFreeRegister(mediaObjectFromParameterIndex + 1, selfClassRegister, buttonAdderInstanceRegister, mediaObjectRegister, tempReg1)
                val tempReg3 = findFreeRegister(mediaObjectFromParameterIndex + 1, selfClassRegister, buttonAdderInstanceRegister, mediaObjectRegister, tempReg1, tempReg2)

                addInstructions(
                    mediaObjectFromParameterIndex + 1,
                    """
                    iget-object v$tempReg1, v$selfClassRegister, $appActivityField
                    move-object/from16 v$tempReg2, v$buttonAdderInstanceRegister
                    move-object/from16 v$tempReg3, v$mediaObjectRegister
                    invoke-static {v$tempReg1,v$tempReg2,v$tempReg3},$ADD_REEL_BTN_OVERFLOW_MENU_BUTTON_CLASS->includeCustomReelOverflowButtons(Landroid/content/Context;Ljava/lang/Object;Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
        }
    }

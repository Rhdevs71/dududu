/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.facebook.misc.extension.hooks

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patches.all.misc.extension.ExtensionHook
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode

internal val facebookInitHook =
    ExtensionHook(
        fingerprint =
            Fingerprint(
                name = "onCreate",
                custom = { _, classDef ->
                    classDef.endsWith("/FacebookApplication;")
                },
            ),
        insertIndexResolver = { method ->
            method.instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.INVOKE_SUPER
            } + 1
        },
        contextRegisterResolver = { method ->
            val invokeSuperInstruction =
                method.instructions.first { instruction ->
                    instruction.opcode == Opcode.INVOKE_SUPER
                }

            "v${invokeSuperInstruction.registersUsed[0]}"
        },
    )

package app.crimera.patches.duolingo

import app.crimera.patches.duolingo.Constants.DUOLINGO_COMPATIBILITY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val duolingoEnableDebugMenuPatch = bytecodePatch(
    name = "Enable Debug Menu",
    description = "Enables Duolingo internal debug menu in settings and unlocks video call debug overrides.",
    default = true,
) {
    compatibleWith(DUOLINGO_COMPATIBILITY)

    execute {
        // 1. Enable internal debug menu
        val isDebugField = BuildTargetFieldFingerprint.method.instructions
            .elementAt(BuildTargetFieldFingerprint.instructionMatches.first().index + 1)
            .let { instruction ->
                (instruction as? ReferenceInstruction)?.reference as? FieldReference
            } ?: throw PatchException("Could not find debug field in DebugMenuProvider")

        mutableClassDefBy(isDebugField.definingClass).methods
            .first { method -> method.name == "<init>" }
            .apply {
                val returnIndex = instructions.indexOfLast { instruction ->
                    instruction.opcode == Opcode.RETURN_VOID
                }
                if (returnIndex < 0) throw PatchException("Could not find debug provider constructor return")

                addInstructions(
                    returnIndex,
                    """
                    const/4 v0, 0x1
                    iput-boolean v0, p0, ${isDebugField.definingClass}->${isDebugField.name}:Z
                    """.trimIndent(),
                )
            }

        // 2. Unlock VideoCallDebugSettings (showPromptSelect, showVideoCallTab, completeSessionOnHangup)
        try {
            val debugSettingsType = VideoCallDebugSettingsFingerprint.classDef.type
            val debugSettingsClass = mutableClassDefBy(debugSettingsType)
            val constructor = debugSettingsClass.methods
                .firstOrNull { it.name == "<init>" && it.returnType == "V" }
            if (constructor != null) {
                val returnIndex = constructor.instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
                if (returnIndex >= 0) {
                    // Use v1 for values so p0 (v0) is not overwritten
                    constructor.addInstructions(
                        returnIndex,
                        """
                        const/4 v1, 0x1
                        iput-boolean v1, p0, $debugSettingsType->a:Z
                        iput-boolean v1, p0, $debugSettingsType->b:Z
                        const/4 v1, 0x0
                        iput-boolean v1, p0, $debugSettingsType->c:Z
                        const/4 v1, 0x1
                        iput-boolean v1, p0, $debugSettingsType->d:Z
                        iput-boolean v1, p0, $debugSettingsType->e:Z
                        const/4 v1, 0x0
                        iput-boolean v1, p0, $debugSettingsType->h:Z
                        iput-boolean v1, p0, $debugSettingsType->i:Z
                        """.trimIndent(),
                    )
                }
            }
        } catch (e: Exception) {
            println("[DebugMenuPatch] Warning: Failed to hook VideoCallDebugSettings: ${e.message}")
        }

        // 3. Force VideoCallTabEligibility to always true
        try {
            val eligibilityType = VideoCallTabEligibilityFingerprint.classDef.type
            val eligibilityClass = mutableClassDefBy(eligibilityType)
            val invokeSuspend = eligibilityClass.methods.firstOrNull { it.name == "invokeSuspend" }
            if (invokeSuspend != null) {
                // Instruction 0 is move-object/from16 v0, v16 (stores p0 in v0).
                // Insert at index 1 using 4-bit register v0 for p0.
                invokeSuspend.addInstructions(
                    1,
                    """
                    const/4 v1, 0x1
                    iput-boolean v1, v0, $eligibilityType->j:Z
                    """.trimIndent(),
                )
            }
        } catch (e: Exception) {
            println("[DebugMenuPatch] Warning: Failed to hook VideoCallTabEligibility: ${e.message}")
        }
    }
}

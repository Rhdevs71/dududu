/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.comment

import app.crimera.patches.instagram.entity.decoder.COMMENT_BUTTON_CLASS
import app.crimera.utils.extensionToClassName
import app.crimera.utils.fieldExtractor
import app.crimera.utils.methodExtractor
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

context(patchContext: BytecodePatchContext)
fun addButtonAttribute(
    stringLateral: Long,
    drawableLateral: Long,
    button2CheckFingerprint: Fingerprint,
    buttonInstanceFingerprint: Fingerprint,
) {
    AddCommentButtonFingerprint.method.apply {
        val drawableId = getResourceId(ResourceType.DRAWABLE, "instagram_eye_off_outline_24")
        val drawableIndex = indexOfFirstLiteralInstruction(drawableId)

        val arrayAddInstruction =
            instructions.last { it.opcode == Opcode.INVOKE_VIRTUAL && it.location.index < drawableIndex }
        val existingButtonSGetObjectInstruction =
            instructions.last {
                it.opcode == Opcode.SGET_OBJECT &&
                    it.location.index < drawableIndex
            }

        val existingButtonClass =
            extensionToClassName(existingButtonSGetObjectInstruction.fieldExtractor().definingClass)

        val isEqualsInstruction =
            instructions.last { it.opcode == Opcode.INVOKE_STATIC && it.location.index < drawableIndex }
        val isEqualsClass = extensionToClassName(isEqualsInstruction.methodExtractor().definingClass)
        val compareButtonRegister = isEqualsInstruction.registersUsed[0]
        val ourButtonRegister = isEqualsInstruction.registersUsed[1]

        val buttonStyleInstruction = getInstruction(indexOfFirstInstruction(drawableIndex, Opcode.SGET_OBJECT))
        val buttonStyleClass = extensionToClassName(buttonStyleInstruction.fieldExtractor().definingClass)

        val gotoIndex = indexOfFirstInstruction(drawableIndex, Opcode.GOTO)
        val bundleInstruction = getInstruction(gotoIndex - 1)
        val bundleMethodRef = bundleInstruction.getReference<MethodReference>()!!
        val bundleClass = bundleMethodRef.definingClass
        val bundleParameters = bundleMethodRef.parameterTypes
        val bundleRegisters = bundleInstruction.registersUsed

        val bundleRegister = bundleRegisters[0]

        val buttonStyleParentClass = bundleParameters[0]
        val buttonStyleRegister = bundleRegisters[1]

        val drawableInitClass = bundleParameters[1]
        val drawableInitRegister = bundleRegisters[2]

        val stringInitClass = bundleParameters[2]
        val stringInitRegister = bundleRegisters[3]

        val buttonInvokeRelatedClass = bundleParameters[3]
        val buttonInvokeRelatedRegister = bundleRegisters[4]

        var b0 = -1
        var b1 = -1
        var b2 = -1
        var b3 = -1
        var b4 = -1

        fun allocateRegisters() {
            b0 = findFreeRegister(existingButtonSGetObjectInstruction.location.index + 1)
            b1 = findFreeRegister(existingButtonSGetObjectInstruction.location.index + 1, b0)
            b2 = findFreeRegister(existingButtonSGetObjectInstruction.location.index + 1, b0, b1)
            b3 = findFreeRegister(existingButtonSGetObjectInstruction.location.index + 1, b0, b1, b2)
            b4 = findFreeRegister(existingButtonSGetObjectInstruction.location.index + 1, b0, b1, b2, b3)
        }

        try {
            allocateRegisters()
        } catch (e: Exception) {
            val m = patchContext.method
            try {
                val getImpl = m.javaClass.methods.find { it.name == "getImplementation" }
                val impl = getImpl?.invoke(m)
                if (impl != null) {
                    val getRegs = impl.javaClass.methods.find { it.name == "getRegisterCount" }
                    val setRegs = impl.javaClass.methods.find { it.name == "setRegisterCount" }
                    if (getRegs != null && setRegs != null) {
                        val current = getRegs.invoke(impl) as Int
                        setRegs.invoke(impl, current + 5)
                    }
                }
            } catch (ex: Exception) {
                // Ignore reflection errors and let the second attempt throw
            }
            allocateRegisters()
        }

        addInstructionsWithLabels(
            existingButtonSGetObjectInstruction.location.index + 1,
            """
            move-object/16 v$b0, v0
            move-object/16 v$b1, v1
            move-object/16 v$b2, v2
            move-object/16 v$b3, v3
            move-object/16 v$b4, v4

            sget-object v0, ${button2CheckFingerprint.classDef.fields.first()}
            move-object/16 v1, v$compareButtonRegister
            invoke-static {v1, v0}, $isEqualsClass->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z
            move-result v0

            if-eqz v0, :next_button

            const v0, $stringLateral
            new-instance v1, $stringInitClass
            invoke-direct {v1, v0}, $stringInitClass-><init>(I)V
            move-object/16 v$stringInitRegister, v1

            const v0, $drawableLateral
            new-instance v2, $drawableInitClass
            invoke-direct {v2, v0}, $drawableInitClass-><init>(I)V
            move-object/16 v$drawableInitRegister, v2

            sget-object v3, $buttonStyleClass->A00:$buttonStyleClass
            move-object/16 v$buttonStyleRegister, v3

            move-object/16 v4, v$buttonInvokeRelatedRegister

            new-instance v0, ${buttonInstanceFingerprint.definingClass}
            invoke-direct {v0, v3, v2, v1, v4}, $bundleClass-><init>($buttonStyleParentClass $drawableInitClass $stringInitClass $buttonInvokeRelatedClass)V
            move-object/16 v$bundleRegister, v0

            move-object/16 v0, v$b0
            move-object/16 v1, v$b1
            move-object/16 v2, v$b2
            move-object/16 v3, v$b3
            move-object/16 v4, v$b4
            goto :array_add

            :next_button
            move-object/16 v0, v$b0
            move-object/16 v1, v$b1
            move-object/16 v2, v$b2
            move-object/16 v3, v$b3
            move-object/16 v4, v$b4

            sget-object v0, $existingButtonClass->A00:$existingButtonClass
            """.trimIndent(),
            ExternalLabel("array_add", arrayAddInstruction),
        )

        // Dynamically inject super class to  our extension class
        buttonInstanceFingerprint.classDef.setSuperClass(bundleClass)
    }
}

context(patchContext: BytecodePatchContext)
fun addButtonInterface(buttonInstanceFingerprint: Fingerprint) {
    // Dynamically inject interface to our extension class.
    buttonInstanceFingerprint.classDef.interfaces.add(COMMENT_BUTTON_CLASS)
}

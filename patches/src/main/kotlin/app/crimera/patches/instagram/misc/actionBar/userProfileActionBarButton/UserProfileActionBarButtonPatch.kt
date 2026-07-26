/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.actionBar.userProfileActionBarButton

import app.crimera.patches.instagram.entity.decoder.USER_MODEL_CLASS_NAME
import app.crimera.patches.instagram.entity.decoder.decoderEntity
import app.crimera.patches.instagram.utils.Constants
import app.crimera.patches.instagram.utils.Constants.ACTIONBAR_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.Constants.USER_DETAIL_VIEW_MODEL_CLASS
import app.crimera.patches.instagram.utils.Constants.USER_SESSION_CLASS
import app.crimera.patches.instagram.utils.addFlags
import app.crimera.utils.methodExtractor
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.findFreeRegister
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ProfileActionBarRelatedFingerprint : Fingerprint(
    strings = listOf("notifications_entry_point_impression", "impression_cast_to_tv"),
    returnType = "V",
)

internal object ProfileHeaderRelatedFingerprint : Fingerprint(
    strings = listOf("user_profile_header"),
    custom = { _, classDef ->
        classDef.fields.any { it.type == app.crimera.patches.instagram.utils.Constants.USER_DETAIL_VIEW_MODEL_CLASS }
    }
)

internal object ProfileActionBarViewBinderClassFingerprint : Fingerprint(
    strings = listOf("ProfileActionBarViewBinder"),
)

internal object ProfileActionBarFingerprint : Fingerprint(
    classFingerprint = ProfileActionBarViewBinderClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { methodDef, _ ->
        methodDef.implementation?.instructions?.any {
            it.opcode == Opcode.INVOKE_VIRTUAL && it.methodExtractor().name == "removeAllViews"
        } == true
    }
)

val userProfileActionBarButtonPatch =
    bytecodePatch(
        description = "This patch is adds support for adding buttons on user profile action bar.",
    ) {
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        dependsOn(decoderEntity)

        execute {

            val actionBarRelatedClass: String
            val profileHeaderFieldInActionBarRelatedClass: MutableField

            ProfileActionBarRelatedFingerprint.apply {
                actionBarRelatedClass = classDef.type
                profileHeaderFieldInActionBarRelatedClass =
                    classDef.fields.first { it.type == ProfileHeaderRelatedFingerprint.classDef.type }
            }

            val userDetailViewModelFieldInProfileHeaderRelatedClass: MutableField =
                ProfileHeaderRelatedFingerprint.classDef.fields.first {
                    it.type == USER_DETAIL_VIEW_MODEL_CLASS
                }

            val userDetailsClassFields = classDefBy { it.type == USER_DETAIL_VIEW_MODEL_CLASS }.fields

            val userDataFieldInUserDetailClass = userDetailsClassFields.first { it.type == USER_MODEL_CLASS_NAME }

            val userSessionFieldInUserDetailClass = userDetailsClassFields.first { it.type == USER_SESSION_CLASS }

            val profileActionBarMethod = ProfileActionBarViewBinderClassFingerprint.classDef.methods.first { methodDef ->
                methodDef.implementation?.instructions?.any {
                    it.opcode == Opcode.INVOKE_VIRTUAL && it.methodExtractor().name == "removeAllViews"
                } == true
            }

            profileActionBarMethod.apply {

                instructions.filter { it.opcode == Opcode.INVOKE_VIRTUAL }.first {
                        val methodExt = it.methodExtractor()
                        if (methodExt.name == "removeAllViews") {
                            val index = it.location.index + 1
                            val viewGroupRegister = getInstruction(index).registersUsed[0]
                            val nextInstruction = getInstruction(index + 1)
                            val freeRegister = findFreeRegister(index, viewGroupRegister)

                            val invokeStaticWithParams = instructions
                                .subList(index, instructions.size)
                                .first { inst -> inst.opcode == Opcode.INVOKE_STATIC && inst.registersUsed.size >= 3 }

                            val actionBarRelatedObjectParameterRegister = invokeStaticWithParams.registersUsed[2]
                            val userSessionRegister = invokeStaticWithParams.registersUsed[1]

                            val CODE =
                                """
                                if-eqz v$actionBarRelatedObjectParameterRegister, :piko
                                iget-object v$freeRegister, v$actionBarRelatedObjectParameterRegister, $profileHeaderFieldInActionBarRelatedClass
                                
                                if-eqz v$freeRegister, :piko
                                iget-object v$freeRegister,v$freeRegister, $userDetailViewModelFieldInProfileHeaderRelatedClass
                                
                                if-eqz v$freeRegister, :piko
                                iget-object v$freeRegister,v$freeRegister, $userDataFieldInUserDetailClass
                                
                                invoke-static {v$viewGroupRegister, v$userSessionRegister, v$freeRegister}, $ACTIONBAR_DESCRIPTOR->userProfileActionBarButton(Landroid/view/ViewGroup;${USER_SESSION_CLASS}Ljava/lang/Object;)V
                                """.trimIndent()

                            addInstructionsWithLabels(
                                index + 1,
                                CODE,
                                ExternalLabel("piko", nextInstruction),
                            )

                            addFlags("profileActionBarFlags")
                            true
                        } else {
                            false
                        }
                    }
                }
        }
    }

/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.unlockPlusBenefits

import app.crimera.patches.instagram.misc.settings.settingsPatch
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.Constants.PREF_DESCRIPTOR
import app.crimera.patches.instagram.utils.enableSettings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel

import app.crimera.patches.instagram.utils.Constants.PATCHES_DESCRIPTOR
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal object ActiveBenefitCheckerClassFingerprint : Fingerprint(
    strings = listOf("is_benefit_active"),
)

internal object ActiveBenefitCheckerFingerprint : Fingerprint(
    classFingerprint = ActiveBenefitCheckerClassFingerprint,
    parameters = listOf("Ljava/lang/String;"),
    returnType = "Z",
    custom = { methodDef, _ ->
        AccessFlags.PUBLIC.isSet(methodDef.accessFlags)
    },
)

internal object AppIconSwitchManagerClassFingerprint : Fingerprint(
    strings = listOf("AuraAppIconSwitchManager", "Failed to get current app icon"),
)

internal object AppIconSwitchFingerprint : Fingerprint(
    classFingerprint = AppIconSwitchManagerClassFingerprint,
    returnType = "V",
    custom = { methodDef, _ ->
        methodDef.parameters.size == 5 && methodDef.parameters[0].type == "Landroid/content/Context;"
    },
)

internal object AuraAppIconPickerFragmentFingerprint : Fingerprint(
    strings = listOf("AuraAppIconPickerFragment"),
)

internal object MetaSubscriptionUpsellFingerprint : Fingerprint(
    strings = listOf("com.bloks.www.mv.unified_entry_point.controller"),
    parameters = listOf("Landroidx/fragment/app/FragmentActivity;", "Lcom/instagram/common/session/UserSession;", "Ljava/lang/String;"),
    returnType = "V",
)

internal object SetBiographyRequestBuilderFingerprint : Fingerprint(
    strings = listOf("accounts/set_biography/", "raw_text"),
    parameters = listOf(
        "Lcom/instagram/common/session/UserSession;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

@Suppress("unused")
val unlockPlusBenefitsPatch =
    bytecodePatch(
        name = "Unlock Plus benefits",
        description = "Unlocks 'Plus' subscription benefits that are checked locally. USE IT AT YOUR OWN RISK",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        dependsOn(settingsPatch)
        execute {

            ActiveBenefitCheckerFingerprint.method.apply {

                addInstructionsWithLabels(
                    0,
                    """
                    invoke-static {p1}, $PREF_DESCRIPTOR->isBenefitAllowed(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :piko_continue
                    return v0
                    """.trimIndent(),
                    ExternalLabel("piko_continue", getInstruction(0)),
                )

                enableSettings("unlockPlusBenefits")
            }

            AppIconSwitchFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {p1, p2}, $PATCHES_DESCRIPTOR/appicon/InstaAppIconManager;->applyIcon(Landroid/content/Context;Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }

            AuraAppIconPickerFragmentFingerprint.classDef.apply {
                val onViewCreated = methods.firstOrNull { it.name == "onViewCreated" }
                val viewModelCall = onViewCreated?.implementation?.instructions
                    ?.filterIsInstance<ReferenceInstruction>()
                    ?.mapNotNull { it.reference as? MethodReference }
                    ?.firstOrNull {
                        it.parameterTypes.size == 2 &&
                            it.parameterTypes[1] == it.definingClass &&
                            it.returnType == "V"
                    }
                if (viewModelCall != null) {
                    val viewModelClass = mutableClassDefByOrNull(viewModelCall.definingClass)
                    val targetMethod = viewModelClass?.methods?.firstOrNull { it.name == viewModelCall.name }
                    var statusFieldRef: FieldReference? = null
                    targetMethod?.apply {
                        val instList = implementation?.instructions?.toList() ?: return@apply
                        for (i in 0 until instList.size - 1) {
                            if (instList[i].opcode == Opcode.SGET_OBJECT && instList[i + 1].opcode == Opcode.IF_EQ) {
                                val reg = (instList[i] as? OneRegisterInstruction)?.registerA ?: 0
                                statusFieldRef = (instList[i] as? ReferenceInstruction)?.reference as? FieldReference
                                replaceInstruction(i, "const/4 v$reg, 0")
                                break
                            }
                        }
                    }

                    // 1. In LX/0GuK (status enum), neutralize A06 in <clinit> so it points to null
                    if (statusFieldRef != null) {
                        val statusEnumClass = mutableClassDefByOrNull(statusFieldRef!!.definingClass)
                        val clinit = statusEnumClass?.methods?.firstOrNull { it.name == "<clinit>" }
                        clinit?.apply {
                            val instList = implementation?.instructions?.toList() ?: return@apply
                            val retIdx = instList.indexOfLast { it.opcode == Opcode.RETURN_VOID }
                            if (retIdx >= 0) {
                                addInstructions(
                                    retIdx,
                                    """
                                    const/4 v0, 0
                                    sput-object v0, ${statusFieldRef!!.definingClass}->${statusFieldRef!!.name}:${statusFieldRef!!.type}
                                    """.trimIndent(),
                                )
                            }
                        }
                    }

                    // 2. In LX/0RAH (click handler lambda), neutralize SGET_OBJECT + IF_NE check
                    val onCreateView = methods.firstOrNull { it.name == "onCreateView" }
                    val f3uType = onCreateView?.implementation?.instructions
                        ?.filterIsInstance<ReferenceInstruction>()
                        ?.mapNotNull { (it.reference as? TypeReference)?.type }
                        ?.firstOrNull()
                    if (f3uType != null) {
                        val f3uClass = mutableClassDefByOrNull(f3uType)
                        val f3uInvoke = f3uClass?.methods?.firstOrNull { it.name == "invoke" }
                        val rahType = f3uInvoke?.implementation?.instructions
                            ?.filterIsInstance<ReferenceInstruction>()
                            ?.mapNotNull { (it.reference as? TypeReference)?.type }
                            ?.firstOrNull { it.startsWith("LX/0R") }
                        if (rahType != null) {
                            val rahClass = mutableClassDefByOrNull(rahType)
                            val rahInvoke = rahClass?.methods?.firstOrNull { it.name == "invoke" }
                            rahInvoke?.apply {
                                val instList = implementation?.instructions?.toList() ?: return@apply
                                for (i in 0 until instList.size - 1) {
                                    if (instList[i].opcode == Opcode.SGET_OBJECT && instList[i + 1].opcode == Opcode.IF_NE) {
                                        val ref = (instList[i] as? ReferenceInstruction)?.reference as? FieldReference
                                        if (statusFieldRef == null || (ref?.definingClass == statusFieldRef!!.definingClass && ref?.name == statusFieldRef!!.name)) {
                                            val reg = (instList[i] as? OneRegisterInstruction)?.registerA ?: 0
                                            replaceInstruction(i, "const/4 v$reg, 0")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MetaSubscriptionUpsellFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    return-void
                    """.trimIndent(),
                )
            }

            SetBiographyRequestBuilderFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {p1, p4}, $PATCHES_DESCRIPTOR/userprofile/BioFontTransformer;->transformBio(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                    invoke-static {p4}, $PATCHES_DESCRIPTOR/userprofile/BioFontTransformer;->sanitizeFontParam(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p4
                    """.trimIndent(),
                )
            }
        }
    }

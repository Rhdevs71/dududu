/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.entity.developerOptions

import app.crimera.utils.changeFirstString
import app.crimera.utils.classNameToExtension
import app.crimera.utils.methodExtractor
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

val developerOptionsEntity =
    bytecodePatch(
        description = "This patch is used for decoding obfuscated code of developer options items",
    ) {
        execute {
            ExperimentsValueBuilderFingerprint.apply {
                GetQuickExperimentHelperClassExtension.changeFirstString(classNameToExtension(classDef.type))

                val getAllExperimentsMethod = classDef.methods.first { it.returnType == "Ljava/util/List;" }
                GetAllExperimentsClassExtension.changeFirstString(getAllExperimentsMethod.name)

                val experimentItemClass =
                    getAllExperimentsMethod.implementation?.instructions
                        ?.filter { it.opcode == Opcode.INVOKE_DIRECT }
                        ?.mapNotNull { runCatching { it.methodExtractor() }.getOrNull() }
                        ?.firstOrNull { it.name == "<init>" && !it.definingClass.startsWith("java.") }
                        ?.definingClass
                if (experimentItemClass != null) {
                    GetExperimentItemHelperClassExtension.changeFirstString(experimentItemClass)
                }
            }

            ExperimentsGetMobileConfigSpecifier.apply {
                method.apply {
                    val getUniversalIdInstructionData =
                        implementation?.instructions
                            ?.filter { it.opcode == Opcode.INVOKE_STATIC }
                            ?.mapNotNull { runCatching { it.methodExtractor() }.getOrNull() }
                            ?.firstOrNull { !it.definingClass.startsWith("java.") }
                    if (getUniversalIdInstructionData != null) {
                        GetUniversalIdHelperClassExtension.changeFirstString(getUniversalIdInstructionData.definingClass)
                    }
                }
            }
        }
    }

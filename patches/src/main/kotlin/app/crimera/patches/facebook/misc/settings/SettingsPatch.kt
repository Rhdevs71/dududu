/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.facebook.misc.settings

import app.crimera.patches.facebook.misc.extension.facebookExtensionPatch
import app.crimera.patches.facebook.utils.Constants.COMPATIBILITY_FACEBOOK
import app.crimera.patches.facebook.utils.Constants.FB_FRAGMENT_ACTIVITY_CLASS
import app.crimera.patches.facebook.utils.Constants.INJECTOR_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode

internal object FbFragmentActivityOnResumeFingerprint : Fingerprint(
    name = "onResume",
    definingClass = FB_FRAGMENT_ACTIVITY_CLASS,
)

@Suppress("unused")
val facebookSettingsPatch =
    bytecodePatch(
        name = "RHpatch Facebook Mod Menu",
        description = "Adds the [RHpatch] floating button on Profile screen and categorized mod menu dialog for Ghost Mode, Downloader, Ad-blocker, and Utilities.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_FACEBOOK)
        dependsOn(facebookExtensionPatch)

        execute {
            runCatching {
                FbFragmentActivityOnResumeFingerprint.method.apply {
                    val returnVoidIndex = indexOfFirstInstruction(Opcode.RETURN_VOID)
                    addInstruction(
                        returnVoidIndex,
                        """
                        invoke-static {p0}, $INJECTOR_CLASS->onActivityResume(Landroid/app/Activity;)V
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook FbFragmentActivity.onResume: ${e.message}")
            }
        }
    }

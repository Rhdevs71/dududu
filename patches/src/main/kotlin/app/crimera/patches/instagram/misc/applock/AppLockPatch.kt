/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.applock

import app.crimera.patches.instagram.misc.settings.settingsPatch
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.enableSettings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode

internal object IgFragmentActivityOnResumeFingerprint : Fingerprint(
    name = "onResume",
    definingClass = "Lcom/instagram/base/activity/IgFragmentActivity;",
)

internal object IgFragmentActivityOnStopFingerprint : Fingerprint(
    name = "onStop",
    definingClass = "Lcom/instagram/base/activity/IgFragmentActivity;",
)

@Suppress("unused")
val appLockPatch =
    bytecodePatch(
        name = "App Lock",
        description = "Locks Instagram with Biometric / Device PIN when opening or resuming the app.",
        default = true,
    ) {
        dependsOn(settingsPatch)
        compatibleWith(COMPATIBILITY_INSTAGRAM)

        execute {
            enableSettings("appLock")

            runCatching {
                IgFragmentActivityOnResumeFingerprint.method.apply {
                    val returnVoidIndex = indexOfFirstInstruction(Opcode.RETURN_VOID)
                    addInstruction(
                        returnVoidIndex,
                        """
                        invoke-static {p0}, Lapp/morphe/extension/instagram/patches/applock/PikoAppLockManager;->onActivityResumed(Landroid/app/Activity;)V
                        """.trimIndent(),
                    )
                }
            }

            runCatching {
                IgFragmentActivityOnStopFingerprint.method.apply {
                    val returnVoidIndex = indexOfFirstInstruction(Opcode.RETURN_VOID)
                    addInstruction(
                        returnVoidIndex,
                        """
                        invoke-static {p0}, Lapp/morphe/extension/instagram/patches/applock/PikoAppLockManager;->onActivityStopped(Landroid/app/Activity;)V
                        """.trimIndent(),
                    )
                }
            }
        }
    }

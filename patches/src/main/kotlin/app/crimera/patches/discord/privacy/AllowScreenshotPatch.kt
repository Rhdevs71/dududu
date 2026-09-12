/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.privacy

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal object MainActivityFingerprint : Fingerprint(
    definingClass = "Lcom/discord/main/MainActivity;",
)

@Suppress("unused")
val allowScreenshotPatch =
    bytecodePatch(
        name = "Allow Screenshots & Screen Recording",
        description = "Removes FLAG_SECURE window restrictions so screenshots and screen recording work everywhere.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        execute {
            MainActivityFingerprint.classDefOrNull?.let { classDef ->
                val mutableClass = mutableClassDefBy(classDef)

                // Add onResume to MainActivity that strips FLAG_SECURE from Window
                val onResumeMethod = ImmutableMethod(
                    mutableClass.type,
                    "onResume",
                    listOf(),
                    "V",
                    AccessFlags.PUBLIC.value,
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                        invoke-super {p0}, Lcom/discord/react_activities/ReactActivity;->onResume()V
                        invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                        move-result-object v0
                        if-eqz v0, :cond_0
                        const/16 v1, 0x2000
                        invoke-virtual {v0, v1}, Landroid/view/Window;->clearFlags(I)V
                        :cond_0
                        return-void
                        """.trimIndent(),
                    )
                }
                mutableClass.methods.add(onResumeMethod)
            }
        }
    }

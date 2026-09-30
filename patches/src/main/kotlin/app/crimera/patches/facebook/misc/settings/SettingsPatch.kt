/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.facebook.misc.settings

import app.crimera.patches.facebook.misc.extension.facebookExtensionPatch
import app.crimera.patches.facebook.utils.Constants.COMPATIBILITY_FACEBOOK
import app.crimera.patches.facebook.utils.Constants.FB_APPLICATION_CLASS
import app.crimera.patches.facebook.utils.Constants.FB_FRAGMENT_ACTIVITY_CLASS
import app.crimera.patches.facebook.utils.Constants.INJECTOR_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode

internal object FbApplicationOnCreateFingerprint : Fingerprint(
    name = "onCreate",
    definingClass = FB_APPLICATION_CLASS,
)

internal object FbFragmentActivityOnResumeFingerprint : Fingerprint(
    name = "onResume",
    definingClass = FB_FRAGMENT_ACTIVITY_CLASS,
)

internal object FbMainActivityOnStartFingerprint : Fingerprint(
    name = "onStart",
    definingClass = "Lcom/facebook/katana/app/mainactivity/FbMainActivity;",
)

internal object BrowserLiteActivityOnCreateFingerprint : Fingerprint(
    name = "onCreate",
    definingClass = "Lcom/facebook/browser/lite/BrowserLiteActivity;",
)

@Suppress("unused")
val facebookSettingsPatch =
    bytecodePatch(
        name = "RHpatch Facebook Mod Menu",
        description = "Adds global Activity lifecycle tracking, floating capsule mod button, categorized settings dialog, external browser redirect, and HD video downloader.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_FACEBOOK)
        dependsOn(facebookExtensionPatch)

        execute {
            // 1. Hook FacebookApplication.onCreate() to register global ActivityLifecycleCallbacks
            runCatching {
                FbApplicationOnCreateFingerprint.method.apply {
                    val returnVoidIndex = indexOfFirstInstruction(Opcode.RETURN_VOID)
                    addInstruction(
                        returnVoidIndex,
                        """
                        invoke-static {p0}, $INJECTOR_CLASS->initApplication(Landroid/app/Application;)V
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook FacebookApplication.onCreate: ${e.message}")
            }

            // 2. Direct hook into FbFragmentActivity.onResume()
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

            // 3. Direct hook into FbMainActivity.onStart()
            runCatching {
                FbMainActivityOnStartFingerprint.method.apply {
                    val returnVoidIndex = indexOfFirstInstruction(Opcode.RETURN_VOID)
                    addInstruction(
                        returnVoidIndex,
                        """
                        invoke-static {p0}, $INJECTOR_CLASS->onActivityResume(Landroid/app/Activity;)V
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook FbMainActivity.onStart: ${e.message}")
            }

            // 4. Hook BrowserLiteActivity.onCreate() for External Browser & Link Shim bypass
            runCatching {
                BrowserLiteActivityOnCreateFingerprint.method.apply {
                    addInstruction(
                        0,
                        """
                        invoke-static {p0}, Lapp/morphe/extension/facebook/patches/FacebookLinkManager;->checkAndInterceptBrowser(Landroid/app/Activity;)V
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook BrowserLiteActivity.onCreate: ${e.message}")
            }
        }
    }

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
import app.crimera.patches.facebook.utils.Constants.PREF_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

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

internal object FeedStoryMenuF2rFingerprint : Fingerprint(
    name = "F2r",
    definingClass = "LX/Rbs;",
)

internal object BgPlaybackManagerOnStopFingerprint : Fingerprint(
    name = "onActivityStopped",
    definingClass = "Lcom/facebook/video/bgplayback/manager/BackgroundPlaybackManager;",
)

internal object ReelsVideoMenuA0iFingerprint : Fingerprint(
    name = "A0i",
    definingClass = "LX/S2R;",
)

@Suppress("unused")
val facebookSettingsPatch =
    bytecodePatch(
        name = "RHpatch Facebook Mod Menu",
        description = "Adds global Activity lifecycle tracking, floating capsule mod button, categorized settings dialog, external browser redirect, post 3-dots downloader, and background video audio playback.",
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

            // 5. Hook LX/Rbs;->F2r to inject post 3-dots media download menu
            runCatching {
                FeedStoryMenuF2rFingerprint.method.apply {
                    val returnObjIndex = indexOfFirstInstruction(Opcode.RETURN_OBJECT)
                    addInstruction(
                        returnObjIndex,
                        """
                        invoke-static {v0, p1, p2}, Lapp/morphe/extension/facebook/patches/FacebookPostMenuHook;->onPostMenuCreated(Landroid/app/Dialog;Landroid/view/View;Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook LX/Rbs;->F2r: ${e.message}")
            }

            // 6. Hook BackgroundPlaybackManager.onActivityStopped() for background video playback
            runCatching {
                BgPlaybackManagerOnStopFingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBackgroundAudioPlay()Z
                        move-result v0
                        if-eqz v0, :cond_normal_stop
                        return-void
                        :cond_normal_stop
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook BackgroundPlaybackManager.onActivityStopped: ${e.message}")
            }

            // 7. Hook LX/TXv;->A01()Ljava/util/List; to inject native Reels bottom sheet download row
            runCatching {
                val method = mutableClassDefByOrNull("LX/TXv;")?.methods?.firstOrNull { it.name == "A01" }
                method?.apply {
                    val returnInstructions = instructions
                        .filter { it.opcode == Opcode.RETURN_OBJECT }
                        .toList()
                        .reversed()

                    var hookCount = 0
                    for (retInst in returnInstructions) {
                        val retIdx = retInst.location.index
                        val retReg = (retInst as OneRegisterInstruction).registerA

                        addInstructions(
                            retIdx,
                            """
                            move-object/from16 v1, v$retReg
                            move-object/from16 v0, p0
                            invoke-static {v0, v1}, Lapp/morphe/extension/facebook/patches/FacebookReelsMenuHook;->onReelsItemsCreated(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;
                            move-result-object v$retReg
                            """.trimIndent(),
                        )
                        hookCount++
                    }
                    println("[SettingsPatch] Successfully hooked LX/TXv;->A01 at $hookCount return points")
                } ?: println("[SettingsPatch] Warning: LX/TXv;->A01 method not found")
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook LX/TXv;->A01: ${e.message}")
            }

            // 8. Legacy fallback hook for LX/S2R;->A0i
            runCatching {
                val method = mutableClassDefByOrNull("LX/S2R;")?.methods?.firstOrNull { it.name == "A0i" }
                    ?: ReelsVideoMenuA0iFingerprint.methodOrNull
                method?.apply {
                    addInstructions(
                        0,
                        """
                        move-object/from16 v0, p1
                        move-object/from16 v1, p2
                        move-object/from16 v2, p3
                        invoke-static {v0, v1, v2}, Lapp/morphe/extension/facebook/patches/FacebookReelsMenuHook;->onReelsMenuCreated(Landroid/view/Menu;Landroid/view/View;Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[SettingsPatch] Failed to hook LX/S2R;->A0i: ${e.message}")
            }
        }
    }

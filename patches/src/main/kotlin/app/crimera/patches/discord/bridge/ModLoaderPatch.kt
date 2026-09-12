/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.bridge

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.crimera.patches.discord.misc.extension.discordExtensionPatch
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode

internal object ReactInstanceLoadJSBundleFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/runtime/ReactInstance${'$'}loadJSBundle${'$'}1;",
)

internal val modLoaderResourcePatch =
    resourcePatch {
        execute {
            try {
                val stream = inputStreamFromBundledResource("discord", "assets/piko_discord.js")
                if (stream != null) {
                    val target = get("assets").resolve("piko_discord.js")
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> stream.copyTo(out) }
                }
            } catch (ignored: Exception) {
            }
        }
    }

@Suppress("unused")
val modLoaderPatch =
    bytecodePatch(
        name = "Discord Client Mod Loader",
        description = "Injects out-of-the-box Revenge/Pyoncord client mod runtime with built-in Nitro perks, and supports dynamic loading from internal app storage.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        dependsOn(discordExtensionPatch, modLoaderResourcePatch)

        execute {
            ReactInstanceLoadJSBundleFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "loadScriptFromAssets" && method.parameters.size == 3) {
                    method.addInstructions(
                        0,
                        """
                        invoke-static {p0, p1, p2}, Lapp/morphe/extension/discord/loader/PikoDiscordLoader;->beforeLoadScriptFromAssets(Ljava/lang/Object;Landroid/content/res/AssetManager;Ljava/lang/String;)V
                        """.trimIndent(),
                    )
                } else if (method.name == "loadScriptFromFile" && method.parameters.size == 3) {
                    method.addInstructions(
                        0,
                        """
                        invoke-static {p0, p1, p2}, Lapp/morphe/extension/discord/loader/PikoDiscordLoader;->beforeLoadScriptFromFile(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V
                        """.trimIndent(),
                    )
                }
            }
        }
    }

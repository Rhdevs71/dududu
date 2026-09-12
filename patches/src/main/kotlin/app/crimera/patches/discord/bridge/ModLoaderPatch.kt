/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.bridge

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object DCDReactNativeHostFingerprint : Fingerprint(
    definingClass = "Lcom/discord/bridge/DCDReactNativeHost;",
)

@Suppress("unused")
val modLoaderPatch =
    bytecodePatch(
        name = "Discord Client Mod Loader",
        description = "Allows dynamic loading of external JS bundles (Pyoncord / Revenge / custom mods) from /sdcard/Download/Piko/Discord/bundle.js.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        execute {
            DCDReactNativeHostFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "getJSBundleFile" && method.returnType == "Ljava/lang/String;") {

                    method.addInstructions(
                        0,
                        """
                        new-instance v0, Ljava/io/File;
                        const-string v1, "/sdcard/Download/Piko/Discord/bundle.js"
                        invoke-direct {v0, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V
                        invoke-virtual {v0}, Ljava/io/File;->exists()Z
                        move-result v1
                        if-eqz v1, :cond_piko_custom
                        invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
                        move-result-object v0
                        return-object v0

                        :cond_piko_custom
                        new-instance v0, Ljava/io/File;
                        const-string v1, "/sdcard/Download/Piko/discord_bundle.js"
                        invoke-direct {v0, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V
                        invoke-virtual {v0}, Ljava/io/File;->exists()Z
                        move-result v1
                        if-eqz v1, :cond_piko_orig
                        invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;
                        move-result-object v0
                        return-object v0

                        :cond_piko_orig
                        """.trimIndent(),
                    )
                }
            }
        }
    }

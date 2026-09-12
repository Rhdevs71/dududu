/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.customization

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.crimera.patches.discord.misc.extension.discordExtensionPatch
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object AppIconModuleFingerprint : Fingerprint(
    definingClass = "Lcom/discord/app_icon/AppIconModule;",
)

internal object AppIconUtilFingerprint : Fingerprint(
    definingClass = "Lcom/discord/app_icon/AppIconUtil;",
)

@Suppress("unused")
val unlockNitroAppIconsPatch =
    bytecodePatch(
        name = "Unlock Nitro Launcher Icons",
        description = "Unlocks custom Discord app launcher icons on your home screen without Nitro restrictions.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        dependsOn(discordExtensionPatch)

        execute {
            // Ensure AppIconModule.setIcon succeeds without throwing rejected promises
            AppIconModuleFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "setIcon" && method.returnType == "V" && method.parameters.size == 2) {
                    method.addInstructions(
                        0,
                        """
                        :try_start_piko
                        invoke-virtual {p0}, Lcom/facebook/react/bridge/BaseJavaModule;->getReactApplicationContext()Lcom/facebook/react/bridge/ReactApplicationContext;
                        move-result-object v0
                        invoke-static {v0, p1}, Lapp/morphe/extension/discord/appicon/PikoAppIconManager;->applyAppIcon(Landroid/content/Context;Ljava/lang/String;)V
                        sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                        invoke-interface {p2, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                        return-void
                        :try_end_piko
                        .catch Ljava/lang/Exception; {:try_start_piko .. :try_end_piko} :catch_piko
                        :catch_piko
                        sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                        invoke-interface {p2, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                        return-void
                        """.trimIndent(),
                    )
                }
            }
        }
    }

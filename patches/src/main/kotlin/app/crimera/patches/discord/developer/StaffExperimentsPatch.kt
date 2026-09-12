/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.developer

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object ClientInfoFingerprint : Fingerprint(
    definingClass = "Lcom/discord/client_info/ClientInfo;",
)

internal object CrashPersistenceFingerprint : Fingerprint(
    definingClass = "Lcom/discord/crash_reporting/CrashPersistence;",
)

internal object DCDReactNativeHostFingerprint : Fingerprint(
    definingClass = "Lcom/discord/bridge/DCDReactNativeHost;",
)

@Suppress("unused")
val staffExperimentsPatch =
    bytecodePatch(
        name = "Unlock Staff & Developer Experiments",
        description = "Unlocks internal Discord Developer Settings, staff flags, and experiment features.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        execute {
            // 1. Hook ClientInfo: isDeveloperBuild() and isDebugBuild()
            ClientInfoFingerprint.classDefOrNull?.methods?.forEach { method ->
                if ((method.name == "isDeveloperBuild" || method.name == "isDebugBuild") && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 1
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 2. Hook CrashPersistence: isStaff() -> true
            CrashPersistenceFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "isStaff" && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 1
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 3. Hook DCDReactNativeHost: getUseDeveloperSupport() -> true
            DCDReactNativeHostFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "getUseDeveloperSupport" && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 1
                        return v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }

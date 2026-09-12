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

internal object CrashReportingFingerprint : Fingerprint(
    definingClass = "Lcom/discord/crash_reporting/CrashReporting;",
)

internal object TelemetryRingFingerprint : Fingerprint(
    definingClass = "Lcom/discord/crash_reporting/TelemetryRing;",
)

internal object InstallReferrerModuleFingerprint : Fingerprint(
    definingClass = "Lcom/discord/analytics/InstallReferrerModule;",
)

@Suppress("unused")
val noTrackPatch =
    bytecodePatch(
        name = "Disable Telemetry & Tracking",
        description = "Blocks Discord analytics, Sentry crash reporting, install referrers, and telemetry logging.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        execute {
            // 1. Hook CrashReporting: force isDisabled() -> true and stub event capture
            CrashReportingFingerprint.classDefOrNull?.methods?.forEach { method ->
                when {
                    method.name == "isDisabled" && method.returnType == "Z" -> {
                        method.addInstructions(
                            0,
                            """
                            const/4 v0, 1
                            return v0
                            """.trimIndent(),
                        )
                    }
                    (method.name == "addBreadcrumb" ||
                        method.name == "captureException" ||
                        method.name == "captureMessage" ||
                        method.name == "addBreadcrumbBatchBinary") && method.returnType == "V" -> {
                        method.addInstructions(
                            0,
                            """
                            return-void
                            """.trimIndent(),
                        )
                    }
                }
            }

            // 2. Hook TelemetryRing: stub out append and flush
            TelemetryRingFingerprint.classDefOrNull?.methods?.forEach { method ->
                if ((method.name == "append" || method.name == "scheduleFlush" || method.name == "flushPending") &&
                    method.returnType == "V") {
                    method.addInstructions(
                        0,
                        """
                        return-void
                        """.trimIndent(),
                    )
                }
            }

            // 3. Hook InstallReferrerModule: resolve promise with null immediately
            InstallReferrerModuleFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "get" && method.returnType == "V" && method.parameters.size == 1) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-interface {p1, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                        return-void
                        """.trimIndent(),
                    )
                }
            }
        }
    }

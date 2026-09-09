/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.adblock

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.AD_MANAGER_CLASS
import app.crimera.patches.spotify.utils.Constants.AUTOVALUE_PLAYER_STATE_CLASS
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.crimera.patches.spotify.utils.Constants.CREATE_SLOT_CLASS
import app.crimera.patches.spotify.utils.Constants.ES_AD_BREAK_CONTEXT_CLASS
import app.crimera.patches.spotify.utils.Constants.ES_CONTEXT_PLAYER_STATE_CLASS
import app.crimera.patches.spotify.utils.Constants.GET_ADS_CLASS
import app.crimera.patches.spotify.utils.Constants.GET_SLOT_CLASS
import app.crimera.patches.spotify.utils.Constants.HSP0_CLASS
import app.crimera.patches.spotify.utils.Constants.SUB_IN_STREAM_CLASS
import app.crimera.patches.spotify.utils.Constants.SUB_SLOT_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object AutoValuePlayerStateFingerprint : Fingerprint(
    definingClass = AUTOVALUE_PLAYER_STATE_CLASS,
)

internal object SubInStreamFingerprint : Fingerprint(
    definingClass = SUB_IN_STREAM_CLASS,
)

internal object SubSlotFingerprint : Fingerprint(
    definingClass = SUB_SLOT_CLASS,
)

internal object GetSlotFingerprint : Fingerprint(
    definingClass = GET_SLOT_CLASS,
)

internal object TriggerSlotFingerprint : Fingerprint(
    definingClass = TRIGGER_SLOT_CLASS,
)

internal object GetAdsFingerprint : Fingerprint(
    definingClass = GET_ADS_CLASS,
)

internal object CreateSlotFingerprint : Fingerprint(
    definingClass = CREATE_SLOT_CLASS,
)

internal object EsContextPlayerStateFingerprint : Fingerprint(
    definingClass = ES_CONTEXT_PLAYER_STATE_CLASS,
)

internal object Hsp0Fingerprint : Fingerprint(
    definingClass = HSP0_CLASS,
)

internal object GbkFingerprint : Fingerprint(
    definingClass = "Lp/gbk;",
)

internal object SbkFingerprint : Fingerprint(
    definingClass = "Lp/sbk;",
)


@Suppress("unused")
val spotifyAdblockPatch =
    bytecodePatch(
        name = "Spotify Ad-Block",
        description = "Blocks audio, video ads, sponsored content, ad breaks, and removes the Premium tab from navigation.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            // 1. Remove adBreakContext in PlayerState (returns Absent instance)
            AutoValuePlayerStateFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "adBreakContext") {
                    method.addInstructions(
                        0,
                        """
                        sget-object v0, Lp/p5;->a:Lp/p5;
                        return-object v0
                        """.trimIndent(),
                    )
                }
            }

            // 2. SubInStreamResponse -> neutralize ad presence
            SubInStreamFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "n" && method.returnType == "Lcom/spotify/ads/esperanto/proto/Ad;") {
                    method.addInstructions(
                        0,
                        """
                        invoke-static {}, Lcom/spotify/ads/esperanto/proto/Ad;->r()Lcom/spotify/ads/esperanto/proto/Ad;
                        move-result-object v0
                        return-object v0
                        """.trimIndent(),
                    )
                }
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $AD_MANAGER_CLASS->neutralizeAdFlag(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 3. SubSlotResponse -> neutralize ad presence
            SubSlotFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $AD_MANAGER_CLASS->neutralizeAdFlag(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 4. GetSlotResponse -> neutralize ad presence
            GetSlotFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $AD_MANAGER_CLASS->neutralizeAdFlag(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 5. TriggerSlotResponse -> neutralize trigger flag (o()Z)
            TriggerSlotFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $AD_MANAGER_CLASS->neutralizeAdFlag(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 6. GetAdsResponse -> neutralize ads returned flag
            GetAdsFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $AD_MANAGER_CLASS->neutralizeAdFlag(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 7. CreateSlotResponse -> neutralize create slot flag
            CreateSlotFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $AD_MANAGER_CLASS->neutralizeAdFlag(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 8. EsContextPlayerState -> return empty default AdBreakContext
            EsContextPlayerStateFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "o" && method.returnType == ES_AD_BREAK_CONTEXT_CLASS) {
                    method.addInstructions(
                        0,
                        """
                        invoke-static {}, $ES_AD_BREAK_CONTEXT_CLASS->o()$ES_AD_BREAK_CONTEXT_CLASS
                        move-result-object v0
                        return-object v0
                        """.trimIndent(),
                    )
                }
            }

            // 9. Lp/hsp0; -> disable Premium tab (b()Z) and Premium marketing (a()Z)
            Hsp0Fingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    if (method.name == "b" || method.name == "a") {
                        method.addInstructions(
                            0,
                            """
                            const/4 v0, 0
                            return v0
                            """.trimIndent(),
                        )
                    }
                }
            }

            // 10. Lp/gbk;->w(ContextTrack)Z -> always return false (no track is an ad)
            GbkFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "w" && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 11. Lp/sbk;->p & q -> always return false
            SbkFingerprint.classDefOrNull?.methods?.forEach { method ->
                if ((method.name == "p" || method.name == "q") && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        return v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }


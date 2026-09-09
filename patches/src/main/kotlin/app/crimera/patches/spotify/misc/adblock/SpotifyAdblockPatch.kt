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
import app.crimera.patches.spotify.utils.Constants.GET_SLOT_CLASS
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

@Suppress("unused")
val spotifyAdblockPatch =
    bytecodePatch(
        name = "Spotify Ad-Block",
        description = "Blocks audio, video ads, sponsored content, and ad breaks across Spotify playback and playlists.",
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
        }
    }

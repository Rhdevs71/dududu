/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.lyrics

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.crimera.patches.spotify.utils.Constants.PLAYBACK_MANAGER_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val LYRICS_FULLSCREEN_ACTIVITY = "Lcom/spotify/lyrics/fullscreenview/page/LyricsFullscreenPageActivity;"

internal object LyricsFullscreenFingerprint : Fingerprint(
    definingClass = LYRICS_FULLSCREEN_ACTIVITY,
)

@Suppress("unused")
val spotifyLyricsPatch =
    bytecodePatch(
        name = "Spotify Unlimited Lyrics",
        description = "Bypasses lyrics capping, restrictions, and paywalls so live lyrics are always available.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            LyricsFullscreenFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.returnType == "Z" && method.parameters.isEmpty() && method.name.contains("Capped", ignoreCase = true)) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        invoke-static {v0}, $PLAYBACK_MANAGER_CLASS->isLyricsCapped(Z)Z
                        move-result v0
                        return v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }

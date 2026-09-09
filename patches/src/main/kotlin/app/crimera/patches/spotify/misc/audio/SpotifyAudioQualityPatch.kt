/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.audio

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val BITRATE_LEVEL_CLASS = "Lcom/spotify/player/model/BitrateLevel;"
private const val AUTOVALUE_PLAYBACK_QUALITY_CLASS = "Lcom/spotify/player/model/AutoValue_PlaybackQuality;"

internal object PlaybackQualityFingerprint : Fingerprint(
    definingClass = AUTOVALUE_PLAYBACK_QUALITY_CLASS,
)

@Suppress("unused")
val spotifyAudioQualityPatch =
    bytecodePatch(
        name = "Spotify Very High Audio Quality Unlock",
        description = "Unlocks and enforces 320kbps \"Very High\" extreme bitrate audio streaming quality.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            PlaybackQualityFingerprint.classDefOrNull?.methods?.forEach { method ->
                when (method.name) {
                    "bitrateLevel",
                    "highestAvailableQuality",
                    "targetBitrateLevel" -> {
                        if (method.returnType == BITRATE_LEVEL_CLASS) {
                            method.addInstructions(
                                0,
                                """
                                sget-object v0, $BITRATE_LEVEL_CLASS->VERY_HIGH:$BITRATE_LEVEL_CLASS
                                return-object v0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            }
        }
    }

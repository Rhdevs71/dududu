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
private const val PREMIUM_BADGE_VIEW_CLASS = "Lcom/spotify/encoreconsumermobile/elements/badge/premium/PremiumBadgeView;"
private const val P17_CLASS = "Lp/p17;"

internal object PlaybackQualityFingerprint : Fingerprint(
    definingClass = AUTOVALUE_PLAYBACK_QUALITY_CLASS,
)

internal object PremiumBadgeViewFingerprint : Fingerprint(
    definingClass = PREMIUM_BADGE_VIEW_CLASS,
)

internal object P17Fingerprint : Fingerprint(
    definingClass = P17_CLASS,
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
            // 1. Force PlaybackQuality getters to return VERY_HIGH
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
                                "".trimIndent(),
                            )
                        }
                    }
                }
            }

            // 2. Hide green Premium badge in settings and app-wide
            PremiumBadgeViewFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "d" && method.returnType == "V" && method.parameters.size == 1 && method.parameters[0] == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/16 p1, 0x8
                        invoke-virtual {p0, p1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                        """.trimIndent(),
                    )
                }
            }

            // 3. Unlock Very High & Lossless rows in Media Quality settings (p17->a)
            P17Fingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "a" && method.returnType == "Lp/lmh0;" && method.parameters.size == 3 && method.parameters[0] == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 p1, 0
                        """.trimIndent(),
                    )
                }
            }
        }
    }

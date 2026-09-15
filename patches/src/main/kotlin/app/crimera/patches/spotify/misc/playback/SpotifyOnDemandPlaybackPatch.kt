/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.playback

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.AUTOVALUE_RESTRICTIONS_CLASS
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.crimera.patches.spotify.utils.Constants.RESTRICTIONS_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object OnDemandRestrictionsFingerprint : Fingerprint(
    definingClass = AUTOVALUE_RESTRICTIONS_CLASS,
)

@Suppress("unused")
val spotifyOnDemandPlaybackPatch =
    bytecodePatch(
        name = "Spotify On-Demand Playback & Disable Force-Shuffle",
        description = "Unlocks direct on-demand track selection from any playlist or album and removes forced shuffle mode.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            OnDemandRestrictionsFingerprint.classDefOrNull?.methods?.forEach { method ->
                when (method.name) {
                    "disallowTapToPlayTrackInNextTracksReasons",
                    "disallowViewingOrderedTracksInNextTracksReasons",
                    "disallowTogglingShuffleReasons",
                    "disallowPlayAsNextInQueueReasons",
                    "disallowAddToQueueReasons",
                    "disallowAddToQueueTrackReasons" -> {
                        method.addInstructions(
                            0,
                            """
                            invoke-static {}, Ljava/util/Collections;->emptySet()Ljava/util/Set;
                            move-result-object v0
                            return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        }
    }

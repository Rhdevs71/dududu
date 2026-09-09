/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.playback

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.AUTOVALUE_PLAYER_STATE_CLASS
import app.crimera.patches.spotify.utils.Constants.AUTOVALUE_RESTRICTIONS_CLASS
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.crimera.patches.spotify.utils.Constants.RESTRICTIONS_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object AutoValuePlayerStateRestrictionsFingerprint : Fingerprint(
    definingClass = AUTOVALUE_PLAYER_STATE_CLASS,
)

internal object AutoValueRestrictionsFingerprint : Fingerprint(
    definingClass = AUTOVALUE_RESTRICTIONS_CLASS,
)

@Suppress("unused")
val spotifyPlaybackRestrictionsPatch =
    bytecodePatch(
        name = "Spotify Playback Restrictions Unlock",
        description = "Unlocks unlimited track skips, scrubbing/seeking on player bar, repeat track/context, and toggle shuffle without limits.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            // 1. Hook AutoValue_PlayerState restrictions & contextRestrictions
            AutoValuePlayerStateRestrictionsFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "restrictions" || method.name == "contextRestrictions") {
                    method.addInstructions(
                        0,
                        """
                        sget-object v0, $RESTRICTIONS_CLASS->EMPTY:$RESTRICTIONS_CLASS
                        return-object v0
                        """.trimIndent(),
                    )
                }
            }

            // 2. Hook AutoValue_Restrictions disallow* methods to always return empty Set / Map
            AutoValueRestrictionsFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name.startsWith("disallow") && method.parameters.isEmpty()) {
                    if (method.returnType == "Ljava/util/Set;") {
                        method.addInstructions(
                            0,
                            """
                            invoke-static {}, Ljava/util/Collections;->emptySet()Ljava/util/Set;
                            move-result-object v0
                            return-object v0
                            """.trimIndent(),
                        )
                    } else if (method.returnType == "Ljava/util/Map;") {
                        method.addInstructions(
                            0,
                            """
                            invoke-static {}, Ljava/util/Collections;->emptyMap()Ljava/util/Map;
                            move-result-object v0
                            return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        }
    }

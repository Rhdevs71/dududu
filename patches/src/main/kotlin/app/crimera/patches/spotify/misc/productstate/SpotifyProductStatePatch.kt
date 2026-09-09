/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.productstate

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val APP_PROTOCOL_CAPABILITIES_CLASS = "Lcom/spotify/interapp/model/AppProtocol\$Capabilities;"

internal object AppProtocolCapabilitiesFingerprint : Fingerprint(
    definingClass = APP_PROTOCOL_CAPABILITIES_CLASS,
)

@Suppress("unused")
val spotifyProductStatePatch =
    bytecodePatch(
        name = "Spotify ProductState & Capabilities Unlock",
        description = "Unlocks native player capabilities (can_play_on_demand, interruption-free) and spoofs Premium product attributes.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            AppProtocolCapabilitiesFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "getCanPlayOnDemand" && method.returnType == "Ljava/lang/Boolean;") {
                    method.addInstructions(
                        0,
                        """
                        sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                        return-object v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }

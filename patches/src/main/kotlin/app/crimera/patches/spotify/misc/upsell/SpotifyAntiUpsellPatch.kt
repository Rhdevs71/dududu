/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.upsell

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val SHOULD_UPSELL_RESPONSE_CLASS = "Lcom/spotify/upsells/v1/proto/ShouldUpsellResponse;"
private const val UPSELL_RESULT_ENUM_CLASS = "Lp/uf91;"
private const val JNE1_CLASS = "Lp/jne1;"

internal object ShouldUpsellResponseFingerprint : Fingerprint(
    definingClass = SHOULD_UPSELL_RESPONSE_CLASS,
)

internal object Jne1Fingerprint : Fingerprint(
    definingClass = JNE1_CLASS,
)

@Suppress("unused")
val spotifyAntiUpsellPatch =
    bytecodePatch(
        name = "Spotify Clean UI & Anti-Upsell",
        description = "Suppresses in-app subscription pop-ups (\"Dapatkan Premium\") and forces NO_UPSELL on all upsell evaluation requests.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            // 1. Hook ShouldUpsellResponse getters
            ShouldUpsellResponseFingerprint.classDefOrNull?.methods?.forEach { method ->
                // Hook getter returning the upsell decision enum
                if (method.returnType == UPSELL_RESULT_ENUM_CLASS && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        sget-object v0, $UPSELL_RESULT_ENUM_CLASS->d:$UPSELL_RESULT_ENUM_CLASS
                        return-object v0
                        """.trimIndent(),
                    )
                }

                // Hook boolean checks if any
                if (method.returnType == "Z" && method.parameters.isEmpty()) {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 2. Silence Compose Upsell Dialog (e.g. "Mau mengontrol cara mendengarkan?")
            Jne1Fingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "d" && method.returnType == "V" && method.parameters.size == 7) {
                    method.addInstructions(
                        0,
                        """
                        return-void
                        """.trimIndent(),
                    )
                }
            }
        }
    }

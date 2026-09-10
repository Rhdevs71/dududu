/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.productstate

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.crimera.patches.spotify.utils.Constants.PRODUCT_STATE_SPOOFER_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val APP_PROTOCOL_CAPABILITIES_CLASS = "Lcom/spotify/interapp/model/AppProtocol\$Capabilities;"
private const val I6Z_CLASS = "Lp/i6z;"

internal object AppProtocolCapabilitiesFingerprint : Fingerprint(
    definingClass = APP_PROTOCOL_CAPABILITIES_CLASS,
)

internal object I6zFingerprint : Fingerprint(
    definingClass = I6Z_CLASS,
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
            // 1. AppProtocol$Capabilities -> getCanPlayOnDemand always TRUE
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

            // 2. Lp/i6z; -> intercept single key lookups for booleans and string attributes at source
            I6zFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "a" && method.parameters.size == 2 && method.returnType == "Lp/f2z;") {
                    method.addInstructions(
                        0,
                        """
                        invoke-static {p1, p2}, $PRODUCT_STATE_SPOOFER_CLASS->getProductStateBoolean(Ljava/lang/String;Z)Z
                        move-result p2
                        """.trimIndent(),
                    )
                } else if (method.name == "c" && method.parameters.size == 2 && method.returnType == "Lp/f2z;") {
                    method.addInstructions(
                        0,
                        """
                        invoke-static {p1, p2}, $PRODUCT_STATE_SPOOFER_CLASS->getProductStateAttribute(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p2
                        """.trimIndent(),
                    )
                }
            }
        }
    }

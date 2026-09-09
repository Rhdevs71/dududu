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

private const val APP_PROTOCOL_CAPABILITIES_CLASS = "Lcom/spotify/interapp/model/AppProtocol\$Capabilities;"
private const val I6Z_CLASS = "Lp/i6z;"
private const val D6Z_CLASS = "Lp/d6z;"
private const val NMJ_CLASS = "Lp/nmj;"

internal object AppProtocolCapabilitiesFingerprint : Fingerprint(
    definingClass = APP_PROTOCOL_CAPABILITIES_CLASS,
)

internal object I6zFingerprint : Fingerprint(
    definingClass = I6Z_CLASS,
)

internal object D6zFingerprint : Fingerprint(
    definingClass = D6Z_CLASS,
)

internal object NmjFingerprint : Fingerprint(
    definingClass = NMJ_CLASS,
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

            // 3. Lp/d6z; -> intercept dynamic boolean emissions in emit()
            D6zFingerprint.classDefOrNull?.methods?.firstOrNull { it.name == "emit" }?.let { m ->
                val instructions = m.implementation?.instructions?.toList() ?: return@let
                for ((index, ins) in instructions.withIndex()) {
                    if (ins.opcode == Opcode.INVOKE_STATIC && ins.toString().contains("nrf1->i")) {
                        m.addInstructions(
                            index + 2,
                            """
                            iget-object p2, p0, Lp/d6z;->b:Ljava/lang/String;
                            invoke-static {p2, p1}, $PRODUCT_STATE_SPOOFER_CLASS->getProductStateBoolean(Ljava/lang/String;Z)Z
                            move-result p1
                            """.trimIndent(),
                        )
                        break
                    }
                }
            }

            // 4. Lp/nmj; -> intercept dynamic string attribute emissions in emit()
            NmjFingerprint.classDefOrNull?.methods?.firstOrNull { it.name == "emit" }?.let { m ->
                val instructions = m.implementation?.instructions?.toList() ?: return@let
                for ((index, ins) in instructions.withIndex()) {
                    if (ins.opcode == Opcode.INVOKE_INTERFACE && ins.toString().contains("Lp/n2z;->emit")) {
                        m.addInstructions(
                            index,
                            """
                            iget-object v2, p0, Lp/nmj;->c:Ljava/lang/Object;
                            check-cast v2, Ljava/lang/String;
                            invoke-static {v2, p1}, $PRODUCT_STATE_SPOOFER_CLASS->getProductStateAttribute(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object p1
                            """.trimIndent(),
                        )
                        break
                    }
                }
            }
        }
    }

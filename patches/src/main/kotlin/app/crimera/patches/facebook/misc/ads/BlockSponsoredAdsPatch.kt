/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.facebook.misc.ads

import app.crimera.patches.facebook.misc.extension.facebookExtensionPatch
import app.crimera.patches.facebook.utils.Constants.COMPATIBILITY_FACEBOOK
import app.crimera.patches.facebook.utils.Constants.PREF_CLASS
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

internal object SponsoredLabelPluginA00Fingerprint : Fingerprint(
    name = "A00",
    definingClass = "Lcom/facebook/feed/plugins/header/subtitle/impl/sponsoredlabel/SponsoredLabelPlugin;",
)

@Suppress("unused")
val blockSponsoredAdsPatch =
    bytecodePatch(
        name = "Block Sponsored Ads",
        description = "Blocks and neutralizes sponsored advertisements and promotional labels in Facebook feed.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_FACEBOOK)
        dependsOn(facebookExtensionPatch)

        execute {
            runCatching {
                SponsoredLabelPluginA00Fingerprint.method.apply {
                    addInstruction(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_ad
                        const/4 v0, 0x0
                        return v0
                        :cond_bypass_ad
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking SponsoredLabelPlugin.A00: ${e.message}")
            }
        }
    }

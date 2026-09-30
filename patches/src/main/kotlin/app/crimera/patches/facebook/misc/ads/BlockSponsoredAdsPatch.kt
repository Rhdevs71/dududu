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
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object SponsoredLabelPluginA00Fingerprint : Fingerprint(
    name = "A00",
    definingClass = "Lcom/facebook/feed/plugins/header/subtitle/impl/sponsoredlabel/SponsoredLabelPlugin;",
)

internal object SponsoredLabelPluginA01Fingerprint : Fingerprint(
    name = "A01",
    definingClass = "Lcom/facebook/feed/plugins/header/subtitle/impl/sponsoredlabel/SponsoredLabelPlugin;",
)

internal object LX2SuA00Fingerprint : Fingerprint(
    name = "A00",
    definingClass = "LX/2Su;",
)

internal object LX2SuA01Fingerprint : Fingerprint(
    name = "A01",
    definingClass = "LX/2Su;",
)

internal object LX2SuA03Fingerprint : Fingerprint(
    name = "A03",
    definingClass = "LX/2Su;",
)

internal object LX2SuA04Fingerprint : Fingerprint(
    name = "A04",
    definingClass = "LX/2Su;",
)

internal object LX2SuA05Fingerprint : Fingerprint(
    name = "A05",
    definingClass = "LX/2Su;",
)

@Suppress("unused")
val blockSponsoredAdsPatch =
    bytecodePatch(
        name = "Block Sponsored Ads",
        description = "Blocks and neutralizes sponsored advertisements and promotional labels in Facebook feed and video stream.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_FACEBOOK)
        dependsOn(facebookExtensionPatch)

        execute {
            // 1. SponsoredLabelPlugin.A00(GraphQLStory)Z -> return false
            runCatching {
                SponsoredLabelPluginA00Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_label_a00
                        const/4 v0, 0x0
                        return v0
                        :cond_bypass_label_a00
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking SponsoredLabelPlugin.A00: ${e.message}")
            }

            // 2. SponsoredLabelPlugin.A01(3Zl)CharSequence -> return null
            runCatching {
                SponsoredLabelPluginA01Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_label_a01
                        const/4 v0, 0x0
                        return-object v0
                        :cond_bypass_label_a01
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking SponsoredLabelPlugin.A01: ${e.message}")
            }

            // 3. LX/2Su.A03(2QD)Z -> return false (blocks sponsored feed subtitle & unit)
            runCatching {
                LX2SuA03Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_2su_a03
                        const/4 v0, 0x0
                        return v0
                        :cond_bypass_2su_a03
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking LX/2Su.A03: ${e.message}")
            }

            // 4. LX/2Su.A04(2NV)Z -> return false (causes Litho render to return empty component LX/740)
            runCatching {
                LX2SuA04Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_2su_a04
                        const/4 v0, 0x0
                        return v0
                        :cond_bypass_2su_a04
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking LX/2Su.A04: ${e.message}")
            }

            // 5. LX/2Su.A05(GraphQLStory)Z -> return false (neutralizes isSponsored check)
            runCatching {
                LX2SuA05Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_2su_a05
                        const/4 v0, 0x0
                        return v0
                        :cond_bypass_2su_a05
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking LX/2Su.A05: ${e.message}")
            }

            // 6. LX/2Su.A01(Object)LX/57E -> return null (neutralizes ad impression object)
            runCatching {
                LX2SuA01Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_2su_a01
                        const/4 v0, 0x0
                        return-object v0
                        :cond_bypass_2su_a01
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking LX/2Su.A01: ${e.message}")
            }

            // 7. LX/2Su.A00(LX/57E;)BaseImpression -> return null (neutralizes impression logging)
            runCatching {
                LX2SuA00Fingerprint.method.apply {
                    addInstructions(
                        0,
                        """
                        invoke-static {}, $PREF_CLASS->isBlockSponsoredAds()Z
                        move-result v0
                        if-eqz v0, :cond_bypass_2su_a00
                        const/4 v0, 0x0
                        return-object v0
                        :cond_bypass_2su_a00
                        """.trimIndent(),
                    )
                }
            }.onFailure { e ->
                println("[BlockSponsoredAdsPatch] Error hooking LX/2Su.A00: ${e.message}")
            }
        }
    }

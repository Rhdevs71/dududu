/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.efootball.ads

import app.crimera.patches.efootball.utils.Constants.ADMOB_REWARD_CLASS
import app.crimera.patches.efootball.utils.Constants.COMPATIBILITY_EFOOTBALL
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val bypassRewardedAdPatch =
    bytecodePatch(
        name = "Instant Rewarded Ad Claimer",
        description = "Bypasses all 3 types of video advertisements in eFootball (Kotak Masuk random rewards, Toko Koin 5 coins, and Kontrak Spesial free gacha), immediately granting rewards without waiting.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_EFOOTBALL)

        execute {
            try {
                val adMobRewardClass = mutableClassDefBy(ADMOB_REWARD_CLASS)

                // 1. Hook IsLoaded()I to always return 1 (LOADEND)
                // Ensures all 3 ad buttons (Kotak Masuk, Toko Koin, Kontrak Spesial) are always active and clickable!
                adMobRewardClass.methods.firstOrNull { it.name == "IsLoaded" && it.returnType == "I" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent(),
                    )
                }

                // 2. Hook IsEarnedReward()Z to always return true
                adMobRewardClass.methods.firstOrNull { it.name == "IsEarnedReward" && it.returnType == "Z" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent(),
                    )
                }

                // 3. Hook Show(Context)V to immediately set reward earned and status to SHOWEND
                // Method Show has 5 registers (v0..v3 locals, v4 parameter). Using only v0, v1, v2 is strictly safe.
                adMobRewardClass.methods.firstOrNull { it.name == "Show" && it.returnType == "V" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        sput-boolean v0, $ADMOB_REWARD_CLASS->s_isEarnedReward:Z
                        sput-boolean v0, $ADMOB_REWARD_CLASS->s_isCMP_Show_End:Z
                        const v0, 0x7fffffff
                        sput v0, $ADMOB_REWARD_CLASS->s_errorCodeByShow:I
                        sget-object v0, $ADMOB_REWARD_CLASS->s_adStatusList:[Ljp/konami/AdMobReward${'$'}AdStatus;
                        if-eqz v0, :skip_status
                        const/4 v1, 0x0
                        sget-object v2, Ljp/konami/AdMobReward${'$'}AdStatus;->SHOWEND:Ljp/konami/AdMobReward${'$'}AdStatus;
                        aput-object v2, v0, v1
                        :skip_status
                        return-void
                        """.trimIndent(),
                    )
                }
            } catch (e: Exception) {
                println("[BypassRewardedAdPatch] Warning: Failed to hook AdMobReward: ${'$'}{e.message}")
            }
        }
    }

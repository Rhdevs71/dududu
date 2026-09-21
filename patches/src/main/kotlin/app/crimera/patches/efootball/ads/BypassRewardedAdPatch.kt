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
        description = "Bypasses 30-second video advertisements in eFootball, immediately granting daily GP, Exp, and item rewards.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_EFOOTBALL)

        execute {
            try {
                val adMobRewardClass = mutableClassDefBy(ADMOB_REWARD_CLASS)

                // 1. Hook IsEarnedReward()Z to always return true (reward earned)
                adMobRewardClass.methods.firstOrNull { it.name == "IsEarnedReward" && it.returnType == "Z" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent(),
                    )
                }

                // 2. Hook IsShowEnd()I to always return 1 (ad show completed successfully)
                adMobRewardClass.methods.firstOrNull { it.name == "IsShowEnd" && it.returnType == "I" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent(),
                    )
                }

                // 3. Hook Show(Context)V to immediately set reward flags and return without showing ads
                adMobRewardClass.methods.firstOrNull { it.name == "Show" && it.returnType == "V" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        sput-boolean v0, $ADMOB_REWARD_CLASS->s_isEarnedReward:Z
                        sput-boolean v0, $ADMOB_REWARD_CLASS->s_isCMP_Show_End:Z
                        return-void
                        """.trimIndent(),
                    )
                }

                // 4. Hook ShowFunc(Context)V as fallback
                adMobRewardClass.methods.firstOrNull { it.name == "ShowFunc" && it.returnType == "V" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        sput-boolean v0, $ADMOB_REWARD_CLASS->s_isEarnedReward:Z
                        sput-boolean v0, $ADMOB_REWARD_CLASS->s_isCMP_Show_End:Z
                        return-void
                        """.trimIndent(),
                    )
                }
            } catch (e: Exception) {
                println("[BypassRewardedAdPatch] Warning: Failed to hook AdMobReward: ${e.message}")
            }
        }
    }

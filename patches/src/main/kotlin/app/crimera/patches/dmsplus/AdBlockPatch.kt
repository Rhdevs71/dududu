package app.crimera.patches.dmsplus

import app.crimera.patches.dmsplus.Constants.DMSPLUS_COMPATIBILITY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val adBlockPatch =
    bytecodePatch(
        name = "Block Mobile Ads",
        description = "Blocks Google Mobile Ads and silences native ad factories in DMS+ video and feeds.",
        default = true,
    ) {
        compatibleWith(DMSPLUS_COMPATIBILITY)

        execute {
            // 1. Silence GoogleMobileAdsPlugin onMethodCall for ad loading methods
            try {
                val adsPluginClass = mutableClassDefBy("Lio/flutter/plugins/googlemobileads/GoogleMobileAdsPlugin;")
                adsPluginClass.methods
                    .firstOrNull { it.name == "onMethodCall" && it.parameterTypes.size == 2 }
                    ?.let { method ->
                        method.addInstructions(
                            0,
                            """
                            # Pindahkan p1 (v26) ke v0 menggunakan move-object/from16 agar aman pada format instruksi 4-bit
                            move-object/from16 v0, p1
                            iget-object v0, v0, Lio/flutter/plugin/common/MethodCall;->method:Ljava/lang/String;
                            if-eqz v0, :cond_ads_normal
                            const-string v1, "load"
                            invoke-virtual {v0, v1}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
                            move-result v1
                            if-nez v1, :cond_silence_ad
                            const-string v1, "show"
                            invoke-virtual {v0, v1}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
                            move-result v1
                            if-eqz v1, :cond_ads_normal

                            :cond_silence_ad
                            # Pindahkan p2 (v27) ke v1 menggunakan move-object/from16
                            move-object/from16 v1, p2
                            const/4 v0, 0x0
                            invoke-interface {v1, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                            return-void

                            :cond_ads_normal
                            """.trimIndent(),
                        )
                    }
            } catch (e: Exception) {
                println("[AdBlockPatch] Warning: Failed to hook GoogleMobileAdsPlugin: ${e.message}")
            }
        }
    }

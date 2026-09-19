package app.crimera.patches.dmsplus

import app.crimera.patches.dmsplus.Constants.DMSPLUS_COMPATIBILITY
import app.crimera.patches.dmsplus.Constants.SCREEN_PROTECTOR_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val allowScreenshotPatch =
    bytecodePatch(
        name = "Allow Screenshot & Screen Recording",
        description = "Menghapus proteksi FLAG_SECURE (layar hitam) agar tangkapan layar dan perekaman layar dapat dilakukan bebas di seluruh konten.",
        default = true,
    ) {
        compatibleWith(DMSPLUS_COMPATIBILITY)

        execute {
            try {
                val protectorClass = mutableClassDefBy(SCREEN_PROTECTOR_CLASS)
                protectorClass.methods
                    .firstOrNull { it.name == "onMethodCall" && it.parameterTypes.size == 2 }
                    ?.apply {
                        addInstructions(
                            0,
                            """
                            # 1. Bersihkan FLAG_SECURE (0x2000) dari Window Activity jika ada
                            iget-object v0, p0, Lg4/a;->a:Landroid/app/Activity;
                            if-eqz v0, :cond_skip_clear
                            invoke-virtual {v0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                            move-result-object v0
                            if-eqz v0, :cond_skip_clear
                            const/16 v1, 0x2000
                            invoke-virtual {v0, v1}, Landroid/view/Window;->clearFlags(I)V

                            :cond_skip_clear
                            # 2. Jika Flutter mengecek status isRecording, selalu jawab FALSE (tidak sedang merekam)
                            iget-object v0, p1, Lio/flutter/plugin/common/MethodCall;->method:Ljava/lang/String;
                            if-eqz v0, :cond_normal_ret
                            const-string v1, "isRecording"
                            invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                            move-result v0
                            if-eqz v0, :cond_normal_ret
                            sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                            invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                            return-void

                            # 3. Untuk panggilan preventScreenshotOn / protectDataLeakageOn, balas TRUE tanpa memasang FLAG_SECURE
                            :cond_normal_ret
                            sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                            invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                            return-void
                            """.trimIndent(),
                        )
                    }
            } catch (e: Exception) {
                println("[AllowScreenshotPatch] Warning: Gagal hook ScreenProtectorPlugin: ${e.message}")
            }
        }
    }

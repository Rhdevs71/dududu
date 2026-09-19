package app.crimera.patches.dmsplus

import app.crimera.patches.dmsplus.Constants.DMSPLUS_COMPATIBILITY
import app.crimera.patches.dmsplus.Constants.URL_LAUNCHER_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val forceInAppSubscriptionPatch =
    bytecodePatch(
        name = "Force In-App Subscription Menu",
        description = "Mengarahkan tautan langganan web eksternal langsung ke layar pembelian In-App Purchase internal Flutter (/link/dashboard/subscribe).",
        default = true,
    ) {
        compatibleWith(DMSPLUS_COMPATIBILITY)

        execute {
            try {
                val launcherClass = mutableClassDefBy(URL_LAUNCHER_CLASS)
                launcherClass.methods
                    .firstOrNull { it.name == "launchUrl" && it.parameterTypes.size == 3 }
                    ?.let { method ->
                        // Parameters in smali: p0 = this, p1 = url (String), p2 = headers (Map), p3 = inApp (Z)
                        method.addInstructions(
                            0,
                            """
                            if-eqz p1, :cond_skip_all
                            
                            # 1. Cek apakah URL memuat kata kunci pembelian / langganan
                            const-string v0, "subscribe"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-eqz v0, :cond_check_web
                            const-string p1, "https://dmsplus.id/link/dashboard/subscribe"
                            goto :cond_launch_internal

                            # 2. Cek apakah diarahkan ke website dmsplus.id (dialihkan pengembang untuk bayar di web)
                            :cond_check_web
                            const-string v0, "https://dmsplus.id"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                            move-result v0
                            if-nez v0, :cond_rewrite_sub
                            const-string v0, "https://dmsplus.id/"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                            move-result v0
                            if-nez v0, :cond_rewrite_sub
                            const-string v0, "pricing"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-nez v0, :cond_rewrite_sub
                            const-string v0, "dmsplus.id/link/"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-eqz v0, :cond_skip_all
                            goto :cond_launch_internal

                            :cond_rewrite_sub
                            const-string p1, "https://dmsplus.id/link/dashboard/subscribe"

                            # 3. Bentuk Intent ACTION_VIEW internal ke package DMS+ sendiri
                            :cond_launch_internal
                            new-instance v0, Landroid/content/Intent;
                            const-string v1, "android.intent.action.VIEW"
                            invoke-direct {v0, v1}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
                            invoke-static {p1}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
                            move-result-object v1
                            invoke-virtual {v0, v1}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
                            const-string v1, "com.cinematichororuniverse.dmsplus"
                            invoke-virtual {v0, v1}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;
                            const/high16 v1, 0x10000000
                            invoke-virtual {v0, v1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;

                            # 4. Buka intent via Activity atau ApplicationContext
                            iget-object v1, p0, Lio/flutter/plugins/urllauncher/UrlLauncher;->activity:Landroid/app/Activity;
                            if-eqz v1, :cond_try_app_ctx
                            invoke-virtual {v1, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
                            const/4 v0, 0x1
                            return v0

                            :cond_try_app_ctx
                            iget-object v1, p0, Lio/flutter/plugins/urllauncher/UrlLauncher;->applicationContext:Landroid/content/Context;
                            if-eqz v1, :cond_skip_all
                            invoke-virtual {v1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                            const/4 v0, 0x1
                            return v0

                            :cond_skip_all
                            """.trimIndent(),
                        )
                    }
            } catch (e: Exception) {
                println("[ForceInAppSubscriptionPatch] Warning: Gagal hook UrlLauncher: ${e.message}")
            }
        }
    }

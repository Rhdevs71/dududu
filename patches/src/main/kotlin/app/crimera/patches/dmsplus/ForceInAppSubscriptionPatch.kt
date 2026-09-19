package app.crimera.patches.dmsplus

import app.crimera.patches.dmsplus.Constants.DMSPLUS_COMPATIBILITY
import app.crimera.patches.dmsplus.Constants.MAIN_ACTIVITY_CLASS
import app.crimera.patches.dmsplus.Constants.URL_LAUNCHER_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val forceInAppSubscriptionPatch =
    bytecodePatch(
        name = "Force In-App Subscription Menu",
        description = "Menyediakan akses langsung ke menu paket langganan internal Flutter (/link/dashboard/subscribe) via Launcher App Shortcut, Status Bar Quick Menu, serta mencegat redirect web eksternal/webview.",
        default = true,
    ) {
        compatibleWith(DMSPLUS_COMPATIBILITY)

        execute {
            // 1. Injeksi Launcher Shortcut, Quick Status Bar Notification, & Info Toast pada MainActivity
            try {
                val mainActivityClass = mutableClassDefBy(MAIN_ACTIVITY_CLASS)
                mainActivityClass.methods
                    .firstOrNull { it.name == "configureFlutterEngine" && it.parameterTypes.size == 1 }
                    ?.let { method ->
                        method.addInstructions(
                            0,
                            """
                            :try_start_sub_menu
                            sget v0, Landroid/os/Build${'$'}VERSION;->SDK_INT:I
                            const/16 v1, 0x19
                            if-lt v0, v1, :cond_skip_sub_menu

                            # A. Tambahkan Dynamic Launcher Shortcut ("👑 Beli Paket Langganan")
                            const-class v0, Landroid/content/pm/ShortcutManager;
                            invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/Class;)Ljava/lang/Object;
                            move-result-object v0
                            check-cast v0, Landroid/content/pm/ShortcutManager;
                            if-eqz v0, :cond_setup_notification

                            new-instance v1, Landroid/content/Intent;
                            const-string v2, "android.intent.action.VIEW"
                            invoke-direct {v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
                            const-string v2, "https://dmsplus.id/link/dashboard/subscribe"
                            invoke-static {v2}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
                            move-result-object v2
                            invoke-virtual {v1, v2}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
                            const-string v2, "com.cinematichororuniverse.dmsplus"
                            invoke-virtual {v1, v2}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;

                            new-instance v2, Landroid/content/pm/ShortcutInfo${'$'}Builder;
                            const-string v3, "dms_subs_shortcut"
                            invoke-direct {v2, p0, v3}, Landroid/content/pm/ShortcutInfo${'$'}Builder;-><init>(Landroid/content/Context;Ljava/lang/String;)V
                            const-string v3, "Beli Paket Langganan"
                            invoke-virtual {v2, v3}, Landroid/content/pm/ShortcutInfo${'$'}Builder;->setShortLabel(Ljava/lang/CharSequence;)Landroid/content/pm/ShortcutInfo${'$'}Builder;
                            move-result-object v2
                            const-string v3, "👑 Beli Paket Langganan (DMS+)"
                            invoke-virtual {v2, v3}, Landroid/content/pm/ShortcutInfo${'$'}Builder;->setLongLabel(Ljava/lang/CharSequence;)Landroid/content/pm/ShortcutInfo${'$'}Builder;
                            move-result-object v2
                            invoke-virtual {v2, v1}, Landroid/content/pm/ShortcutInfo${'$'}Builder;->setIntent(Landroid/content/Intent;)Landroid/content/pm/ShortcutInfo${'$'}Builder;
                            move-result-object v2
                            invoke-virtual {v2}, Landroid/content/pm/ShortcutInfo${'$'}Builder;->build()Landroid/content/pm/ShortcutInfo;
                            move-result-object v1

                            invoke-static {v1}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;
                            move-result-object v1
                            invoke-virtual {v0, v1}, Landroid/content/pm/ShortcutManager;->setDynamicShortcuts(Ljava/util/List;)Z

                            # B. Pasang Quick Status Bar Notification agar menu langganan dapat dibuka kapan saja
                            :cond_setup_notification
                            const-string v0, "notification"
                            invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
                            move-result-object v0
                            check-cast v0, Landroid/app/NotificationManager;
                            if-eqz v0, :cond_show_toast

                            new-instance v1, Landroid/content/Intent;
                            const-string v2, "android.intent.action.VIEW"
                            invoke-direct {v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
                            const-string v2, "https://dmsplus.id/link/dashboard/subscribe"
                            invoke-static {v2}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
                            move-result-object v2
                            invoke-virtual {v1, v2}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
                            const-string v2, "com.cinematichororuniverse.dmsplus"
                            invoke-virtual {v1, v2}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;
                            const/16 v2, 0x101
                            const/high16 v3, 0x4000000
                            invoke-static {p0, v2, v1, v3}, Landroid/app/PendingIntent;->getActivity(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;
                            move-result-object v1

                            new-instance v2, Landroid/app/Notification${'$'}Builder;
                            const-string v3, "dms_notification_channel"
                            invoke-direct {v2, p0, v3}, Landroid/app/Notification${'$'}Builder;-><init>(Landroid/content/Context;Ljava/lang/String;)V
                            const-string v3, "👑 Menu Langganan DMS+"
                            invoke-virtual {v2, v3}, Landroid/app/Notification${'$'}Builder;->setContentTitle(Ljava/lang/CharSequence;)Landroid/app/Notification${'$'}Builder;
                            move-result-object v2
                            const-string v3, "Ketuk untuk melihat harga & beli paket resmi"
                            invoke-virtual {v2, v3}, Landroid/app/Notification${'$'}Builder;->setContentText(Ljava/lang/CharSequence;)Landroid/app/Notification${'$'}Builder;
                            move-result-object v2
                            invoke-virtual {p0}, Landroid/content/Context;->getApplicationInfo()Landroid/content/pm/ApplicationInfo;
                            move-result-object v3
                            iget v3, v3, Landroid/content/pm/ApplicationInfo;->icon:I
                            invoke-virtual {v2, v3}, Landroid/app/Notification${'$'}Builder;->setSmallIcon(I)Landroid/app/Notification${'$'}Builder;
                            move-result-object v2
                            invoke-virtual {v2, v1}, Landroid/app/Notification${'$'}Builder;->setContentIntent(Landroid/app/PendingIntent;)Landroid/app/Notification${'$'}Builder;
                            move-result-object v2
                            const/4 v1, 0x1
                            invoke-virtual {v2, v1}, Landroid/app/Notification${'$'}Builder;->setOngoing(Z)Landroid/app/Notification${'$'}Builder;
                            move-result-object v2
                            invoke-virtual {v2}, Landroid/app/Notification${'$'}Builder;->build()Landroid/app/Notification;
                            move-result-object v1

                            const/16 v2, 0x26ad
                            invoke-virtual {v0, v2, v1}, Landroid/app/NotificationManager;->notify(ILandroid/app/Notification;)V

                            # C. Tampilkan Toast Panduan Singkat
                            :cond_show_toast
                            const-string v0, "👑 Menu Langganan: Buka Notifikasi atau Tekan Lama Ikon DMS+ di Beranda!"
                            const/4 v1, 0x1
                            invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
                            move-result-object v0
                            invoke-virtual {v0}, Landroid/widget/Toast;->show()V

                            :cond_skip_sub_menu
                            :try_end_sub_menu
                            .catch Ljava/lang/Throwable; {:try_start_sub_menu .. :try_end_sub_menu} :catch_sub_menu

                            :catch_sub_menu
                            """.trimIndent(),
                        )
                    }
            } catch (e: Exception) {
                println("[ForceInAppSubscriptionPatch] Warning: Gagal hook MainActivity: ${e.message}")
            }

            // 2. Mencegat launchUrl (Browser Eksternal) dan mengalihkannya ke rute internal DMS+
            try {
                val launcherClass = mutableClassDefBy(URL_LAUNCHER_CLASS)
                launcherClass.methods
                    .firstOrNull { it.name == "launchUrl" && it.parameterTypes.size == 3 }
                    ?.let { method ->
                        // Parameters in smali: p0 = this, p1 = url (String), p2 = headers (Map), p3 = inApp (Z)
                        method.addInstructions(
                            0,
                            """
                            if-eqz p1, :cond_skip_launch
                            
                            # Cek apakah URL memuat kata kunci pembelian / langganan / website dmsplus
                            const-string v0, "subscribe"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-eqz v0, :cond_check_web_launch
                            const-string p1, "https://dmsplus.id/link/dashboard/subscribe"
                            goto :cond_launch_internal

                            :cond_check_web_launch
                            const-string v0, "dmsplus.id"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-nez v0, :cond_skip_launch

                            const-string p1, "https://dmsplus.id/link/dashboard/subscribe"

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

                            iget-object v1, p0, Lio/flutter/plugins/urllauncher/UrlLauncher;->activity:Landroid/app/Activity;
                            if-eqz v1, :cond_try_launch_app_ctx
                            invoke-virtual {v1, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
                            const/4 v0, 0x1
                            return v0

                            :cond_try_launch_app_ctx
                            iget-object v1, p0, Lio/flutter/plugins/urllauncher/UrlLauncher;->applicationContext:Landroid/content/Context;
                            if-eqz v1, :cond_skip_launch
                            invoke-virtual {v1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                            const/4 v0, 0x1
                            return v0

                            :cond_skip_launch
                            """.trimIndent(),
                        )
                    }

                // 3. Mencegat openUrlInApp (In-App WebView & CustomTabs)
                launcherClass.methods
                    .firstOrNull { it.name == "openUrlInApp" && it.parameterTypes.size == 4 }
                    ?.let { method ->
                        // Parameters in smali: p0 = this, p1 = url (String), p2 = js (Z), p3 = webViewOptions, p4 = browserOptions
                        method.addInstructions(
                            0,
                            """
                            if-eqz p1, :cond_skip_inapp
                            
                            # Cek apakah URL memuat kata kunci pembelian / langganan / website dmsplus
                            const-string v0, "subscribe"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-eqz v0, :cond_check_inapp_web
                            const-string p1, "https://dmsplus.id/link/dashboard/subscribe"
                            goto :cond_launch_inapp_internal

                            :cond_check_inapp_web
                            const-string v0, "dmsplus.id"
                            invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                            move-result v0
                            if-nez v0, :cond_skip_inapp

                            const-string p1, "https://dmsplus.id/link/dashboard/subscribe"

                            :cond_launch_inapp_internal
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

                            iget-object v1, p0, Lio/flutter/plugins/urllauncher/UrlLauncher;->activity:Landroid/app/Activity;
                            if-eqz v1, :cond_try_inapp_ctx
                            invoke-virtual {v1, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
                            const/4 v0, 0x1
                            return v0

                            :cond_try_inapp_ctx
                            iget-object v1, p0, Lio/flutter/plugins/urllauncher/UrlLauncher;->applicationContext:Landroid/content/Context;
                            if-eqz v1, :cond_skip_inapp
                            invoke-virtual {v1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                            const/4 v0, 0x1
                            return v0

                            :cond_skip_inapp
                            """.trimIndent(),
                        )
                    }
            } catch (e: Exception) {
                println("[ForceInAppSubscriptionPatch] Warning: Gagal hook UrlLauncher: ${e.message}")
            }
        }
    }

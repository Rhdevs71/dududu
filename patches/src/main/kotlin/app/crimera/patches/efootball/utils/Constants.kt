/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.efootball.utils

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi.ARM64_V8A

object Constants {
    val COMPATIBILITY_EFOOTBALL =
        Compatibility(
            name = "eFootball",
            packageName = "jp.konami.pesam",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0x002B7F,
            targets =
                listOf(
                    AppTarget(
                        version = "11.0.1",
                        versionCodes =
                            mapOf(
                                ARM64_V8A to 311000101,
                            ),
                    ),
                ),
        )

    const val GAME_ACTIVITY_CLASS = "Lcom/epicgames/ue4/GameActivity;"
    const val SPLASH_ACTIVITY_CLASS = "Lcom/epicgames/ue4/SplashActivity;"
    const val GOOGLE_PLAY_LICENSING_CALLBACK = "Lcom/epicgames/ue4/GooglePlayLicensing\$MyLicenseCheckerCallback;"
    const val LICENSE_VALIDATOR_CLASS = "Lcom/google/android/vending/licensing/LicenseValidator;"
    const val DOWNLOADER_ACTIVITY_CLASS = "Ljp/konami/pesam/DownloaderActivity;"
    const val EFB_OVERLAY_MANAGER_CLASS = "Lapp/morphe/extension/crimera/efootball/EfbOverlayManager;"
}

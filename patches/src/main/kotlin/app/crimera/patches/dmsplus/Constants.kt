package app.crimera.patches.dmsplus

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val DMSPLUS_COMPATIBILITY = Compatibility(
        name = "DMS+",
        packageName = "com.cinematichororuniverse.dmsplus",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x8B0000,
        targets = listOf(
            AppTarget(version = "4.11.10", versionCode = 159),
        ),
    )

    const val PAIRIP_LICENSE_CLIENT = "Lcom/pairip/licensecheck/LicenseClient;"
    const val PAIRIP_APPLICATION = "Lcom/pairip/application/Application;"
    const val ROOT_CHECK_CLASS = "Lj2/v;"
    const val SCREEN_PROTECTOR_CLASS = "Lg4/a;"
    const val URL_LAUNCHER_CLASS = "Lio/flutter/plugins/urllauncher/UrlLauncher;"
    const val MAIN_ACTIVITY_CLASS = "Lcom/cinematichororuniverse/dmsplus/MainActivity;"
    const val IN_APP_PURCHASE_HANDLER_CLASS = "Lio/flutter/plugins/inapppurchase/MethodCallHandlerImpl;"
}

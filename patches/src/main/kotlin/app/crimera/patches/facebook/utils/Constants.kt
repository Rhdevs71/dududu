/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.facebook.utils

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi.ARM64_V8A

object Constants {
    val COMPATIBILITY_FACEBOOK =
        Compatibility(
            name = "Facebook",
            packageName = "com.facebook.katana",
            apkFileType = ApkFileType.APK,
            appIconColor = 0x1877F2,
            targets =
                listOf(
                    AppTarget(
                        version = "582.0.0.0.30",
                        versionCodes =
                            mapOf(
                                ARM64_V8A to 475404750,
                            ),
                    ),
                ),
        )

    // Facebook Native Classes
    const val FB_APPLICATION_CLASS = "Lcom/facebook/katana/app/FacebookApplication;"
    const val FB_FRAGMENT_ACTIVITY_CLASS = "Lcom/facebook/base/activity/FbFragmentActivity;"
    const val FB_MAIN_TAB_ACTIVITY_CLASS = "Lcom/facebook/katana/activity/FbMainTabActivity;"
    const val PROFILE_FRAGMENT_CLASS = "Lcom/facebook/timeline/fragment/ProfileFragment;"

    // Extension classes
    const val INTEGRATIONS_PACKAGE = "Lapp/morphe/extension/facebook"
    const val SETTINGS_CLASS = "$INTEGRATIONS_PACKAGE/settings/FacebookSettings;"
    const val PREF_CLASS = "$INTEGRATIONS_PACKAGE/settings/FacebookPref;"
    const val INJECTOR_CLASS = "$INTEGRATIONS_PACKAGE/ui/RhpatchFacebookInjector;"
    const val DIALOG_CLASS = "$INTEGRATIONS_PACKAGE/ui/RhpatchFacebookDialog;"
}

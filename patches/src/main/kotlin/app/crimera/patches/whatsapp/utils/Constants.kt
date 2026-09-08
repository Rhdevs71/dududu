/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.utils

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi.ARM64_V8A

object Constants {
    val COMPATIBILITY_WHATSAPP =
        Compatibility(
            name = "WhatsApp",
            packageName = "com.whatsapp",
            apkFileType = ApkFileType.APK,
            appIconColor = 0x25D366,
            targets =
                listOf(
                    AppTarget(
                        version = "2.26.35.71",
                        versionCodes =
                            mapOf(
                                ARM64_V8A to 263571000,
                            ),
                    ),
                ),
        )

    // WhatsApp native classes
    const val APP_SHELL_CLASS = "Lcom/whatsapp/AppShell;"
    const val ABSTRACT_APP_CLASS = "LX/004;"
    const val PROMO_ELIGIBILITY_MANAGER = "Lcom/whatsapp/nova/manager/PromoEligibilityManager;"

    // Extension classes
    const val INTEGRATIONS_PACKAGE = "Lapp/morphe/extension/whatsapp"
    const val SETTINGS_CLASS = "$INTEGRATIONS_PACKAGE/settings/WhatsAppSettings;"
    const val PREF_CLASS = "$INTEGRATIONS_PACKAGE/settings/WhatsAppPref;"
    const val SPOOFER_CLASS = "$INTEGRATIONS_PACKAGE/patches/clone/WhatsAppPackageSpoofer;"
    const val NOVA_MANAGER_CLASS = "$INTEGRATIONS_PACKAGE/patches/nova/NovaBenefitsManager;"
    const val ANTI_REVOKE_CLASS = "$INTEGRATIONS_PACKAGE/patches/privacy/AntiRevokeManager;"
    const val GHOST_RECEIPT_CLASS = "$INTEGRATIONS_PACKAGE/patches/privacy/GhostReceiptManager;"
    const val DOWNLOADER_CLASS = "$INTEGRATIONS_PACKAGE/utils/WhatsAppMediaDownloader;"
}

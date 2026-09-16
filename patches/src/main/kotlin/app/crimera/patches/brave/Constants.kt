/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.brave

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val BRAVE_COMPATIBILITY =
        Compatibility(
            name = "Brave Browser",
            packageName = "com.brave.browser",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0xFF4400,
            targets =
                listOf(
                    AppTarget(
                        version = "1.95.101",
                        versionCode = 429510104,
                    ),
                ),
        )
}

/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val DISCORD_COMPATIBILITY = Compatibility(
        name = "Discord",
        packageName = "com.discord",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x5865F2,
        targets = listOf(
            AppTarget(version = "346.0 - Alpha", versionCode = 346200),
        ),
    )
}

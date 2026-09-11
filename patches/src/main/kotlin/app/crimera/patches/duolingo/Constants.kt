package app.crimera.patches.duolingo

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val DUOLINGO_COMPATIBILITY = Compatibility(
        name = "Duolingo",
        packageName = "com.duolingo",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x58CC02,
        targets = listOf(
            AppTarget(version = "6.95.4", versionCode = 2470),
            AppTarget(version = "6.90.3", versionCode = 2422),
            AppTarget(version = "6.88.3", versionCode = 2390),
        ),
    )
}

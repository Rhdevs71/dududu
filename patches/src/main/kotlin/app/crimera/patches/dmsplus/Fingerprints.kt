package app.crimera.patches.dmsplus

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// Fingerprint for Root Detection checker class (contains su binary check path)
object RootCheckFingerprint : Fingerprint(
    returnType = "Ljava/util/Map;",
    strings = listOf("/data/local/su", "/system/bin/su", "reasons"),
)

// Fingerprint for ScreenProtectorPlugin (contains preventScreenshotOn and FLAG_SECURE)
object ScreenProtectorFingerprint : Fingerprint(
    strings = listOf("preventScreenshotOn", "protectDataLeakageOn"),
)

// Fingerprint for UrlLauncher launchUrl method
object UrlLauncherFingerprint : Fingerprint(
    name = "launchUrl",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;", "Z"),
)

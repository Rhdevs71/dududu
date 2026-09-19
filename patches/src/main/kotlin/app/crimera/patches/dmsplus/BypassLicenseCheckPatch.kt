package app.crimera.patches.dmsplus

import app.crimera.patches.dmsplus.Constants.DMSPLUS_COMPATIBILITY
import app.crimera.patches.dmsplus.Constants.PAIRIP_LICENSE_CLIENT
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val bypassLicenseCheckPatch =
    bytecodePatch(
        name = "Bypass Play Protect & Pairip License Check",
        description = "Meniadakan verifikasi Google Play Protect / Pairip licensing untuk mencegah penutupan paksa aplikasi atau dialog error lisensi.",
        default = true,
    ) {
        compatibleWith(DMSPLUS_COMPATIBILITY)

        execute {
            try {
                val licenseClass = mutableClassDefBy(PAIRIP_LICENSE_CLIENT)

                // 1. Hook static checkLicense(Context)V agar return seketika
                licenseClass.methods
                    .firstOrNull { it.name == "checkLicense" && it.parameterTypes.size == 1 }
                    ?.apply {
                        addInstructions(0, "return-void")
                    }

                // 2. Hook virtual initializeLicenseCheck()V agar return seketika
                licenseClass.methods
                    .firstOrNull { it.name == "initializeLicenseCheck" && it.parameterTypes.isEmpty() }
                    ?.apply {
                        addInstructions(0, "return-void")
                    }

                // 3. Hook performLocalInstallerCheck()Z agar selalu return true (1)
                licenseClass.methods
                    .firstOrNull { it.name == "performLocalInstallerCheck" && it.returnType == "Z" }
                    ?.apply {
                        addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent(),
                        )
                    }

                // 4. Hook scheduleAppShutdown()V agar tidak pernah mematikan proses aplikasi
                licenseClass.methods
                    .firstOrNull { it.name == "scheduleAppShutdown" }
                    ?.apply {
                        addInstructions(0, "return-void")
                    }

                // 5. Hook startErrorDialogActivity()V agar tidak memunculkan dialog pemblokiran
                licenseClass.methods
                    .firstOrNull { it.name == "startErrorDialogActivity" }
                    ?.apply {
                        addInstructions(0, "return-void")
                    }
            } catch (e: Exception) {
                println("[BypassLicenseCheckPatch] Note: Hook LicenseClient dilewati: ${e.message}")
            }
        }
    }

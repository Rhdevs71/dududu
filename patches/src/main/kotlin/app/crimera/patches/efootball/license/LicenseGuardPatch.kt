/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.efootball.license

import app.crimera.patches.efootball.utils.Constants.COMPATIBILITY_EFOOTBALL
import app.crimera.patches.efootball.utils.Constants.DOWNLOADER_ACTIVITY_CLASS
import app.crimera.patches.efootball.utils.Constants.GOOGLE_PLAY_LICENSING_CALLBACK
import app.crimera.patches.efootball.utils.Constants.LICENSE_VALIDATOR_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val licenseGuardPatch =
    bytecodePatch(
        name = "Google Play License Guard",
        description = "Guards and maintains Google Play License verification and OBB delivery for modified eFootball installs.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_EFOOTBALL)

        execute {
            // 1. Hook GooglePlayLicensing$MyLicenseCheckerCallback (dontAllow & applicationError -> redirect to allow(256))
            try {
                val callbackClass = mutableClassDefBy(GOOGLE_PLAY_LICENSING_CALLBACK)

                callbackClass.methods.firstOrNull { it.name == "dontAllow" && it.parameterTypes == listOf("I") }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/16 v0, 0x100
                        invoke-virtual {p0, v0}, $GOOGLE_PLAY_LICENSING_CALLBACK->allow(I)V
                        return-void
                        """.trimIndent(),
                    )
                }

                callbackClass.methods.firstOrNull { it.name == "applicationError" && it.parameterTypes == listOf("I") }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/16 v0, 0x100
                        invoke-virtual {p0, v0}, $GOOGLE_PLAY_LICENSING_CALLBACK->allow(I)V
                        return-void
                        """.trimIndent(),
                    )
                }
            } catch (e: Exception) {
                println("[LicenseGuardPatch] Warning: Failed to hook GooglePlayLicensing callback: ${e.message}")
            }

            // 2. Hook LicenseValidator.handleResponse to always dispatch allow(256)
            try {
                val validatorClass = mutableClassDefBy(LICENSE_VALIDATOR_CLASS)
                validatorClass.methods.firstOrNull { it.name == "handleResponse" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        iget-object v0, p0, $LICENSE_VALIDATOR_CLASS->mCallback:Lcom/google/android/vending/licensing/LicenseCheckerCallback;
                        if-eqz v0, :cond_skip_val
                        const/16 v1, 0x100
                        invoke-interface {v0, v1}, Lcom/google/android/vending/licensing/LicenseCheckerCallback;->allow(I)V
                        return-void
                        :cond_skip_val
                        """.trimIndent(),
                    )
                }
            } catch (e: Exception) {
                println("[LicenseGuardPatch] Warning: LicenseValidator class hook note: ${e.message}")
            }

            // 3. Hook DownloaderActivity to confirm expansion delivery
            try {
                val downloaderClass = mutableClassDefBy(DOWNLOADER_ACTIVITY_CLASS)

                downloaderClass.methods.firstOrNull { it.name == "expansionFilesDelivered" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent(),
                    )
                }

                downloaderClass.methods.firstOrNull { it.name == "expansionFilesUptoData" }?.let { method ->
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent(),
                    )
                }
            } catch (e: Exception) {
                println("[LicenseGuardPatch] Warning: DownloaderActivity hook note: ${e.message}")
            }
        }
    }

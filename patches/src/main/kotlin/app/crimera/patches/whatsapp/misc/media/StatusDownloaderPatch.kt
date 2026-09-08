/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.media

import app.crimera.patches.whatsapp.misc.extension.whatsAppExtensionPatch
import app.crimera.patches.whatsapp.utils.Constants.COMPATIBILITY_WHATSAPP
import app.crimera.patches.whatsapp.utils.Constants.DOWNLOADER_CLASS
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val statusDownloaderPatch =
    bytecodePatch(
        name = "Status & Media Downloader",
        description = "Adds the capability to download status photos/videos and media directly to storage (/sdcard/Download/Piko/WhatsApp/).",
    ) {
        dependsOn(whatsAppExtensionPatch)
        compatibleWith(COMPATIBILITY_WHATSAPP)

        execute {
            // Extension integration point for Status Download
        }
    }

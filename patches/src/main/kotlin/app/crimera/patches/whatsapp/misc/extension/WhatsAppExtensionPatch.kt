/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.whatsapp.misc.extension

import app.crimera.patches.whatsapp.misc.extension.hooks.whatsAppInitHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val whatsAppExtensionPatch =
    sharedExtensionPatch(
        listOf("shared", "whatsapp"),
        whatsAppInitHook,
    )

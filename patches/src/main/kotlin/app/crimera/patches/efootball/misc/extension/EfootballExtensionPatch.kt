/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.efootball.misc.extension

import app.crimera.patches.efootball.misc.extension.hooks.efootballInitHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val efootballExtensionPatch =
    sharedExtensionPatch(
        listOf("shared"),
        efootballInitHook,
    )

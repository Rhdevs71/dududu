/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.facebook.misc.extension

import app.crimera.patches.facebook.misc.extension.hooks.facebookInitHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val facebookExtensionPatch =
    sharedExtensionPatch(
        listOf("shared", "facebook"),
        facebookInitHook,
    )

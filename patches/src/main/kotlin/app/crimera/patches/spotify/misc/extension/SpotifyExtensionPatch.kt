/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.extension

import app.crimera.patches.spotify.misc.extension.hooks.spotifyInitHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val spotifyExtensionPatch =
    sharedExtensionPatch(
        listOf("shared", "spotify"),
        spotifyInitHook,
    )

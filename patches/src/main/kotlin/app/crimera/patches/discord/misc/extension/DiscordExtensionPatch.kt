/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.misc.extension

import app.crimera.patches.discord.misc.extension.hooks.discordInitHook
import app.morphe.patches.all.misc.extension.sharedExtensionPatch

val discordExtensionPatch =
    sharedExtensionPatch(
        listOf("shared", "discord"),
        discordInitHook,
    )

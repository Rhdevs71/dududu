/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.brave

import app.crimera.patches.brave.Constants.BRAVE_COMPATIBILITY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string

object BraveBackgroundPlaybackFingerprint :
    Fingerprint(
        returnType = "Z",
        filters =
            listOf(
                string("BraveBackgroundVideoPlayback"),
                string("brave.background_video_playback"),
            ),
    )

@Suppress("unused")
val braveBackgroundPlayPatch =
    bytecodePatch(
        name = "Default Background Video Playback",
        description = "Enables background video playback by default so media continues playing when switching apps or locking the screen.",
        default = true,
    ) {
        compatibleWith(BRAVE_COMPATIBILITY)

        execute {
            BraveBackgroundPlaybackFingerprint.method.apply {
                addInstructions(
                    0,
                    """
                    const/4 v0, 0x1
                    return v0
                    """.trimIndent(),
                )
            }
        }
    }

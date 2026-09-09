/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.settings

import app.crimera.patches.spotify.misc.extension.spotifyExtensionPatch
import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val SPOTIFY_MAIN_ACTIVITY_CLASS = "Lcom/spotify/music/SpotifyMainActivity;"
private const val SETTINGS_INJECTOR_CLASS = "Lapp/morphe/extension/spotify/settings/SpotifySettingsInjector;"

internal object SpotifyMainActivityFingerprint : Fingerprint(
    definingClass = SPOTIFY_MAIN_ACTIVITY_CLASS,
    name = "onResume",
)

@Suppress("unused")
val spotifySettingsPatch =
    bytecodePatch(
        name = "Spotify RHpatch Settings Menu",
        description = "Injects an interactive floating RHpatch settings button on Spotify main screen to customize adblock, playback, lyrics, and quality preferences.",
    ) {
        dependsOn(spotifyExtensionPatch)
        compatibleWith(COMPATIBILITY_SPOTIFY)

        execute {
            val method = SpotifyMainActivityFingerprint.method
            method.addInstructions(
                0,
                """
                invoke-static {p0}, $SETTINGS_INJECTOR_CLASS->onActivityResume(Landroid/app/Activity;)V
                """.trimIndent(),
            )
        }
    }

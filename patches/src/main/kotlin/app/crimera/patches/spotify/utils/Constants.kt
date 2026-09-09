/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.utils

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi.ARM64_V8A

object Constants {
    val COMPATIBILITY_SPOTIFY =
        Compatibility(
            name = "Spotify",
            packageName = "com.spotify.music",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0x1DB954,
            targets =
                listOf(
                    AppTarget(
                        version = "9.1.82.1596",
                        versionCodes =
                            mapOf(
                                ARM64_V8A to 146025380,
                            ),
                    ),
                ),
        )

    // Spotify native classes
    const val SPOTIFY_APP_CLASS = "Lcom/spotify/music/SpotifyApplication;"
    const val AUTOVALUE_PLAYER_STATE_CLASS = "Lcom/spotify/player/model/AutoValue_PlayerState;"
    const val RESTRICTIONS_CLASS = "Lcom/spotify/player/model/Restrictions;"
    const val AUTOVALUE_RESTRICTIONS_CLASS = "Lcom/spotify/player/model/AutoValue_Restrictions;"
    const val SUB_IN_STREAM_CLASS = "Lcom/spotify/ads/esperanto/proto/SubInStreamResponse;"
    const val SUB_SLOT_CLASS = "Lcom/spotify/ads/esperanto/proto/SubSlotResponse;"
    const val GET_SLOT_CLASS = "Lcom/spotify/ads/esperanto/proto/GetSlotResponse;"
    const val TRIGGER_SLOT_CLASS = "Lcom/spotify/ads/esperanto/proto/TriggerSlotResponse;"
    const val GET_ADS_CLASS = "Lcom/spotify/ads/esperanto/proto/GetAdsResponse;"
    const val CREATE_SLOT_CLASS = "Lcom/spotify/ads/esperanto/proto/CreateSlotResponse;"
    const val ES_CONTEXT_PLAYER_STATE_CLASS = "Lcom/spotify/player/esperanto/proto/EsContextPlayerState\$ContextPlayerState;"
    const val ES_AD_BREAK_CONTEXT_CLASS = "Lcom/spotify/player/esperanto/proto/EsAdBreakContext\$AdBreakContext;"
    const val HSP0_CLASS = "Lp/hsp0;"
    const val AD_CLASS = "Lcom/spotify/ads/esperanto/proto/Ad;"
    const val PRODUCT_STATE_MAP_CLASS = "Lcom/spotify/connectivity/auth/esperanto/proto/EsSession\$ProductStateMap;"
    const val SHOULD_UPSELL_REQUEST_CLASS = "Lcom/spotify/upsells/v1/proto/ShouldUpsellRequest;"
    const val BATCH_SHOULD_UPSELL_REQUEST_CLASS = "Lcom/spotify/upsells/v1/proto/BatchShouldUpsellRequest;"
    const val GET_UPSELL_REQUEST_CLASS = "Lcom/spotify/upsells/v1/proto/GetUpsellRequest;"
    const val MAIN_ACTIVITY_CLASS = "Lcom/spotify/music/MainActivity;"

    // Extension classes
    const val INTEGRATIONS_PACKAGE = "Lapp/morphe/extension/spotify"
    const val SETTINGS_CLASS = "$INTEGRATIONS_PACKAGE/settings/SpotifySettings;"
    const val PREF_CLASS = "$INTEGRATIONS_PACKAGE/settings/SpotifyPref;"
    const val AD_MANAGER_CLASS = "$INTEGRATIONS_PACKAGE/patches/adblock/SpotifyAdManager;"
    const val PLAYBACK_MANAGER_CLASS = "$INTEGRATIONS_PACKAGE/patches/playback/SpotifyPlaybackManager;"
    const val PRODUCT_STATE_SPOOFER_CLASS = "$INTEGRATIONS_PACKAGE/patches/productstate/SpotifyProductStateSpoofer;"
    const val ANTI_UPSELL_CLASS = "$INTEGRATIONS_PACKAGE/patches/upsell/SpotifyAntiUpsell;"
    const val SETTINGS_DIALOG_CLASS = "$INTEGRATIONS_PACKAGE/settings/SpotifySettingsDialog;"
    const val SPOOFER_CLASS = "$INTEGRATIONS_PACKAGE/patches/clone/SpotifyPackageSpoofer;"
}

/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */


package app.morphe.extension.instagram.patches;

import static app.morphe.extension.instagram.utils.IgStr.str;

import java.util.List;
import java.util.Arrays;
import java.io.File;

import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.instagram.utils.InstaUtils;
import app.morphe.extension.instagram.constants.Constants;
import app.morphe.extension.instagram.settings.SettingsStatus;
import app.morphe.extension.shared.Utils;

public class Block {
    private static List<String> SUGGESTED_CONTENT_KEY = Arrays.asList(
        "clips_netego",
        "stories_netego",
        "in_feed_survey",
        "bloks_netego",
        "suggested_igd_channels",
        "suggested_channels",
        "channels_netego",
        "channel_netego",
        "suggested_broadcast_channels",
        "suggested_top_accounts",
        "suggested_users"
    );

    // Returns an invalid string, such that json parsing fails for the key.
    public static String replaceJsonParserKey(String key) {
        boolean condition = false;

        boolean disableSuggested = Pref.hideSuggestedContent() && SettingsStatus.hideSuggestedContent;
        if (disableSuggested && key != null) {
            if (SUGGESTED_CONTENT_KEY.contains(key)) condition = true;
        }

        return condition ? Constants.PIKO : key;
    }

    public static void deleteAnalyticsCacheFolder() {
        File analyticDirectory = new File(Utils.getContext().getDataDir(), "app_analytics");
        Boolean done = InstaUtils.deleteRecursive(analyticDirectory);
        if (done) {
            app.morphe.extension.crimera.PikoUtils.toast(str("piko_deleted"));
        }
    }
}
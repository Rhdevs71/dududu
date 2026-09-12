/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.discord.appicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.discord.utils.DiscordLog;

public class PikoAppIconManager {
    public static final String[] ALL_ALIASES = {
        "com.discord.main.MainDefault",
        "com.discord.main.MainBrandInverted",
        "com.discord.main.MainMatteDark",
        "com.discord.main.MainMatteLight",
        "com.discord.main.MainBrandDark",
        "com.discord.main.MainPastel",
        "com.discord.main.MainPirate",
        "com.discord.main.MainCamo",
        "com.discord.main.MainSunset",
        "com.discord.main.MainGalaxy",
        "com.discord.main.MainY2K",
        "com.discord.main.MainCherryBlossom",
        "com.discord.main.MainBeanie",
        "com.discord.main.MainGaming",
        "com.discord.main.MainCircuit",
        "com.discord.main.MainHoloWaves",
        "com.discord.main.MainBlush",
        "com.discord.main.MainAngry",
        "com.discord.main.MainManga",
        "com.discord.main.MainController",
        "com.discord.main.MainMushroom",
        "com.discord.main.MainZombie",
        "com.discord.main.MainClydeStein",
        "com.discord.main.MainSlimy",
        "com.discord.main.MainDrip",
        "com.discord.main.MainBlurpleTwilight",
        "com.discord.main.MainInRainbows",
        "com.discord.main.MainMidnightPrism",
        "com.discord.main.MainColorWave",
        "com.discord.main.MainTreat",
        "com.discord.main.MainTrick"
    };

    public static void applyAppIcon(Context context, String targetIdOrAlias) {
        try {
            if (context == null || targetIdOrAlias == null) return;
            PackageManager pm = context.getPackageManager();
            String pkg = context.getPackageName();

            String cleanTarget = targetIdOrAlias.trim().replace("_", "").toLowerCase();
            String matchedAlias = null;

            for (String alias : ALL_ALIASES) {
                String cleanAlias = alias.substring(alias.lastIndexOf('.') + 1).replace("Main", "").toLowerCase();
                if (cleanAlias.equals(cleanTarget) || alias.equalsIgnoreCase(targetIdOrAlias) || alias.endsWith(targetIdOrAlias)) {
                    matchedAlias = alias;
                    break;
                }
            }

            if (matchedAlias == null) {
                matchedAlias = ALL_ALIASES[0]; // fallback to MainDefault
            }

            for (String alias : ALL_ALIASES) {
                ComponentName comp = new ComponentName(pkg, alias);
                boolean isTarget = alias.equals(matchedAlias);
                int state = isTarget ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                pm.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP);
            }

            String displayName = matchedAlias.substring(matchedAlias.lastIndexOf('.') + 1).replace("Main", "");
            DiscordLog.i("PikoAppIconManager", "Activated Discord launcher icon alias: " + matchedAlias + " (" + displayName + ")");
            PikoUtils.toast("Icon Discord berhasil diubah ke: " + displayName + "!\n(Muat ulang launcher jika belum berubah)");
        } catch (Throwable t) {
            DiscordLog.e("PikoAppIconManager", "Failed to switch app icon to " + targetIdOrAlias, t);
        }
    }
}

/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.discord.loader;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.lang.reflect.Method;
import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.discord.utils.DiscordLog;

public class PikoDiscordLoader {
    private static volatile boolean sInjected = false;

    public static void beforeLoadScriptFromAssets(Object delegate, AssetManager assetManager, String assetURL) {
        if (sInjected) return;
        sInjected = true;
        DiscordLog.i("PikoDiscordLoader", "Intercepted loadScriptFromAssets: " + assetURL);
        runModScript(delegate, assetManager);
    }

    public static void beforeLoadScriptFromFile(Object delegate, String fileName, String sourceURL) {
        if (sInjected) return;
        sInjected = true;
        DiscordLog.i("PikoDiscordLoader", "Intercepted loadScriptFromFile: " + fileName);
        Context ctx = PikoUtils.getContext();
        AssetManager assetManager = (ctx != null) ? ctx.getAssets() : null;
        runModScript(delegate, assetManager);
    }

    private static void runModScript(Object delegate, AssetManager assetManager) {
        try {
            // 1. Check for custom external bundle in app private storage (bypasses Scoped Storage)
            Context ctx = PikoUtils.getContext();
            if (ctx != null) {
                File pikoDir = new File(ctx.getFilesDir(), "piko");
                if (!pikoDir.exists()) pikoDir.mkdirs();
                File candidate = new File(pikoDir, "bundle.js");
                if (candidate.exists() && candidate.length() > 0) {
                    DiscordLog.i("PikoDiscordLoader", "Running custom mod bundle from: " + candidate.getAbsolutePath());
                    Method loadFromFile = delegate.getClass().getDeclaredMethod(
                        "loadScriptFromFile",
                        String.class,
                        String.class,
                        boolean.class
                    );
                    loadFromFile.setAccessible(true);
                    loadFromFile.invoke(delegate, candidate.getAbsolutePath(), candidate.getAbsolutePath(), false);
                    DiscordLog.i("PikoDiscordLoader", "Successfully loaded custom mod bundle!");
                    return;
                }
            }

            // 2. Load out-of-the-box embedded mod bundle from APK assets before main bundle
            if (assetManager != null) {
                DiscordLog.i("PikoDiscordLoader", "Running embedded mod bundle: assets://piko_discord.js");
                Method loadFromAssets = delegate.getClass().getDeclaredMethod(
                    "loadScriptFromAssets",
                    AssetManager.class,
                    String.class,
                    boolean.class
                );
                loadFromAssets.setAccessible(true);
                loadFromAssets.invoke(delegate, assetManager, "assets://piko_discord.js", false);
                DiscordLog.i("PikoDiscordLoader", "Successfully loaded assets://piko_discord.js into Hermes runtime!");
            } else {
                DiscordLog.w("PikoDiscordLoader", "AssetManager is null, cannot load embedded mod bundle");
            }
        } catch (Throwable t) {
            DiscordLog.e("PikoDiscordLoader", "Failed to load mod bundle", t);
        }
    }
}

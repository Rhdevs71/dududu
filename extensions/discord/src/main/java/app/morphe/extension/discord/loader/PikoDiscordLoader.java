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
import app.morphe.extension.discord.utils.DiscordLog;

public class PikoDiscordLoader {
    private static volatile boolean sInjected = false;

    public static void onAssetBundleLoaded(Object reactInstance, AssetManager assetManager, String assetURL) {
        if (sInjected) return;
        if (assetURL == null || !assetURL.contains("index.android.bundle")) return;

        DiscordLog.i("PikoDiscordLoader", "React Native loaded asset bundle: " + assetURL);
        injectMod(reactInstance, assetManager);
    }

    public static void onFileBundleLoaded(Object reactInstance, String fileName, String sourceURL) {
        if (sInjected) return;
        DiscordLog.i("PikoDiscordLoader", "React Native loaded file bundle: " + fileName);
    }

    private static synchronized void injectMod(Object reactInstance, AssetManager assetManager) {
        if (sInjected) return;
        sInjected = true;

        try {
            Class<?> reactInstanceClass = reactInstance.getClass();

            // 1. Check for custom external bundle in app private storage (bypasses Scoped Storage)
            Context ctx = getReactContext(reactInstance);
            File customBundle = null;
            if (ctx != null) {
                File pikoDir = new File(ctx.getFilesDir(), "piko");
                if (!pikoDir.exists()) pikoDir.mkdirs();
                File candidate = new File(pikoDir, "bundle.js");
                if (candidate.exists() && candidate.length() > 0) {
                    customBundle = candidate;
                }
            }

            if (customBundle != null) {
                DiscordLog.i("PikoDiscordLoader", "Loading custom mod bundle from private storage: " + customBundle.getAbsolutePath());
                Method loadFromFile = reactInstanceClass.getDeclaredMethod(
                    "access$loadJSBundleFromFile",
                    reactInstanceClass,
                    String.class,
                    String.class
                );
                loadFromFile.setAccessible(true);
                loadFromFile.invoke(null, reactInstance, customBundle.getAbsolutePath(), "piko_custom_bundle.js");
                DiscordLog.i("PikoDiscordLoader", "Successfully loaded custom bundle!");
                return;
            }

            // 2. Load out-of-the-box embedded mod bundle from APK assets
            DiscordLog.i("PikoDiscordLoader", "Loading out-of-the-box mod bundle: piko_discord.js");
            Method loadFromAssets = reactInstanceClass.getDeclaredMethod(
                "access$loadJSBundleFromAssets",
                reactInstanceClass,
                AssetManager.class,
                String.class
            );
            loadFromAssets.setAccessible(true);
            loadFromAssets.invoke(null, reactInstance, assetManager, "piko_discord.js");

            DiscordLog.i("PikoDiscordLoader", "Successfully loaded piko_discord.js into Hermes runtime!");
        } catch (Throwable t) {
            DiscordLog.e("PikoDiscordLoader", "Failed to load mod bundle", t);
        }
    }

    private static Context getReactContext(Object reactInstance) {
        try {
            Method getContextMethod = reactInstance.getClass().getDeclaredMethod("access$getContext$p", reactInstance.getClass());
            getContextMethod.setAccessible(true);
            return (Context) getContextMethod.invoke(null, reactInstance);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

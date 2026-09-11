/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.appicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.instagram.utils.PikoLog;

public class InstaAppIconManager {
    private static final String TAG = "InstaAppIconManager";

    /**
     * All 14 activity-alias components defined in Instagram's AndroidManifest.xml
     */
    private static final String[] ALL_ICON_ALIASES = new String[] {
        "com.instagram.android.activity.MainTabActivity",
        "com.instagram.android.activity.MainTabActivity.neon",
        "com.instagram.android.activity.MainTabActivity.flame",
        "com.instagram.android.activity.MainTabActivity.floral",
        "com.instagram.android.activity.MainTabActivity.slime",
        "com.instagram.android.activity.MainTabActivity.metal",
        "com.instagram.android.activity.MainTabActivity.kpop",
        "com.instagram.android.activity.MainTabActivity.haruko",
        "com.instagram.android.activity.MainTabActivity.felipe",
        "com.instagram.android.activity.MainTabActivity.humberto",
        "com.instagram.android.activity.MainTabActivity.zipeng",
        "com.instagram.android.activity.MainTabActivity.uzo",
        "com.instagram.android.activity.MainTabActivity.ricky",
        "com.instagram.android.activity.MainTabActivity.throwback"
    };

    private static final String PREF_NAME = "rhpatch_app_icon_pref";
    private static final String KEY_SAVED_ALIAS = "saved_icon_alias";
    private static final String KEY_SAVED_NAME = "saved_icon_name";
    private static final java.util.concurrent.atomic.AtomicBoolean sRestoreStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.ExecutorService sExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    /**
     * Memulihkan icon launcher kustom pengguna yang tersimpan setelah aplikasi diperbarui/di-reinstall.
     * Dijalankan di background thread hanya 1 kali per cold-boot agar tidak membebani PackageManagerService.
     */
    public static void restoreSavedIcon(Context context) {
        if (sRestoreStarted.getAndSet(true)) return;
        final Context appContext = (context != null) ? context.getApplicationContext() : PikoUtils.getContext();
        if (appContext == null) return;

        sExecutor.execute(() -> {
            try {
                String savedAlias = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .getString(KEY_SAVED_ALIAS, null);
                if (savedAlias == null || savedAlias.isEmpty()) return;

                PackageManager pm = appContext.getPackageManager();
                String pkgName = appContext.getPackageName();
                ComponentName targetComponent = new ComponentName(pkgName, savedAlias);
                int state = pm.getComponentEnabledSetting(targetComponent);
                if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    PikoLog.e(TAG, "Restoring custom launcher icon across update: " + savedAlias, null);
                    pm.setComponentEnabledSetting(
                        targetComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    );
                    for (String alias : ALL_ICON_ALIASES) {
                        if (!alias.equals(savedAlias)) {
                            try {
                                ComponentName otherComp = new ComponentName(pkgName, alias);
                                pm.setComponentEnabledSetting(
                                    otherComp,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP
                                );
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            } catch (Throwable t) {
                PikoLog.e(TAG, "Failed to restore custom app icon on startup", t);
            }
        });
    }

    /**
     * Directly and immediately applies the selected app icon.
     *
     * @param context Application or Activity context
     * @param iconEnum Selected enum instance from LX/0ClA;
     */
    public static void applyIcon(Context context, Object iconEnum) {
        if (context == null) {
            context = PikoUtils.getContext();
        }
        if (context == null || iconEnum == null) {
            PikoLog.e(TAG, "Cannot apply icon: context or iconEnum is null (context=" + context + ", iconEnum=" + iconEnum + ")", null);
            return;
        }

        try {
            String targetComponentClass = null;
            String iconDisplayName = iconEnum.toString();

            // 1. Try to extract A02 (activity alias class name) from iconEnum
            try {
                Field fA02 = iconEnum.getClass().getDeclaredField("A02");
                fA02.setAccessible(true);
                Object val = fA02.get(iconEnum);
                if (val instanceof String) {
                    targetComponentClass = (String) val;
                }
            } catch (Throwable t) {
                PikoLog.e(TAG, "Field A02 not found directly on enum, checking all fields", t);
                for (Field f : iconEnum.getClass().getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(iconEnum);
                        if (val instanceof String && ((String) val).contains("MainTabActivity")) {
                            targetComponentClass = (String) val;
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // 2. Try to extract A03 (display name, e.g. CANNES_NEON)
            try {
                Field fA03 = iconEnum.getClass().getDeclaredField("A03");
                fA03.setAccessible(true);
                Object val = fA03.get(iconEnum);
                if (val instanceof String) {
                    iconDisplayName = (String) val;
                }
            } catch (Throwable ignored) {}

            // 3. Fallback matching if reflection could not find alias string
            if (targetComponentClass == null) {
                String enumStr = iconEnum.toString().toLowerCase();
                for (String alias : ALL_ICON_ALIASES) {
                    String suffix = alias.substring(alias.lastIndexOf('.') + 1).toLowerCase();
                    if (enumStr.contains(suffix)) {
                        targetComponentClass = alias;
                        break;
                    }
                }
            }

            if (targetComponentClass == null) {
                targetComponentClass = "com.instagram.android.activity.MainTabActivity";
            }

            PackageManager pm = context.getPackageManager();
            String pkgName = context.getPackageName();

            PikoLog.e(TAG, "Switching app icon to: " + iconDisplayName + " [" + targetComponentClass + "] in package: " + pkgName, null);

            // Step 1: Enable the target launcher icon first
            ComponentName targetComponent = new ComponentName(pkgName, targetComponentClass);
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );

            // Step 2: Disable all other known icon aliases
            for (String alias : ALL_ICON_ALIASES) {
                if (!alias.equals(targetComponentClass)) {
                    try {
                        ComponentName otherComp = new ComponentName(pkgName, alias);
                        pm.setComponentEnabledSetting(
                            otherComp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        );
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }

            // Step 3: Also disable any other aliases from enum.values() if available
            try {
                Method valuesMethod = iconEnum.getClass().getMethod("values");
                Object[] allEnums = (Object[]) valuesMethod.invoke(null);
                if (allEnums != null) {
                    for (Object other : allEnums) {
                        if (other != null && !other.equals(iconEnum)) {
                            for (Field f : other.getClass().getDeclaredFields()) {
                                try {
                                    f.setAccessible(true);
                                    Object val = f.get(other);
                                    if (val instanceof String && ((String) val).contains("MainTabActivity")) {
                                        String otherCls = (String) val;
                                        if (!otherCls.equals(targetComponentClass)) {
                                            ComponentName comp = new ComponentName(pkgName, otherCls);
                                            pm.setComponentEnabledSetting(
                                                comp,
                                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                                PackageManager.DONT_KILL_APP
                                            );
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Step 4: Persist the selected icon alias so it survives APK updates
            try {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SAVED_ALIAS, targetComponentClass)
                    .putString(KEY_SAVED_NAME, iconDisplayName)
                    .apply();
            } catch (Throwable ignored) {}

            // Step 5: Show immediate confirmation toast to the user
            PikoUtils.toast("Icon aplikasi berhasil diubah ke: " + iconDisplayName + "!\n(Jika belum berubah di beranda, muat ulang launcher Anda)");

        } catch (Throwable t) {
            PikoLog.e(TAG, "Failed to apply custom app icon", t);
            PikoUtils.toast("Gagal mengubah icon aplikasi: " + t.getMessage());
        }
    }
}

/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.sharedPreference.SharedPref;
import app.morphe.extension.facebook.settings.FacebookSettings;
import app.morphe.extension.facebook.ui.RhpatchFacebookInjector;

public class FacebookReelsMenuHook {

    private static final String TAG = "FacebookReelsMenuHook";
    public static final int ITEM_ID_DOWNLOAD_REEL = 999901;
    public static final int ITEM_ID_COPY_REEL_LINK = 999902;

    /**
     * Diinjeksi di LX/TXv;->A01()Ljava/util/List; saat daftar item Bottom Sheet Reels dibuat.
     * Menginjeksi item "🎬 Unduh Reel (HD MP4)" langsung ke menu titik 3 Facebook Reels.
     */
    public static List onReelsItemsCreated(Object txvInstance, List items) {
        if (items == null) return items;
        try {
            // Cek apakah fitur downloader Reels diaktifkan di pengaturan
            if (!SharedPref.getBoolean(FacebookSettings.DOWNLOAD_REELS, true)) {
                return items;
            }

            // Cek apakah item sudah pernah disisipkan untuk mencegah duplikasi
            for (Object itm : items) {
                if (itm != null) {
                    try {
                        Field fA02 = itm.getClass().getDeclaredField("A02");
                        fA02.setAccessible(true);
                        Object title = fA02.get(itm);
                        if (title != null && title.toString().contains("Unduh Reel")) {
                            return items;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // Ambil data media dari objek txvInstance (LX/S1X memiliki field A05 bertipe LX/Rdi)
            Object rdi = null;
            if (txvInstance != null) {
                try {
                    Field fA05 = txvInstance.getClass().getDeclaredField("A05");
                    fA05.setAccessible(true);
                    rdi = fA05.get(txvInstance);
                } catch (Throwable ignored) {
                    try {
                        for (Field f : txvInstance.getClass().getDeclaredFields()) {
                            if (f.getType().getName().contains("Rdi")) {
                                f.setAccessible(true);
                                rdi = f.get(txvInstance);
                                break;
                            }
                        }
                    } catch (Throwable ignored2) {}
                }
            }

            // Ekstrak PostMediaInfo dari rdi atau fallback ke media terakhir yang aktif
            FacebookPostMenuHook.PostMediaInfo mediaInfo = null;
            if (rdi != null) {
                mediaInfo = FacebookPostMenuHook.extractMediaInfo(rdi);
            }
            if (mediaInfo == null || (mediaInfo.hdVideoUrl == null && mediaInfo.sdVideoUrl == null)) {
                if (FacebookMediaDownloader.sLastActiveMedia != null) {
                    mediaInfo = FacebookMediaDownloader.sLastActiveMedia;
                }
            }
            if (mediaInfo != null) {
                FacebookMediaDownloader.sLastActiveMedia = mediaInfo;
            }

            final FacebookPostMenuHook.PostMediaInfo targetMedia = mediaInfo;

            // Dapatkan contoh icon native Facebook dari item pertama di list
            Object sampleIcon = null;
            if (!items.isEmpty()) {
                Object firstItem = items.get(0);
                if (firstItem != null) {
                    try {
                        Field fA00 = firstItem.getClass().getDeclaredField("A00");
                        fA00.setAccessible(true);
                        sampleIcon = fA00.get(firstItem);
                    } catch (Throwable ignored) {}
                }
            }

            // Dapatkan class-class model Reels Action Sheet Facebook
            ClassLoader cl = txvInstance != null ? txvInstance.getClass().getClassLoader() : items.getClass().getClassLoader();
            Class<?> uaqClass = Class.forName("LX.Uaq", true, cl);
            Class<?> s0aClass = Class.forName("LX.S0a", true, cl);
            Class<?> tfeClass = Class.forName("LX.Tfe", true, cl);

            // Buat listener callback dengan Dynamic Proxy LX/Uaq
            Object uaqProxy = Proxy.newProxyInstance(
                cl,
                new Class<?>[]{ uaqClass },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("DIJ".equals(method.getName())) {
                            Context context = RhpatchFacebookInjector.getCurrentActivity();
                            FacebookPostMenuHook.PostMediaInfo current = targetMedia != null ? targetMedia : FacebookMediaDownloader.sLastActiveMedia;
                            String videoUrl = (current != null && current.hdVideoUrl != null) ? current.hdVideoUrl : (current != null ? current.sdVideoUrl : null);

                            if (videoUrl != null && !videoUrl.isEmpty()) {
                                FacebookMediaDownloader.downloadDirectStream(context, videoUrl, "Facebook_Reel", true);
                            } else {
                                if (context != null) {
                                    Toast.makeText(context, "⏳ Mengambil URL Reels HD...", Toast.LENGTH_SHORT).show();
                                }
                                if (current != null && current.permalink != null) {
                                    FacebookMediaDownloader.startDownload(context, current.permalink);
                                }
                            }
                        }
                        return null;
                    }
                }
            );

            // Buat instance baris menu LX/S0a menggunakan static factory method A00
            Object newItem = null;
            try {
                Method factoryA00 = s0aClass.getMethod("A00", uaqClass, tfeClass, CharSequence.class, CharSequence.class);
                newItem = factoryA00.invoke(null, uaqProxy, sampleIcon, "🎬 [RHpatch] Unduh Reel (HD MP4)", "Unduh video Reels resolusi tinggi ke galeri");
            } catch (Throwable t) {
                for (Constructor<?> ctor : s0aClass.getConstructors()) {
                    if (ctor.getParameterTypes().length == 7) {
                        newItem = ctor.newInstance(uaqProxy, sampleIcon, null, "🎬 [RHpatch] Unduh Reel (HD MP4)", "Unduh video Reels resolusi tinggi ke galeri", null, null);
                        break;
                    }
                }
            }

            if (newItem != null) {
                List mutableList = items;
                try {
                    mutableList.add(0, newItem);
                } catch (UnsupportedOperationException e) {
                    mutableList = new ArrayList(items);
                    mutableList.add(0, newItem);
                }
                return mutableList;
            }

        } catch (Throwable t) {
            PikoUtils.logger(TAG, "onReelsItemsCreated failed", t);
        }
        return items;
    }

    /**
     * Fallback lama: Diinjeksi di LX/S2R;->A0i(Landroid/view/Menu;Landroid/view/View;LX/2QD;...)V
     * jika ada menu legacy yang masih memakai Menu Android.
     */
    public static void onReelsMenuCreated(Menu menu, View view, Object feedUnit) {
        if (menu == null) return;
        try {
            if (menu.findItem(ITEM_ID_DOWNLOAD_REEL) != null) return;

            final Context context = view != null ? view.getContext() : null;
            final FacebookPostMenuHook.PostMediaInfo info = FacebookPostMenuHook.extractMediaInfo(feedUnit);

            if (info != null) {
                FacebookMediaDownloader.sLastActiveMedia = info;
            }

            // 1. Menu Item Unduh Reel HD
            MenuItem downloadItem = menu.add(0, ITEM_ID_DOWNLOAD_REEL, 0, "🎬 [RHpatch] Unduh Reel (HD MP4)");
            downloadItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        Context ctx = context != null ? context : RhpatchFacebookInjector.getCurrentActivity();
                        FacebookPostMenuHook.PostMediaInfo currentInfo = info != null ? info : FacebookMediaDownloader.sLastActiveMedia;
                        String videoUrl = (currentInfo != null && currentInfo.hdVideoUrl != null) ? currentInfo.hdVideoUrl : (currentInfo != null ? currentInfo.sdVideoUrl : null);

                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            FacebookMediaDownloader.downloadDirectStream(ctx, videoUrl, "Facebook_Reel", true);
                        } else {
                            if (ctx != null) {
                                Toast.makeText(ctx, "⏳ Mencari URL video Reel, silakan coba lagi...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Throwable t) {
                        PikoUtils.logger(TAG, "Error downloading reel", t);
                    }
                    return true;
                }
            });

            // 2. Menu Item Salin Tautan Reel
            MenuItem copyLinkItem = menu.add(0, ITEM_ID_COPY_REEL_LINK, 1, "🔗 [RHpatch] Salin Tautan Reel");
            copyLinkItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        Context ctx = context != null ? context : RhpatchFacebookInjector.getCurrentActivity();
                        FacebookPostMenuHook.PostMediaInfo currentInfo = info != null ? info : FacebookMediaDownloader.sLastActiveMedia;
                        String link = (currentInfo != null && currentInfo.permalink != null) ? currentInfo.permalink : null;

                        if (link != null && ctx != null) {
                            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cm != null) {
                                cm.setPrimaryClip(ClipData.newPlainText("Reel Link", link));
                                Toast.makeText(ctx, "🔗 Tautan Reel berhasil disalin!", Toast.LENGTH_SHORT).show();
                            }
                        } else if (ctx != null) {
                            Toast.makeText(ctx, "⚠️ Tautan Reel tidak ditemukan.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Throwable t) {
                        PikoUtils.logger(TAG, "Error copying reel link", t);
                    }
                    return true;
                }
            });

        } catch (Throwable t) {
            PikoUtils.logger(TAG, "onReelsMenuCreated failed", t);
        }
    }
}

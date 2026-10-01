/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import app.morphe.extension.crimera.PikoUtils;

public class FacebookReelsMenuHook {

    private static final String TAG = "FacebookReelsMenuHook";
    public static final int ITEM_ID_DOWNLOAD_REEL = 999901;
    public static final int ITEM_ID_COPY_REEL_LINK = 999902;

    /**
     * Diinjeksi di LX/S2R;->A0i(Landroid/view/Menu;Landroid/view/View;LX/2QD;...)V
     * saat menu titik 3 pada Reels / pemutar video dibuat.
     */
    public static void onReelsMenuCreated(Menu menu, View view, Object feedUnit) {
        if (menu == null) return;
        try {
            // Cegah duplikasi item jika menu di-inflate ulang
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
                        Context ctx = context;
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
                        Context ctx = context;
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

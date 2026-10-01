/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import java.lang.reflect.Method;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.facebook.settings.FacebookPref;

public class FacebookAdFilter {

    private static final String TAG = "FacebookAdFilter";
    private static Method sB77Method = null;
    private static boolean sMethodLookupAttempted = false;

    /**
     * Diinjeksi di awal GraphQLFeedUnitEdge->A03()LX/2NV; dan BPb()LX/2NV;
     * Jika mengembalikan true, bytecode akan langsung return-object null,
     * sehingga postingan iklan/promosi dibuang total dari Feed (bukan hanya label teksnya).
     */
    public static boolean shouldDropEdge(Object edge) {
        if (edge == null) return false;
        try {
            if (!sMethodLookupAttempted) {
                try {
                    sB77Method = edge.getClass().getMethod("B77");
                    sB77Method.setAccessible(true);
                } catch (Throwable t) {
                    PikoUtils.logger(TAG, "Failed to resolve B77 on edge", t);
                }
                sMethodLookupAttempted = true;
            }

            if (sB77Method != null) {
                Object categoryEnum = sB77Method.invoke(edge);
                if (categoryEnum != null) {
                    String catName = categoryEnum.toString();

                    // 1. Blokir Iklan Bersponsor & Promosi Berbayar
                    if (FacebookPref.isBlockSponsoredAds()) {
                        if ("SPONSORED".equals(catName) || 
                            "PROMOTION".equals(catName) || 
                            "HIGH_VALUE_PROMOTION".equals(catName)) {
                            return true;
                        }
                    }

                    // 2. Sembunyikan Reels di Beranda jika opsi aktif
                    if (FacebookPref.isHideReelsInFeed() && "FB_SHORTS".equals(catName)) {
                        return true;
                    }

                    // 3. Sembunyikan Saran Teman (PYMK) jika opsi aktif
                    if (FacebookPref.isHidePymk() && "FRIENDLY_FEED_MID_CARD".equals(catName)) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            // Abaikan agar feed normal tidak terganggu
        }
        return false;
    }
}

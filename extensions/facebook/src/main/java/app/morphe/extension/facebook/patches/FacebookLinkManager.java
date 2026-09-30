/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.facebook.settings.FacebookPref;

public class FacebookLinkManager {
    private static final String TAG = "FacebookLinkManager";

    public static String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();

            // Link Shim bypass: https://l.facebook.com/l.php?u=<real_url>&h=...
            if (host != null && (host.contains("l.facebook.com") || host.contains("lm.facebook.com"))) {
                String target = uri.getQueryParameter("u");
                if (target != null && !target.isEmpty()) {
                    return stripTrackingParams(Uri.parse(target));
                }
            }

            if (url.contains("fbclid=")) {
                return stripTrackingParams(uri);
            }
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Error sanitizing url: " + url, t);
        }

        return url;
    }

    public static String stripTrackingParams(Uri uri) {
        try {
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : uri.getQueryParameterNames()) {
                if ("fbclid".equalsIgnoreCase(param) ||
                    "__cft__".equalsIgnoreCase(param) ||
                    "__tn__".equalsIgnoreCase(param) ||
                    "mibextid".equalsIgnoreCase(param) ||
                    "h".equalsIgnoreCase(param) && uri.getHost() != null && uri.getHost().contains("facebook.com")) {
                    continue; // Skip tracking parameters
                }
                for (String val : uri.getQueryParameters(param)) {
                    builder.appendQueryParameter(param, val);
                }
            }
            return builder.build().toString();
        } catch (Throwable t) {
            return uri.toString();
        }
    }

    public static boolean handleOpenUrl(Context context, String url) {
        if (url == null || url.isEmpty() || context == null) {
            return false;
        }

        try {
            String cleanUrl = url;
            if (FacebookPref.isBypassLinkShim()) {
                cleanUrl = sanitizeUrl(url);
            }

            if (FacebookPref.isOpenExternalBrowser()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "Error handling open url: " + url, t);
        }

        return false;
    }

    public static boolean checkAndInterceptBrowser(android.app.Activity activity) {
        if (activity == null || !FacebookPref.isOpenExternalBrowser()) {
            return false;
        }

        try {
            android.content.Intent intent = activity.getIntent();
            if (intent == null) return false;

            Uri data = intent.getData();
            String url = (data != null) ? data.toString() : null;
            if (url == null && intent.hasExtra("EXTRA_URL")) {
                url = intent.getStringExtra("EXTRA_URL");
            }

            if (url != null && !url.isEmpty()) {
                boolean handled = handleOpenUrl(activity, url);
                if (handled) {
                    activity.finish();
                    return true;
                }
            }
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "checkAndInterceptBrowser error", t);
        }

        return false;
    }
}

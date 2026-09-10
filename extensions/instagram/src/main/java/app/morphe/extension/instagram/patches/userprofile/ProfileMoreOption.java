/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.userprofile;

import static app.morphe.extension.instagram.utils.IgStr.str;

import android.content.Context;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.instagram.utils.PikoLog;
import app.morphe.extension.instagram.entity.UserData;
import app.morphe.extension.instagram.entity.ProfileInfo;
import app.morphe.extension.instagram.constants.UI;
import app.morphe.extension.crimera.ObjectBrowser;
import app.morphe.extension.instagram.settings.ActivityHook;
import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.instagram.patches.download.DownloadUtils;
import app.morphe.extension.instagram.entity.InstagramDialogBox;
import app.morphe.extension.instagram.entity.InstagramButton;
import app.morphe.extension.instagram.entity.InstagramButtonStyleEnum;
import app.morphe.extension.shared.ui.Dim;

import com.instagram.igds.components.button.IgdsButton;

public class ProfileMoreOption {
    private static boolean DEBUG;

    static {
        DEBUG = Pref.pikoDebug();
    }

    public static void moreOptionsDailogueBox(Context context, UserData userData) {
        if (context == null || userData == null) return;
        try {
            float density = context.getResources().getDisplayMetrics().density;
            android.app.Dialog dialog = new android.app.Dialog(context);
            dialog.requestWindowFeature(android.view.WindowFeature.NO_TITLE);

            android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);

            android.widget.LinearLayout contentLayout = new android.widget.LinearLayout(context);
            contentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad = (int) (18 * density);
            contentLayout.setPadding(pad, pad, pad, pad);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(android.graphics.Color.parseColor("#141414"));
            bg.setCornerRadius(20 * density);
            bg.setStroke((int) (1 * density), android.graphics.Color.parseColor("#26FFFFFF"));
            contentLayout.setBackground(bg);

            // Header Title
            android.widget.TextView titleView = new android.widget.TextView(context);
            String uName = userData.getUsername() != null ? "@" + userData.getUsername() : "";
            titleView.setText("[AKSI PROFIL] " + uName);
            titleView.setTextColor(android.graphics.Color.WHITE);
            titleView.setTextSize(17f);
            titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            contentLayout.addView(titleView);

            // Subtitle
            android.widget.TextView subView = new android.widget.TextView(context);
            subView.setText("Pusat Aksi & Ekstraksi Data Akun");
            subView.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            subView.setTextSize(12f);
            android.widget.LinearLayout.LayoutParams subLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = (int) (2 * density);
            subLp.bottomMargin = (int) (14 * density);
            subView.setLayoutParams(subLp);
            contentLayout.addView(subView);

            // Action Items
            class ActionItem {
                final String label;
                final Runnable action;
                ActionItem(String label, Runnable action) {
                    this.label = label;
                    this.action = action;
                }
            }

            java.util.List<ActionItem> actions = new java.util.ArrayList<>();
            actions.add(new ActionItem("Lihat Foto Profil (Ukuran Penuh)", () -> {
                ProfilePictureViewer.show(context, userData);
            }));
            actions.add(new ActionItem("Unduh Foto Profil (Kualitas HD Asli)", () -> {
                String url = userData.getProfilePictureUrl();
                String username = userData.getUsername();
                String downloadFilename = username + "_dp.jpg";
                String subFolder = DownloadUtils.getSubfolderName(username);
                DownloadUtils.downloadMediaUrl(context, url, subFolder, downloadFilename);
            }));
            actions.add(new ActionItem("Salin Nama Pengguna (" + uName + ")", () -> {
                Utils.setClipboard(userData.getUsername());
                Utils.showToastShort("Nama pengguna disalin");
            }));
            actions.add(new ActionItem("Salin Nama Lengkap Akun", () -> {
                Utils.setClipboard(userData.getFullName());
                Utils.showToastShort("Nama lengkap disalin");
            }));
            actions.add(new ActionItem("Salin ID Pengguna (User ID)", () -> {
                Utils.setClipboard(userData.getUserId());
                Utils.showToastShort("User ID disalin");
            }));
            actions.add(new ActionItem("Salin Tautan Profil Akun", () -> {
                Utils.setClipboard(userData.getProfileLink());
                Utils.showToastShort("Tautan profil disalin");
            }));
            actions.add(new ActionItem("Bagikan Profil ke Aplikasi Lain", () -> {
                PikoUtils.shareText(userData.getProfileLink());
            }));
            actions.add(new ActionItem("Salin Teks Bio", () -> {
                Utils.setClipboard(userData.getBio());
                Utils.showToastShort("Bio disalin");
            }));
            actions.add(new ActionItem("Salin Tautan dalam Bio", () -> {
                String links = linksFrom(userData.getBio());
                if (links == null) {
                    Utils.showToastShort("Tidak ada tautan di bio");
                } else {
                    Utils.setClipboard(links);
                    Utils.showToastShort("Tautan bio disalin");
                }
            }));
            if (DEBUG) {
                actions.add(new ActionItem("Debug RHpatch", () -> {
                    ObjectBrowser.browseObject(context, userData);
                }));
            }

            for (ActionItem act : actions) {
                android.widget.TextView itemBtn = new android.widget.TextView(context);
                itemBtn.setText(act.label);
                itemBtn.setTextColor(android.graphics.Color.WHITE);
                itemBtn.setTextSize(14f);
                int pV = (int) (12 * density);
                int pH = (int) (14 * density);
                itemBtn.setPadding(pH, pV, pH, pV);

                android.graphics.drawable.GradientDrawable itemBg = new android.graphics.drawable.GradientDrawable();
                itemBg.setColor(android.graphics.Color.parseColor("#1F1F1F"));
                itemBg.setCornerRadius(10 * density);
                itemBg.setStroke((int) (1 * density), android.graphics.Color.parseColor("#1FFFFFFF"));
                itemBtn.setBackground(itemBg);

                android.widget.LinearLayout.LayoutParams itemLp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                itemLp.bottomMargin = (int) (8 * density);
                itemBtn.setLayoutParams(itemLp);

                itemBtn.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                        act.action.run();
                    } catch (Throwable t) {
                        PikoLog.e("ProfileMoreOption", "Error executing action: " + act.label, t);
                    }
                });
                contentLayout.addView(itemBtn);
            }

            // Close button
            android.widget.Button closeBtn = new android.widget.Button(context);
            closeBtn.setText("Tutup");
            closeBtn.setTextColor(android.graphics.Color.WHITE);
            closeBtn.setTextSize(14f);
            android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
            closeBg.setColor(android.graphics.Color.parseColor("#333333"));
            closeBg.setCornerRadius(10 * density);
            closeBtn.setBackground(closeBg);
            android.widget.LinearLayout.LayoutParams closeLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (42 * density));
            closeLp.topMargin = (int) (6 * density);
            closeBtn.setLayoutParams(closeLp);
            closeBtn.setOnClickListener(v -> dialog.dismiss());
            contentLayout.addView(closeBtn);

            scrollView.addView(contentLayout);
            dialog.setContentView(scrollView);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
                dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
        } catch (Exception e) {
            PikoLog.e("ProfileMoreOption", "Error at moreOptionsDailogueBox", e);
            Utils.showToastShort(e.getMessage());
        }
    }

    /**
     * Every link written in a bio, one per line. Instagram stores the bio as plain
     * text, so a URL there is only ever a URL by how it reads — matched loosely on
     * purpose, since most bios write "example.com" without a scheme.
     */
    private static String linksFrom(String bio) {
        if (bio == null || bio.isEmpty()) return null;
        Matcher matcher = BIO_LINK.matcher(bio);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String link = matcher.group();
            if (out.indexOf(link) >= 0) continue; // the same link twice reads as noise
            if (out.length() > 0) out.append('\n');
            out.append(link);
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static final Pattern BIO_LINK = Pattern.compile(
            "(?:https?://)?(?:[\\w-]+\\.)+[a-z]{2,}(?:/[^\\s]*)?",
            Pattern.CASE_INSENSITIVE);

    public static void addProfileMoreOptionsButton(ViewGroup viewGroup, ProfileInfo profileInfo) {
        try {
            UserData userData = profileInfo.getUserData();

            Context context = viewGroup.getContext();
            InstagramButton button = new InstagramButton(context);
            button.setText(str("piko_more_profile_options"));
            button.setStyle(InstagramButtonStyleEnum.PRIMARY);
            button.setOnClickListener(() ->
                    moreOptionsDailogueBox(context, userData)
            );

            int marginPx = Dim.dp12;
            button.setMargins(marginPx, marginPx, marginPx, marginPx);

            IgdsButton igdsButton = button.getIgdsButton();
            viewGroup.addView(igdsButton);
            igdsButton.bringToFront();
            viewGroup.requestLayout();
            viewGroup.invalidate();
        } catch (Exception e) {
            PikoLog.e("ProfileMoreOption", "Failed to add profile more button", e);
        }
    }
}

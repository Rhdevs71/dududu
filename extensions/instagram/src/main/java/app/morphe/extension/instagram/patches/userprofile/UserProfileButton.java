/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 */

package app.morphe.extension.instagram.patches.userprofile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.Set;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.instagram.utils.PikoLog;
import app.morphe.extension.instagram.entity.ProfileInfo;
import app.morphe.extension.instagram.entity.UserData;
import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.instagram.constants.UI;
import app.morphe.extension.instagram.constants.Constants;
import app.morphe.extension.instagram.patches.userprofile.ProfileMoreOption;
import app.morphe.extension.instagram.ui.RhpatchInstagramInjector;

public class UserProfileButton {
    private static final String TAG_MINI_PROFILE_ICON = "rhpatch_mini_profile_icon";
    private static volatile UserData currentUserData = null;

    public static UserData getCurrentUserData() {
        return currentUserData;
    }

    public static void clearCurrentUserData() {
        currentUserData = null;
    }

    public static void addButtons(final ViewGroup viewGroup, Object object) {
        if (viewGroup == null) return;

        try {
            final Context context = viewGroup.getContext();
            UserData userData = null;
            try {
                userData = new ProfileInfo(object).getUserData();
            } catch (Throwable ignored) {}
            currentUserData = userData;

            // Pastikan kapsul RHpatch aktif di halaman profil
            RhpatchInstagramInjector.setCapsuleVisibility(context, View.VISIBLE);

            // Listener attach/detach agar kapsul langsung lenyap begitu keluar dari profil
            viewGroup.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    RhpatchInstagramInjector.setCapsuleVisibility(v.getContext(), View.VISIBLE);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    RhpatchInstagramInjector.setCapsuleVisibility(v.getContext(), View.GONE);
                    clearCurrentUserData();
                }
            });

            // Ikon mini estetik melingkar (minimalis, tidak merusak proporsi tata letak profil)
            if (viewGroup.findViewWithTag(TAG_MINI_PROFILE_ICON) == null && userData != null) {
                float density = context.getResources().getDisplayMetrics().density;
                int size = (int) (34 * density);

                TextView miniBtn = new TextView(context);
                miniBtn.setTag(TAG_MINI_PROFILE_ICON);
                miniBtn.setText("⋮");
                miniBtn.setTextColor(Color.WHITE);
                miniBtn.setTextSize(18f);
                miniBtn.setGravity(Gravity.CENTER);

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.parseColor("#26FFFFFF")); // Semi-transparan elegan
                bg.setStroke((int) (1 * density), Color.parseColor("#4DFFFFFF"));
                miniBtn.setBackground(bg);

                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(size, size);
                lp.setMargins((int) (6 * density), (int) (4 * density), (int) (6 * density), (int) (4 * density));
                miniBtn.setLayoutParams(lp);

                miniBtn.setOnClickListener(v -> {
                    try {
                        ProfileMoreOption.moreOptionsDailogueBox(context, userData);
                    } catch (Throwable t) {
                        PikoLog.e("UserProfileButton", "Error opening profile more options", t);
                    }
                });

                viewGroup.addView(miniBtn);
            }
        } catch (Throwable t) {
            PikoLog.e("UserProfileButton", "addButtons failed", t);
        }
    }
}


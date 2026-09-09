/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.video;

import static app.morphe.extension.instagram.utils.IgStr.str;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.ArrayList;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.sharedPreference.SharedPref;
import app.morphe.extension.instagram.entity.InstagramDialogBox;
import app.morphe.extension.instagram.utils.PikoLog;

public class PlaybackSpeedController {
    private static final String TAG = "PlaybackSpeedController";
    public static final String PREF_SPEED_LOCK_ENABLED = "rhpatch_speed_lock_enabled";
    public static final String PREF_LOCKED_SPEED = "rhpatch_locked_playback_speed";

    public static volatile float currentPlaybackSpeed = 1.0f;

    public static boolean isSpeedLockEnabled() {
        return Boolean.TRUE.equals(SharedPref.getBooleanPref(PREF_SPEED_LOCK_ENABLED, false));
    }

    public static void setSpeedLockEnabled(boolean enabled) {
        SharedPref.setBooleanPref(PREF_SPEED_LOCK_ENABLED, enabled);
    }

    public static float getLockedSpeed() {
        try {
            String val = SharedPref.getStringPref(PREF_LOCKED_SPEED, "2.0");
            return Float.parseFloat(val);
        } catch (Throwable t) {
            return 2.0f;
        }
    }

    public static void setLockedSpeed(float speed) {
        SharedPref.setStringPref(PREF_LOCKED_SPEED, String.valueOf(speed));
        currentPlaybackSpeed = speed;
    }

    public static void showSpeedDialog(Context context) {
        try {
            InstagramDialogBox dialog = new InstagramDialogBox(context);
            ArrayList<String> speedLabels = new ArrayList<>();
            final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

            speedLabels.add("0.5x");
            speedLabels.add("0.75x");
            speedLabels.add("1.0x (Normal)");
            speedLabels.add("1.25x");
            speedLabels.add("1.5x");
            speedLabels.add("2.0x");

            CharSequence[] items = speedLabels.toArray(new CharSequence[0]);

            dialog.addDialogMenuItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    try {
                        if (which >= 0 && which < speedValues.length) {
                            float selectedSpeed = speedValues[which];
                            currentPlaybackSpeed = selectedSpeed;
                            applySpeed(selectedSpeed);
                            String msg = "Kecepatan video: " + selectedSpeed + "x";
                            PikoUtils.toast(msg);
                            PikoLog.d(TAG, "Video playback speed set to: " + selectedSpeed + "x");
                        }
                    } catch (Throwable t) {
                        PikoLog.e(TAG, "Error applying speed", t);
                        PikoUtils.toast(t.getMessage());
                    }
                }
            });

            dialog.setTitle("Kecepatan Pemutaran Video");
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            Dialog dlg = dialog.getDialog();
            dlg.show();
        } catch (Throwable t) {
            PikoLog.e(TAG, "showSpeedDialog failed", t);
        }
    }

    public static void applySpeed(float speed) {
        currentPlaybackSpeed = speed;
        PikoLog.d(TAG, "Applying playback speed: " + speed);
    }

    public static float getPlaybackSpeed() {
        if (isSpeedLockEnabled()) {
            return getLockedSpeed();
        }
        return currentPlaybackSpeed;
    }
}

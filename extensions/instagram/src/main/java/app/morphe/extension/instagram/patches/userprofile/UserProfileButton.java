/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.userprofile;

import android.view.ViewGroup;
import java.util.Set;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.instagram.utils.PikoLog;
import app.morphe.extension.instagram.entity.ProfileInfo;
import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.instagram.constants.UI;
import app.morphe.extension.instagram.constants.Constants;
import app.morphe.extension.instagram.patches.userprofile.ProfileMoreOption;

public class UserProfileButton {
    private static boolean isSettingsInActionBar;
    private static Set userProfileABPref;

    static {
        userProfileABPref = Pref.userProfileActionBarButtons();
        if(userProfileABPref.contains(Constants.AB_SETTINGS_ICON)){
            isSettingsInActionBar = true;
        } else if(Pref.mainFeedActionBarButtons().contains(Constants.AB_SETTINGS_ICON)){
            isSettingsInActionBar = true;
        } else if(Pref.chatActionBarButtons().contains(Constants.AB_SETTINGS_ICON)){
            isSettingsInActionBar = true;
        } else if(Pref.inboxActionBarButtons().contains(Constants.AB_SETTINGS_ICON)){
            isSettingsInActionBar = true;
        } else{
            isSettingsInActionBar = false;
        }
    }

    public static void addButtons(ViewGroup viewGroup, Object object) {
        // Kept clean: No intrusive buttons are injected into the profile header
        // so that the profile looks 100% authentic and native on screenshots.
        // All Rhpatch settings and profile utilities are accessed via the floating ● RHpatch capsule!
    }
}


/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */


package app.morphe.extension.instagram.utils;

import java.util.Set;

import app.morphe.extension.instagram.settings.Settings;
import app.morphe.extension.instagram.settings.SettingsStatus;
import app.morphe.extension.instagram.constants.Constants;

import app.morphe.extension.crimera.sharedPreference.SharedPref;

@SuppressWarnings("unused")
public class Pref {
    private static final int MAX_IMAGE_SIZE = 4096;
    public static boolean SHOULD_MARK_CHAT_AS_READ;
    static {
        SHOULD_MARK_CHAT_AS_READ = false;
    }
    public static void setMarkChatAsReadIndicator(boolean bool) {
        SHOULD_MARK_CHAT_AS_READ = bool;
    }

    public static boolean clearAllPreferences() {
        return SharedPref.clearAll();
    }
    
    public static boolean pikoDebug() {
        return SharedPref.getBooleanPref(Settings.PIKO_DEBUG);
    }

    public static boolean firstTimePiko() {
        return SharedPref.getBooleanPref(Settings.FIRST_TIME_PIKO);
    }
    public static boolean setFirstTimePiko(boolean bool) {
        return SharedPref.setBooleanPref(Settings.FIRST_TIME_PIKO.key,bool);
    }

    public static boolean unlockPlusBenefits() {
        return SharedPref.getBooleanPref(Settings.UNLOCK_PLUS_BENEFITS);
    }

    public static boolean isBenefitAllowed(String benefit) {
        if (benefit != null) {
            String b = benefit.toLowerCase();
            if (b.contains("custom_app_icon") ||
                b.contains("custom_profile_bio_font") ||
                b.contains("bio_font") ||
                b.contains("biography_font") ||
                b.contains("story_font") ||
                b.contains("font_pack")) {
                PikoLog.d("BenefitChecker", "Unlocking benefit unconditionally: " + benefit);
                return true;
            }
        }
        return unlockPlusBenefits();
    }

    public static boolean disableAds() {
        return SharedPref.getBooleanPref(Settings.DISABLE_ADS);
    }

    public static boolean hideSuggestedContent() {
        return SharedPref.getBooleanPref(Settings.HIDE_SUGGESTED_CONTENT);
    }

    public static boolean saveDeletedMessages() {
        return SharedPref.getBooleanPref(Settings.SAVE_DELETED_MESSAGES);
    }

    public static boolean openLinksExternally() {
        return SharedPref.getBooleanPref(Settings.OPEN_LINKS_EXTERNALLY);
    }

    public static boolean sanitizeShareLinks() {
        return SharedPref.getBooleanPref(Settings.SANITIZE_SHARE_LINKS);
    }

    public static String customSharingDomain() {
        return SharedPref.getStringPref(Settings.CUSTOM_SHARING_DOMAIN);
    }

    public static boolean getTurnOnAllGhostModes() {
        return SharedPref.getBooleanPref(Settings.TURN_ON_ALL_GHOST_MODES);
    }

    public static boolean setTurnOnAllGhostModes(boolean bool) {
        return SharedPref.setBooleanPref(Settings.TURN_ON_ALL_GHOST_MODES.key,bool);
    }

    public static boolean isMoreOptionsOnProfilePatched(){
        return SettingsStatus.moreOptionsOnProfile;
    }

    public static boolean viewStoriesAnonymously() {
        return SharedPref.getBooleanPref(Settings.VIEW_STORIES_ANONYMOUSLY) || Pref.getTurnOnAllGhostModes();
    }

    public static boolean viewLiveAnonymously() {
        return SharedPref.getBooleanPref(Settings.VIEW_LIVE_ANONYMOUSLY) || Pref.getTurnOnAllGhostModes();
    }

    public static boolean disableScreenshotDetection() {
        return SharedPref.getBooleanPref(Settings.DISABLE_SCREENSHOT_DETECTION) || Pref.getTurnOnAllGhostModes();
    }

    public static boolean disableTypingStatus() {
        return SharedPref.getBooleanPref(Settings.DISABLE_TYPING_STATUS) || Pref.getTurnOnAllGhostModes();
    }

    public static boolean enableMarkChatAsReadOption() {
        return SharedPref.getBooleanPref(Settings.ENABLE_MARK_CHAT_AS_READ) && SettingsStatus.markChatAsRead;
    }

    // Return false = call the message seen api.
    // Return true = blocks the message seen api.
    public static boolean viewDmAnonymously() {
        if(enableMarkChatAsReadOption() && SHOULD_MARK_CHAT_AS_READ){
            return false;
        }
        return SharedPref.getBooleanPref(Settings.VIEW_DM_ANONYMOUSLY) || Pref.getTurnOnAllGhostModes();
    }

    /**
     * Determines whether DM seen receipts should be suppressed.
     * Broadcast Channels (Saluran Siaran) are excluded from suppression so membership status
     * and thread retention work seamlessly without repeated join pop-ups.
     *
     * @return true to block the seen API call (ghost DM), false to allow it to proceed.
     */
    public static boolean shouldSuppressDmSeen(Object session, Object dummyOrKey, String threadId) {
        if (!viewDmAnonymously()) {
            return false;
        }
        if (isBroadcastChannel(session, dummyOrKey, threadId)) {
            PikoLog.d("Pref", "Allowing seen receipt for broadcast channel: " + threadId);
            return false;
        }
        return true;
    }

    public static boolean isBroadcastChannel(Object session, Object dummyOrKey, String threadId) {
        try {
            if (threadId != null && !threadId.isEmpty()) {
                String tLower = threadId.toLowerCase();
                if (tLower.contains("channel") || tLower.contains("broadcast") || tLower.contains("saluran") || tLower.contains("siaran")) {
                    PikoLog.d("Pref", "Detected broadcast channel via threadId keyword: " + threadId);
                    return true;
                }
                // Meta broadcast channels are single large numeric IDs without '_' (1-on-1 DMs always contain '_')
                if (!threadId.contains("_") && threadId.matches("^[0-9]{8,45}$")) {
                    PikoLog.d("Pref", "Detected broadcast channel via numeric threadId: " + threadId);
                    return true;
                }
            }
            if (dummyOrKey == null) {
                // If directThreadKey is null, it could be broadcast channel or manual seen call
                return true;
            }
            String str = dummyOrKey.toString();
            String strLower = str.toLowerCase();
            if (strLower.contains("channel") || strLower.contains("broadcast") || strLower.contains("saluran") || strLower.contains("siaran")) {
                PikoLog.d("Pref", "Detected broadcast channel via dummyOrKey.toString(): " + str);
                return true;
            }
            Class<?> clazz = dummyOrKey.getClass();
            String className = clazz.getName().toLowerCase();
            if (className.contains("channel") || className.contains("broadcast") || className.contains("saluran")) {
                PikoLog.d("Pref", "Detected broadcast channel via class name: " + clazz.getName());
                return true;
            }

            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(dummyOrKey);
                if (val instanceof String) {
                    String sVal = (String) val;
                    if (sVal.isEmpty()) continue;
                    String sLower = sVal.toLowerCase();
                    if (sLower.contains("channel") || sLower.contains("broadcast") || sLower.contains("saluran") || sLower.contains("siaran")) {
                        PikoLog.d("Pref", "Detected broadcast channel via field '" + f.getName() + "' string: " + sVal);
                        return true;
                    }
                    // If thread ID does not contain '_' and is numeric (8-45 digits), it is a broadcast channel / non-1-on-1 thread
                    if (!sVal.contains("_") && sVal.matches("^[0-9]{8,45}$")) {
                        PikoLog.d("Pref", "Detected broadcast channel via field '" + f.getName() + "' numeric ID: " + sVal);
                        return true;
                    }
                } else if (val instanceof Long) {
                    long lVal = ((Long) val).longValue();
                    // Broadcast channel IDs and thread timestamps are large positive longs
                    if (lVal > 10000000L) {
                        PikoLog.d("Pref", "Detected broadcast channel via field '" + f.getName() + "' Long: " + lVal);
                        return true;
                    }
                } else if (val instanceof Integer) {
                    int type = ((Integer) val).intValue();
                    // 29 = Broadcast Channel, 32 = Social Channel, 33 = Subscriber Channel
                    if (type == 29 || type == 32 || type == 33 || type >= 28) {
                        PikoLog.d("Pref", "Detected broadcast channel via field '" + f.getName() + "' type: " + type);
                        return true;
                    }
                } else if (val instanceof java.util.List) {
                    java.util.List list = (java.util.List) val;
                    // Broadcast channels have empty recipient lists (members are subscribers)
                    if (list.isEmpty()) {
                        PikoLog.d("Pref", "Detected broadcast channel via empty recipients in field '" + f.getName() + "'");
                        return true;
                    }
                }
            }

            // General heuristic: If dummyOrKey is LX/01AX (or similar key object) and its primary string field does not contain '_',
            // it cannot be a 1-on-1 DM (which ALWAYS contains '_' between two user IDs).
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(dummyOrKey);
                if (val instanceof String) {
                    String sVal = (String) val;
                    if (!sVal.isEmpty() && !sVal.contains("_") && sVal.length() >= 6) {
                        PikoLog.d("Pref", "Non 1-on-1 thread without underscore detected in field '" + f.getName() + "': " + sVal + " -> allowing seen");
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            PikoLog.e("Pref", "Error detecting broadcast channel", t);
        }
        return false;
    }

    public static boolean disableVideoAutoplay() {
        return SharedPref.getBooleanPref(Settings.DISABLE_VIDEO_AUTOPLAY);
    }

    public static boolean storiesAudioAutoplay() {
        return SharedPref.getBooleanPref(Settings.STORIES_AUDIO_AUTOPLAY);
    }

    public
    static boolean disableStories() {
        return SharedPref.getBooleanPref(Settings.DISABLE_STORIES);
    }

    public static boolean disableHighlights() {
        return SharedPref.getBooleanPref(Settings.DISABLE_HIGHLIGHTS);
    }

    public static boolean disableExplore() {
        return SharedPref.getBooleanPref(Settings.DISABLE_EXPLORE);
    }

    public static boolean disableComments() {
        return SharedPref.getBooleanPref(Settings.DISABLE_COMMENTS);
    }

    public static boolean limitFollowingFeed() {
        return SharedPref.getBooleanPref(Settings.LIMIT_FOLLOWING_FEED);
    }

    public static boolean hideStoriesTray() {
        return SharedPref.getBooleanPref(Settings.HIDE_STORIES_TRAY) && SettingsStatus.hideStoriesTray;
    }

    public static boolean hideNotesTray() {
        return SharedPref.getBooleanPref(Settings.HIDE_NOTES_TRAY) && SettingsStatus.hideNotesTray;
    }

    public static boolean disableReelsScrolling() {
        return SharedPref.getBooleanPref(Settings.DISABLE_REELS_SCROLLING) && SettingsStatus.disableReelsScrolling;
    }

    public static boolean disableSwipeToCreate() {
        return SharedPref.getBooleanPref(Settings.DISABLE_SWIPE_TO_CREATE) && SettingsStatus.disableSwipeToCreate;
    }

    public static boolean makeEphemeralMediaPermanent() {
        return SharedPref.getBooleanPref(Settings.UNLIMITED_REPLAYS) && SettingsStatus.unlimitedReplaysOnEphemeralMedia;
    }

    public static boolean hideReshareButton() {
        return SharedPref.getBooleanPref(Settings.HIDE_RESHARE_BUTTON) && SettingsStatus.hideReshareButton;
    }

    public static boolean hideGroupCreationOnSharesheet() {
        return SharedPref.getBooleanPref(Settings.HIDE_GROUP_CREATION_BUTTON_ON_SHARESHEET);
    }

    public static boolean enableDevOptions() {
        return SharedPref.getBooleanPref(Settings.DEVELOPER_OPTIONS);
    }
    public static boolean directlyOpenMetaConfig() {
        return SharedPref.getBooleanPref(Settings.DIRECTLY_OPEN_METACONFIG);
    }
    public static boolean enableEmployeeOptions() {
        return SharedPref.getBooleanPref(Settings.ENABLE_EMP_OPTIONS);
    }
    public static boolean allowUserNetworkCertificate() {
        return SharedPref.getBooleanPref(Settings.ALLOW_USER_NETWORK_CERTIFICATE);
    }

    public static int buildAge(int appAge) {
        return SharedPref.getBooleanPref(Settings.REMOVE_BUILD_EXPIRE_POPUP) ? 1 : appAge;
    }

    public static boolean disableAnalytics() {
        return SharedPref.getBooleanPref(Settings.DISABLE_ANALYTICS);
    }

    public static boolean disableDiscoverPeople() {
        return SharedPref.getBooleanPref(Settings.DISABLE_DISCOVER_PEOPLE);
    }

    public static boolean followBackIndicator() {
        return SharedPref.getBooleanPref(Settings.FOLLOW_BACK_INDICATOR);
    }
    public static boolean followBackColorIndicator() {
        return SharedPref.getBooleanPref(Settings.FOLLOW_BACK_COLOR_INDICATOR);
    }

    public static boolean disableStoryFlipping() {
        return SharedPref.getBooleanPref(Settings.DISABLE_STORY_FLIPPING);
    }

    public static boolean loopStory() {
        return SharedPref.getBooleanPref(Settings.LOOP_STORY);
    }

    public static boolean viewStoryMentions() {
        return SharedPref.getBooleanPref(Settings.VIEW_STORY_MENTIONS);
    }

    public static String customiseStoryTimestamp() {
        return SharedPref.getStringPref(Settings.CUSTOMISE_STORY_TIMESTAMP);
    }

    public static int improveImageViewing(int defaultSize) {
        return SharedPref.getBooleanPref(Settings.IMPROVE_IMAGE_VIEWING) ? MAX_IMAGE_SIZE : defaultSize;
    }

    public static Integer improveImageViewing(Integer defaultSize) {
        return SharedPref.getBooleanPref(Settings.IMPROVE_IMAGE_VIEWING) ? MAX_IMAGE_SIZE : defaultSize;
    }

    public static boolean enableDownload() {
        return SharedPref.getBooleanPref(Settings.ENABLE_DOWNLOAD) && SettingsStatus.downloadMedia;
    }

    public static boolean enableDirectDownload() {
        return SharedPref.getBooleanPref(Settings.ENABLE_DIRECT_DOWNLOAD);
    }

    public static boolean downloadUsernameFolder() {
        return SharedPref.getBooleanPref(Settings.DOWNLOAD_USERNAME_FOLDER);
    }

    public static boolean hideNavigationFeed() {
        return SharedPref.getBooleanPref(Settings.HIDE_NAVIGATION_FEED);
    }

    public static boolean hideNavigationReels() {
        return SharedPref.getBooleanPref(Settings.HIDE_NAVIGATION_REELS);
    }

    public static boolean hideNavigationDirect() {
        return SharedPref.getBooleanPref(Settings.HIDE_NAVIGATION_DIRECT);
    }

    public static boolean hideNavigationSearch() {
        return SharedPref.getBooleanPref(Settings.HIDE_NAVIGATION_SEARCH);
    }

    public static boolean hideNavigationCreate() {
        return SharedPref.getBooleanPref(Settings.HIDE_NAVIGATION_CREATE);
    }

    public static boolean removeEmptyBottomSpace() {
        return SharedPref.getBooleanPref(Settings.REMOVE_EMPTY_BOTTOM_SPACE);
    }

    public static boolean commentCopyButton() {
        return SharedPref.getBooleanPref(Settings.COMMENT_COPY_BUTTON) && SettingsStatus.copyCommentButton;
    }

    public static boolean commentSaveMediaButton() {
        return SharedPref.getBooleanPref(Settings.COMMENT_SAVE_MEDIA_BUTTON) && SettingsStatus.saveMediaCommentButton;
    }

    public static String changeLikeAnimation() {
        return SharedPref.getStringPref(Settings.CHANGE_LIKE_ANIMATION);
    }

    public static float customiseStoryRingSize() {
        try {
            return Float.parseFloat(SharedPref.getStringPref(Settings.CUSTOMISE_STORY_RING_SIZE));
        } catch (Exception ex) {
            return 100.0f;
        }
    }

    public static boolean disableDoubleTapPost() {
        return SharedPref.getBooleanPref(Settings.DISABLE_DOUBLE_TAP_LIKE_POST);
    }
    public static boolean disableDoubleTapReel() {
        return SharedPref.getBooleanPref(Settings.DISABLE_DOUBLE_TAP_LIKE_REEL);
    }
    public static boolean disableDoubleTapComment() {
        return SharedPref.getBooleanPref(Settings.DISABLE_DOUBLE_TAP_LIKE_COMMENT);
    }
    public static boolean disableDoubleTapMessage() {
        return SharedPref.getBooleanPref(Settings.DISABLE_DOUBLE_TAP_LIKE_MESSAGE);
    }
    public static boolean moreOptionsOnPost() {
        return SharedPref.getBooleanPref(Settings.ENABLE_MORE_OPTIONS_ON_POST) && SettingsStatus.moreOptionsOnPost;
    }
    public static boolean downloadWithExternalDownloader() {
        return SharedPref.getBooleanPref(Settings.DOWNLOAD_WITH_EXTERNAL_DOWNLOADER) && SettingsStatus.downloadWithExternalDownloader;
    }

    public static String externalDownloaderPackageName() {
        return SharedPref.getStringPref(Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME);
    }

    public static Set<String> mainFeedActionBarButtons() {
        return SharedPref.getSetPref(Settings.ACTION_BAR_MAIN_FEED);
    }

    public static Set<String> userProfileActionBarButtons() {
        return SharedPref.getSetPref(Settings.ACTION_BAR_USER_PROFILE);
    }

    public static Set<String> chatActionBarButtons() {
        return SharedPref.getSetPref(Settings.ACTION_BAR_CHAT);
    }

    public static Set<String> inboxActionBarButtons() {
        return SharedPref.getSetPref(Settings.ACTION_BAR_INBOX);
    }

    public static Set<String> filterStoryByType() {
        return SharedPref.getSetPref(Settings.FILTER_STORY_BY_TYPE);
    }

    public static Set<String> filterStoryByUserType() {
        return SharedPref.getSetPref(Settings.FILTER_STORY_BY_USER_TYPE);
    }

    public static Integer filterStoryByMinStoryItems() {
        return Integer.valueOf(SharedPref.getStringPref(Settings.FILTER_STORY_MIN_STORY_ITEMS));
    }

    public static Integer filterStoryByMaxStoryItems() {
        return Integer.valueOf(SharedPref.getStringPref(Settings.FILTER_STORY_MAX_STORY_ITEMS));
    }

    public static boolean hideOnlineStatus() {
        return SharedPref.getBooleanPref(Settings.HIDE_ONLINE_STATUS) || Pref.getTurnOnAllGhostModes();
    }

    public static boolean reelsPlaybackSpeed() {
        return SharedPref.getBooleanPref(Settings.REELS_PLAYBACK_SPEED);
    }

    public static boolean saveEditedMessages() {
        return SharedPref.getBooleanPref(Settings.SAVE_EDITED_MESSAGES);
    }

    public static boolean appLock() {
        return SharedPref.getBooleanPref(Settings.APP_LOCK);
    }

    //end
}

package com.zype.fire.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.zype.fire.api.Model.AppData;
import com.zype.fire.api.Util.FileHelper;

/**
 * Created by Evgeny Cherkasov on 13.11.2017.
 */

public class ZypeConfiguration {
    private static final String PREFERENCE_APP_ID = "ZypeAppId";
    private static final String PREFERENCE_DEVICE_LINKING = "ZypeDeviceLinking";
    private static final String PREFERENCE_DEVICE_LINKING_URL = "ZypeDeviceLinkingUrl";
    private static final String PREFERENCE_FAVORITES_API = "ZypeFavoritesApi";
    private static final String PREFERENCE_NATIVE_SUBSCRIPTION = "ZypeNativeSubscription";
    private static final String PREFERENCE_MARKETPLACE_CONNECT_SVOD = "ZypeMarketplaceConnectSVOD";
    private static final String PREFERENCE_NATIVE_TVOD = "ZypeNativeTVOD";
    private static final String PREFERENCE_ROOT_PLAYLIST_ID = "ZypeRootPlaylistId";
    private static final String PREFERENCE_SITE_ID = "ZypeSiteId";
    private static final String PREFERENCE_SUBSCRIBE_TO_WATCH_AD_FREE = "ZypeSubscribeToWatchAdFree";
    private static final String PREFERENCE_UNIVERSAL_SUBSCRIPTION = "ZypeUniversalSubscription";
    private static final String PREFERENCE_UNIVERSAL_TVOD = "ZypeUniversalTVOD";

    private static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void update(AppData appData, Context context) {
        clear(context);

        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        if (!TextUtils.isEmpty(appData.Id)) {
            editor.putString(PREFERENCE_APP_ID, appData.Id);
        }
        if (!TextUtils.isEmpty(appData.deviceLinking)) {
            editor.putBoolean(PREFERENCE_DEVICE_LINKING, Boolean.valueOf(appData.deviceLinking));
        }
        if (!TextUtils.isEmpty(appData.deviceLinkingUrl)) {
            editor.putString(PREFERENCE_DEVICE_LINKING_URL, appData.deviceLinkingUrl);
        }
        if (!TextUtils.isEmpty(appData.favoritesViaApi)) {
            editor.putBoolean(PREFERENCE_FAVORITES_API, Boolean.valueOf(appData.favoritesViaApi));
        }
        if (!TextUtils.isEmpty(appData.nativeSubscription)) {
            editor.putBoolean(PREFERENCE_NATIVE_SUBSCRIPTION, Boolean.valueOf(appData.nativeSubscription));
        }
//        if (!TextUtils.isEmpty(appData.nativeToUniversalSubscription)) {
//            editor.putBoolean(PREFERENCE_MARKETPLACE_CONNECT_SVOD, Boolean.valueOf(appData.nativeToUniversalSubscription));
//        }
        if (!TextUtils.isEmpty(appData.nativeTVOD)) {
            editor.putBoolean(PREFERENCE_NATIVE_TVOD, Boolean.valueOf(appData.nativeTVOD));
        }
        if (!TextUtils.isEmpty(appData.featuredPlaylistId)) {
            editor.putString(PREFERENCE_ROOT_PLAYLIST_ID, appData.featuredPlaylistId);
        }
        if (!TextUtils.isEmpty(appData.siteId)) {
            editor.putString(PREFERENCE_SITE_ID, appData.siteId);
        }
        if (!TextUtils.isEmpty(appData.subscribeToWatchAdFree)) {
            editor.putBoolean(PREFERENCE_SUBSCRIBE_TO_WATCH_AD_FREE, Boolean.valueOf(appData.subscribeToWatchAdFree));
        }
        if (!TextUtils.isEmpty(appData.universalSubscription)) {
            editor.putBoolean(PREFERENCE_UNIVERSAL_SUBSCRIPTION, Boolean.valueOf(appData.universalSubscription));
        }
        if (!TextUtils.isEmpty(appData.universalTVOD)) {
            editor.putBoolean(PREFERENCE_UNIVERSAL_TVOD, Boolean.valueOf(appData.universalTVOD));
        }
        editor.apply();
    }

    public static void clear(Context context) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREFERENCE_APP_ID);
        editor.remove(PREFERENCE_DEVICE_LINKING);
        editor.remove(PREFERENCE_FAVORITES_API);
        editor.remove(PREFERENCE_NATIVE_SUBSCRIPTION);
        editor.remove(PREFERENCE_MARKETPLACE_CONNECT_SVOD);
        editor.remove(PREFERENCE_NATIVE_TVOD);
        editor.remove(PREFERENCE_ROOT_PLAYLIST_ID);
        editor.remove(PREFERENCE_SITE_ID);
        editor.remove(PREFERENCE_SUBSCRIBE_TO_WATCH_AD_FREE);
        editor.remove(PREFERENCE_UNIVERSAL_SUBSCRIPTION);
        editor.remove(PREFERENCE_UNIVERSAL_TVOD);
        editor.apply();
    }

    private static boolean getBooleanPreference(String key, boolean defaultValue, Context context) {
        SharedPreferences prefs = getPreferences(context);
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, defaultValue);
        }
        else {
            return defaultValue;
        }
    }

    private static String getStringPreference(String key, String defaultValue, Context context) {
        SharedPreferences prefs = getPreferences(context);
        if (prefs.contains(key)) {
            return prefs.getString(key, defaultValue);
        }
        else {
            return defaultValue;
        }
    }

    public static String getAppId(Context context) {
        return getStringPreference(PREFERENCE_APP_ID, "", context);
    }

    public static String getRootPlaylistId(Context context) {
        return getStringPreference(PREFERENCE_ROOT_PLAYLIST_ID, ZypeSettings.ROOT_PLAYLIST_ID, context);
    }

    public static String getSiteId(Context context) {
        return getStringPreference(PREFERENCE_SITE_ID, "", context);
    }

    public static boolean isDeviceLinkingEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_DEVICE_LINKING, ZypeSettings.DEVICE_LINKING, context);
    }

    public static boolean isFavoritesViaApiEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_FAVORITES_API, ZypeSettings.FAVORITES_VIA_API, context);
    }

    public static boolean isNativeSubscriptionEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_NATIVE_SUBSCRIPTION, ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED, context);
    }

    public static boolean marketplaceConnectSvodEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_MARKETPLACE_CONNECT_SVOD, ZypeSettings.MARKETPLACE_CONNECT_SVOD, context);
    }

    public static boolean isNativeTVODEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_NATIVE_TVOD, ZypeSettings.NATIVE_TVOD, context);
    }

    public static boolean isSubscribeToWatchAdFreeEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_SUBSCRIBE_TO_WATCH_AD_FREE, ZypeSettings.SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED, context);
    }

    public static boolean isUniversalSubscriptionEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_UNIVERSAL_SUBSCRIPTION, ZypeSettings.UNIVERSAL_SUBSCRIPTION_ENABLED, context);
    }

    public static boolean isUniversalTVODEnabled(Context context) {
        return getBooleanPreference(PREFERENCE_UNIVERSAL_TVOD, ZypeSettings.UNIVERSAL_TVOD, context);
    }

    // UI
    public static boolean displayWatchedBarOnVideoThumbnails() {
        return true;
    }

    // Navigation

    public static boolean displayLeftMenu() {
        return ZypeSettings.SHOW_LEFT_MENU;
    }

    public static boolean displayAccountNavigationButton() {
        return ZypeSettings.ACCOUNT_NAV_BUTTON_DISPLAY;
    }

    public static boolean displayTermsNavigationButton() {
        return ZypeSettings.TERMS_NAV_BUTTON_DISPLAY;
    }

    public static boolean isCreateAccountTermsOfServiceRequired() {
        return ZypeSettings.ACCOUNT_CREATION_TOS;
    }

    // App configuration
    public static AppConfiguration readAppConfiguration(Context context) {
        AppConfiguration result = null;

        String jsonAppConfiguration = FileHelper.readAssetsFile(context, R.raw.zype_app_configuration);
        if (!TextUtils.isEmpty(jsonAppConfiguration)) {
            Gson gson = new Gson();
            result = gson.fromJson(jsonAppConfiguration, AppConfiguration.class);
        }

        return result;
    }
}

package com.zype.fire.api;

import android.content.Context;

/**
 * Created by Evgeny Cherkasov on 17.04.2017.
 */

public class ZypeSettings {
    // Zype app key
    public static final String APP_KEY = "<APP_KEY>";
    // OAuth credentials
    public static final String CLIENT_ID = "<CLIENT_ID>";
    public static final String CLIENT_SECRET = "<CLIENT_SECRET>";
    // Playlist
    public static final String ROOT_PLAYLIST_ID = "<ROOT_PLAYLIST_ID>";

    public static final String FAVORITES_PLAYLIST_ID = "Favorites";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";
    public static final String ROOT_FAVORITES_PLAYLIST_ID = "RootFavorites";
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";

    // Template version
    public static final String TEMPLATE_VERSION = "1.6.0";

    // Features
    public static final boolean DEVICE_LINKING = Boolean.valueOf("<DEVICE_LINKING>");
    public static final boolean FAVORITES_VIA_API = Boolean.valueOf("<FAVORITES_VIA_API>");
    public static final boolean ACCOUNT_CREATION_TOS = false;

    // Monetization
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = Boolean.valueOf("<NATIVE_SUBSCRIPTION_ENABLED>");
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = Boolean.valueOf("<NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED>");
    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = Boolean.valueOf("<SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED>");
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = Boolean.valueOf("<UNIVERSAL_SUBSCRIPTION_ENABLED>");
    public static final boolean UNIVERSAL_TVOD = Boolean.valueOf("<UNIVERSAL_TVOD>");

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "";
}


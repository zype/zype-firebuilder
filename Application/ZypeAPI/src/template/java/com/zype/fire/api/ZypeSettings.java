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
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";

    // Template version
    public static final String TEMPLATE_VERSION = "1.2.0";

    // App features
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = false;
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = false;
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean UNIVERSAL_TVOD = false;

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "";
}


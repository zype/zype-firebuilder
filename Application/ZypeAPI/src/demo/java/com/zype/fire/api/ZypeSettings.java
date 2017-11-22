package com.zype.fire.api;

import android.content.Context;

/**
 * Created by Evgeny Cherkasov on 17.04.2017.
 */

public class ZypeSettings {
    // Zype app key
    public static final String APP_KEY = "iBjj-jnjT12tQGmyeOR9op8_RjsGmT1Nq5OEETkbxOXnNmDE1m9MrB0wpti0__9l";
    // OAuth credentials
    public static final String CLIENT_ID = "62f1d247b4c5e77b6111d9a9ed8b3b64bab6be66cc8b7513a928198083cd1c72";
    public static final String CLIENT_SECRET = "06f45687da00bbe3cf51dddc7dbd7a288d1c852cf0b9a6e76e25bb115dcf872c";
    // Playlist
    public static final String ROOT_PLAYLIST_ID = "577e65c85577de0d1000c1ee";
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";

    // Template version
    public static final String TEMPLATE_VERSION = "1.4.0";

    // App features
//    public static final boolean BACKGROUND_PLAYBACK_ENABLED = false;
//    public static final boolean DOWNLOADS_ENABLED = true;
//    public static final boolean DOWNLOADS_ENABLED_FOR_GUESTS = true;
//    public static final boolean SHARE_VIDEO_ENABLED = false;
//    public static final boolean THEME_LIGHT = true;
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = false;
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = true;
    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = true;
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean UNIVERSAL_TVOD = true;

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "2:QwDSMRWE6-QskpjXS0LDjFOqu9jWadiFvygv15Onw2Xt917Bm-9vHXUfwWmTFyKo:Lntf8F3vQfgCgGjTIh7kVw==";
}


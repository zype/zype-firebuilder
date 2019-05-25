package com.zype.fire.api;

import android.content.Context;

/**
 * Created by Evgeny Cherkasov on 17.04.2017.
 */

public class ZypeSettings {
    // Zype app key
    public static final String APP_KEY = "cGNGmy8RznIaeJy_9uw8WJjSSnSf0NiBTl5v80bs4yoaAQAoK9RReHR27vLgz-EZ";
    // OAuth credentials
    public static final String CLIENT_ID = "1296f3f31939fa7bc157ba9098452d7980876b922952b049b30a2748023c89f2";
    // Playlist
    public static final String ROOT_PLAYLIST_ID = "593065d9345e3b150500090f";

    public static final String FAVORITES_PLAYLIST_ID = "Favorites";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";
    public static final String ROOT_FAVORITES_PLAYLIST_ID = "RootFavorites";
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";
    public static final String ROOT_SLIDERS_PLAYLIST_ID = "RootSliders";

    // Template version
    public static final String TEMPLATE_VERSION = "1.8.0";

    public static final boolean EPG_ENABLED = false;


    // Features
    public static final boolean ACCOUNT_CREATION_TOS = false;
    public static final boolean DEVICE_LINKING = false;
    public static final boolean FAVORITES_VIA_API = false;

    // Monetization
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = false;
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean NATIVE_TVOD = false;
    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = false;
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean UNIVERSAL_TVOD = false;

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "2:QwDSMRWE6-QskpjXS0LDjFOqu9jWadiFvygv15Onw2Xt917Bm-9vHXUfwWmTFyKo:Lntf8F3vQfgCgGjTIh7kVw==";
}

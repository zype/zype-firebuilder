package com.zype.fire.api;

/**
 * Created by Evgeny Cherkasov on 17.04.2017.
 */

public class ZypeSettings {
    // Zype app key
    public static final String APP_KEY = "meUhMQrojuKOakuvhNLNz80qQ6tkSqe_jceQD-BhXh_qDVDSXBU9PvvbyUIRw55S";
    // OAuth credentials
    public static final String CLIENT_ID = "76c1b7fcc367bb65a76ee2ef5f82ea414c0aa871bbc7fadeffe8f72c1d3cabfe";
    //    public static final String CLIENT_SECRET = "<CLIENT_SECRET>";
    // Playlist
    public static final String ROOT_PLAYLIST_ID = "5cdf40fdfb7282a31f19e3e2";

    public static final String FAVORITES_PLAYLIST_ID = "Favorites";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";
    public static final String ROOT_FAVORITES_PLAYLIST_ID = "RootFavorites";
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";
    public static final String ROOT_SLIDERS_PLAYLIST_ID = "RootSliders";

    // Template version
    public static final String TEMPLATE_VERSION = "1.8.0";

    public static final boolean EPG_ENABLED = true;

    // Features
    public static final boolean DEVICE_LINKING = Boolean.valueOf("false");
    public static final boolean FAVORITES_VIA_API = Boolean.valueOf("true");
    public static final boolean ACCOUNT_CREATION_TOS = false;

    // Monetization
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = Boolean.valueOf("false");
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = Boolean.valueOf("false");
    public static final boolean NATIVE_TVOD = Boolean.valueOf("false");

    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = Boolean.valueOf("false");
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = Boolean.valueOf("true");
    public static final boolean UNIVERSAL_TVOD = Boolean.valueOf("false");

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "";
}


package com.zype.fire.api;

import android.content.Context;

/**
 * Created by Evgeny Cherkasov on 17.04.2017.
 */

public class ZypeSettings {
//    // Zype app key
//    public static final String APP_KEY = "vTlJuVm2hLnXui5eagy9cxOqWoZNijfGrBmdY9q-64vQEoYufrFBpQeu80vVzBR8";
//    // OAuth credentials
//    public static final String CLIENT_ID = "a5e203a720af81c0cf0d5e22cfc9fb1b490b2cfe0d71d51e46995751e3f4cc6f";
//    public static final String CLIENT_SECRET = "31e6f8cbb887ba5dd6b6cf9dfb3bb89e81ae298b58455663a9f50c4609c2466d ";
    // Zype app key
    public static final String APP_KEY = "iBjj-jnjT12tQGmyeOR9op8_RjsGmT1Nq5OEETkbxOXnNmDE1m9MrB0wpti0__9l";
    // OAuth credentials
    public static final String CLIENT_ID = "62f1d247b4c5e77b6111d9a9ed8b3b64bab6be66cc8b7513a928198083cd1c72";
    public static final String CLIENT_SECRET = "06f45687da00bbe3cf51dddc7dbd7a288d1c852cf0b9a6e76e25bb115dcf872c";
    // Playlist
    public static final String ROOT_PLAYLIST_ID = "577e65c85577de0d1000c1ee";

    public static final String FAVORITES_PLAYLIST_ID = "Favorites";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";
    public static final String ROOT_FAVORITES_PLAYLIST_ID = "RootFavorites";
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";

    // Template version
    public static final String TEMPLATE_VERSION = "1.5.0";

    // Features
    public static final boolean DEVICE_LINKING = true;
    public static final boolean FAVORITES_VIA_API = true;

    // Monetization
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = false;
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = false;
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = true;
    public static final boolean UNIVERSAL_TVOD = true;

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "2:QwDSMRWE6-QskpjXS0LDjFOqu9jWadiFvygv15Onw2Xt917Bm-9vHXUfwWmTFyKo:Lntf8F3vQfgCgGjTIh7kVw==";
}


package com.zype.fire.api;

import android.content.Context;

/**
 * Created by Evgeny Cherkasov on 17.04.2017.
 */

public class ZypeSettings {
    // Zype app key
    public static final String APP_KEY = "C0zChR4FyNVixEOsyJCi1_HLdmklFlj-uUbOAamfEq0f7ML7aJkekOFIbPmbiS9j";
    // OAuth credentials
    public static final String CLIENT_ID = "6f861db9b340975edddd68d853069e095a37a1609ca328b96b35b17be68b0470";
    // Playlist
    public static final String ROOT_PLAYLIST_ID = "5c91b0d2be3b9e14f51c450e";

    public static final String FAVORITES_PLAYLIST_ID = "Favorites";
    public static final String MY_LIBRARY_PLAYLIST_ID = "MyLibrary";
    public static final String ROOT_FAVORITES_PLAYLIST_ID = "RootFavorites";
    public static final String ROOT_MY_LIBRARY_PLAYLIST_ID = "RootMyLibrary";
    public static final String ROOT_SLIDERS_PLAYLIST_ID = "RootSliders";

    // Template version
    public static final String TEMPLATE_VERSION = "1.8.0";

    public static final boolean ACCOUNT_NAV_BUTTON_DISPLAY = true;
    public static final boolean EPG_ENABLED = false;
    public static final boolean DETAIL_BACKGROUND_IMAGE = false;
    /* Define the app theme
     *
     * The default theme is dark. To make the app theme light, set this flag to 'true'.
     * This flag controls the following:
     * - Text color on Terms and Privacy Policy screen
    */
    public static final boolean LIGHT_THEME = false;
    public static final boolean SHOW_EPISODE_NUMBER = false;
    public static final boolean SHOW_TITLE = false;
    public static final boolean SHOW_LEFT_MENU = true;
    public static final boolean SHOW_SEARCH_ICON = true;
    public static final boolean SETTINGS_PLAYLIST_ENABLED = true;
    public static final boolean SHOW_MENU_ICON = true;
    public static final boolean TERMS_NAV_BUTTON_DISPLAY = true;
    public static final boolean UNLOCK_TRANSPARENT = false;

    public static final String TERMS_CONDITION_URL  = "https://www.zype.com/";

    // Features
    public static final boolean ACCOUNT_CREATION_TOS = false;
    public static final boolean DEVICE_LINKING = false;
    public static final boolean FAVORITES_VIA_API = false;
    public static final boolean LIBRARY_ENABLED = false;

    // Monetization
    public static final boolean MARKETPLACE_CONNECT_SVOD = false;
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = false;
    public static final boolean NATIVE_TVOD = false;
    public static final boolean PLAYLIST_PURCHASE_ENABLED = false;
    public static final boolean SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED = false;
    public static final boolean UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    public static final boolean UNIVERSAL_TVOD = false;

    /**
     * Amazon shared key is required for native subscription feature. It is used in request to Zype Bifrost
     * service for verifying subscription.
     */
    public static final String AMAZON_SHARED_KEY = "2:QwDSMRWE6-QskpjXS0LDjFOqu9jWadiFvygv15Onw2Xt917Bm-9vHXUfwWmTFyKo:Lntf8F3vQfgCgGjTIh7kVw==";
}

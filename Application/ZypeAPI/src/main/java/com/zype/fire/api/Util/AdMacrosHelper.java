package com.zype.fire.api.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 03.08.2017.
 */

public class AdMacrosHelper {
    private static final String APP_BUNDLE = "[app_bundle]";
    private static final String APP_DOMAIN = "[app_domain]";
    private static final String APP_ID = "[app_id]";
    private static final String APP_NAME = "[app_name]";
    private static final String DEVICE_TYPE = "[device_type]";
    private static final String DEVICE_IFA = "[device_ifa]";
    private static final String DEVICE_MAKE = "[device_make]";
    private static final String DEVICE_MODEL = "[device_model]";
    private static final String UUID = "[uuid]";
    private static final String VPI = "[vpi]";

    private static final String PREFERENCE_UUID = "ZypeUUID";

    public static String updateAdTagParameters(Context context, String tag) {
        Map<String, String> values = getValues(context.getApplicationContext());
        String result = tag;
        for (String key : values.keySet()) {
            String value = values.get(key);
            if (value != null) {
                if (result.contains(key)) {
                    result = result.replace(key, Uri.encode(value));
                }
            }
        }
        return result;
    }

    private static Map<String, String> getValues(Context context) {
        Map<String, String> result = new HashMap<>();
        ApplicationInfo appInfo = context.getApplicationContext().getApplicationInfo();
        // App data
        result.put(APP_BUNDLE, appInfo.packageName);
        result.put(APP_DOMAIN, appInfo.packageName);
        result.put(APP_ID, appInfo.packageName);
        result.put(APP_NAME, (appInfo.labelRes == 0) ? appInfo.nonLocalizedLabel.toString() : context.getString(appInfo.labelRes));
        // Advertising ID
        String advertisingID = getAdvertisingId(context);
        result.put(DEVICE_IFA, advertisingID);
        // Device data
        result.put(DEVICE_MAKE, Build.MANUFACTURER);
        result.put(DEVICE_MODEL, Build.MODEL);
        // Default device type is '7' (set top box device)
        // TODO: Find how to detect device type - 3 (smart TV) or 7 (set top box)
        result.put(DEVICE_TYPE, "7");
        // Default VPI is 'MP4'
        result.put(VPI, "MP4");
        // UUID us the same as Advertising id
        result.put(UUID, advertisingID);
        return result;
    }

    /**
     * Get advertising id on Fire TV according to this guide https://developer.amazon.com/public/solutions/devices/fire-tv/docs/fire-tv-advertising-id.
     * If it is not available then generate random UUID and save it to preferences for further usage.
     * TODO: Get advertising id on Android TV devices from Google Play services
     *
     * @param context
     * @return
     */
    public static String getAdvertisingId(Context context) {
        ContentResolver cr = context.getContentResolver();
        String result = Settings.Secure.getString(cr, "advertising_id");
        if (TextUtils.isEmpty(result)) {
            // Try to get it from preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            result = prefs.getString(PREFERENCE_UUID, null);
            if (TextUtils.isEmpty(result)) {
                // Generate random UUID
                result = java.util.UUID.randomUUID().toString();
                // Save to preferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREFERENCE_UUID, result);
                editor.apply();
            }
        }
        return result;
    }
}

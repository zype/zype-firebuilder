package com.zype.fire.api.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

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
        // Advertizing ID
        // Get advertising id on Fire TV according to this guide https://developer.amazon.com/public/solutions/devices/fire-tv/docs/fire-tv-advertising-id
        // TODO: Get advertising id on Android TV devices from Google Play services
        ContentResolver cr = context.getContentResolver();
        String advertisingID = Settings.Secure.getString(cr, "advertising_id");
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

}

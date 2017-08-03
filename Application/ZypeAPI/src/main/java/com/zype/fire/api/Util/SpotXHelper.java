package com.zype.fire.api.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.zype.fire.api.BuildConfig;
import com.zype.fire.api.Model.Advertising;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 03.08.2017.
 */

public class SpotXHelper {
    private static final String APP_BUNDLE = "app[bundle]";
    private static final String APP_ID = "app[id]";
    private static final String APP_NAME = "app[name]";
    private static final String DEVICE_DEVICE_TYPE = "device[devicetype]";
    private static final String DEVICE_IFA = "device[ifa]";
    private static final String DEVICE_MAKE = "device[make]";
    private static final String DEVICE_MODEL = "device[model]";
    private static final String UUID = "uuid";
    private static final String VPI = "VPI";

    private static final String REPLACE_VALUE = "REPLACE_ME";

    public static String addSpotXParameters(Context context, String tag) {
        Map<String, String> spotXParameters = getSpotXParameters(context.getApplicationContext());
        Map<String, String> queryParameters;
        try {
            queryParameters = getUrlQueryParameters(tag);
        }
        catch (MalformedURLException e) {
            queryParameters = new HashMap<>();
            e.printStackTrace();
        }
        final Uri uri = Uri.parse(tag);
        final Uri.Builder resultUri = uri.buildUpon().clearQuery();
        for (String paramName : queryParameters.keySet()) {
            String value = queryParameters.get(paramName);
            if ((value == null || value.equals(REPLACE_VALUE))
                    && spotXParameters.containsKey(paramName)) {
                value = Uri.encode(spotXParameters.get(paramName));
            }
            resultUri.appendQueryParameter(paramName, value);
        }
        return resultUri.build().toString();
    }

    private static Map<String, String> getSpotXParameters(Context context) {
        Map<String, String> result = new HashMap<>();
        ApplicationInfo appInfo = context.getApplicationContext().getApplicationInfo();
        // App data
        result.put(APP_BUNDLE, appInfo.packageName);
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
        result.put(DEVICE_DEVICE_TYPE, "7");
        // Default VPI is 'MP4'
        result.put(VPI, "MP4");
        // UUID us the same as Advertising id
        result.put(UUID, advertisingID);
        return result;
    }

    private static Map<String, String> getUrlQueryParameters(String urlString) throws
            MalformedURLException {

        Map<String, String> queryParams = new HashMap<>();
        URL url = new URL(urlString);
        String query = url.getQuery();
        String[] strParams = query.split("&");

        for (String param : strParams) {
            String[] split = param.split("=");
            // Get the parameter name.
            if (split.length > 0) {
                String name = split[0];
                // Get the parameter value.
                if (split.length > 1) {
                    String value = split[1];
                    queryParams.put(name, value);
                }
                // If there is no value just put an empty string as placeholder.
                else {
                    queryParams.put(name, "");
                }
            }
        }

        return queryParams;
    }

}

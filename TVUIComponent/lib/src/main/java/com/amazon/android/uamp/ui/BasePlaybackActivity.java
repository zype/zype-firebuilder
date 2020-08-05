package com.amazon.android.uamp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.zype.fire.api.IZypeApi;
import com.zype.fire.api.Util.AdMacrosHelper;

import java.util.HashMap;

public class BasePlaybackActivity extends Activity {
    private static final String APP_BUNDLE = "app_bundle";
    private static final String APP_DOMAIN = "app_domain";
    private static final String APP_ID = "app_id";
    private static final String APP_NAME = "app_name";
    private static final String DEVICE_TYPE = "device_type";
    private static final String DEVICE_IFA = "device_ifa";
    private static final String DEVICE_MAKE = "device_make";
    private static final String DEVICE_MODEL = "device_model";
    private static final String UUID = "uuid";
    private static final String VPI = "vpi";

    public HashMap<String, String> getValues() {
        HashMap<String, String> params = new HashMap<>();

        ApplicationInfo appInfo = getApplication().getApplicationInfo();
        // App data
//        params.put(APP_BUNDLE, appInfo.packageName);
        params.put(APP_BUNDLE, "B07JZHXT64");
        params.put(APP_DOMAIN, appInfo.packageName);
        params.put(APP_ID, appInfo.packageName);
        params.put(APP_NAME, (appInfo.labelRes == 0) ? appInfo.nonLocalizedLabel.toString() : getString(appInfo.labelRes));
        // Advertizing ID
        String advertisingID = AdMacrosHelper.getAdvertisingId(this);
        params.put(DEVICE_IFA, advertisingID);
        // Device data
        params.put(DEVICE_MAKE, Build.MANUFACTURER);
        params.put(DEVICE_MODEL, Build.MODEL);
        // Default device type is '7' (set top box device)
        params.put(DEVICE_TYPE, "7");
        // Default VPI is 'MP4'
        params.put(VPI, "MP4");
        // UUID us the same as Advertising id
        params.put(UUID, advertisingID);
        return params;
    }

    public String getUserAgent(Context context, String applicationName) {
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return IZypeApi.HEADER_USER_AGENT + applicationName + "/" + versionName;
    }

}

package com.zype.fire.api.Util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

public class FileHelper {
    public static String readAssetsFile(Context context, int fileResId) {
        String result;

        AssetManager am = context.getAssets();
        byte[] buffer = null;
        InputStream is;
        try {
            is = context.getResources().openRawResource(fileResId);
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        result = new String(buffer);
        return result;
    }
}

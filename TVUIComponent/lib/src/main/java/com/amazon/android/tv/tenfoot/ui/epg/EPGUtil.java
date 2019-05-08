package com.amazon.android.tv.tenfoot.ui.epg;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class EPGUtil {
    private static final String TAG = "EPGUtil";
    private static final DateTimeFormatter dtfShortTime = DateTimeFormat.forPattern("h:mm a");

    public static String getShortTime(long timeMillis) {
        return dtfShortTime.print(timeMillis);
    }

    public static String getWeekdayName(long dateMillis) {
        LocalDate date = new LocalDate(dateMillis);
        return date.dayOfWeek().getAsText();
    }

    public static String getEPGdayName(long dateMillis) {
        LocalDate date = new LocalDate(dateMillis);
        return date.monthOfYear().getAsText() + ", " + date.getDayOfMonth();

    }

    public static void loadImageInto(Context context, String url, int width, int height, SimpleTarget<Bitmap> simpleTarget) {
        if (TextUtils.isEmpty(url))
            return;

        Glide.with(context)
                .load(url)
                .asBitmap()
                .centerCrop()
                //.into(width, height);
                .into(simpleTarget);
    }


}

package se.kmdev.tvepg.epg.misc;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by Kristoffer.
 */
public class EPGUtil {
  private static final String TAG = "EPGUtil";
  private static final DateTimeFormatter dtfShortTime = DateTimeFormat.forPattern("HH:mm");

  public static String getShortTime(long timeMillis) {
    return dtfShortTime.print(timeMillis);
  }

  public static String getWeekdayName(long dateMillis) {
    LocalDate date = new LocalDate(dateMillis);
    return date.dayOfWeek().getAsText();
  }

  public static String getEPGdayName(long dateMillis) {
    LocalDate date = new LocalDate(dateMillis);
    return date.dayOfWeek().getAsShortText() + " " + date.getDayOfMonth() + "/" + date.getMonthOfYear();
  }

  public static void loadImageInto(Context context, String url, int width, int height, SimpleTarget<Bitmap> simpleTarget) {
    Glide.with(context)
        .load(url)
        .asBitmap()
        .centerCrop()
        //.into(width, height);
        .into(simpleTarget);
  }


}

/**
 * Copyright 2020 Zype Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.zype.fire.analytics.segment;


import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.amazon.analytics.AnalyticsTags;
import com.amazon.analytics.CustomAnalyticsTags;
import com.amazon.analytics.IAnalytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.zype.fire.api.ZypeSettings;

import org.w3c.dom.Text;

/**
 * An implementation of the Segment analytics
 *
 */
public class SegmentAnalytics implements IAnalytics {

    private static final String TAG = SegmentAnalytics.class.getSimpleName();

    /**
     * Name used for implementation creator registration to Module Manager.
     */
    static final String IMPL_CREATOR_NAME = SegmentAnalytics.class.getSimpleName();

    private CustomAnalyticsTags mCustomTags = new CustomAnalyticsTags();

    /**
     * Shows if the video is playing or not.
     * Updated on ACTION_PLAYBACK_STARTED and ACTION_PLAYBACK_ENDED events.
     */
    private boolean isPlaying = false;

    private long previousPlaybackPosition = -1;

    private Context context;

    /**
     * {@inheritDoc}
     *
     * @param context The application context.
     */
    @Override
    public void configure(Context context) {

        this.context = context.getApplicationContext();

        mCustomTags.init(this.context, R.string.segment_analytics_custom_tags);

        // Create an analytics client with the given context and Segment write key.
        Analytics analytics = new Analytics.Builder(this.context,
                ZypeSettings.SEGMENT_ANALYTICS_WRITE_KEY)
                .trackApplicationLifecycleEvents()
//                .recordScreenViews()
                .logLevel(Analytics.LogLevel.VERBOSE)
                .build();

        // Set the initialized instance as a globally accessible instance.
        Analytics.setSingletonInstance(analytics);

        Log.d(TAG, "Configuration done.");
    }

    /**
     * {@inheritDoc}
     *
     * @param activity The activity to log.
     * @param active   True if data collecting should be active; False if collecting should
     *                 be paused.
     */
    @Override
    public void collectLifeCycleData(Activity activity, boolean active) {
        Log.d(TAG, "Collecting life cycle data for activity: " + activity.toString() + ", active: "
                + active);
    }

    /**
     * {@inheritDoc}
     *
     * @param data Map of Strings to Objects that represent data that is necessary.
     */
    @Override
    public void trackAction(HashMap<String, Object> data) {

        String action = (String) data.get(AnalyticsTags.ACTION_NAME);

        HashMap<String, Object> attributes =
                (HashMap<String, Object>) data.get(AnalyticsTags.ATTRIBUTES);

        Log.d(TAG, "trackAction(): action=" + mCustomTags.getCustomTag(action));
//        Log.d(TAG, "trackAction(): attributes=" + String.valueOf(mCustomTags.getCustomTags(attributes)));

        Properties properties = new Properties();
        if (actionIsPlayerEvent(action)) {
            properties = attributesToProperties(attributes);
        }

        if (action.equals(AnalyticsTags.ACTION_PLAY_VIDEO)) {
            isPlaying = false;
        }
        switch (action) {
            case AnalyticsTags.ACTION_PLAYBACK_STARTED: {
                if (!isPlaying) {
                    isPlaying = true;
//                    Properties properties = attributesToProperties(attributes);
                    Analytics.with(context).track("Video Content Started", properties);
                    Analytics.with(context).track("Video Playback Started", properties);
                    Log.d(TAG, "trackAction(): action is tracked");
                }
                break;
            }
            case AnalyticsTags.ACTION_PLAYBACK: {
                if (isPlaying) {
//                    Properties properties = attributesToProperties(attributes);
                    String videoId = properties.getString("videoId");
                    if (TextUtils.isEmpty(videoId)) {
                        Log.d(TAG, "trackAction(): action is not tracked");
                        break;
                    }
                    Analytics.with(context).track("Video Content Playing", properties);

                    long duration = properties.getLong("videoContentDuration", 0);
                    long position = properties.getLong("videoContentPosition", 0);
                    if (previousPlaybackPosition < 3 && position >=3) {
                        Analytics.with(context).track("Video Content Started (after 3 seconds)", properties);
                    }
                    else if (previousPlaybackPosition < duration * 0.25 && position >= duration * 0.25) {
                        Analytics.with(context).track("Video Content Completed 25 percent", properties);
                    }
                    else if (previousPlaybackPosition < duration * 0.5 && position >= duration * 0.5) {
                        Analytics.with(context).track("Video Content Completed 50 percent", properties);
                    }
                    else if (previousPlaybackPosition < duration * 0.75 && position >= duration * 0.75) {
                        Analytics.with(context).track("Video Content Completed 75 percent", properties);
                    }
                    previousPlaybackPosition = position;
                    Log.d(TAG, "trackAction(): action is tracked");
                }
                break;
            }
            case AnalyticsTags.ACTION_PLAYBACK_FINISHED: {
                isPlaying = false;
//                Properties properties = attributesToProperties(attributes);
                long duration = properties.getLong("videoContentDuration", 0);
                long position = properties.getLong("videoContentPosition", 0);
                if (duration - position > 1) {
                    Log.d(TAG, "trackAction(): action is not tracked");
                    break;
                }
                Analytics.with(context).track("Video Content Completed", properties);
                Analytics.with(context).track("Video Playback Completed", properties);
                Log.d(TAG, "trackAction(): action is tracked");
                break;
            }
            case AnalyticsTags.ACTION_AUTOPLAY_FINISHED:
                Analytics.with(context).track("Exiting Initial Stream to Homepage", properties);
                break;
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PLAY:
                isPlaying = true;
                Analytics.with(context).track("Video Playback Resumed", properties);
                break;
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PAUSE:
                isPlaying = false;
                Analytics.with(context).track("Video Playback Paused", properties);
                break;
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_FF:
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_REWIND:
                Analytics.with(context).track("Video Playback Seek Started", properties);
                Analytics.with(context).track("Video Playback Seek Completed", properties);
                break;
            case AnalyticsTags.ACTION_ERROR:
                Analytics.with(context).track("Video Player Error", properties);
                break;
        }
//        }
    }

    /**
     * {@inheritDoc}
     *
     * @param screen The screen that is displayed.
     */
    @Override
    public void trackState(String screen) {

        Log.d(TAG, "Tracking screen " + screen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trackCaughtError(String errorMessage, Throwable t) {

        Log.e(TAG, errorMessage, t);
    }


    private Properties attributesToProperties(Map<String, Object> attributes) {
        Properties properties = new Properties();

//        String sessionId = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_SESSION_ID);
//        properties.putValue("session_id", sessionId);

        String contentCmsCategory = "null";
        properties.putValue("contentCmsCategory", contentCmsCategory);

        String adType = "null";
        properties.putValue("Ad Type", adType);

        String contentShownOnPlatform = "ott";
        properties.putValue("contentShownOnPlatform", contentShownOnPlatform);

//        String streamingDevice = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_DEVICE);
        String streamingDevice = Build.MANUFACTURER + " " + Build.MODEL;
        properties.putValue("streaming_device", streamingDevice);

        String videoAccountId = "416418724";
        properties.putValue("videoAccountId", videoAccountId);

        String videoAccountName = "People";
        properties.putValue("videoAccountName", videoAccountName);

        String videoAdDuration = "null";
        properties.putValue("videoAdDuration", videoAdDuration);

        String videoAdVolume = "null";
        properties.putValue("videoAdVolume", videoAdVolume);

        // total_length
        long duration = (Long) attributes.get(AnalyticsTags.ATTRIBUTE_VIDEO_DURATION);
        properties.putValue("videoContentDuration", duration);

        // position
        long position = (Long) attributes.get(AnalyticsTags.ATTRIBUTE_VIDEO_CURRENT_POSITION) / 1000;
        properties.putValue("videoContentPosition", position);

        long percent = (duration != 0) ? position * 100 / duration : 0;
        properties.putValue("videoContentPercentComplete", percent);

        String videoCreatedAt = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_CREATED_AT);
        properties.putValue("videoCreatedAt", videoCreatedAt);

        String videoFranchise = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_SERIES_ID);
        if (TextUtils.isEmpty(videoFranchise)) {
            videoFranchise = "null";
        }
        properties.putValue("videoFranchise", videoFranchise );

        // asset_id
        String videoId = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_VIDEO_ID);
        properties.putValue("videoId", videoId);

        // title
        String title = (String) attributes.get(AnalyticsTags.ATTRIBUTE_TITLE);
        properties.putValue("videoName", title);

        // airdate
        String airdate = (String) attributes.get(AnalyticsTags.ATTRIBUTE_AIRDATE);
        if (TextUtils.isEmpty(airdate)) {
            airdate = "null";
        }
        properties.putValue("videoPublishedAt", airdate);

        String videoSyndicate = "null";
        properties.putValue("videoSyndicate", videoSyndicate);

        String videoTags = "null";
        properties.putValue("videoTags", videoTags);

        String videoThumbnail = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_THUMBNAIL);
        properties.putValue("videoThumbnail", videoThumbnail);

        String videoUpdatedAt = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_UPDATED_AT);
        properties.putValue("videoUpdatedAt", videoUpdatedAt);

//        String description = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_DESCRIPTION);
//        properties.putValue("description", description);
//
//        String season = (String) attributes.get(AnalyticsTags.ATTRIBUTE_SEASON_NUMBER);
//        properties.putValue("season", season);
//
//        String episode = (String) attributes.get(AnalyticsTags.ATTRIBUTE_EPISODE_NUMBER);
//        properties.putValue("episode", episode);
//
//        String publisher = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_CHANNEL);
//        properties.putValue("publisher", publisher);
//
//        properties.putValue("channel", publisher);
//
//        Boolean livestream = (Boolean) attributes.get(AnalyticsTags.ATTRIBUTE_LIVE_FEED);
//        properties.putValue("livestream", livestream);
//
//        int bitrate = 0;
//        properties.putValue("bitrate", bitrate);
//
//        double framerate = 0;
//        properties.putValue("framerate", framerate);

        Log.d(TAG, "attributesToProperties(): " + properties.toString());
        return properties;
    }

    private boolean actionIsPlayerEvent(String action) {
        switch (action) {
            case AnalyticsTags.ACTION_PLAYBACK_STARTED:
            case AnalyticsTags.ACTION_PLAYBACK:
            case AnalyticsTags.ACTION_PLAYBACK_FINISHED:
            case AnalyticsTags.ACTION_AUTOPLAY_FINISHED:
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PLAY:
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PAUSE:
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_FF:
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_REWIND:
            case AnalyticsTags.ACTION_ERROR:
                return true;
            default:
                return false;
        }
    }
}

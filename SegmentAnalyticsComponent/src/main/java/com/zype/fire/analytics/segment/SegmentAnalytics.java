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
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.amazon.analytics.AnalyticsTags;
import com.amazon.analytics.CustomAnalyticsTags;
import com.amazon.analytics.IAnalytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;

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
                this.context.getString(R.string.segment_analytics_write_key))
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

        if (action.equals(AnalyticsTags.ACTION_PLAY_VIDEO)) {
            isPlaying = false;
        }
        switch (action) {
            case AnalyticsTags.ACTION_PLAYBACK_STARTED: {
                if (!isPlaying) {
                    isPlaying = true;
                    Properties properties = attributesToProperties(attributes);
                    Analytics.with(context).track("Video Content Started", properties);
                    Log.d(TAG, "trackAction(): action is tracked");
                }
                break;
            }
            case AnalyticsTags.ACTION_PLAYBACK: {
                if (isPlaying) {
                    Properties properties = attributesToProperties(attributes);
                    String videoId = properties.getString("videoId");
                    if (TextUtils.isEmpty(videoId)) {
                        Log.d(TAG, "trackAction(): action is not tracked");
                        break;
                    }
                    Analytics.with(context).track("Video Content Playing", properties);
                    Log.d(TAG, "trackAction(): action is tracked");
                }
                break;
            }
            case AnalyticsTags.ACTION_PLAYBACK_FINISHED: {
                isPlaying = false;
                Properties properties = attributesToProperties(attributes);
                long duration = properties.getLong("videoContentDuration", 0);
                long position = properties.getLong("videoContentPosition", 0) / 1000;
                if (duration - position > 1) {
                    Log.d(TAG, "trackAction(): action is not tracked");
                    break;
                }
                Analytics.with(context).track("Video Content Completed", properties);
                Log.d(TAG, "trackAction(): action is tracked");
                break;
            }
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PLAY:
                isPlaying = true;
                break;
            case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PAUSE:
                isPlaying = false;
                break;
            case AnalyticsTags.ACTION_ERROR:
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
        properties.putValue("videoId",
                attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_VIDEO_ID));
        properties.putValue("contentShownOnPlatform",
                attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_DEVICE));
        long duration = (Long) attributes.get(AnalyticsTags.ATTRIBUTE_VIDEO_DURATION);
        properties.putValue("videoContentDuration", duration);
        properties.putValue("videoName",
                attributes.get(AnalyticsTags.ATTRIBUTE_TITLE));
        long position = (Long) attributes.get(AnalyticsTags.ATTRIBUTE_VIDEO_CURRENT_POSITION);
        properties.putValue("videoContentPosition", position);
        long percent = (duration != 0) ? (position / 1000) * 100 / duration : 0;
        properties.putValue("videoContentPercentComplete", percent);
        Log.d(TAG, "attributesToProperties(): " + properties.toString());
        return properties;
    }
}

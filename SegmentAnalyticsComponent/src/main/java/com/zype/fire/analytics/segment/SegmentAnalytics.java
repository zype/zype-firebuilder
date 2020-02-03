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

//    /**
//     * Akamai analytics plugin
//     */
//    private AnalyticsPlugin akamaiPlugin;

    /**
     * Shows if the video is playing or not.
     * Updated on ACTION_PLAYBACK_STARTED and ACTION_PLAYBACK_ENDED events.
     */
    private boolean isPlaying = false;

    /**
     * Shows if the video is seeking.
     * Turned on by ACTION_PLAYBACK_CONTROL_FF or ACTION_PLAYBACK_CONTROL_REWIND event.
     */
    private boolean isSeeking = false;

    /**
     * This flag is set to true on ACTION_PLAY_VIDEO event. The next video related action handler should
     * check this flag and perform setup Akamai plugin before processing the event.
     */
    private boolean isSetupRequired = true;

//    /**
//     * Video attributes used in the Akamai plugin
//     */
//    private String beacon;
//    private String consumerId;
//    private long currentPosition;
//    private String deviceType;
//    private boolean isLive;
//    private String playerId;
//    private String siteId;
//    private long videoDuration;
//    private String videoId;
//    private String videoUrl;
//
//    /**
//     * Akamai analytics dimensions
//     */
//    private static final String DIMENSION_CONSUMER_ID = "consumerId";
//    private static final String DIMENSION_DEVICE_TYPE = "deviceType";
//    private static final String DIMENSION_PLAYER_ID = "playerId";
//    private static final String DIMENSION_SITE_ID = "siteId";
//    private static final String DIMENSION_VIDEO_ID = "videoId";

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
        Analytics analytics = new Analytics.Builder(this.context, "etP6NsW0ahoGMsH7JnoCuCpdd5lFGUio")
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

//        if (!active) {
//            if (akamaiPlugin != null) {
//                akamaiPlugin.handleEnterBackground();
//            }
//        }

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

//        updateVideoAttributes(attributes);

        if (action.equals(AnalyticsTags.ACTION_PLAY_VIDEO)) {
            isPlaying = false;
//            stopVideoTracking();
        }
//        else {
//            if (isSetupRequired) {
//                if (!TextUtils.isEmpty(beacon)) {
//                    setupAkamai();
//                }
//                else {
//                    Log.e(TAG, "trackAction(): Can't handle event. Akamai plugin didn't set up");
//                    return;
//                }
//            }
//            if (isSeeking
//                    && !action.equals(AnalyticsTags.ACTION_PLAYBACK_CONTROL_FF)
//                    && !action.equals(AnalyticsTags.ACTION_PLAYBACK_CONTROL_REWIND)) {
//                isSeeking = false;
//                akamaiPlugin.handleSeekEnd(currentPosition);
//            }
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
//                    stopVideoTracking();
                    break;
                }
//                case AnalyticsTags.ACTION_PLAYBACK_BUFFER_START:
//                    isPlaying = false;
//                    break;
//                case AnalyticsTags.ACTION_PLAYBACK_BUFFER_END:
//                    isPlaying = true;
//                    break;
                case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PLAY:
                    isPlaying = true;
                    break;
                case AnalyticsTags.ACTION_PLAYBACK_CONTROL_PAUSE:
                    isPlaying = false;
                    break;
//                case AnalyticsTags.ACTION_PLAYBACK_CONTROL_FF:
//                case AnalyticsTags.ACTION_PLAYBACK_CONTROL_REWIND:
//                    isSeeking = true;
//                    akamaiPlugin.handleSeekStart(currentPosition);
//                    break;
                case AnalyticsTags.ACTION_ERROR:
//                    akamaiPlugin.handleError((String) attributes.get(AnalyticsTags.ATTRIBUTE_ERROR_MSG));
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


    private void stopVideoTracking() {
        isSetupRequired = true;
//        akamaiPlugin = null;
        clearVideoAttributes();
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

    // //////////
    // Akamai plugin
    //

//    private void setupAkamai() {
//        akamaiPlugin = new AnalyticsPlugin(context, beacon);
////        if (ContentBrowser.getInstance(this).isUserLoggedIn()) {
////            AnalyticsPlugin.setViewerId(Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_ID));
////        }
//        akamaiPlugin.handleSessionInit(new PluginCallBacks() {
//            @Override
//            public float streamHeadPosition() {
//                return currentPosition;
//            }
//
//            @Override
//            public float streamLength() {
//                return videoDuration;
//            }
//
//            @Override
//            public float getFps() {
//                return 0;
//            }
//
//            @Override
//            public String streamURL() {
//                return videoUrl;
//            }
//
//            @Override
//            public boolean isLive() {
//                return isLive;
//            }
//
//            @Override
//            public String videoSize() {
////                return mPlayer.getCurrentVideoWidth() + "x" + mPlayer.getCurrentVideoHeight();
//                return "";
//            }
//
//            @Override
//            public String viewSize() {
////                return mVideoView.getWidth() + "x" + mVideoView.getHeight();
//                return "";
//            }
//
//            @Override
//            public long bytesLoaded() {
//                return 0;
//            }
//
//            @Override
//            public int droppedFrames() {
//                return 0;
//            }
//
//            @Override
//            public boolean isPlaying() {
//                return isPlaying;
//            }
//        }, false);
//        akamaiPlugin.setData(DIMENSION_CONSUMER_ID, consumerId);
//        akamaiPlugin.setData(DIMENSION_DEVICE_TYPE, deviceType);
//        akamaiPlugin.setData(DIMENSION_PLAYER_ID, playerId);
//        akamaiPlugin.setData(DIMENSION_SITE_ID, siteId);
//        akamaiPlugin.setData(DIMENSION_VIDEO_ID, videoId);
//
//        isSetupRequired = false;
//
//        Log.d(TAG, "setupAkamai(): Completed");
//    }

    private void clearVideoAttributes() {
//        beacon = "";
//        consumerId = "";
//        currentPosition = 0;
//        deviceType = "";
//        isLive = false;
//        playerId = "";
//        siteId = "";
//        videoDuration = 0;
//        videoId = "";
//        videoUrl = "";
    }

    private void updateVideoAttributes(HashMap<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            switch (entry.getKey()) {
                case AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_BEACON:
//                    beacon = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_BEACON);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_CONSUMER_ID:
//                    consumerId = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_CONSUMER_ID);
                    break;
                case AnalyticsTags.ATTRIBUTE_VIDEO_CURRENT_POSITION:
//                    currentPosition = (Long) attributes.get(AnalyticsTags.ATTRIBUTE_VIDEO_CURRENT_POSITION);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_DEVICE:
//                    deviceType = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_DEVICE);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_LIVE:
//                    isLive = (Boolean) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_LIVE);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_PLAYER_ID:
//                    playerId = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_PLAYER_ID);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_SITE_ID:
//                    siteId = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_SITE_ID);
                    break;
                case AnalyticsTags.ATTRIBUTE_VIDEO_DURATION:
//                    videoDuration = (Long) attributes.get(AnalyticsTags.ATTRIBUTE_VIDEO_DURATION);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_VIDEO_ID:
//                    videoId = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_VIDEO_ID);
                    break;
                case AnalyticsTags.ATTRIBUTE_CONTENT_VIDEO_URL:
//                    videoUrl = (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_VIDEO_URL);
                    break;
            }
        }
    }
}

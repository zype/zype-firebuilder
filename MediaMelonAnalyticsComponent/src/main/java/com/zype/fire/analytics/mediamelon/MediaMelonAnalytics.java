/**
 * Copyright 2018 Zype Inc. or its affiliates. All Rights Reserved.
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
package com.zype.fire.analytics.mediamelon;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;

import com.amazon.analytics.AnalyticsTags;
import com.amazon.analytics.CustomAnalyticsTags;
import com.amazon.analytics.IAnalytics;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.mediamelon.smartstreaming.MMQBRMode;
import com.mediamelon.smartstreaming.MMSmartStreamingExo2_6;
import com.mediamelon.smartstreaming.MMSmartStreamingInitializationStatus;
import com.mediamelon.smartstreaming.MMSmartStreamingObserver;

import static com.amazon.analytics.AnalyticsTags.ATTRIBUTE_PLAYER;
import static com.amazon.analytics.AnalyticsTags.ATTRIBUTE_VIDEO_ID;

/**
 * An implementation of the Akamai analytics
 *
 */
public class MediaMelonAnalytics implements IAnalytics, MMSmartStreamingObserver {

    private static final String TAG = MediaMelonAnalytics.class.getSimpleName();

    /**
     * Name used for implementation creator registration to Module Manager.
     */
    static final String IMPL_CREATOR_NAME = MediaMelonAnalytics.class.getSimpleName();

    private CustomAnalyticsTags mCustomTags = new CustomAnalyticsTags();

    /**
     * The MediaMelon must be initialized before start tracking actions.
     * We can't do this in the 'configure()' method because of missing required data.
     * So the initialization is made in 'trackAction()'.
     */
    private boolean isInitialized = false;

    private Context context;

    /**
     * {@inheritDoc}
     *
     * @param context The application context.
     */
    @Override
    public void configure(Context context) {

        this.context = context.getApplicationContext();

        mCustomTags.init(context, R.string.mediamelon_analytics_custom_tags);

        MMSmartStreamingExo2_6.enableLogTrace(true);
        isInitialized = MMSmartStreamingExo2_6.getRegistrationStatus();

        Log.d(TAG, "Configuration done.");
    }

    private void init(HashMap<String, Object> attributes) {
        if (!isInitialized) {
            MMSmartStreamingExo2_6.registerMMSmartStreaming(
                    "ExoPlayer_" + ExoPlayerLibraryInfo.VERSION,
                    context.getString(R.string.mediamelon_customer_id),
                    (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_CONSUMER_ID),       // SubscriberId
                    "",         // DomainName
                    "",         // SubscriberType
                    ""          // SubscriberTag
            );
            MMSmartStreamingExo2_6.reportPlayerInfo("MediaMelon", "ExoPlayer/" + ExoPlayerLibraryInfo.VERSION, "1.0");
            MMSmartStreamingExo2_6.getInstance().setContext(context);
            isInitialized = true;
            Log.d(TAG, "init(): MediaMelon initialized");
        }
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
        Log.d(TAG, "collectingLifeCycleData(): Activity: " + activity.toString() + ", active: "
                + active + ". Not implemented.");
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

        Log.d(TAG, "Tracking action " + mCustomTags.getCustomTag(action) + " with attributes: "
                + String.valueOf(mCustomTags.getCustomTags(attributes)));

        switch (action) {
            case AnalyticsTags.ACTION_PLAYBACK_STARTED:
                init(attributes);
                MMSmartStreamingExo2_6.getInstance().initializeSession(
                        (SimpleExoPlayer) attributes.get(ATTRIBUTE_PLAYER),
                        MMQBRMode.QBRModeDisabled,
                        (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_VIDEO_URL),
                        null,
                        "",     // AssetId
                        "",     // AssetName
                        (String) attributes.get(ATTRIBUTE_VIDEO_ID),
                        this
                );
                MMSmartStreamingExo2_6.getInstance().reportCustomMetadata("siteId",
                        (String) attributes.get(AnalyticsTags.ATTRIBUTE_CONTENT_ANALYTICS_SITE_ID));
                MMSmartStreamingExo2_6.getInstance().reportCustomMetadata("subscriptionId",
                        (String) attributes.get(AnalyticsTags.ATTRIBUTE_SUBSCRIPTION_ID));
                MMSmartStreamingExo2_6.getInstance().reportUserInitiatedPlayback();
                break;
            case AnalyticsTags.ACTION_PLAYBACK_FINISHED:
                MMSmartStreamingExo2_6.getInstance().reportPlayerState(false, Player.STATE_ENDED);
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param screen The screen that is displayed.
     */
    @Override
    public void trackState(String screen) {
        Log.d(TAG, "trackState(): Tracking screen " + screen + ". Not implemented.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trackCaughtError(String errorMessage, Throwable t) {
        Log.e(TAG, errorMessage, t);
    }

    @Override
    public void sessionInitializationCompleted(Integer initCmdId, MMSmartStreamingInitializationStatus status, String description) {
        Log.d(TAG,"sessionInitializationCompleted(): Init Cmd Id " + initCmdId + " completed with the status "+ status + description);
    }

}

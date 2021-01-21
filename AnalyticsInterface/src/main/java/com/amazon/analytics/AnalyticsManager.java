/**
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazon.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * This class handles common analytics tasks and tracks Activity lifecycle events.
 */
public class AnalyticsManager implements Application.ActivityLifecycleCallbacks {

  /**
   * DEBUG TAG.
   */
  private static final String TAG = AnalyticsManager.class.getSimpleName();

  /**
   * Analytics intent action constant.
   */
  private static final String ANALYTICS_INTENT_ACTION = "ANALYTICS_INTENT_ACTION";

  /**
   * Analytics intent extra data key constant.
   */
  private static final String ANALYTICS_INTENT_ACTION_DATA = "ANALYTICS_INTENT_ACTION_DATA";
  /**
   * Lock object.
   */
  private static final Object sLock = new Object();
  /**
   * Singleton instance reference.
   */
  private static AnalyticsManager sInstance;
  /**
   * Application context.
   */
  private final Context mAppContext;
  /**
   * Map for Analytics constants.
   */
  private final Map<String, String> mAnalyticsConstantMap = new HashMap<>();

  private Set<IAnalytics> analyticsSet = new HashSet<>();
  /**
   * Local broadcast receiver.
   */
  private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      if (intent.getAction().equals(ANALYTICS_INTENT_ACTION)) {
        Log.v(TAG, "Got Analytics broadcast!!! : " + intent);
        HashMap<String, Object> data = intent.getParcelableExtra
            (ANALYTICS_INTENT_ACTION_DATA);
//                if (mIAnalytics != null) {
//                    mIAnalytics.trackAction(data);
//                }
        for (IAnalytics analytics : analyticsSet) {
          analytics.trackAction(data);
        }
      }
    }
  };
  private String sessionId;

  /**
   * Singleton constructor.
   *
   * @param context Context.
   */
  private AnalyticsManager(Context context) {

    mAppContext = context.getApplicationContext();

    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ANALYTICS_INTENT_ACTION);
    LocalBroadcastManager.getInstance(mAppContext)
        .registerReceiver(mLocalBroadcastReceiver,
            intentFilter);

    ExtraContentAttributes.init(context);
  }

  /**
   * Singleton get instance method.
   *
   * @param context Context.
   * @return Analytics manager singleton instance.
   */
  public static AnalyticsManager getInstance(Context context) {

    synchronized (sLock) {
      if (sInstance == null) {
        if (context == null) {
          return null;
        }
        sInstance = new AnalyticsManager(context.getApplicationContext());
      }
      return sInstance;
    }
  }

  /**
   * Get Activity name, static internal method.
   *
   * @param activity Activity reference.
   * @return Name of the Activity.
   */
  private static String getActivityName(Activity activity) {

    String activityName = activity.getLocalClassName();
    return getExtension(activityName);
  }

  /**
   * Get extension of a string.
   *
   * @param value Provided string.
   * @return Return the extension of a string.
   */
  private static String getExtension(String value) {

    int dotPos = value.lastIndexOf(".");
    if (dotPos == -1) {
      dotPos = 0;
    } else {
      // Skip dot.
      dotPos++;
    }
    return value.substring(dotPos, value.length());
  }

  /**
   * Set {@link IAnalytics} interface.
   *
   * @param iAnalytics Analytics interface.
   */
  public void setAnalyticsInterface(IAnalytics iAnalytics) {

//        if (mIAnalytics == null) {
//            mIAnalytics = iAnalytics;
//            mIAnalytics.configure(mAppContext);
//        }
    analyticsSet.add(iAnalytics);
    iAnalytics.configure(mAppContext);
  }

  public Set<IAnalytics> getIAnalyticsSet() {
    return analyticsSet;
  }

  /**
   * Add analytics constant of an Activity.
   *
   * @param activityName      Activity name.
   * @param analyticsConstant Analytics constant of given activity.
   * @return Return Analytics instance for easy cascaded settings.
   */
  public AnalyticsManager addAnalyticsConstantForActivity(String activityName, String
      analyticsConstant) {

    mAnalyticsConstantMap.put(activityName, analyticsConstant);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityStarted(Activity activity) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityResumed(Activity activity) {

    String activityName = getActivityName(activity);
    Log.d(TAG, activityName + " onActivityResumed, analytics tracking.");
//        if (mIAnalytics != null) {
//            mIAnalytics.collectLifeCycleData(activity, true);
//
//            // Track state through analytics.
//            String analyticsConstant = mAnalyticsConstantMap.get(activityName);
//            if (analyticsConstant != null) {
//                mIAnalytics.trackState(analyticsConstant);
//            }
//        }
    for (IAnalytics analytics : analyticsSet) {
      analytics.collectLifeCycleData(activity, true);

      // Track state through analytics.
      String analyticsConstant = mAnalyticsConstantMap.get(activityName);
      if (analyticsConstant != null) {
        analytics.trackState(analyticsConstant);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityPaused(Activity activity) {

    String activityName = getActivityName(activity);
    Log.d(TAG, activityName + " onActivityPaused, analytics tracking.");
//        if (mIAnalytics != null) {
//            mIAnalytics.collectLifeCycleData(activity, false);
//        }
    for (IAnalytics analytics : analyticsSet) {
      analytics.collectLifeCycleData(activity, false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityStopped(Activity activity) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityDestroyed(Activity activity) {

  }

  /**
   * Returns the analytics constant given an Activity name.
   * Used for testing and not intended for production.
   *
   * @param activityName Activity name.
   * @return The analytics constant if it exists or an empty String if it doesn't.
   */
  @VisibleForTesting
  public String getConstant(String activityName) {

    if (mAnalyticsConstantMap.keySet().contains(activityName)) {
      return mAnalyticsConstantMap.get(activityName);
    } else {
      return "";
    }
  }

  /**
   * Resets the singleton. Used for testing and not intended for production.
   */
  @VisibleForTesting
  void reset() {

    sInstance = null;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String value) {
    sessionId = value;
  }
}

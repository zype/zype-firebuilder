/**
 * This file was modified by Amazon:
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
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.amazon.android.tv.tenfoot.ui.activities;


import com.amazon.analytics.AnalyticsManager;
import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.ErrorHelper;
import com.amazon.android.interfaces.ICancellableLoad;
import com.amazon.android.tv.tenfoot.ui.epg.EPGDataManager;
import com.amazon.android.tv.tenfoot.ui.sliders.HeroSlider;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.Helpers;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;
import com.zype.fire.api.Model.ZobjectTopPlaylist;
import com.zype.fire.api.Model.ZobjectTopPlaylistResponse;
import com.zype.fire.api.Util.AdMacrosHelper;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeSettings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * This is a splash activity to load when the app is initializing and loading its content.
 */
public class SplashActivity extends BaseActivity implements ICancellableLoad {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private ImageView mAppLogo;
    private ProgressBar mProgress;
    private boolean isLoadingCancelled = false;
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity_layout);
        mAppLogo = (ImageView) findViewById(R.id.main_logo);
        mProgress = (ProgressBar) findViewById(R.id.feed_progress);
//        TextView mProgressText = (TextView) findViewById(R.id.feed_loader);
//        TextView copyrightTextView = (TextView) findViewById(R.id.copyright);

        try {
            // Update copyright text with app version.
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//            copyrightTextView.append("\nVersion " + pInfo.versionName);
        }
        catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource not found: ", exception);
        }
        catch (PackageManager.NameNotFoundException exception) {
            Log.e(TAG, "Package name not found: ", exception);
        }
        // Check to see if this activity is not called from the TenFootApp.
        if (!getIntent().hasExtra(ContentBrowser.CONTENT_WILL_UPDATE)) {
//            mProgressText.setText(R.string.feed_loading);
        }
        // If this activity was called from the TenFootApp call activity method.
        else {
//            mProgressText.setText(R.string.feed_reloading);
        }
        String sessionId = AdMacrosHelper.getAdvertisingId(this) + "-" +
                Calendar.getInstance().getTimeInMillis();
        Log.d(TAG, "onCreate(): sessionId=" + sessionId);
        AnalyticsManager.getInstance(this).setSessionId(sessionId);

    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (Helpers.DEBUG) {
            Log.d(TAG, "Splash onNewIntent:" + intent);
        }
        this.setIntent(intent);
    }

    @Override
    public void onResume() {

        super.onResume();
        isLoadingCancelled = false;
        if (!getIntent().hasExtra(ContentBrowser.CONTENT_WILL_UPDATE)) {
            Log.d(TAG, "First loading");
            new AsyncTask<Activity, Void, String>() {

                @Override
                protected String doInBackground(Activity... activity) {

                    ContentBrowser contentBrowser = ContentBrowser.getInstance(activity[0]);
                    try {

                        SplashActivity splashAct = (SplashActivity) activity[0];

                        contentBrowser.onAllModulesLoaded();

                        // Notify content browser that the intent is coming from an app launch.
                        splashAct.getIntent().putExtra(ContentBrowser.RESTORE_ACTIVITY, true);


                        HeroSlider.getInstance().loadContent().subscribe(s-> {

                        }, throwable -> {

                        });

                        if(ZypeSettings.EPG_ENABLED) {
                            EPGDataManager.getInstance().setNowPlayingText(getString(com.amazon.android.contentbrowser.R.string.currently_playing));
                            EPGDataManager.getInstance().load();
                        }

                        autoPlayContent();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Failed to put data in cache for recipe ", e);
                    }
                    catch (NoClassDefFoundError error) {
                        Log.e("Did not find class ", error.getMessage());
                        return error.toString();
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(String error) {
                    if(error != null && error.contains(NoClassDefFoundError.class.getSimpleName())) {
                        Log.e(TAG, "onPostExecute "+error);
                        showAuthErrorDialog();
                    }
                }
            }.execute(this);

        }
    }

    /**
     * Shows an error dialog
     */
    private void showAuthErrorDialog() {
        ErrorHelper.injectErrorFragment(this, ErrorUtils.ERROR_CATEGORY
                .AUTHENTICATION_SYSTEM_ERROR, (errorDialogFragment, errorButtonType,
                                               errorCategory) -> {
            if (errorButtonType ==
                    ErrorUtils.ERROR_BUTTON_TYPE.EXIT_APP) {
                this.finishAffinity();
            }
        });
    }
    @Override
    public void setRestoreActivityValues() {
        // not restoring state.
    }

    /**
     * Returns if the loading request is cancelled or not.
     * For this class it will never be cancelled.
     *
     * @return True if loading is cancelled
     */
    public boolean isLoadingCancelled() {

        return isLoadingCancelled;
    }

    @Override
    public void onPause() {

        isLoadingCancelled = true;
        super.onPause();
    }

    private void autoPlayContent() {
      ContentBrowser contentBrowser = ContentBrowser.getInstance(this);

      loadAutoPlayContent().subscribe(autoPlayList-> {
          boolean found=false;
          for (ZobjectTopPlaylist model: autoPlayList){
            if (model.active &&
                model.zobjectTypeTitle.equalsIgnoreCase("autoplay_hero")){
              found=true;
              mCompositeSubscription.add(contentBrowser.getContentById(model.videoid).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                  .subscribe(content -> {
                    //move the user to the detail screen
                    contentBrowser.runGlobalRecipes(this, this,content);
                    //contentBrowser.switchToRendererScreen(content);

                  }, throwable -> {
                    contentBrowser.runGlobalRecipes(this, this);

                  }));
              break;
            }
          }

          if (!found){
            contentBrowser.runGlobalRecipes(this, this);
          }

      }, throwable -> {
        contentBrowser.runGlobalRecipes(this, this);
      });
    }

    public Observable<List<ZobjectTopPlaylist>> loadAutoPlayContent() {
        return Observable.just(true).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).flatMap(s -> {
            ZobjectTopPlaylistResponse response = ZypeApi.getInstance().getZobjectAutoPlayHeroLists();

            return Observable.just(response.zobjectContents);
        });
    }


}

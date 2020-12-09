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

package com.amazon.android.uamp.ui;

import com.amazon.analytics.AnalyticsManager;
import com.amazon.android.configuration.ConfigurationManager;
import com.amazon.android.model.event.ContentUpdateEvent;
import com.amazon.android.tv.tenfoot.base.TenFootApp;
import com.amazon.android.ui.constants.ConfigurationConstants;
import com.amazon.android.utils.GlideHelper;
import com.amazon.mediaplayer.tracks.MediaFormat;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.SubtitleLayout;

import com.amazon.ads.IAds;
import com.amazon.ads.AdMetaData;
import com.amazon.analytics.AnalyticsTags;
import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.database.helpers.RecentDatabaseHelper;
import com.amazon.android.contentbrowser.database.helpers.RecommendationDatabaseHelper;
import com.amazon.android.contentbrowser.database.records.RecentRecord;
import com.amazon.android.contentbrowser.helper.AnalyticsHelper;
import com.amazon.android.model.content.Content;
import com.amazon.android.module.ModuleManager;

import com.amazon.android.recipe.Recipe;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.uamp.DrmProvider;
import com.amazon.android.uamp.UAMP;
import com.amazon.android.uamp.mediaSession.MediaSessionController;
import com.amazon.android.uamp.constants.PreferencesConstants;
import com.amazon.android.uamp.helper.CaptioningHelper;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.Helpers;
import com.amazon.android.utils.Preferences;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.session.PlaybackState;

import com.amazon.mediaplayer.AMZNMediaPlayer;
import com.amazon.mediaplayer.AMZNMediaPlayer.PlayerState;
import com.amazon.mediaplayer.playback.text.Cue;
import com.amazon.mediaplayer.tracks.TrackType;
import com.zype.fire.api.Model.AdvertisingSchedule;
import com.zype.fire.api.Model.ErrorBody;
import com.zype.fire.api.Model.PlayerData;
import com.zype.fire.api.Model.PlayerResponse;
import com.zype.fire.api.Util.AdMacrosHelper;
import com.zype.fire.api.Util.ErrorHelper;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;
import com.zype.fire.auth.ZypeAuthentication;
import com.amazon.utils.DateAndTimeHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.leanback.app.TenFootPlaybackOverlayFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyUtils;

import static com.amazon.android.contentbrowser.ContentBrowser.SHOW_PLAYLIST_AUTOPLAY;

/**
 * PlaybackOverlayActivity for content playback that loads PlaybackOverlayFragment
 */
public class PlaybackActivity extends BasePlaybackActivity implements
        PlaybackOverlayFragment.OnPlayPauseClickedListener, AMZNMediaPlayer
        .OnStateChangeListener, AMZNMediaPlayer.OnErrorListener, AMZNMediaPlayer.OnInfoListener,
        AudioManager.OnAudioFocusChangeListener, AMZNMediaPlayer.OnCuesListener,
        ErrorDialogFragment.ErrorDialogFragmentListener {

    private static final int TRANSPORT_CONTROLS_DELAY_PERIOD = 50;
    private static final String TAG = PlaybackActivity.class.getSimpleName();
    private static final String HLS_VIDEO_FORMAT = "HLS";

    private static final int HDMI_AUDIO_STATE_UNPLUGGED = 0;
    private static final int HDMI_AUDIO_STATE_PLUGGED = 1;

    private static final float AUDIO_FOCUS_DUCK_VOLUME = 0.1f;
    private static final float AUDIO_FOCUS_DEFAULT_VOLUME = 1.0f;
    //Reporting interval in seconds for media session
    private static final int MEDIA_SESSION_REPORTING_INTERVAL = 5;

    private FrameLayout mVideoView;
    private SubtitleLayout mSubtitleLayout;
    private FrameLayout mAudioView;
    private FrameLayout mAdsView;
    private UAMP mPlayer;
    private Content mSelectedContent;
    private LeanbackPlaybackState mPlaybackState = LeanbackPlaybackState.IDLE;
    private PlayerState mPrevState;
    private PlayerState mCurrentState;
    private boolean mIsActivityResumed;
    private boolean mIsContentChangeRequested;
    private int mTotalSegments = 0;

    private ProgressBar mProgressBar;
    private Window mWindow;
    private long mCurrentPlaybackPosition;
    private long mStartingPlaybackPosition;

    private AudioManager mAudioManager;
    private AudioFocusState mAudioFocusState = AudioFocusState.NoFocusNoDuck;
    private PlaybackOverlayFragment mPlaybackOverlayFragment;
    private ErrorDialogFragment mErrorDialogFragment = null;

    private Handler mTransportControlsUpdateHandler;
    private ContinualFwdUpdater mContinualFwdUpdater;
    private ContinualRewindUpdater mContinualRewindUpdater;
    private boolean mIsLongPress;
    private boolean mAutoPlay = false;
    private boolean mIsNetworkError;

    //Media session for adding Alexa media commands capability
    private MediaSessionController mMediaSessionController;
    private ScheduledExecutorService mScheduledExecutorService;

    private CaptioningHelper mCaptioningHelper;
    private CaptioningManager.CaptioningChangeListener mCaptioningChangeListener;

    /**
     * State of CC in Subtitle view.
     */
    private boolean mIsClosedCaptionEnabled = false;

    /**
     * Is Content support outband close caption flag.
     */
    private boolean mHasOutbandCC = false;

    /**
     * Flag for when the activity's onResume is received when the activity is created.
     */
    private boolean mResumeOnCreation = false;

    /**
     * Flag for when the activity's onResume is received when the activity is started.
     */
    private boolean mResumeOnStart = false;

    private boolean isAutoPlay=false;
    private boolean messageAlreadyShow=false;

    private boolean isAudio = false;


  enum AudioFocusState {
        Focused,
        NoFocusNoDuck,
        NoFocusCanDuck
    }

    /**
     * Video position tracking handler.
     */
    private Handler mVideoPositionTrackingHandler;

    /**
     * Video position tracking runnable.
     */
    private Runnable mVideoPositionTrackingRunnable;

    /**
     * Ads implementation reference.
     */
    private IAds mAdsImplementation;
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();


    /**
     * Video position tracking poll time in ms.
     */
    private static final int VIDEO_POSITION_TRACKING_POLL_TIME_MS = 1000;

    /* Zype, Evgeny Cherkasov */
    // Akamai analytics
//    AnalyticsPlugin akamaiPlugin;

    // Closed captions
    private String ccTrack = "";
    private View messageView;

    // Need to setup cookie policy to make ExoPlayer play live streams from Akamai
    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /* Zype, Evgney Cherkasov */
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        //flag for onResume to know this is being called at activity creation
        mResumeOnCreation = true;
        // Create video position tracking handler.
        mVideoPositionTrackingHandler = new Handler();

        // Create a runnable for video position tracking.
        mVideoPositionTrackingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // If player exists and playing then set video position to ads implementation.
                    if (mPlayer != null && isPlaying()) {
                        if (mAdsImplementation != null) {
                            mAdsImplementation.setCurrentVideoPosition(
                                    mPlayer.getCurrentPosition());
                        }
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "Video position tracking failed.", e);
                }
                mVideoPositionTrackingHandler.postDelayed(this, VIDEO_POSITION_TRACKING_POLL_TIME_MS);
            }
        };

        // Trigger GC to clean up prev player.
        // In regular Java vm this is not advised but for Android we need this.
        // This will let things going as CC became a must and this code will be handled out of
        // TenFootUI in a different way.
        System.gc();

        setContentView(R.layout.playback_controls);

        mWindow = getWindow();

        mProgressBar = (ProgressBar) findViewById(R.id.playback_progress);

        TextView messageTextView= (TextView) findViewById(R.id.autoplayMessage);
        messageTextView.setText(getString(R.string.autoplay_message));
        CalligraphyUtils.applyFontToTextView(TenFootApp.getInstance(), messageTextView,
            ConfigurationManager
                .getInstance(this).getTypefacePath(ConfigurationConstants
                .BOLD_FONT));
        messageView= findViewById(R.id.messageView);
        mPlaybackOverlayFragment =
                (PlaybackOverlayFragment) getFragmentManager()
                        .findFragmentById(R.id.playback_controls_fragment);

        mSelectedContent =
                (Content) getIntent().getSerializableExtra(Content.class.getSimpleName());

        isAutoPlay= getIntent().getBooleanExtra(SHOW_PLAYLIST_AUTOPLAY,false);

        if (mSelectedContent == null || TextUtils.isEmpty(mSelectedContent.getUrl())) {
            AnalyticsHelper.trackError(TAG, "Received an Intent to play content without a " +
                    "content object or content URL");
            finish();
        }

        loadViews();
        createPlayerAndInitializeListeners();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mCaptioningHelper = new CaptioningHelper(this, mSubtitleLayout);
        mCurrentPlaybackPosition = 0;
        mTransportControlsUpdateHandler = new Handler(Looper.getMainLooper());
        mContinualFwdUpdater = new ContinualFwdUpdater();
        mContinualRewindUpdater = new ContinualRewindUpdater();
        mIsLongPress = false;
        mIsNetworkError = false;

        // Auto-play the selected content.
        mAutoPlay = true;

        //initialize the media session
        initMediaSession();

        mCaptioningHelper = new CaptioningHelper(this, mSubtitleLayout);
        mCaptioningChangeListener = new CaptioningManager.CaptioningChangeListener() {
            @Override
            public void onEnabledChanged(boolean enabled) {

                Log.d(TAG, "onEnabledChanged: " + enabled);
                super.onEnabledChanged(enabled);

                if (mCaptioningHelper.useGlobalSetting()) {
                    mIsClosedCaptionEnabled = enabled;
                    modifyClosedCaptionState(mIsClosedCaptionEnabled);
                }
            }

            @Override
            public void onUserStyleChanged(@NonNull CaptioningManager.CaptionStyle userStyle) {

                Log.d(TAG, "onUserStyleChanged");
                super.onUserStyleChanged(userStyle);
                mSubtitleLayout.setStyle(CaptionStyleCompat.createFromCaptionStyle(userStyle));
            }

            @Override
            public void onLocaleChanged(Locale locale) {

                Log.d(TAG, "onLocaleChanged");
                super.onLocaleChanged(locale);
            }

            @Override
            public void onFontScaleChanged(float fontScale) {

                Log.d(TAG, "onFontScaleChanged");
                super.onFontScaleChanged(fontScale);
                mSubtitleLayout.setFractionalTextSize(
                        fontScale * SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION);
            }
        };

        mCaptioningHelper.setCaptioningManagerListener(mCaptioningChangeListener);

    }

    /**
     * Init media session
     */
    private void initMediaSession() {

        List<String> declaredPermissions = Helpers.getDeclaredPermissions(this);
        String mediaSessionPermission = getResources().getString(R.string.alexa_media_session_permission);
        if (!declaredPermissions.contains(mediaSessionPermission)) {
            Log.d(TAG, "Media session permission hasn't been declared by app, not initializing " +
                    "media session");
            return;
        }

        //Get playback fragment to set in media session for callbacks
        TenFootPlaybackOverlayFragment playbackFragment = (TenFootPlaybackOverlayFragment)
                getFragmentManager().findFragmentById(R.id.playback_controls_fragment);
        //Initialize the media session helper and create the media session
        mMediaSessionController = new MediaSessionController(playbackFragment);

        if (mMediaSessionController == null) {
            Log.v(TAG, "Failed in initializing media session controller");
            return;
        }
        //Create media session instance
        mMediaSessionController.createMediaSession(this);
    }

    /**
     * Enable Media Session
     */
    private void enableMediaSession() {

        if (mMediaSessionController != null) {
            mMediaSessionController.setMediaSessionActive(true);
            stopPlaybackReportingService();
            //Start the reporting service which reports the playback state every few seconds
            startPlaybackReportingService();
        }
    }

    /**
     * Disable Media Session
     */
    private void disableMediaSession() {

        //Disable the media session
        if (mMediaSessionController != null) {
            mMediaSessionController.setMediaSessionActive(false);
            //Stop the reporting service which reports the playback state every few seconds
            stopPlaybackReportingService();
        }
    }

    /**
     * Reports the current playback state every few seconds.
     * The more precise this is, the better alexa is about seeking
     * to the correct position. It is recommended that the state is
     * updated every 5 to 10 seconds.
     */
    private void startPlaybackReportingService() {

        if (mMediaSessionController == null) {
            return;
        }
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                if (!Thread.currentThread().isInterrupted()) {
                    long position = getCurrentPosition();
                    // Executor has probably asked us to stop
                    AnalyticsHelper.trackPlayback(mSelectedContent, position);
                    mMediaSessionController.updatePlaybackState(position);
                }

            }
        }, 0, MEDIA_SESSION_REPORTING_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Stop reporting the playback state now
     */
    private void stopPlaybackReportingService() {

        if (mScheduledExecutorService == null) {
            return;
        }
        mScheduledExecutorService.shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {

        super.onStart();

        registerHDMIUnpluggedStateChangeBroadcast();
        requestAudioFocus();

        //flag for onResume to know this is being called at activity start
        mResumeOnStart = true;

        // Let ads implementation track player activity lifecycle.
        if (mAdsImplementation != null) {
            mAdsImplementation.setActivityState(IAds.ActivityState.START);
        }
    }

    /**
     * resume Playback Activity
     */
    private void resumePlayback() {

        // Start tracking video position changes.
        mVideoPositionTrackingHandler.post(mVideoPositionTrackingRunnable);

        // Check to see if a previous network failure was now rectified.
        if (!mIsActivityResumed && mIsNetworkError && Helpers.isConnectedToNetwork(this)) {
            mIsActivityResumed = true;
            mIsNetworkError = false;
            finish();
            Log.i(TAG, "Traversing to details page since network connection is now detected");
            return;
        }

        //Check if onResume called with onStart of activity we need to start the ad flow.
        if(mResumeOnStart) {
            mResumeOnStart = false;
            openSelectedContent();
        }

        if (mCaptioningHelper.useGlobalSetting()) {
            mIsClosedCaptionEnabled = mCaptioningHelper.isEnabled();

        }
        else {
            mIsClosedCaptionEnabled =
                    Preferences.getBoolean(PreferencesConstants.IS_CLOSE_CAPTION_FLAG_PERSISTED);
        }

        // Reset playback position to 0.
        mCurrentPlaybackPosition = 0;

        // Live content can't be resumed.
        if (!isContentLive(mSelectedContent)) {
            loadContentPlaybackState();
        }
        mStartingPlaybackPosition = mCurrentPlaybackPosition;
        mIsActivityResumed = true;
        Log.d(TAG, "onResume() current state is " + mCurrentState);
        switch (mCurrentState) {
            case READY:
                if (mCurrentPlaybackPosition > 0 && mCurrentPlaybackPosition !=
                        getCurrentPosition()) {
                    mPlayer.seekTo(mCurrentPlaybackPosition);
                }
                else {
                    if (mAutoPlay) {
                        play();
                        mAutoPlay = false;
                    }
                }
                break;
            case OPENED:
                mPlayer.prepare();
                break;
        }

        // Let ads implementation track player activity lifecycle.
        if (mAdsImplementation != null) {
            mAdsImplementation.setActivityState(IAds.ActivityState.RESUME);
        }
        //Enable the media session
        enableMediaSession();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {

        super.onResume();
        // TODO: Move this handling from here to content browser after refactoring
        // VerifyScreenSwitch DEVTECH-4038

        //Check before resume as user might not authenticated any more. One such scenario is
        // coming back from next/prev screen when user account has been disabled from server.
        if (mResumeOnCreation) {
            mResumeOnCreation = false;
            resumePlayback();
        }
        else {
            ContentBrowser.getInstance(this).verifyScreenSwitch(ContentBrowser
                            .CONTENT_RENDERER_SCREEN,
                    mSelectedContent, extra ->
                            resumePlayback(),
                    errorExtra -> finish());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {

        super.onPause();

        // Persist CC state if user has changed local state.
        if (!mCaptioningHelper.useGlobalSetting()) {
            Preferences.setBoolean(PreferencesConstants.IS_CLOSE_CAPTION_FLAG_PERSISTED,
                    mIsClosedCaptionEnabled);
        }

        if (mPlayer.getCurrentPosition() > 0) {

            storeContentPlaybackState();
            // User has stopped watching content so track it with analytics
            AnalyticsHelper.trackPlaybackFinished(mSelectedContent, mStartingPlaybackPosition,
                    getCurrentPosition());

            // After the user has stopped watching the content, send recommendations for related
            // content of the selected content if any exist.
            if (mSelectedContent.getRecommendations().size() > 0) {
                ContentBrowser.getInstance(this).getRecommendationManager()
                        .executeRelatedRecommendationsTask(getApplicationContext(),
                                mSelectedContent);
            }
        }
        mIsActivityResumed = false;
        pause();

        // Stop tracking video position changes.
        mVideoPositionTrackingHandler.removeCallbacks(mVideoPositionTrackingRunnable);

        // Let ads implementation track player activity lifecycle.
        if (mAdsImplementation != null) {
            mAdsImplementation.setActivityState(IAds.ActivityState.PAUSE);
        }
        //Disable the media session
        disableMediaSession();

        /* Zype, Evgeny Cherkasov */
        // Akamai analytics
//        if (akamaiPlugin != null) {
//            akamaiPlugin.handleEnterBackground();
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {

        super.onStop();

        unregisterHDMIUnpluggedStateChangeBroadcast();
        abandonAudioFocus();
        mIsContentChangeRequested = false;

        if (mPlayer != null) {
            if (!isContentLive(mSelectedContent)) {
                mCurrentPlaybackPosition = getCurrentPosition();
            }
            mPlayer.close();
        }

        // Let ads implementation track player activity lifecycle.
        if (mAdsImplementation != null) {
            mAdsImplementation.setActivityState(IAds.ActivityState.STOP);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.v(TAG, "onActivityResult called with requestCode:" + requestCode +
                " resultCode:" + requestCode + " intent:" + data);
        super.onActivityResult(requestCode, resultCode, data);

        ContentBrowser.getInstance(this)
                .handleOnActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // This lets us get global font support.
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    /**
     * Inner class implementing repeating fast-forward media key transport control
     */
    private final class ContinualFwdUpdater implements Runnable {

        @Override
        public void run() {

            mPlaybackOverlayFragment.fastForward();
            mTransportControlsUpdateHandler.postDelayed(new ContinualFwdUpdater(),
                    TRANSPORT_CONTROLS_DELAY_PERIOD);
        }
    }

    /**
     * Inner class implementing repeating rewind media key transport control
     */
    private final class ContinualRewindUpdater implements Runnable {

        @Override
        public void run() {

            mPlaybackOverlayFragment.fastRewind();
            mTransportControlsUpdateHandler.postDelayed(new ContinualRewindUpdater(),
                    TRANSPORT_CONTROLS_DELAY_PERIOD);
        }
    }

    /**
     * Starts the repeating fast-forward media transport control action
     */
    private void startContinualFastForward() {

        mTransportControlsUpdateHandler.post(mContinualFwdUpdater);
        mIsLongPress = true;
    }

    /**
     * Starts the repeating rewind media transport control action
     */
    private void startContinualRewind() {

        mTransportControlsUpdateHandler.post(mContinualRewindUpdater);
        mIsLongPress = true;
    }

    /**
     * Stops the currently on-going (if any) media transport control action since the press &
     * hold of corresponding transport control ceased or {@link @KeyEvent.KEYCODE_HOME} was pressed
     */
    private void stopTransportControlAction() {

        mTransportControlsUpdateHandler.removeCallbacksAndMessages(null);
        mIsLongPress = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {

        super.onDestroy();
        // Let ads implementation track player activity lifecycle.
        if (mAdsImplementation != null) {
            mAdsImplementation.setActivityState(IAds.ActivityState.DESTROY);
        }
        releasePlayer();
        mCaptioningHelper.removeCaptioningManagerListener(mCaptioningChangeListener);
        //Release the media session as well
        if (mMediaSessionController != null) {
            mMediaSessionController.setMediaSessionActive(false);
            mMediaSessionController.releaseMediaSession();
            mMediaSessionController = null;
        }
    }

    private void showProgress() {

        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {

        mProgressBar.setVisibility(View.INVISIBLE);
    }
    private void setupMessageView(){
        if (!isAutoPlay || messageAlreadyShow)
            return;

        messageAlreadyShow=true;
        messageView.setVisibility(View.VISIBLE);

        mCompositeSubscription.add(Observable.timer(5, TimeUnit.SECONDS).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(aLong -> {
            messageView.setVisibility(View.GONE);
        }));

    }

    private void play() {

        if (mPlayer != null) {
            if (mAudioFocusState == AudioFocusState.Focused) {
                mPlayer.play();
            }
            else {
                if (requestAudioFocus()) {
                    mPlayer.play();
                }
                else {
                    showProgress();
                    mPlaybackState = LeanbackPlaybackState.PLAYING;
                    if (mPlaybackOverlayFragment != null) {
                        mPlaybackOverlayFragment.togglePlaybackUI(true);
                    }
                }
            }
        }
    }

    private void pause() {

        if (mPlayer != null && isPlaying()) {
            mPlayer.pause();
        }
        mPlaybackState = LeanbackPlaybackState.PAUSED;
        if (mPlaybackOverlayFragment != null) {
            mPlaybackOverlayFragment.togglePlaybackUI(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDuration() {

        long duration = 0;
        if (mPlayer != null) {
            duration = mPlayer.getDuration();
            if (duration == AMZNMediaPlayer.UNKNOWN_TIME) {
                Log.i(TAG, "Content duration is unknown. Returning 0.");
                duration = 0;
            }
        }
        // Duration wasn't found using the player, try getting it directly from the content.
        if (duration == 0 && mSelectedContent != null) {
            duration = mSelectedContent.getDuration();
        }
        return (int) duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentPosition() {

        if (mPlayer != null) {
            return (int) mPlayer.getCurrentPosition();
        }
        return 0;
    }

    private void seekTo(int pos) {

        if (mPlayer != null) {
            mPlayer.seekTo(pos);
        }
    }

    /**
     * Returns true if the video is playing, else false
     *
     * @return true if the video is playing, else false
     */
    public boolean isPlaying() {

        boolean isPlaying = false;
        if (mPlayer != null) {
            isPlaying = (mPlayer.getPlayerState() == PlayerState.PLAYING);
        }
        return isPlaying;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBufferProgressPosition() {

        if (mPlayer != null) {
            return (mPlayer.getBufferedPercentage() * getDuration()) / 100;
        }
        return 0;
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    @Override
    public void changeContent(Content content) {

        if (!mIsContentChangeRequested && !content.equals(mSelectedContent)) {
            Log.d(TAG, "In changeContent");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    showProgress();
                }
            });

            // Save previous content's state before changing.
            storeContentPlaybackState();

            // User has stopped watching this content so track it with analytics.
            AnalyticsHelper.trackPlaybackFinished(mSelectedContent, mStartingPlaybackPosition,
                    getCurrentPosition());

            // Since the user is done watching this content, send recommendations for related
            // content of the selected content (if any exist) before changing to the next content.
            if (mSelectedContent.getRecommendations().size() > 0) {
                ContentBrowser.getInstance(this).getRecommendationManager()
                        .executeRelatedRecommendationsTask(getApplicationContext(),
                                mSelectedContent);
            }

            mIsContentChangeRequested = true;
            mCurrentPlaybackPosition = 0;
            mSelectedContent = content;

            loadContentPlaybackState();
            mStartingPlaybackPosition = mCurrentPlaybackPosition;

            if (mPlayer != null) {
                mPlayer.close();
            }
        }
    }

    /**
     * Uses the {@link RecentDatabaseHelper} to check the database for a stored playback state
     * of the current selected content. If the state exists and playback is not complete,
     * it loads the content's current playback position.
     */
    private void loadContentPlaybackState() {

        RecentDatabaseHelper database = RecentDatabaseHelper.getInstance();
        if (database != null) {
            // Check database for content's previously watched position.
            if (database.recordExists(getApplicationContext(), mSelectedContent.getId())) {

                RecentRecord record = database.getRecord(getApplicationContext(),
                        mSelectedContent.getId());
                // Set the playback position to the stored position if a recent position
                // exists for this content and playback is not complete.
                if (record != null && !record.isPlaybackComplete()) {
                    mCurrentPlaybackPosition = record.getPlaybackLocation();
                    Log.d(TAG, "loadContentPlaybackState(): restored position " + mCurrentPlaybackPosition);
                }
            }
        }
        else {
            Log.e(TAG, "Unable to load content playback state because database is null");
        }
    }

    /**
     * Store the current playback state of the selected content to the database. Calculates if
     * playback was finished or not using the  {@link ContentBrowser#GRACE_TIME_MS}.
     * If the content playback is finished, recommendation manager will dismiss the recommendation
     * for this content (if it exists).
     */
    private void storeContentPlaybackState() {

        // Calculate if the content has finished playing
        boolean isFinished = (mPlayer.getDuration() - ContentBrowser.GRACE_TIME_MS)
                <= mPlayer.getCurrentPosition();

        RecommendationDatabaseHelper recommendationDatabaseHelper =
                RecommendationDatabaseHelper.getInstance();

        // Dismiss the recommendation notification for this content if the content is finished.
        if (isFinished && recommendationDatabaseHelper != null) {
            if (recommendationDatabaseHelper.recordExists(getApplicationContext(),
                    mSelectedContent.getId())) {

                ContentBrowser.getInstance(this).getRecommendationManager()
                        .dismissRecommendation(mSelectedContent.getId());
                AnalyticsHelper.trackDismissRecommendationForCompleteContent(mSelectedContent);
            }
        }

        // Save the recently played content to database
        RecentDatabaseHelper recentDatabaseHelper = RecentDatabaseHelper.getInstance();
        if (recentDatabaseHelper != null && !isContentLive(mSelectedContent)) {

            recentDatabaseHelper.addRecord(getApplicationContext(), mSelectedContent.getId(),
                    mPlayer.getCurrentPosition(), isFinished,
                    DateAndTimeHelper.getCurrentDate().getTime(),
                    mPlayer.getDuration());

            if (ZypeConfiguration.displayWatchedBarOnVideoThumbnails()) {
                EventBus.getDefault().post(new ContentUpdateEvent(mSelectedContent.getId()));
            }

            Log.d(TAG, "storeContentPlaybackState(): saved position " + mPlayer.getCurrentPosition());
        }
        else {
            Log.e(TAG, "Cannot update recent content playback state. Database is null");
        }
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    @Override
    public void onFragmentPlayPause(boolean playPause) {

        if (playPause) {
            play();
        }
        else {
            pause();
        }
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    @Override
    public void onFragmentFfwRwd(int position) {

        if (position >= 0) {
            seekTo(position);
            if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
                play();
            }
        }
    }

    /**
     * Triggered by overlay when there is a CC button state change.
     *
     * @param state CC state
     */
    @Override
    public void onCloseCaptionButtonStateChanged(boolean state) {

        // The CC button has been pushed so now we don't want to use global settings for CC state.
        mCaptioningHelper.setUseGlobalSetting(false);
//        /* Zype, Evgeny Cherkasov */
//        if (state) {
//            showClosedCaptionDialog();
//        }
//        else {
            modifyClosedCaptionState(state);
//        }
    }

    /**
     * Change the closed captioning state.
     *
     * @param state The new state.
     */
    public void modifyClosedCaptionState(boolean state) {

        if (mPlayer != null && mPlaybackOverlayFragment != null) {
            if (isClosedCaptionAvailable()) {

                // Enable CC. Prioritizing CLOSED_CAPTION before SUBTITLE if enabled
                if (ContentBrowser.getInstance(this).isEnableCEA608() &&
                        mPlayer.getTrackCount(TrackType.CLOSED_CAPTION) > 0) {
                    mPlayer.enableTextTrack(TrackType.CLOSED_CAPTION, state);
                }
                else {
                    mPlayer.enableTextTrack(TrackType.SUBTITLE, state);
                }
//                /* Zype, Evgeny Cherkasov */
//                int ccTrackIndex = -1;
//                if (state) {
//                    ccTrackIndex = getClosedCaptionsTrackIndex(ccTrack);
//                }
//                TrackType type;
//                if (ContentBrowser.getInstance(this).isEnableCEA608()) {
//                    type = TrackType.CLOSED_CAPTION;
//                }
//                else {
//                    type = TrackType.SUBTITLE;
//                }
//                mPlayer.setSelectedTrack(type, ccTrackIndex);

                // Update internal state.
                mIsClosedCaptionEnabled = state;
                mPlaybackOverlayFragment.updateCCButtonState(state, true);
                mSubtitleLayout.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
                Log.d(TAG, "Content support CC. Change CC state to " + state);
            }
            else {
                // Disable CC button back.
                mPlaybackOverlayFragment.updateCCButtonState(false, false);
                // Do not disable mIsCloseCaptionEnabled as we want it persistent.
                Log.d(TAG, "Content does not support CC. Change CC state to false");
            }
        }
    }

    private void loadViews() {

        mVideoView = (FrameLayout) findViewById(R.id.videoView);
        // Avoid focus stealing.
        mVideoView.setFocusable(false);
        mVideoView.setFocusableInTouchMode(false);
        mVideoView.setClickable(false);

        mSubtitleLayout = (SubtitleLayout) findViewById(R.id.subtitles);

        mAudioView = (FrameLayout) findViewById(R.id.audioView);

        mAdsView = (FrameLayout) findViewById(R.id.adsView);
        // Avoid focus stealing.
        mAdsView.setFocusable(false);
        mAdsView.setFocusableInTouchMode(false);
        mAdsView.setClickable(false);

        // Make Ads visible and video invisible.
        mAdsView.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.INVISIBLE);

    }

    /**
     * Set visibility of a view group with its child surface views.
     *
     * @param viewGroup  View group object.
     * @param visibility Visibility flag to be set.
     */
    private void setVisibilityOfViewGroupWithInnerSurfaceView(ViewGroup viewGroup, int
            visibility) {

        // Hide the view group.
        viewGroup.setVisibility(visibility);
        // Traverse all the views and hide the child surface views.
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof SurfaceView || v instanceof ImageView) {
                v.setVisibility(visibility);
            }
            //Need to do it recursively if it is not a surface view. No the end of view hierarchy!!
            else if (v instanceof FrameLayout) {
                setVisibilityOfViewGroupWithInnerSurfaceView((FrameLayout) v, visibility);
            }
        }
    }

    private void switchToContentView() {
        if (isAudio) {
            switchToAudioView();
        }
        else {
            switchToVideoView();
        }
    }

    private void switchToVideoView() {
        // Show Video view.
        setVisibilityOfViewGroupWithInnerSurfaceView(mVideoView, View.VISIBLE);
        // Show Subtitle view.
        mSubtitleLayout.setVisibility(View.VISIBLE);
        // Hide Audio view
        setVisibilityOfViewGroupWithInnerSurfaceView(mAudioView, View.GONE);
        // Hide Ads view.
        setVisibilityOfViewGroupWithInnerSurfaceView(mAdsView, View.GONE);
    }

    private void switchToAudioView() {
        Log.d(TAG, "switchToAudioView()");
        // Show Audio view
        setVisibilityOfViewGroupWithInnerSurfaceView(mAudioView, View.VISIBLE);
        ImageView imageThumbnail = (ImageView) mAudioView.findViewById(R.id.imageThumbnail);
        String thumbnailUrl = mSelectedContent.getExtraValueAsString(Content.EXTRA_THUMBNAIL_SQUARE_URL);
        if (TextUtils.isEmpty(thumbnailUrl) || thumbnailUrl.equals("null")) {
            thumbnailUrl = mSelectedContent.getBackgroundImageUrl();
        }
        if (!TextUtils.isEmpty(thumbnailUrl) && !thumbnailUrl.equals("null")) {
            GlideHelper.loadImageWithCrossFadeTransition(this,
                    imageThumbnail,
                    thumbnailUrl,
                    1000,
                    R.color.browse_background_color);
        }
        // Hide Video view.
        setVisibilityOfViewGroupWithInnerSurfaceView(mVideoView, View.GONE);
        // Hide Subtitle view.
        mSubtitleLayout.setVisibility(View.GONE);
        // Hide Ads view.
        setVisibilityOfViewGroupWithInnerSurfaceView(mAdsView, View.GONE);
    }

    private void switchToAdsView() {
        // Show Ads view.
        setVisibilityOfViewGroupWithInnerSurfaceView(mAdsView, View.VISIBLE);
        // Hide Video view.
        setVisibilityOfViewGroupWithInnerSurfaceView(mVideoView, View.GONE);
        // Hide Subtitle view.
        mSubtitleLayout.setVisibility(View.GONE);
        // Hide Audio view
        setVisibilityOfViewGroupWithInnerSurfaceView(mAudioView, View.GONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onVisibleBehindCanceled() {

        super.onVisibleBehindCanceled();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // If ad is in focus then don't respond to key events.
        if (mAdsView.getVisibility() == View.VISIBLE) {
            return super.onKeyDown(keyCode, event);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_L1:
                if (!isContentLive(mSelectedContent)) {
                    event.startTracking();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // If ad is in focus then don't respond to key events.
        if (mAdsView.getVisibility() == View.VISIBLE) {
            return super.onKeyLongPress(keyCode, event);
        }

        // Ignore fast-forward and rewind for live content.
        if (isContentLive(mSelectedContent)) {
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                startContinualFastForward();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                startContinualRewind();
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                startContinualFastForward();
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                startContinualRewind();
                return true;
            default:
                return super.onKeyLongPress(keyCode, event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        // If ad is in focus then don't respond to key events.
        if (mAdsView.getVisibility() == View.VISIBLE) {
            return super.onKeyUp(keyCode, event);
        }

        // Ignore fast-forward and rewind keys for live content.
        if (isContentLive(mSelectedContent) && (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
                keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_R1 ||
                keyCode == KeyEvent.KEYCODE_BUTTON_L1)) {
            return true;
        }

        PlaybackOverlayFragment playbackOverlayFragment = (PlaybackOverlayFragment)
                getFragmentManager().findFragmentById(R.id.playback_controls_fragment);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                playbackOverlayFragment.togglePlayback(false);
                AnalyticsHelper.trackPlaybackControlAction(
                        AnalyticsTags.ACTION_PLAYBACK_CONTROL_PLAY, mSelectedContent,
                        getCurrentPosition());
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                playbackOverlayFragment.togglePlayback(false);
                AnalyticsHelper.trackPlaybackControlAction(
                        AnalyticsTags.ACTION_PLAYBACK_CONTROL_PAUSE, mSelectedContent,
                        getCurrentPosition());
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
                    playbackOverlayFragment.togglePlayback(false);
                    AnalyticsHelper.trackPlaybackControlAction(
                            AnalyticsTags.ACTION_PLAYBACK_CONTROL_PAUSE, mSelectedContent,
                            getCurrentPosition());
                }
                else {
                    playbackOverlayFragment.togglePlayback(true);
                    AnalyticsHelper.trackPlaybackControlAction(
                            AnalyticsTags.ACTION_PLAYBACK_CONTROL_PLAY, mSelectedContent,
                            getCurrentPosition());
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (mIsLongPress) {
                    stopTransportControlAction();
                }
                else {
                    playbackOverlayFragment.fastForward();
                }
                AnalyticsHelper.trackPlaybackControlAction(AnalyticsTags.ACTION_PLAYBACK_CONTROL_FF,
                        mSelectedContent, getCurrentPosition());
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (mIsLongPress) {
                    stopTransportControlAction();
                }
                else {
                    playbackOverlayFragment.fastRewind();
                }
                AnalyticsHelper.trackPlaybackControlAction(
                        AnalyticsTags.ACTION_PLAYBACK_CONTROL_REWIND, mSelectedContent,
                        getCurrentPosition());
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                if (mIsLongPress) {
                    stopTransportControlAction();
                }
                else {
                    playbackOverlayFragment.fastForward();
                }
                AnalyticsHelper.trackPlaybackControlAction(AnalyticsTags.ACTION_PLAYBACK_CONTROL_FF,
                        mSelectedContent, getCurrentPosition());
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                if (mIsLongPress) {
                    stopTransportControlAction();
                }
                else {
                    playbackOverlayFragment.fastRewind();
                }
                AnalyticsHelper.trackPlaybackControlAction(
                        AnalyticsTags.ACTION_PLAYBACK_CONTROL_REWIND, mSelectedContent,
                        getCurrentPosition());
                return true;
          case KeyEvent.KEYCODE_BACK:
          case KeyEvent.KEYCODE_MENU:{
            if (isAutoPlay) {
                AnalyticsHelper.trackAutoplayFinished(mSelectedContent, getCurrentPosition());
              ContentBrowser.getInstance(PlaybackActivity.this).switchToHomeScreen();
              finish();
            }
            return super.onKeyUp(keyCode, event);
          }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void registerHDMIUnpluggedStateChangeBroadcast() {

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        registerReceiver(mHDMIUnpluggedStateChangeReceiver, intentFilter);
    }

    private void unregisterHDMIUnpluggedStateChangeBroadcast() {

        unregisterReceiver(mHDMIUnpluggedStateChangeReceiver);
    }

    private BroadcastReceiver mHDMIUnpluggedStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "mHDMIUnpluggedStateChangeReceiver " + intent);
            if (isInitialStickyBroadcast()) {
                // Ignore initial sticky broadcast.
                return;
            }

            int plugState = intent.getIntExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, -1);
            if (plugState == HDMI_AUDIO_STATE_UNPLUGGED) {
                if (mPlayer != null) {
                    if (!isContentLive(mSelectedContent)) {
                        mCurrentPlaybackPosition = getCurrentPosition();
                    }
                    if (isPlaying()) {
                        pause();// No audio focus, pause media!
                    }
                }
            }
        }
    };

    /*
     * List of various states that we can be in.
     */
    public enum LeanbackPlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

    private void createPlayerAndInitializeListeners() {

        if (mPlayer == null) {
            Log.d(TAG, "Create Player and Initialize Listeners");
            mPrevState = PlayerState.IDLE;
            mCurrentState = PlayerState.IDLE;
            Bundle playerExtras = new Bundle();
            // Get default Ads implementation without creating a new one.
            try {
                mAdsImplementation = (IAds) ModuleManager.getInstance()
                        .getModule(IAds.class.getSimpleName())
                        .getImpl(false);
                playerExtras.putBundle("ads", mAdsImplementation.getExtra());
            }
            catch (Exception e) {
                Log.e(TAG, "No Ads interface attached.", e);
            }

            // Create a player interface by using the default hooked implementation.
            String playerInterfaceName = UAMP.class.getSimpleName();
            mPlayer = (UAMP) ModuleManager.getInstance()
                    .getModule(playerInterfaceName)
                    .createImpl();

            // Init player interface, this is where it is fully created.
            mPlayer.init(this, mVideoView, playerExtras);

            // Initialize ads.
            if (mAdsImplementation != null) {
                mAdsImplementation.init(this, mAdsView, playerExtras);
            }

            mPlayer.setUserAgent(getUserAgent(PlaybackActivity.this,
                    getString(R.string.app_name_short)));
            mPlayer.addStateChangeListener(this);
            mPlayer.addErrorListener(this);
            mPlayer.addInfoListener(this);
            mPlayer.addCuesListener(this);
        }
    }

    private void clearPlayerCallbacks() {

        if (mPlayer != null) {
            Log.d(TAG, "Clear playback callbacks");
            mPlayer.removeStateChangeListener(this);
            mPlayer.removeErrorListener(this);
            mPlayer.removeInfoListener(this);
            mPlayer.removeCuesListener(this);
        }
    }

    /**
     * This is the entry point for Subtitle rendering.
     *
     * @param cues Subtitle cues.
     */
    @Override
    public void onCues(List<Cue> cues) {
        // Unfortunately need to convert again as interface is player agnostic.
        final List<com.google.android.exoplayer.text.Cue> convertedCues = new ArrayList<>();
        com.google.android.exoplayer.text.Cue aCue;
        for (com.amazon.mediaplayer.playback.text.Cue cue : cues) {
            aCue = new com.google.android.exoplayer.text.Cue(cue.text,
                    cue.textAlignment,
                    cue.line,
                    cue.lineType,
                    cue.lineAnchor,
                    cue.position,
                    cue.positionAnchor,
                    cue.size);
            convertedCues.add(aCue);
        }
        mSubtitleLayout.setCues(convertedCues);
    }

    /**
     * Create and return AdMetaData object from extras received from ad module,
     * consumer of this object is Analytics module
     *
     * @param extras Bundle containing ad metadata
     * @return POJO with ad metadata
     */
    private AdMetaData getAdAnalyticsData(Bundle extras) {

        AdMetaData adMetaData = new AdMetaData();
        if (extras != null) {
            if (extras.getString(IAds.ID) != null) {
                adMetaData.setAdId(extras.getString(IAds.ID));
            }
            if (extras.getString(IAds.AD_TYPE) != null) {
                adMetaData.setAdType(extras.getString(IAds.AD_TYPE));
            }
            adMetaData.setDurationReceived(extras.getLong(IAds.DURATION_RECEIVED));
            adMetaData.setDurationPlayed(extras.getLong(IAds.DURATION_PLAYED));
            Log.d(TAG, "Ad details: " + adMetaData.toString());
        }
        return adMetaData;
    }

    /**
     * Checks if the content's duration is a valid duration in the ad interface's extras bundle. A
     * valid duration is a value greater than zero.
     *
     * @return True if the duration is greater than, false otherwise.
     */
    private boolean isContentDurationSetInAds() {

        if (mAdsImplementation != null && mAdsImplementation.getExtra() != null &&
                mAdsImplementation.getExtra().get(IAds.VIDEO_EXTRAS) != null) {
            Bundle bundle = (Bundle) mAdsImplementation.getExtra().get(IAds.VIDEO_EXTRAS);
            return bundle != null && bundle.getLong(IAds.VIDEO_DURATION) > 0;
        }
        return false;
    }

    private IAds.IAdsEvents mIAdsEvents = new IAds.IAdsEvents() {
        @Override
        public void onAdSlotStarted(Bundle extras) {

            Log.d(TAG, "onAdSlotStarted");

            // Hide media controller.
            if (mPlaybackOverlayFragment != null && mPlaybackOverlayFragment.getView() != null) {
                mPlaybackOverlayFragment.getView().setVisibility(View.INVISIBLE);
            }

            // Pause the video if we are already playing a content.
            if (mPlayer != null && isPlaying()) {
                mPlayer.pause();
                mCurrentPlaybackPosition = getCurrentPosition();
                AnalyticsHelper.trackPlaybackFinished(mSelectedContent, mStartingPlaybackPosition,
                        mCurrentPlaybackPosition);
                mStartingPlaybackPosition = mCurrentPlaybackPosition;
            }

            hideProgress();
            AnalyticsHelper.trackAdStarted(mSelectedContent, getCurrentPosition(),
                    getAdAnalyticsData(extras));
            switchToAdsView();
            disableMediaSession();
        }

        @Override
        public void onAdSlotEnded(final Bundle extras) {

            Log.d(TAG, "onAdSlotEnded");

            // If the extra doesn't exist then we'll treat this with a default value of true.
            boolean adPodComplete = extras == null || !extras.containsKey(IAds.AD_POD_COMPLETE) ||
                    extras.getBoolean(IAds.AD_POD_COMPLETE);

            // We only want to show the video view if the group of ads has finished playing.
            if (adPodComplete) {
                showProgress();
                switchToContentView();
                enableMediaSession();
            }

            AnalyticsHelper.trackAdEnded(mSelectedContent, getCurrentPosition(),
                    getAdAnalyticsData(extras));

            String adType = null;
            if (extras != null) {
                adType = extras.getString(IAds.AD_TYPE);
            }

            if (adPodComplete) {

                if (adType != null && adType.equals(IAds.MID_ROLL_AD)) {

                    // Show the playback overlay and continue playing the video if the group of
                    // ads has finished playing.
                    if (mPlaybackOverlayFragment != null
                            && mPlaybackOverlayFragment.getView() != null) {
                        mPlaybackOverlayFragment.getView().setVisibility(View.VISIBLE);
                    }
                    // Resume movie after a mid roll if activity was not stopped else open the
                    // player again with selected content
                    if (mPlayer != null && mPlayer.getPlayerState() == PlayerState.READY) {
                        mPlayer.play();
                    }
                    else {
                        openContentHelper(mSelectedContent);
                    }
                }
                // If we came back from a post-roll ad that means the content is finished.
                else if (adType != null && adType.equals(IAds.POST_ROLL_AD)) {
                    playbackFinished();
                }
                else {
                    // Open Slider as pre roll ad pod completed.
                    openContentHelper(mSelectedContent);
                }
            }

//            /* Zype, Evgeny Cherkasov */
//            // Set ad number to the next scheduled
//            int adNumber = mAdsImplementation.getExtra().getInt(IAds.AD_NUMBER);
//            if (adNumber >= 0) {
//                if (mSelectedContent.getAdCuePoints().size() > adNumber + 1) {
//                    adNumber += 1;
//                    mAdsImplementation.getExtra().putInt(IAds.AD_NUMBER, adNumber);
//                    String adTag = AdMacrosHelper.updateAdTagParameters(PlaybackActivity.this, (String) mSelectedContent.getExtraValueAsList(Content.EXTRA_AD_TAGS).get(adNumber));
//                    mAdsImplementation.getExtra().putString("VASTAdTag", adTag);
//                    mAdsImplementation.getExtra().putBoolean(IAds.WAS_A_MID_ROLL, true);
//                    // Start tracking video position changes.
//                    mVideoPositionTrackingHandler.post(mVideoPositionTrackingRunnable);
//                }
//                else {
//                    mAdsImplementation.getExtra().putInt(IAds.AD_NUMBER, -1);
//                    mAdsImplementation.getExtra().putString("VASTAdTag", null);
//                }
//            }
        }

    };

    /**
     * Get video extras bundle.
     *
     * @param content Content object.
     * @return Result bundle.
     */
    private Bundle getVideoExtrasBundle(Content content) {

        Bundle videoExtras = new Bundle();
        videoExtras.putString(Content.ID_FIELD_NAME, content.getId());
        String adId = (String) content.getExtraValue(Content.AD_ID_FIELD_NAME);

        // Adding this to keep the code backward compatible
        if (adId == null) {
            adId = content.getId();
        }
        videoExtras.putString(Content.AD_ID_FIELD_NAME, adId);
        videoExtras.putLong(IAds.VIDEO_DURATION, getDuration());

        if (content.getAdCuePoints() != null) {
            int[] adCuePoints = new int[content.getAdCuePoints().size()];
            for (int i = 0; i < adCuePoints.length; i++) {
                adCuePoints[i] = content.getAdCuePoints().get(i);
            }
            videoExtras.putIntArray(Content.AD_CUE_POINTS_FIELD_NAME, adCuePoints);
        }
        /* Zype, Evgeny Cherkasov */
        List adTags = content.getExtraValueAsList(Content.EXTRA_AD_TAGS);
        videoExtras.putStringArrayList(Content.EXTRA_AD_TAGS, (ArrayList<String>) adTags);

        return videoExtras;
    }

    private void openContentHelper(Content content) {

        if (mPlayer != null && mPlayer.getPlayerState() == PlayerState.IDLE) {
            String url = content.getUrl();
            if (TextUtils.isEmpty(url)) {
                AnalyticsHelper.trackError(TAG, "Content URL is either null or empty for content " +
                        content.toString());
                return;
            }

            AMZNMediaPlayer.ContentMimeType type = AMZNMediaPlayer.ContentMimeType
                    .CONTENT_TYPE_UNKNOWN;
            // If the content object contains the video format type, set the ContentMimeType
            // accordingly.
            if (!TextUtils.isEmpty(content.getFormat())) {
                if (content.getFormat().equalsIgnoreCase(HLS_VIDEO_FORMAT)) {
                    type = AMZNMediaPlayer.ContentMimeType.CONTENT_HLS;
                }
            }

            mHasOutbandCC = false;
            AMZNMediaPlayer.TextMimeType ccType = AMZNMediaPlayer.TextMimeType.TEXT_WTT;

            List<String> closeCaptionUrls = content.getCloseCaptionUrls();
            String closeCaptionUrl = null;

            if (content.hasCloseCaption() && closeCaptionUrls.size() > 0) {
                // We prefer first selection against others.
                closeCaptionUrl = closeCaptionUrls.get(0);
            }

            // If there is a close caption url then find the extension and call
            // open accordingly.
            if (content.hasCloseCaption() &&
                    closeCaptionUrl != null &&
                    closeCaptionUrl.length() > 4) {

                int lastDot = closeCaptionUrl.lastIndexOf('.');
                if (lastDot > 0) {
                    String ext = closeCaptionUrl.substring(lastDot + 1);
                    if (ext.equals("vtt")) {
                        mHasOutbandCC = true;
                        ccType = AMZNMediaPlayer.TextMimeType.TEXT_WTT;
                        Log.d(TAG, "Close captioning is enabled & its format is TextWTT");
                    }
                    else if (ext.equals("xml")) {
                        mHasOutbandCC = true;
                        ccType = AMZNMediaPlayer.TextMimeType.TEXT_TTML;
                        Log.d(TAG, "Close captioning is enabled & its format is TextTTML");
                    }
                }
            }

            if (mPlaybackOverlayFragment != null) {
                mPlaybackOverlayFragment.updateCurrentContent(mSelectedContent);
            }

            // TODO: refactor out the Amazon media player code to make this activity player
            // agnostic, Devtech-2634
            AMZNMediaPlayer.ContentParameters contentParameters =
                    new AMZNMediaPlayer.ContentParameters(url, type);
            DrmProvider drmProvider = new DrmProvider(content, this);
            contentParameters.laurl = drmProvider.fetchLaUrl();
            contentParameters.encryptionSchema = getAmznMediaEncryptionSchema(drmProvider);

            if (mHasOutbandCC) {
                contentParameters.oobTextSources = new AMZNMediaPlayer.OutOfBandTextSource[]{new
                        AMZNMediaPlayer.OutOfBandTextSource(closeCaptionUrl, ccType, "en")};
                mPlayer.open(contentParameters);
                Log.d(TAG, "Media player opened with outband close captioning support");
            }
            else {
                mPlayer.open(contentParameters);
                Log.d(TAG, "Media player opened without outband close captioning support");
            }

        }
        else {
            hideProgress();
        }
    }

    /**
     * Fetches the encryption schema from the resources. If the schema is not available default is
     * sent.
     *
     * @param drmProvider DrmProvider instance
     * @return encryption schema
     */
    private AMZNMediaPlayer.EncryptionSchema getAmznMediaEncryptionSchema(DrmProvider drmProvider) {

        String encryptionSchema = drmProvider.getEncryptionSchema();

        switch (encryptionSchema) {
            case "ENCRYPTION_PLAYREADY":
                return AMZNMediaPlayer.EncryptionSchema.ENCRYPTION_PLAYREADY;
            case "ENCRYPTION_WIDEVINE":
                return AMZNMediaPlayer.EncryptionSchema.ENCRYPTION_WIDEVINE;
            default:
                return AMZNMediaPlayer.EncryptionSchema.ENCRYPTION_DEFAULT;
        }
    }

    private void openSelectedContent() {

        Log.d(TAG, "Open content");

        mAdsImplementation.setIAdsEvents(mIAdsEvents);

        // Hide videoView which make adsView visible.
        switchToAdsView();

        // Hide media controller.
        if (mPlaybackOverlayFragment != null && mPlaybackOverlayFragment.getView() != null) {
            mPlaybackOverlayFragment.getView().setVisibility(View.INVISIBLE);
        }
        // Show progress before pre roll ad.
        showProgress();

        /* Zype, Evgeny Cherkasov */
//        // Get video extras bundle.
//        Bundle videoExtras = getVideoExtrasBundle(mSelectedContent);
//        // We need to pass id, duration and adCuePoints to player interface.
//        // Usually required for ads support.
//        // The player interface doesn't know about the video model so we are using Bundles.
//        mPlayer.getExtra().putBundle(IAds.VIDEO_EXTRAS, videoExtras);
//
//        // Set Ads video extras.
//        mAdsImplementation.getExtra().putBundle(IAds.VIDEO_EXTRAS, videoExtras);

        // Request Zype API for player data
        String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
        HashMap<String, String> params = getValues();
        if (!TextUtils.isEmpty(accessToken)) {
            params.put(ZypeApi.ACCESS_TOKEN, accessToken);
        }
        else {
            params.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
        }
        String uuid = AdMacrosHelper.getAdvertisingId(this);
        if (!TextUtils.isEmpty(uuid)) {
            params.put(ZypeApi.UUID, uuid);
        }
        ZypeApi.getInstance().getApi()
                .getPlayer(getUserAgent(PlaybackActivity.this, getString(R.string.app_name_short)),
                                            mSelectedContent.getId(), params)
                    .enqueue(new Callback<PlayerResponse>() {
            @Override
            public void onResponse(Call<PlayerResponse> call, Response<PlayerResponse> response) {
                if (response.isSuccessful()) {
                    if (!response.body().playerData.body.files.isEmpty()) {
                        updateContentWithPlayerData(mSelectedContent, response.body().playerData);
                    }
                    else {
                        updateContentWithPlayerData(mSelectedContent, null);
                    }
                }
                else {
                    updateContentWithPlayerData(mSelectedContent, null);
                    if (response.code() == 403) {
                        String errorMessage = null;
                        ErrorBody errorBody = ErrorHelper.parseError(response);
                        if (errorBody != null) {
                            errorMessage = errorBody.message;
                        }
                        mErrorDialogFragment = ErrorDialogFragment.newInstance(PlaybackActivity.this,
                                ErrorUtils.ERROR_CATEGORY.ZYPE_CUSTOM, errorMessage, PlaybackActivity.this);
                        mErrorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                        return;
                    }
                }
                if (mSelectedContent.getAdCuePoints() != null) {
                    Log.d(TAG, "openSelectedContent(): Ad tags count = " + mSelectedContent.getAdCuePoints().size());
                }
                else {
                    Log.d(TAG, "openSelectedContent(): No ad tags");
                }

                //Alamai analytics
//                setupAkamai();

                showAds();
            }

            @Override
            public void onFailure(Call<PlayerResponse> call, Throwable t) {
                updateContentWithPlayerData(mSelectedContent, null);
                showAds();
            }
        });
    }


    private void releasePlayer() {

        // Remove ads event listener.
        if (mAdsImplementation != null) {
            mAdsImplementation.setIAdsEvents(null);
        }

        if (mPlayer != null) {
            Log.d(TAG, "Release player");
            clearPlayerCallbacks();
            mPlayer.close();
            mPlayer.release();
            mPlayer = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPlayerStateChange(PlayerState oldState, PlayerState newState, Bundle extra) {

        mPrevState = mCurrentState;
        mCurrentState = newState;
        Log.d(TAG, "State change event! Oldstate= " + oldState + " NewState= " + newState);
        if (mPrevState == mCurrentState) {
            // Just to catch this while under dev
            Log.w(TAG, "Duplicate state change message!!! ");
        }
        // If buffering stopped
        if (mPrevState == PlayerState.BUFFERING && mCurrentState != PlayerState.BUFFERING) {
            AnalyticsHelper.trackPlaybackControlAction(AnalyticsTags.ACTION_PLAYBACK_BUFFER_END,
                    mSelectedContent, getCurrentPosition());
            /* Zype, Evgeny Cherkasov */
            // Akamai analytics
//            akamaiPlugin.handleBufferEnd();
        }
        /* Zype, Evgeny Cherkasov */
        // Akamai analytics
        if (mPrevState == PlayerState.SEEKING && mCurrentState != PlayerState.SEEKING) {
//            akamaiPlugin.handleSeekEnd(getCurrentPosition());
        }
        switch (newState) {
            case IDLE:
                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (mIsContentChangeRequested) {
                    openSelectedContent();
                }
                if (mMediaSessionController != null) {
                    mMediaSessionController.updatePlaybackState(PlaybackState.STATE_NONE,
                            getCurrentPosition());
                }
                break;
            case OPENING:
                break;
            case OPENED:
                if (mPlayer != null && mIsActivityResumed) {
                    mPlayer.prepare();
                }
                else {
                    mIsContentChangeRequested = false;
                }
                break;
            case PREPARING:
                // Show media controller.
                if (mPlaybackOverlayFragment != null && mPlaybackOverlayFragment.getView() !=
                        null) {
                    mPlaybackOverlayFragment.getView().setVisibility(View.VISIBLE);
                }
                break;
            case READY:
                mPlaybackState = LeanbackPlaybackState.PAUSED;
                if (mPlaybackOverlayFragment != null) {
                    mPlaybackOverlayFragment.togglePlaybackUI(false);
                }
                hideProgress();
                setupMessageView();

                /* Zype, Evgeny Cherkasov */
                mPlayer.updateSurfaceView();

                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (mPrevState == PlayerState.PREPARING) {
                    if (mPlaybackOverlayFragment != null) {
                        // Remember CC state for playbacks.
                        /* Zype, Evgeny Cherkasov */
                        ccTrack = Preferences.getString(PreferencesConstants.SELECTED_CLOSED_CAPTIONS_TRACK);
                        modifyClosedCaptionState(mIsClosedCaptionEnabled);

                        mPlaybackOverlayFragment.updatePlayback();
                        mPlaybackOverlayFragment.startProgressAutomation();
                    }
                    if (mCurrentPlaybackPosition > 0 && mCurrentPlaybackPosition !=
                            getCurrentPosition()) {
                        mPlayer.seekTo(mCurrentPlaybackPosition);
                    }
                    // One of the causes for the player state transition might be due to
                    // a new content being selected from recommended content.
                    if (mAutoPlay || mIsContentChangeRequested) {
                        play();
                        mAutoPlay = false;
                        if (mIsContentChangeRequested) {
                            mIsContentChangeRequested = false;
                        }
                    }
                }
                // Do not play if ads are currently being played
                else if (mAudioFocusState == AudioFocusState.NoFocusNoDuck &&
                        mAdsView.getVisibility() != View.VISIBLE) {
                    play();
                }

                // Let ads implementation track player state.
                if (mAdsImplementation != null) {
                    // Reset the video extras bundle in the ads interface if a valid
                    // duration isn't set yet.
                    if (!isContentDurationSetInAds()) {
                        mAdsImplementation.getExtra().putBundle(IAds.VIDEO_EXTRAS,
                                getVideoExtrasBundle
                                        (mSelectedContent));
                    }
                    mAdsImplementation.setPlayerState(IAds.PlayerState.PAUSED);
                }
                if (mMediaSessionController != null) {
                    mMediaSessionController.updatePlaybackState(PlaybackState.STATE_PAUSED,
                            getCurrentPosition());
                }
                /* Zype, Evgeny Cherkasov */
                // Akamai analytics
//                akamaiPlugin.handlePause();
                break;
            case PLAYING:
                mPlaybackState = LeanbackPlaybackState.PLAYING;

                if (mPlaybackOverlayFragment != null) {
                    // TODO: remove this update once we find a way to get duration and cc state
                    // from bright cove before PLAYING state. DEVTECH-4973
                    if (mPrevState == PlayerState.READY || mPrevState == PlayerState.SEEKING) {
                        modifyClosedCaptionState(mIsClosedCaptionEnabled);
                        mPlayer.updateSurfaceView();
                        mPlaybackOverlayFragment.updatePlayback();
                    }
                    mPlaybackOverlayFragment.togglePlaybackUI(true);
                }
                hideProgress();
                mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


                // Let ads implementation track player state.
                if (mAdsImplementation != null) {
                    // Reset the video extras bundle in the ads interface if a valid
                    // duration isn't set yet.
                    if (!isContentDurationSetInAds()) {
                        mAdsImplementation.getExtra().putBundle(IAds.VIDEO_EXTRAS,
                                getVideoExtrasBundle
                                        (mSelectedContent));
                    }
                    mAdsImplementation.setPlayerState(IAds.PlayerState.PLAYING);
                }
                mCurrentPlaybackPosition = getCurrentPosition();
                //get analytics detail and send
                int currentSegment = 0;
                if (mAdsImplementation != null) {
                    currentSegment = mAdsImplementation.getCurrentContentSegmentNumber
                            (mCurrentPlaybackPosition / 1000, getDuration());
                    mTotalSegments = mAdsImplementation.getNumberOfSegments();
                }
                if (mMediaSessionController != null) {
                    mMediaSessionController.updatePlaybackState(PlaybackState.STATE_PLAYING,
                            getCurrentPosition());
                }
//                AnalyticsHelper.trackPlaybackStarted(mSelectedContent, getDuration(),
//                        mCurrentPlaybackPosition,
//                        mTotalSegments, currentSegment);
                mSelectedContent.setExtraValue(Content.EXTRA_ANALYTICS_CHANNEL,
                        getString(R.string.app_name_short));
                mSelectedContent.setExtraValue(Content.EXTRA_ANALYTICS_SESSION_ID,
                        AnalyticsManager.getInstance(this).getSessionId());
                AnalyticsHelper.trackPlaybackStarted(mSelectedContent, mSelectedContent.getDuration(),
                        mCurrentPlaybackPosition,
                        mTotalSegments, currentSegment);
                /* Zype, Evgeny Cherkasov */
                // Akamai analytics
//                akamaiPlugin.handlePlay();
                break;
            case BUFFERING:
                showProgress();
                if (mMediaSessionController != null) {
                    mMediaSessionController.updatePlaybackState(PlaybackState.STATE_BUFFERING,
                            getCurrentPosition());
                }
                AnalyticsHelper.trackPlaybackControlAction(AnalyticsTags
                                .ACTION_PLAYBACK_BUFFER_START,
                        mSelectedContent, getCurrentPosition());
                /* Zype, Evgeny Cherkasov */
                // Akamai analytics
//                akamaiPlugin.handleBufferStart();
                break;
            case SEEKING:
                showProgress();
                /* Zype, Evgeny Cherkasov */
                // Akamai analytics
//                akamaiPlugin.handleSeekStart(getCurrentPosition());
                break;
            case ENDED:
                hideProgress();
                // Let ads implementation track player state and check for any post-roll ads.
                if (mAdsImplementation != null && mAdsImplementation.isPostRollAvailable()) {
                    mAdsImplementation.setPlayerState(IAds.PlayerState.COMPLETED);
                }
                // If there's no ads just directly finish playback.
                else {
                    playbackFinished();
                }
                if (mMediaSessionController != null) {
                    mMediaSessionController.updatePlaybackState(PlaybackState.STATE_STOPPED,
                            getCurrentPosition());
                }
                /* Zype, Evgeny Cherkasov */
                // Akamai analytics
//                akamaiPlugin.handlePlayEnd(EndReasonCodes.Play_End_Detected.toString());
                break;
            case CLOSING:
                if (mPlaybackOverlayFragment != null) {
                    mPlaybackOverlayFragment.stopProgressAutomation();
                }
                break;
            case ERROR:
                hideProgress();
                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (mMediaSessionController != null) {
                    mMediaSessionController.updatePlaybackState(PlaybackState.STATE_ERROR,
                            getCurrentPosition());
                }
                Log.e(TAG, "Player encountered an error!");
                /* Zype, Evgeny Cherkasov */
                // Akamai analytics
//                akamaiPlugin.handleError("");
                break;
            default:
                Log.e(TAG, "Unknown state!!!!!");
                break;
        }
    }

    /**
     * Private helper method to do some cleanup when playback has finished.
     */
    private void playbackFinished() {

        if (mPlaybackOverlayFragment != null) {
            mPlaybackOverlayFragment.playbackFinished();
        }
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onInfo(AMZNMediaPlayer.Info info) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(AMZNMediaPlayer.Error e) {

        if (Helpers.isConnectedToNetwork(this)) {
            Log.e(TAG, "Media Player error during playback", e.mException);
            mErrorDialogFragment = ErrorDialogFragment.newInstance(this, ErrorUtils
                    .ERROR_CATEGORY.PLAYER_ERROR, this);
        }
        else {
            Log.e(TAG, "Network error during playback", e.mException);
            mErrorDialogFragment = ErrorDialogFragment.newInstance(this, ErrorUtils
                    .ERROR_CATEGORY.NETWORK_ERROR, this);
            mIsNetworkError = true;
        }
        try{
            if (!isFinishing()) {
                mErrorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
            }
        } catch (Exception ignored){

        }
    }

    private boolean requestAudioFocus() {

        if (mAudioManager == null) {
            Log.e(TAG, "mAudionManager is null in requestAudioFocus");
            return false;
        }
        boolean focus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager
                        .AUDIOFOCUS_GAIN);
        if (focus) {
            mAudioFocusState = AudioFocusState.Focused;
        }
        return focus;
    }

    private boolean abandonAudioFocus() {

        if (mAudioManager == null) {
            Log.e(TAG, "mAudionManager is null in abandonAudioFocus");
            return false;
        }
        boolean focus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager
                .abandonAudioFocus(this);
        if (focus) {
            mAudioFocusState = AudioFocusState.NoFocusNoDuck;
        }
        return focus;
    }

    /**
     * Checks if the content is live as specified by the recipe.
     *
     * @param content The Content to check.
     * @return True if the content is live, false if not live.
     */
    private boolean isContentLive(Content content) {

        return content != null && content.getExtraValue(Recipe.LIVE_FEED_TAG) != null &&
                Boolean.valueOf(content.getExtraValue(Recipe.LIVE_FEED_TAG).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {

        Log.d(TAG, "onAudioFocusChange() focusChange? " + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mAudioFocusState = AudioFocusState.Focused;
                if (mPlayer != null) {
                    mPlayer.setVolume(AUDIO_FOCUS_DEFAULT_VOLUME);
                }
                if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
                    play();
                }
                hideProgress();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mAudioFocusState = AudioFocusState.NoFocusNoDuck;
                if (isPlaying()) {
                    pause();// No audio focus, pause media!
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mAudioFocusState = AudioFocusState.NoFocusCanDuck;
                if (isPlaying()) {
                    mPlayer.setVolume(AUDIO_FOCUS_DUCK_VOLUME);
                }
                break;
            default:
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doButtonClick(ErrorDialogFragment errorDialogFragment, ErrorUtils
            .ERROR_BUTTON_TYPE errorButtonType, ErrorUtils.ERROR_CATEGORY errorCategory) {

        switch (errorCategory) {
            case PLAYER_ERROR:
                // Dismiss the dialog & finish the activity
                if (mErrorDialogFragment != null) {
                    mErrorDialogFragment.dismiss();
                    // Finish the player activity and go back to details page
                    finish();
                }
                break;
            case NETWORK_ERROR:
                if (errorButtonType == ErrorUtils.ERROR_BUTTON_TYPE.NETWORK_SETTINGS) {
                    ErrorUtils.showNetworkSettings(this);
                }
                break;
            /* Zype, Evgeny Cherkasov */
            case ZYPE_CUSTOM:
                if (mErrorDialogFragment != null) {
                    mErrorDialogFragment.dismiss();
                    finish();
                }
                break;
        }

    }

    private void showAds() {
        // Get video extras bundle.
        Bundle videoExtras = getVideoExtrasBundle(mSelectedContent);
        // We need to pass id, duration and adCuePoints to player interface.
        // Usually required for ads support.
        // The player interface doesn't know about the video model so we are using Bundles.
        if (mPlayer != null && mPlayer.getExtra() != null) {
            mPlayer.getExtra().putBundle(IAds.VIDEO_EXTRAS, videoExtras);
        }

        // Set Ads video extras.
        if (mAdsImplementation.getExtra() != null) {
            mAdsImplementation.getExtra().putBundle(IAds.VIDEO_EXTRAS, videoExtras);
        }

        mAdsImplementation.showAds();
    }

    public boolean isClosedCaptionAvailable() {

        if (mPlayer.getTrackCount(TrackType.SUBTITLE) > 0) {
            Log.d(TAG, "Subtitle Tracks Available: " + mPlayer.getTrackCount(TrackType.SUBTITLE));
            return true;
        }
        else if (ContentBrowser.getInstance(this).isEnableCEA608() &&
                mPlayer.getTrackCount(TrackType.CLOSED_CAPTION) > 0) {
            Log.d(TAG, "Closed Caption Tracks Available: " + mPlayer.getTrackCount(TrackType
                    .CLOSED_CAPTION));
            return true;
        }
        else {
            return false;
        }
    }

    /* Zype, Evgeny Cherkasov */
    private Content updateContentWithPlayerData(Content content, PlayerData playerData) {
        if (playerData != null) {
            // Url
            String url = playerData.body.files.get(0).url;
            content.setUrl(url);
            content.setExtraValue(Content.EXTRA_VIDEO_URL, content.getUrl());
            checkIsAudio(url);
            // Ads
            if (playerData.body.advertising != null && playerData.body.advertising.schedule.size() > 0) {
                content.setAdCuePoints(new ArrayList<>());
                List<String> adTags = new ArrayList<>();
                String adTag;
                for (AdvertisingSchedule item : playerData.body.advertising.schedule) {
                    content.getAdCuePoints().add(item.offset);
                    adTag = AdMacrosHelper.updateAdTagParameters(this, item.tag);
                    adTags.add(adTag);
                }
                content.setExtraValue(Content.EXTRA_AD_TAGS, adTags);
            }
            else {
                content.setAdCuePoints(null);
                content.setExtraValue(Content.EXTRA_AD_TAGS, null);
            }
            // Analytics
            if (playerData.body.analytics != null) {
                content.setExtraValue(Content.EXTRA_ANALYTICS_BEACON, playerData.body.analytics.beacon);
                if (playerData.body.analytics.dimensions != null) {
                    if (ContentBrowser.getInstance(this).isUserLoggedIn()) {
                        content.setExtraValue(Content.EXTRA_ANALYTICS_CONSUMER_ID,Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_ID));
                    }
                    content.setExtraValue(Content.EXTRA_ANALYTICS_DEVICE, playerData.body.analytics.dimensions.device);
                    content.setExtraValue(Content.EXTRA_ANALYTICS_PLAYER_ID, playerData.body.analytics.dimensions.playerId);
                    content.setExtraValue(Content.EXTRA_ANALYTICS_SITE_ID, playerData.body.analytics.dimensions.siteId);
                    content.setExtraValue(Content.EXTRA_ANALYTICS_VIDEO_ID, playerData.body.analytics.dimensions.videoId);
                }
            }
        }
        else {
            content.setUrl("null");
            content.setExtraValue(Content.EXTRA_VIDEO_URL, "null");
            content.setAdCuePoints(null);
            content.setExtraValue(Content.EXTRA_AD_TAGS, null);
            content.setExtraValue(Content.EXTRA_ANALYTICS_BEACON, null);
            content.setExtraValue(Content.EXTRA_ANALYTICS_CONSUMER_ID, null);
            content.setExtraValue(Content.EXTRA_ANALYTICS_DEVICE, null);
            content.setExtraValue(Content.EXTRA_ANALYTICS_PLAYER_ID, null);
            content.setExtraValue(Content.EXTRA_ANALYTICS_SITE_ID, null);
            content.setExtraValue(Content.EXTRA_ANALYTICS_VIDEO_ID, null);
        }
        return content;
    }

    private void checkIsAudio(String url) {
        String urlLower = url.toLowerCase();
        isAudio = urlLower.contains(".mp3")
                || urlLower.contains(".m4a")
                || urlLower.contains(".wav")
                || urlLower.contains(".wma")
                || urlLower.contains(".aiff")
                || urlLower.contains(".ac3");
    }
//    private void setupAkamai() {
//        akamaiPlugin = new AnalyticsPlugin(getApplicationContext(), getAkamaiConfigUrl());
//        if (ContentBrowser.getInstance(this).isUserLoggedIn()) {
//            AnalyticsPlugin.setViewerId(Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_ID));
//        }
//        akamaiPlugin.handleSessionInit(new PluginCallBacks() {
//            @Override
//            public float streamHeadPosition() {
//                return getCurrentPosition();
//            }
//
//            @Override
//            public float streamLength() {
//                return mPlayer.getDuration();
//            }
//
//            @Override
//            public float getFps() {
//                return 0;
//            }
//
//            @Override
//            public String streamURL() {
//                return mSelectedContent.getUrl();
//            }
//
//            @Override
//            public boolean isLive() {
//                return isContentLive(mSelectedContent);
//            }
//
//            @Override
//            public String videoSize() {
//                return mPlayer.getCurrentVideoWidth() + "x" + mPlayer.getCurrentVideoHeight();
//            }
//
//            @Override
//            public String viewSize() {
//                return mVideoView.getWidth() + "x" + mVideoView.getHeight();
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
//                return PlaybackActivity.this.isPlaying();
//            }
//        }, false);
//        akamaiPlugin.setData("device", mSelectedContent.getExtraValueAsString(Content.EXTRA_ANALYTICS_DEVICE));
//        akamaiPlugin.setData("playerId", mSelectedContent.getExtraValueAsString(Content.EXTRA_ANALYTICS_PLAYER_ID));
//        akamaiPlugin.setData("siteId", mSelectedContent.getExtraValueAsString(Content.EXTRA_ANALYTICS_SITE_ID));
//        akamaiPlugin.setData("videoId", mSelectedContent.getExtraValueAsString(Content.EXTRA_ANALYTICS_VIDEO_ID));
//    }

//    private String getAkamaiConfigUrl() {
//        return mSelectedContent.getExtraValueAsString(Content.EXTRA_ANALYTICS_BEACON);
//    }

    private int getClosedCaptionsTrackIndex(String track) {
        int result = -1;
        if (mPlayer != null) {
            TrackType type;
            if (ContentBrowser.getInstance(this).isEnableCEA608()) {
                type = TrackType.CLOSED_CAPTION;
            }
            else {
                type = TrackType.SUBTITLE;
            }
            for (int i = 0; i < mPlayer.getTrackCount(type); i++) {
                MediaFormat mediaFormat = mPlayer.getTrackFormat(type, i);
                if (mediaFormat != null) {
                    if (track.equalsIgnoreCase(mediaFormat.mTrackId)) {
                        result = i;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private void showClosedCaptionDialog() {
        if (mPlayer == null) {
            return;
        }
        // Get CC tracks
        TrackType type;
        if (ContentBrowser.getInstance(this).isEnableCEA608()) {
            type = TrackType.CLOSED_CAPTION;
        }
        else {
            type = TrackType.SUBTITLE;
        }
        List<CharSequence> tracks = new ArrayList<>();
        for (int i = 0; i < mPlayer.getTrackCount(type); i++) {
            MediaFormat mediaFormat = mPlayer.getTrackFormat(type, i);
            if (mediaFormat != null) {
                tracks.add(mediaFormat.mTrackId);
            }
        }

        // Show selection dialog
        SubtitlesDialogFragment.createAndShowSubtitlesDialogFragment(this,
                "Select track",
                tracks.toArray(new CharSequence[tracks.size()]),
                getClosedCaptionsTrackIndex(Preferences.getString(PreferencesConstants.SELECTED_CLOSED_CAPTIONS_TRACK)),
                new SubtitlesDialogFragment.ISubtitlesDialogListener() {
                    @Override
                    public void onItemSelected(SubtitlesDialogFragment dialog, int selectedItem) {
                        dialog.dismiss();
                        ccTrack = tracks.get(selectedItem).toString();
                        Preferences.setString(PreferencesConstants.SELECTED_CLOSED_CAPTIONS_TRACK, ccTrack);
                        modifyClosedCaptionState(true);
                    }
                });
    }
}

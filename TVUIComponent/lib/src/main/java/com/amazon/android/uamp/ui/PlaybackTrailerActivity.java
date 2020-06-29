/**
 * This file was modified by Amazon:
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0/
 * <p>
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

import android.content.Context;
import android.media.AudioManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.leanback.app.TenFootPlaybackOverlayFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.amazon.android.contentbrowser.helper.AnalyticsHelper;
import com.amazon.android.model.content.Content;
import com.amazon.android.module.ModuleManager;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.uamp.DrmProvider;
import com.amazon.android.uamp.UAMP;
import com.amazon.android.uamp.mediaSession.MediaSessionController;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.Helpers;
import com.amazon.android.utils.Preferences;
import com.amazon.mediaplayer.AMZNMediaPlayer;
import com.amazon.mediaplayer.AMZNMediaPlayer.PlayerState;
import com.zype.fire.api.Model.ErrorBody;
import com.zype.fire.api.Model.PlayerData;
import com.zype.fire.api.Model.PlayerResponse;
import com.zype.fire.api.Util.ErrorHelper;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeSettings;
import com.zype.fire.auth.ZypeAuthentication;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaybackTrailerActivity extends BasePlaybackActivity implements
    AMZNMediaPlayer
        .OnStateChangeListener, AMZNMediaPlayer.OnErrorListener, AMZNMediaPlayer.OnInfoListener,
    AudioManager.OnAudioFocusChangeListener,
    ErrorDialogFragment.ErrorDialogFragmentListener, PlaybackOverlayFragment.OnPlayPauseClickedListener {

  private static final String TAG = PlaybackTrailerActivity.class.getSimpleName();
  private static final String HLS_VIDEO_FORMAT = "HLS";


  private static final float AUDIO_FOCUS_DUCK_VOLUME = 0.1f;
  private static final float AUDIO_FOCUS_DEFAULT_VOLUME = 1.0f;
  private static final CookieManager DEFAULT_COOKIE_MANAGER;
  private static final int TRANSPORT_CONTROLS_DELAY_PERIOD = 50;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private FrameLayout mVideoView;
  private UAMP mPlayer;
  private Content mSelectedContent;
  private LeanbackPlaybackState mPlaybackState = LeanbackPlaybackState.IDLE;
  private PlayerState mPrevState;
  private PlayerState mCurrentState;
  private ProgressBar mProgressBar;
  private Window mWindow;
  private AudioManager mAudioManager;
  private AudioFocusState mAudioFocusState = AudioFocusState.NoFocusNoDuck;
  private ErrorDialogFragment mErrorDialogFragment = null;
  private MediaSessionController mMediaSessionController;
  private PlaybackOverlayFragment mPlaybackOverlayFragment;
  private Handler mTransportControlsUpdateHandler;
  private ContinualFwdUpdater mContinualFwdUpdater;
  private ContinualRewindUpdater mContinualRewindUpdater;
  private boolean mIsLongPress;
  private String previousPlayUrl = "";

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


    setContentView(R.layout.playback_controls_trailer);

    mWindow = getWindow();

    mProgressBar = (ProgressBar) findViewById(R.id.playback_progress);

    mSelectedContent =
        (Content) getIntent().getSerializableExtra("play_trailer");
    previousPlayUrl = getIntent().getStringExtra("previous_play_url");

    if (mSelectedContent == null || TextUtils.isEmpty(mSelectedContent.getTrailerId())) {
      AnalyticsHelper.trackError(TAG, "Received an Intent to play trailer without a " +
          "trailer id");
      finish();
      return;
    }

    mPlaybackOverlayFragment =
        (PlaybackOverlayFragment) getFragmentManager()
            .findFragmentById(R.id.playback_controls_fragment);

    mTransportControlsUpdateHandler = new Handler(Looper.getMainLooper());
    mContinualFwdUpdater = new ContinualFwdUpdater();
    mContinualRewindUpdater = new ContinualRewindUpdater();
    mIsLongPress = false;

    loadViews();
    createPlayerAndInitializeListeners();
    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    initMediaSession();
    openSelectedContent();
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
   * {@inheritDoc}
   */
  @Override
  protected void onStart() {

    super.onStart();

    requestAudioFocus();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected void onStop() {

    super.onStop();

    abandonAudioFocus();

    if (mPlayer != null) {
      mPlayer.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDestroy() {

    super.onDestroy();
    // Let ads implementation track player activity lifecycle.
    releasePlayer();

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

  private void play() {

    if (mPlayer != null) {
      if (requestAudioFocus()) {
        mPlayer.play();
      } else {
        showProgress();
        if (mPlaybackOverlayFragment != null) {
          mPlaybackOverlayFragment.togglePlaybackUI(true);
        }
      }
    }
  }

  private void pause() {

    if (mPlayer != null && isPlaying()) {
      mPlayer.pause();
    }
    if (mPlaybackOverlayFragment != null) {
      mPlaybackOverlayFragment.togglePlaybackUI(false);
    }
  }


  /**
   * {@inheritDoc}
   */

  public int getDuration() {

    long duration = 0;
    if (mPlayer != null) {
      duration = mPlayer.getDuration();
      if (duration == AMZNMediaPlayer.UNKNOWN_TIME) {
        Log.i(TAG, "Content duration is unknown. Returning 0.");
        duration = 0;
      }
    }

    return (int) duration;
  }

  /**
   * {@inheritDoc}
   */

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


  private void loadViews() {

    mVideoView = (FrameLayout) findViewById(R.id.videoView);
    // Avoid focus stealing.
    mVideoView.setFocusable(false);
    mVideoView.setFocusableInTouchMode(false);
    mVideoView.setClickable(false);
    mVideoView.setVisibility(View.VISIBLE);
    switchToVideoView();
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

  private void switchToVideoView() {
    // Show Video view.
    setVisibilityOfViewGroupWithInnerSurfaceView(mVideoView, View.VISIBLE);

  }

  private void createPlayerAndInitializeListeners() {

    if (mPlayer == null) {
      Log.d(TAG, "Create Player and Initialize Listeners");
      mPrevState = PlayerState.IDLE;
      mCurrentState = PlayerState.IDLE;
      Bundle playerExtras = new Bundle();

      // Create a player interface by using the default hooked implementation.
      String playerInterfaceName = UAMP.class.getSimpleName();
      mPlayer = (UAMP) ModuleManager.getInstance()
          .getModule(playerInterfaceName)
          .createImpl();

      // Init player interface, this is where it is fully created.
      mPlayer.init(this, mVideoView, playerExtras);

      mPlayer.setUserAgent(getUserAgent(PlaybackTrailerActivity.this,
              getString(R.string.app_name_short)));
      mPlayer.addStateChangeListener(this);
      mPlayer.addErrorListener(this);
      mPlayer.addInfoListener(this);
    }
  }

  private void clearPlayerCallbacks() {

    if (mPlayer != null) {
      Log.d(TAG, "Clear playback callbacks");
      mPlayer.removeStateChangeListener(this);
      mPlayer.removeErrorListener(this);
      mPlayer.removeInfoListener(this);
    }
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

      // TODO: refactor out the Amazon media player code to make this activity player
      // agnostic, Devtech-2634
      AMZNMediaPlayer.ContentParameters contentParameters =
          new AMZNMediaPlayer.ContentParameters(url, type);
      DrmProvider drmProvider = new DrmProvider(content, this);
      contentParameters.laurl = drmProvider.fetchLaUrl();
      contentParameters.encryptionSchema = getAmznMediaEncryptionSchema(drmProvider);
      mPlayer.open(contentParameters);

      mPlaybackOverlayFragment.updateCurrentContent(content);
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

  /* Zype, Evgeny Cherkasov */
  private void updateContentWithPlayerData(Content content, PlayerData playerData) {
    if (playerData != null) {
      // Url
      String url = playerData.body.files.get(0).url;

      if (!TextUtils.isEmpty(previousPlayUrl)) {
        url = url + previousPlayUrl;
      }

      content.setUrl(url);
      content.setTitle("");
      content.setExtraValue(Content.EXTRA_VIDEO_URL, content.getUrl());
      openContentHelper(content);
    }
  }

  private void openSelectedContent() {

    showProgress();

    // Request Zype API for player data
    String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
    HashMap<String, String> params = getValues();
    if (!TextUtils.isEmpty(accessToken)) {
      params.put(ZypeApi.ACCESS_TOKEN, accessToken);
    } else {
      params.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
    }

    ZypeApi.getInstance().getApi()
            .getPlayer(getUserAgent(PlaybackTrailerActivity.this, getString(R.string.app_name_short)),
                    mSelectedContent.getTrailerId(), params)
            .enqueue(new Callback<PlayerResponse>() {
      @Override
      public void onResponse(Call<PlayerResponse> call, Response<PlayerResponse> response) {
        if (response.isSuccessful()) {
          if (!response.body().playerData.body.files.isEmpty()) {
            updateContentWithPlayerData(mSelectedContent, response.body().playerData);
          } else {
            updateContentWithPlayerData(mSelectedContent, null);
          }
        } else {
          updateContentWithPlayerData(mSelectedContent, null);
          if (response.code() == 403) {
            String errorMessage = null;
            ErrorBody errorBody = ErrorHelper.parseError(response);
            if (errorBody != null) {
              errorMessage = errorBody.message;
            }
            mErrorDialogFragment = ErrorDialogFragment.newInstance(PlaybackTrailerActivity.this,
                ErrorUtils.ERROR_CATEGORY.ZYPE_CUSTOM, errorMessage, PlaybackTrailerActivity.this);
            mErrorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
            return;
          }
        }
      }

      @Override
      public void onFailure(Call<PlayerResponse> call, Throwable t) {
        updateContentWithPlayerData(mSelectedContent, null);
      }
    });
  }

  private void releasePlayer() {

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

    if (mPrevState == PlayerState.SEEKING && mCurrentState != PlayerState.SEEKING) {
//            akamaiPlugin.handleSeekEnd(getCurrentPosition());
    }
    switch (newState) {
      case IDLE:
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (mMediaSessionController != null) {
          mMediaSessionController.updatePlaybackState(PlaybackState.STATE_NONE,
              getCurrentPosition());
        }
        break;
      case OPENING:
        break;
      case OPENED:
        if (mPlayer != null) {
          mPlayer.prepare();
        }
        break;
      case PREPARING:
        if (mPlaybackOverlayFragment != null && mPlaybackOverlayFragment.getView() !=
            null) {
          mPlaybackOverlayFragment.getView().setVisibility(View.VISIBLE);
        }
        break;
      case READY:
        mPlaybackState = LeanbackPlaybackState.PAUSED;
        hideProgress();

        /* Zype, Evgeny Cherkasov */
        mPlayer.updateSurfaceView();

        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mPrevState == PlayerState.PREPARING) {
          play();
        }
        // Do not play if ads are currently being played
        else if (mAudioFocusState == AudioFocusState.NoFocusNoDuck) {
          play();
        }

        if (mMediaSessionController != null) {
          mMediaSessionController.updatePlaybackState(PlaybackState.STATE_PAUSED,
              getCurrentPosition());
        }

        if (mPlaybackOverlayFragment != null) {
          mPlaybackOverlayFragment.togglePlaybackUI(false);
          mPlaybackOverlayFragment.updatePlayback();
          mPlaybackOverlayFragment.startProgressAutomation();
        }

        /* Zype, Evgeny Cherkasov */
        // Akamai analytics
//                akamaiPlugin.handlePause();
        break;
      case PLAYING:
        mPlaybackState = LeanbackPlaybackState.PLAYING;

        hideProgress();
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (mMediaSessionController != null) {
          mMediaSessionController.updatePlaybackState(PlaybackState.STATE_PLAYING,
              getCurrentPosition());
        }
        if (mPlaybackOverlayFragment != null) {
          mPlaybackOverlayFragment.togglePlaybackUI(true);
        }
        break;
      case BUFFERING:
        showProgress();
        if (mMediaSessionController != null) {
          mMediaSessionController.updatePlaybackState(PlaybackState.STATE_BUFFERING,
              getCurrentPosition());
        }
        break;
      case SEEKING:
        showProgress();
        break;
      case ENDED:
        hideProgress();
        playbackFinished();

        if (mMediaSessionController != null) {
          mMediaSessionController.updatePlaybackState(PlaybackState.STATE_STOPPED,
              getCurrentPosition());
        }
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
    if (mPlaybackOverlayFragment != null) {
      mPlaybackOverlayFragment.stopProgressAutomation();
    }
    mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    finish();
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
    } else {
      Log.e(TAG, "Network error during playback", e.mException);
      mErrorDialogFragment = ErrorDialogFragment.newInstance(this, ErrorUtils
          .ERROR_CATEGORY.NETWORK_ERROR, this);
    }
    mErrorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {

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
  public boolean onKeyUp(int keyCode, KeyEvent event) {

    PlaybackOverlayFragment playbackOverlayFragment = (PlaybackOverlayFragment)
        getFragmentManager().findFragmentById(R.id.playback_controls_fragment);
    switch (keyCode) {
      case KeyEvent.KEYCODE_MEDIA_PLAY:
        playbackOverlayFragment.togglePlayback(false);
        return true;
      case KeyEvent.KEYCODE_MEDIA_PAUSE:
        playbackOverlayFragment.togglePlayback(false);
        return true;
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        if (isPlaying()) {
          playbackOverlayFragment.togglePlayback(false);
        } else {
          playbackOverlayFragment.togglePlayback(true);
        }
        return true;
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        if (mIsLongPress) {
          stopTransportControlAction();
        } else {
          playbackOverlayFragment.fastForward();
        }

        return true;
      case KeyEvent.KEYCODE_MEDIA_REWIND:
        if (mIsLongPress) {
          stopTransportControlAction();
        } else {
          playbackOverlayFragment.fastRewind();
        }

        return true;
      case KeyEvent.KEYCODE_BUTTON_R1:
        if (mIsLongPress) {
          stopTransportControlAction();
        } else {
          playbackOverlayFragment.fastForward();
        }

        return true;
      case KeyEvent.KEYCODE_BUTTON_L1:
        if (mIsLongPress) {
          stopTransportControlAction();
        } else {
          playbackOverlayFragment.fastRewind();
        }
        return true;
      default:
        return super.onKeyUp(keyCode, event);
    }
  }

  @Override
  public void onFragmentPlayPause(boolean playPause) {
    if (playPause) {
      play();
    } else {
      pause();
    }
  }

  public void onFragmentFfwRwd(int position) {
    if (position >= 0) {
      seekTo(position);
      if (isPlaying()) {
        play();
      }
    }
  }

  public int getBufferProgressPosition() {
    if (mPlayer != null) {
      return (mPlayer.getBufferedPercentage() * getDuration()) / 100;
    }
    return 0;
  }

  public void changeContent(Content content) {

  }

  public void onCloseCaptionButtonStateChanged(boolean state) {

  }

  enum AudioFocusState {
    Focused,
    NoFocusNoDuck,
    NoFocusCanDuck
  }


  /*
   * List of various states that we can be in.
   */
  public enum LeanbackPlaybackState {
    PLAYING, PAUSED, BUFFERING, IDLE
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


}

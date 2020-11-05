/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediamelon.smartstreaming;

import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.emsg.EventMessage;
import com.google.android.exoplayer2.metadata.id3.ApicFrame;
import com.google.android.exoplayer2.metadata.id3.CommentFrame;
import com.google.android.exoplayer2.metadata.id3.GeobFrame;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.PrivFrame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.metadata.id3.UrlLinkFrame;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import static com.google.android.exoplayer2.C.DATA_TYPE_MEDIA;

/**
 * Logs player events using {@link Log}.
 */
/* package */ public class EventLoggerExo2_6  implements Player.EventListener,
        MetadataOutput,
        VideoRendererEventListener,
        MediaSourceEventListener,
        DefaultDrmSessionManager.EventListener {

  private static final String TAG = "EventLogger2_6";
  private static final int MAX_TIMELINE_ITEM_LINES = 3;
  private static boolean isPresentationInfoSet = false;
  private static final NumberFormat TIME_FORMAT;
  static {
    TIME_FORMAT = NumberFormat.getInstance(Locale.US);
    TIME_FORMAT.setMinimumFractionDigits(2);
    TIME_FORMAT.setMaximumFractionDigits(2);
    TIME_FORMAT.setGroupingUsed(false);
  }

  private final SimpleExoPlayer player;
  private final Timeline.Window window;
  private final Timeline.Period period;
  private final long startTimeMs;

  public EventLoggerExo2_6(SimpleExoPlayer simpleExo) {
    player = simpleExo;
    window = new Timeline.Window();
    period = new Timeline.Period();
    startTimeMs = SystemClock.elapsedRealtime();
    isPresentationInfoSet = false;
  }

  // Player.EventListener

  @Override
  public void onLoadingChanged(boolean isLoading) {
    Log.d(TAG, "loading [" + isLoading + "]");
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int state) {
    MMSmartStreamingExo2_6.getInstance().reportPlayerState(playWhenReady, state);

    if(isPresentationInfoSet == false && playWhenReady == true && state == 3)  // STATE_READY = 3
    {
      MMPresentationInfo info = new MMPresentationInfo();

      info.isLive = true;
      info.duration = -1L;

      if(player.getDuration() > 0 && player.isCurrentWindowDynamic() == false)
      {
        info.isLive = false;
        info.duration = player.getDuration();

        MMSmartStreamingExo2_6.getInstance().setPresentationInformation(info);
        isPresentationInfoSet = true;

      }

      if(player.getDuration() < 0 && player.isCurrentWindowDynamic() == true)
      {
        MMSmartStreamingExo2_6.getInstance().setPresentationInformation(info);
        isPresentationInfoSet = true;
      }

    }

    if(state == 4)          // STATE_ENDED = 4
    {
      isPresentationInfoSet = false;
    }

    Log.d(TAG, "state " +  playWhenReady + ", " + getStateString(state));
  }

  @Override
  public void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {
    Log.d(TAG, "repeatMode [" + getRepeatModeString(repeatMode) + "]");
  }

  @Override
  public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
    Log.d(TAG, "shuffleModeEnabled [" + shuffleModeEnabled + "]");
  }

  @Override
  public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
    Log.d(TAG, "positionDiscontinuity [" + getDiscontinuityReasonString(reason) + "]");
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    Log.d(TAG, "playbackParameters " + String.format(
            "[speed=%.2f, pitch=%.2f]", playbackParameters.speed, playbackParameters.pitch));
  }


  @Override
  public void onPlayerError(ExoPlaybackException e) {
    Log.e(TAG, "playerFailed [" + getSessionTimeString() + "]", e);
    long eventTime = -1;
    if(player != null){
      eventTime = player.getCurrentPosition();
    }

    String errString = "Error Source - Unknown";
    if (e.type == ExoPlaybackException.TYPE_SOURCE) {
      errString = "Error Source - SOURCE ";
      IOException ex = e.getSourceException();
      errString += ex.getMessage();
    } else if(e.type == ExoPlaybackException.TYPE_RENDERER) {
      errString = "Error Source - RENDERER ";
      Exception ex = e.getRendererException();
      errString += ex.getMessage();
    }else if(e.type == ExoPlaybackException.TYPE_UNEXPECTED) {
      errString = "Error Source - UNEXPECTED ";
      RuntimeException ex = e.getUnexpectedException();
      errString += ex.getMessage();
    }

    MMSmartStreamingExo2_6.getInstance().reportError(e.getMessage() == null?errString:e.getMessage(), eventTime);
    MMSmartStreamingExo2_6.getInstance().reportPlayerState(false, Player.STATE_ENDED);
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    MMPresentationInfo info = new MMPresentationInfo();
    if(timeline.getWindowCount() > 0){
      timeline.getWindow(0, window);
      info.isLive = window.isDynamic;
      info.duration = window.getDurationMs();
    }

    if(info.isLive == true) {
      info.duration = -1L;
    }

    if(info.isLive == false && info.duration > 0)
    {
      MMSmartStreamingExo2_6.getInstance().setPresentationInformation(info);
      isPresentationInfoSet = true;
    }

    if(info.isLive == true && info.duration < 0)
    {
      MMSmartStreamingExo2_6.getInstance().setPresentationInformation(info);
      isPresentationInfoSet = true;
    }

  }

  @Override
  public void onTracksChanged(TrackGroupArray ignored, TrackSelectionArray trackSelections) {

    if(isPresentationInfoSet == false)
    {
      MMPresentationInfo info = new MMPresentationInfo();

      info.isLive = true;
      info.duration = -1L;

      if(player.getDuration() > 0 && player.isCurrentWindowDynamic() == false)
      {
        info.isLive = false;
        info.duration = player.getDuration();

        MMSmartStreamingExo2_6.getInstance().setPresentationInformation(info);
        isPresentationInfoSet = true;

      }

      if(player.getDuration() < 0 && player.isCurrentWindowDynamic() == true)
      {
        MMSmartStreamingExo2_6.getInstance().setPresentationInformation(info);
        isPresentationInfoSet = true;

      }

    }

  }

  @Override
  public void onSeekProcessed() {
    Log.d(TAG, "seekProcessed");
    long eventTime = -1;
    if(player != null){
      eventTime = player.getCurrentPosition();
    }
    MMSmartStreamingExo2_6.getInstance().reportPlayerSeekCompleted(eventTime);
  }

  // MetadataOutput

  @Override
  public void onMetadata(Metadata metadata) {
    Log.d(TAG, "onMetadata [");
    printMetadata(metadata, "  ");
    Log.d(TAG, "]");
  }

  // VideoRendererEventListener

  @Override
  public void onVideoEnabled(DecoderCounters counters) {
    Log.d(TAG, "videoEnabled [" + getSessionTimeString() + "]");
  }

  @Override
  public void onVideoDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                        long initializationDurationMs) {
    Log.d(TAG, "videoDecoderInitialized [" + getSessionTimeString() + ", " + decoderName + "]");
  }

  @Override
  public void onVideoInputFormatChanged(Format format) {
    Log.d(TAG, "videoFormatChanged [" + getSessionTimeString() + ", " + Format.toLogString(format)
            + "]");
  }

  @Override
  public void onVideoDisabled(DecoderCounters counters) {
    Log.d(TAG, "videoDisabled [" + getSessionTimeString() + "]");
  }

  @Override
  public void onDroppedFrames(int count, long elapsed) {
    Log.d(TAG, "droppedFrames [" + getSessionTimeString() + ", " + count + "]");
    MMSmartStreamingExo2_6.getInstance().reportFrameLoss(count);
  }

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                 float pixelWidthHeightRatio) {
    Log.d(TAG, "videoSizeChanged [" + width + ", " + height + "]");
  }

  @Override
  public void onRenderedFirstFrame(Surface surface) {
    Log.d(TAG, "renderedFirstFrame [" + surface + "]");
  }

  // DefaultDrmSessionManager.EventListener

  @Override
  public void onDrmSessionManagerError(Exception e) {
    printInternalError("drmSessionManagerError", e);
    long eventTime = -1;
    if(player != null){
      eventTime = player.getCurrentPosition();
    }
    MMSmartStreamingExo2_6.getInstance().reportError("drmSessionManagerError " + e.getMessage(), eventTime);
  }

  @Override
  public void onDrmKeysRestored() {
    Log.d(TAG, "drmKeysRestored [" + getSessionTimeString() + "]");
  }

  @Override
  public void onDrmKeysRemoved() {
    Log.d(TAG, "drmKeysRemoved [" + getSessionTimeString() + "]");
  }

  @Override
  public void onDrmKeysLoaded() {
    Log.d(TAG, "drmKeysLoaded [" + getSessionTimeString() + "]");
  }

  @Override
  public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                            int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                            long mediaEndTimeMs, long elapsedRealtimeMs) {
    // Do nothing.
  }

  @Override
  public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                          int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                          long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded,
                          IOException error, boolean wasCanceled) {
    printInternalError("loadError", error);
    long eventTime = -1;
    if(player != null){
      eventTime = player.getCurrentPosition();
    }
    MMSmartStreamingExo2_6.getInstance().reportError("loadError" + error.getMessage(), eventTime);
  }

  @Override
  public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                             int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                             long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
    // Do nothing.
  }


  @Override
  public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                              int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                              long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
    // Do nothing.

    if(dataType == DATA_TYPE_MEDIA &&
            trackFormat!= null &&
            ((trackFormat.codecs != null && trackFormat.codecs.toLowerCase().contains("avc")) ||
                    (trackFormat.containerMimeType != null && trackFormat.containerMimeType.toLowerCase().contains("mpegurl")))){
      MMChunkInformation info = new MMChunkInformation();
      info.bitrate = trackFormat.bitrate;
      info.startTime = mediaStartTimeMs;
      info.duration = mediaEndTimeMs - mediaStartTimeMs;
      MMSmartStreamingExo2_6.getInstance().reportChunkRequest(info);
      long downloadRate = (bytesLoaded * 8000 / loadDurationMs);
      MMSmartStreamingExo2_6.getInstance().reportDownloadRate(downloadRate);
    }
  }

  @Override
  public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
    // Do nothing.
  }

  @Override
  public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason,
                                        Object trackSelectionData, long mediaTimeMs) {
    // Do nothing.
  }

  // Internal methods

  private void printInternalError(String type, Exception e) {
    Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e);
  }

  private void printMetadata(Metadata metadata, String prefix) {
    for (int i = 0; i < metadata.length(); i++) {
      Metadata.Entry entry = metadata.get(i);
      if (entry instanceof TextInformationFrame) {
        TextInformationFrame textInformationFrame = (TextInformationFrame) entry;
        Log.d(TAG, prefix + String.format("%s: value=%s", textInformationFrame.id,
                textInformationFrame.value));
      } else if (entry instanceof UrlLinkFrame) {
        UrlLinkFrame urlLinkFrame = (UrlLinkFrame) entry;
        Log.d(TAG, prefix + String.format("%s: url=%s", urlLinkFrame.id, urlLinkFrame.url));
      } else if (entry instanceof PrivFrame) {
        PrivFrame privFrame = (PrivFrame) entry;
        Log.d(TAG, prefix + String.format("%s: owner=%s", privFrame.id, privFrame.owner));
      } else if (entry instanceof GeobFrame) {
        GeobFrame geobFrame = (GeobFrame) entry;
        Log.d(TAG, prefix + String.format("%s: mimeType=%s, filename=%s, description=%s",
                geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
      } else if (entry instanceof ApicFrame) {
        ApicFrame apicFrame = (ApicFrame) entry;
        Log.d(TAG, prefix + String.format("%s: mimeType=%s, description=%s",
                apicFrame.id, apicFrame.mimeType, apicFrame.description));
      } else if (entry instanceof CommentFrame) {
        CommentFrame commentFrame = (CommentFrame) entry;
        Log.d(TAG, prefix + String.format("%s: language=%s, description=%s", commentFrame.id,
                commentFrame.language, commentFrame.description));
      } else if (entry instanceof Id3Frame) {
        Id3Frame id3Frame = (Id3Frame) entry;
        Log.d(TAG, prefix + String.format("%s", id3Frame.id));
      } else if (entry instanceof EventMessage) {
        EventMessage eventMessage = (EventMessage) entry;
        Log.d(TAG, prefix + String.format("EMSG: scheme=%s, id=%d, value=%s",
                eventMessage.schemeIdUri, eventMessage.id, eventMessage.value));
      }
    }
  }

  private String getSessionTimeString() {
    return getTimeString(SystemClock.elapsedRealtime() - startTimeMs);
  }

  private static String getTimeString(long timeMs) {
    return timeMs == C.TIME_UNSET ? "?" : TIME_FORMAT.format((timeMs) / 1000f);
  }

  private static String getStateString(int state) {
    switch (state) {
      case Player.STATE_BUFFERING:
        return "B";
      case Player.STATE_ENDED:
        return "E";
      case Player.STATE_IDLE:
        return "I";
      case Player.STATE_READY:
        return "R";
      default:
        return "?";
    }
  }

  private static String getFormatSupportString(int formatSupport) {
    switch (formatSupport) {
      case RendererCapabilities.FORMAT_HANDLED:
        return "YES";
      case RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES:
        return "NO_EXCEEDS_CAPABILITIES";
      case RendererCapabilities.FORMAT_UNSUPPORTED_DRM:
        return "NO_UNSUPPORTED_DRM";
      case RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE:
        return "NO_UNSUPPORTED_TYPE";
      case RendererCapabilities.FORMAT_UNSUPPORTED_TYPE:
        return "NO";
      default:
        return "?";
    }
  }

  private static String getAdaptiveSupportString(int trackCount, int adaptiveSupport) {
    if (trackCount < 2) {
      return "N/A";
    }
    switch (adaptiveSupport) {
      case RendererCapabilities.ADAPTIVE_SEAMLESS:
        return "YES";
      case RendererCapabilities.ADAPTIVE_NOT_SEAMLESS:
        return "YES_NOT_SEAMLESS";
      case RendererCapabilities.ADAPTIVE_NOT_SUPPORTED:
        return "NO";
      default:
        return "?";
    }
  }

  private static String getTrackStatusString(TrackSelection selection, TrackGroup group,
                                             int trackIndex) {
    return getTrackStatusString(selection != null && selection.getTrackGroup() == group
            && selection.indexOf(trackIndex) != C.INDEX_UNSET);
  }

  private static String getTrackStatusString(boolean enabled) {
    return enabled ? "[X]" : "[ ]";
  }

  private static String getRepeatModeString(@Player.RepeatMode int repeatMode) {
    switch (repeatMode) {
      case Player.REPEAT_MODE_OFF:
        return "OFF";
      case Player.REPEAT_MODE_ONE:
        return "ONE";
      case Player.REPEAT_MODE_ALL:
        return "ALL";
      default:
        return "?";
    }
  }

  private static String getDiscontinuityReasonString(@Player.DiscontinuityReason int reason) {
    switch (reason) {
      case Player.DISCONTINUITY_REASON_PERIOD_TRANSITION:
        return "PERIOD_TRANSITION";
      case Player.DISCONTINUITY_REASON_SEEK:
        return "SEEK";
      case Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
        return "SEEK_ADJUSTMENT";
      case Player.DISCONTINUITY_REASON_INTERNAL:
        return "INTERNAL";
      default:
        return "?";
    }
  }
}

package com.mediamelon.smartstreaming;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.content.Context;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.ref.WeakReference;
/**
 * ExoPlayer interface for the MMSmartStreaming SDK. This class includes wrapper functions to 
 * simplify integration of the MediaMelon SDK with the ExoPlayer media player.
 */

public class MMSmartStreamingExo2_6 implements MMSmartStreamingObserver{

    private ExoPlayer playerExo = null;
    private WeakReference<SimpleExoPlayer> playerSimple = null;
    public void sessionInitializationCompleted(Integer initCmdId, MMSmartStreamingInitializationStatus status, String description){
        if(obs != null){
            obs.get().sessionInitializationCompleted(initCmdId, status, description);
        }

        if (status == MMSmartStreamingInitializationStatus.Success){
            Integer interval = MMSmartStreaming.getInstance().getLocationUpdateInterval();
            MMNetworkInformationRetriever.instance().startRetriever(ctx.get(), interval);
        }
    }

    /**
     * Gets the SDK adapter instance
     * @return SDK adapter instance for Exoplayer
     */
    public static MMSmartStreamingExo2_6 getInstance(){
        if(myObj == null){
            myObj = new MMSmartStreamingExo2_6();
        }
        return myObj;
    }

    /**
     * Gets the SDK version
     * @return SDK version (major.minor.patch)
     */
    public static String getVersion(){
    return MMSmartStreaming.getVersion();
  }

    /**
     * Gets the registration status (done via registerMMSmartStreaming)
     * @return true if the SDK has successfully registered with the registerMMSmartStreaming method;
     * otherwise returns false.
     * @see registerMMSmartStreaming
     */
    public static boolean getRegistrationStatus(){
        return MMSmartStreaming.getRegistrationStatus();
    }

    /**
     * Sets the activity context
     * @param aCtx Player context
     */
    public void setContext(Context aCtx){
        if(logStackTrace){
            Log.v(StackTraceLogTag, "setContext" + aCtx);
        }

        ctx = new WeakReference<Context>(aCtx);

        DisplayMetrics dm = ctx.get().getResources().getDisplayMetrics();
        Integer height = dm.heightPixels;
        Integer width = dm.widthPixels;
	
	mainHandler = new Handler(ctx.get().getMainLooper());

        TelephonyManager tm = (TelephonyManager)ctx.get().getSystemService(Context.TELEPHONY_SERVICE);
        MMSmartStreaming.reportDeviceInfo(Build.BRAND, Build.MODEL, "Android", Build.VERSION.RELEASE, (tm!=null? (tm.getNetworkOperatorName()):null), width, height);

        MMNetworkInformationRetriever.instance().initializeRetriever(ctx.get());
    }

    /**
     * Registers the QBR SmartStreaming engine and performs a license verification. This API should
     * be called once when player starts. The QBR SmartStreaming engine must be successfully
     * registered before initialization.
     * This is a synchronous call. Registration status can be checked at any time using the
     * getRegistrationStatus method.
     *
     * @param playerName Name of the player
     * @param customerID MediaMelon assigned customer ID
     * @param [subscriberID] Viewer's subscriber ID
     * @param [domainName] Content-owner domain name.
     *                   Some business organizations may would like to do analytics segmented
     *                   by group. For example, a Media House may have many divisions, and will like
     *                   to categorize their analysis based on division. Or a content owner has
     *                   distributed content to various resellers and would like to know the reseller
     *                   from whom the user is playing the content. In this case every reseller will
     *                   have separate application, and will configure the domain name.
     *
     * @note Please be aware that this API will be deprecated in the version 4.x.x. Integrators are
     * advised to use another overload of this API that accepts subscriberType as parameter as well
     * @see registerMMSmartStreaming
     * @see getRegistrationStatus
     * @see updateSubscriberID
     */
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName);
    }

    /**
     * Registers the QBR SmartStreaming engine and performs a license verification. This API should
     * be called once when player starts. The QBR SmartStreaming engine must be successfully
     * registered before initialization.
     * This is a synchronous call. Registration status can be checked at any time using the
     * getRegistrationStatus method.
     *
     * @param playerName Name of the player
     * @param customerID MediaMelon assigned customer ID
     * @param [subscriberID] Viewer's subscriber ID
     * @param [domainName] Content-owner domain name.
     *                   Some business organizations may would like to do analytics segmented
     *                   by group. For example, a Media House may have many divisions, and will like
     *                   to categorize their analysis based on division. Or a content owner has
     *                   distributed content to various resellers and would like to know the reseller
     *                   from whom the user is playing the content. In this case every reseller will
     *                   have separate application, and will configure the domain name.
     * @param [subscriberType] Viewer's subscriber type such as "Free", "Basic" or "Premium" as
     *                         configured by the customer for the end user of the player.
     *
     * @see getRegistrationStatus
     * @see updateSubscriberID
     */
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName, String subscriberType){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName, subscriberType);
    }

    /**
     * Registers the QBR SmartStreaming engine and performs a license verification. This API should
     * be called once when player starts. The QBR SmartStreaming engine must be successfully
     * registered before initialization.
     * This is a synchronous call. Registration status can be checked at any time using the
     * getRegistrationStatus method.
     *
     * @param playerName Name of the player
     * @param customerID MediaMelon assigned customer ID
     * @param [subscriberID] Viewer's subscriber ID
     * @param [domainName] Content-owner domain name.
     *                   Some business organizations may would like to do analytics segmented
     *                   by group. For example, a Media House may have many divisions, and will like
     *                   to categorize their analysis based on division. Or a content owner has
     *                   distributed content to various resellers and would like to know the reseller
     *                   from whom the user is playing the content. In this case every reseller will
     *                   have separate application, and will configure the domain name.
     * @param [subscriberType] Viewer's subscriber type such as "Free", "Basic" or "Premium" as
     *                         configured by the customer for the end user of the player.
     * @param [subscriberTag] Viewer's tag using which one can track their pattern

     *
     * @see getRegistrationStatus
     * @see updateSubscriberID
     */
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName, String subscriberType, String subscriberTag){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName, subscriberType,subscriberTag);
    }

    public ArrayList<String> getMissingPermissions(Context context){
        return MMNetworkInformationRetriever.getMissingPermissions(context);
    }

    /**
    * Disables the fetching of manifests by the SDK to determine the presentation information of the content.
    * SDK will rely completely on presentation information provided as part of setPresentationInformation.
    * @param [disable] Disables/Enables the manifest fetch by the SDK
    * @see setPresentationInformation
    */
    public static void disableManifestsFetch(boolean disable){
        try{
            MMSmartStreaming.disableManifestsFetch(disable);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * After the registration, user may will like to update the subscriber ID,
     * for example - user logged off from the Video service website, and logs in again with different
     * user.
     * @note This API will be deprecated for updateSubscriber from version 4.x.x
     *
     * @param subscriberID New Subscriber ID
     * @see registerMMSmartStreaming
     * @see updateSubscriber
     * 
     */
    public static void updateSubscriberID(String subscriberID){
        MMSmartStreaming.updateSubscriberID(subscriberID);
    }

    /**
     * After the registration, user may will like to update the subscriber ID,
     * for example - user logged off from the Video service website, and logs in again with different
     * user.
     * @param subscriberID New Subscriber ID
     * @param subscriberID New Subscriber Type
     * @see registerMMSmartStreaming
     *
     */
    public static void updateSubscriber(String subscriberID, String subscriberType){
        MMSmartStreaming.updateSubscriber(subscriberID, subscriberType);
    }

    /**
     * Reports the media player characteristics to analytics.
     * Use a NULL pointer if the value is unknown or inapplicable.
     *
     * @param [brand] Brand of the player. For example - Brand could be Organisation Name.
     * @param [model] Model of the player. For example - This could be a variant of player.
     *              Say name of third party player used by organisation. Or any human readable name of
     *              the player.
     * @param [version] Version of the player.
     */
    public static void reportPlayerInfo(String brand, String model, String version){
        MMSmartStreaming.reportPlayerInfo(brand, model, version);
    }

    /**
     * Initializes the session for playback with QBR optimization. This API should be called once for
     * every media session and is asynchronous. Its completion is indicated via callback to
     * MMSmartStreamingObserver::sessionInitializationCompleted that user may choose to ignore.
     *
     * @param mode QBR operating mode.
     * @param manifestURL URL of the media manifest
     * @param [metaURL] URL of the media metadata. If it is null, and QBR operating mode is
     *                Bitsave, CostSave, or Quality, a metadata file with manifestUrl base name will
     *                be used. If the metadata cannot be retrieved, mode will default to Disabled.
     * @param [assetID] Content identifier
     * @param [assetName] Content name
     * @param observer MMSmartStreamingObserver that will receive the callback on initialization
     *                 completion.
     * @see MMQBRMode
     * @see MMSmartStreamingObserver
     */
    public void initializeSession(SimpleExoPlayer simplePlayer, MMQBRMode mode, String manifestURL, String metaURL, String assetID, String assetName, MMSmartStreamingObserver observer){
        reset();
        if(observer != null) {
      	   obs = new WeakReference<MMSmartStreamingObserver>(observer);
    	}
        playerSimple = new WeakReference<SimpleExoPlayer>(simplePlayer);
        if(playerSimple != null){
            EventLoggerExo2_6 eventLogger = new EventLoggerExo2_6(simplePlayer);
            playerSimple.get().addListener(eventLogger);
            playerSimple.get().addMetadataOutput(eventLogger);
//            playerSimple.get().setAudioDebugListener(eventLogger);
            playerSimple.get().setVideoDebugListener(eventLogger);
        }
        MMSmartStreaming.getInstance().initializeSession(mode, manifestURL, metaURL, assetID, assetName, this);
    }

    /**
     * Initializes the session for playback with QBR optimization. This API should be called once for
     * every media session and is asynchronous. Its completion is indicated via callback to
     * MMSmartStreamingObserver::sessionInitializationCompleted that user may choose to ignore.
     *
     * @param mode QBR operating mode.
     * @param manifestURL URL of the media manifest
     * @param [metaURL] URL of the media metadata. If it is null, and QBR operating mode is
     *                Bitsave, CostSave, or Quality, a metadata file with manifestUrl base name will
     *                be used. If the metadata cannot be retrieved, mode will default to Disabled.
     * @param [assetID] Content identifier
     * @param [assetName] Content name
     * @param [videoId] Video Id
     * @param observer MMSmartStreamingObserver that will receive the callback on initialization
     *                 completion.
     * @see MMQBRMode
     * @see MMSmartStreamingObserver
     */
    public void initializeSession(SimpleExoPlayer simplePlayer, MMQBRMode mode, String manifestURL, String metaURL, String assetID, String assetName, String videoId, MMSmartStreamingObserver observer){
        reset();
        if(observer != null) {
      	   obs = new WeakReference<MMSmartStreamingObserver>(observer);
    	}
        playerSimple = new WeakReference<SimpleExoPlayer>(simplePlayer);
        if(playerSimple != null){
            EventLoggerExo2_6 eventLogger = new EventLoggerExo2_6(simplePlayer);
            playerSimple.get().addListener(eventLogger);
            playerSimple.get().addMetadataOutput(eventLogger);
//            playerSimple.get().setAudioDebugListener(eventLogger);
            playerSimple.get().setVideoDebugListener(eventLogger);
        }
        MMSmartStreaming.getInstance().initializeSession(mode, manifestURL, metaURL, assetID, assetName, videoId, this);
    }

    /**
     * Reports that user initiated the playback session.
     * This should be called at different instants depending on the mode of operation of player.
     * In Auto Play Mode, should be the called when payer is fed with the manifest URL for playback
     * In non-Auto Play Mode, should be called when the user presses the play button on the
     * player
     */
    public void reportUserInitiatedPlayback(){
        MMSmartStreaming.getInstance().reportUserInitiatedPlayback();
    }

    /**
     * Tells the QBR SmartStreaming engine which representations that the player can present.
     * Representations that are not in this list will not be selected by the QBR SmartStreaming engine.
     * @param presentationInfo PresentationInformation specifying the representations selected by
     *                         the player for playback.
     * @see blacklistRepresentation
     * @see MMPresentationInfo
     */
    public void setPresentationInformation(MMPresentationInfo presentationInfo){
        MMSmartStreaming.getInstance().setPresentationInformation(presentationInfo);
    }

    /**
     * Removes a representation from the list previously defined by setPresentationInformation. This
     * would typically be used to stop referring to a representation that is listed in the manifest
     * but not currently available.
     *
     * @param representationIdx Representation Index for the representation to be (un)blacklisted.
     * @param blacklistRepresentation True to blacklist the representation; False to un-blacklist
     *                                the representation.
     * @see setPresentationInformation
     */
    public void blacklistRepresentation(Integer representationIdx, boolean blacklistRepresentation){
        MMSmartStreaming.getInstance().blacklistRepresentation(representationIdx, blacklistRepresentation);
    }

    /**
     * Returns the bandwidth required for the QBR representation that delivers constant quality across
     * the session.
     *
     * @param representationTrackIdx Track Index of the representation whose corresponding
     *                               quality bitrate is to be evaluated.
     * @param defaultBitrate Bitrate of the CBR representation as advertised in the manifest (in
     *                         bits per second).
     * @param bufferLength Amount of media buffered in player ahead of current playback position (in
     *                    milliseconds).
     * @param playbackPosition Current playback position (in milliseconds).
     * @return Bandwidth of QBR representation (in bits per second).
     */
    public Integer getQBRBandwidth(Integer representationTrackIdx, Integer defaultBitrate, Long bufferLength, Long playbackPosition){
        return MMSmartStreaming.getInstance().getQBRBandwidth(representationTrackIdx, defaultBitrate, bufferLength, playbackPosition);
    }

    /**
     * During the playback session, player is expected to query the constant quality chunk that it
     * should request from server for the chunk selected based on ABR algorithm.
     * This API is used only if Qubitisation of content is to be achieved. 
     * @param cbrChunk MMChunkInformation object identifying the chunk selected by ABR algorithm.
     * For referencing the chunk there are two option:
     * (a) Caller of API may specify resourceURL
     * (b) Caller of API may specify combination of sequence id and track id.
     * Using option b) may result in improved CPU performace of this API and is recommended.
     * @return The chunk selected by the QBR algorithm.
     * @see MMChunkInformation
     */
    public MMChunkInformation getQBRChunk(MMChunkInformation cbrChunk){
        return MMSmartStreaming.getInstance().getQBRChunk(cbrChunk);
    }

    /**
     * Reports the chunk request to analytics. This method is not used when QBR optimization is
     * enabled.
     * @param chunkInfo Chunk selected by the player.
     * @see MMChunkInformation
     */
    public void reportChunkRequest(MMChunkInformation chunkInfo){
        MMSmartStreaming.getInstance().reportChunkRequest(chunkInfo);
    }

    /**
     * Reports current download rate (rate at which chunk is downloaded) to analytics. This should be
     * reported for every chunk download (if possible). If this value is not available on every
     * chunk download, then last updated value with player should be reported every 2 seconds.
     *
     * @param downloadRate Download rate as measured by the player (in bits per second)
     */
    public void reportDownloadRate(Long downloadRate){
        MMSmartStreaming.getInstance().reportDownloadRate(downloadRate);
    }

    /**
     * Reports current download rate (rate at which chunk is downloaded) to analytics. This should be
     * reported for every chunk download (if possible). If this value is not available on every
     * chunk download, then last updated value with player should be reported every 2 seconds.
     *
     * @param bufferLength Download rate as measured by the player (in bits per second)
     */
    public void reportBufferLength(Long bufferLength){
        MMSmartStreaming.getInstance().reportBufferLength(bufferLength);
    }

    /**
     * Reports custom metadata, in the form of a key-value pair, to analytics.
     *
     * @param key Custom metadata key.
     * @param value Custom metadata value.
     */
    public void reportCustomMetadata(String key, String value){
        MMSmartStreaming.getInstance().reportCustomMetadata(key, value);
    }

    /**
     * Reports current playback position in media to analytics. This should be reported every two
     * seconds if possible.
     *
     * @param playbackPos Current playback position (in milliseconds).
     */
    public void reportPlaybackPosition(Long playbackPos){
        MMSmartStreaming.getInstance().reportPlaybackPosition(playbackPos);
    }

    /**
     * Override the SmartSight-calculated metric with a specific value.
     *
     * @param metric : Metric to be overridden.
     * @param value : New metric value. Even if the value of
     *   metric is numeric, int (for example in case of latency), user
     *   is expected to provide its string representation:
     * - For Latency, the latency in seconds, with with millisecond resolution (e.g., "1.236")
     * - For ServerAddress, the name of the cdn (e.g., "PrivateCDN")
     * - For DurationWatched, the duration watched in seconds, with millisecond resolution (e.g., "137.935")
     * @see MMOverridableMetric
     */
    public void reportMetricValue(MMOverridableMetric metric, String value){
        MMSmartStreaming.getInstance().reportMetricValue(metric, value);
    }

    void reset(){
      obs = null;
      cumulativeFramesDropped = 0;
      sendBufferingCompletionOnReady = false;
      playbackPollingStarted = false;
      playerSimple = null;
      if(timer != null) {
        timer.cancel();
        timer = null;
      }
    }

    void stopPlaybackPolling(){
     playbackPollingStarted = false;
     if(timer != null) {
       timer.cancel();
       timer = null;
     }
    }

    void startPlaybackPolling(){
      if(playbackPollingStarted == false){
        playbackPollingStarted = true;
        if(timer == null){
          timer = new Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
          synchronized public void run() {
	    mainHandler.post(new Runnable() {
              public void run() {
                if(playerSimple != null) {
                    MMSmartStreamingExo2_6.getInstance().reportPlaybackPosition(playerSimple.get().getCurrentPosition());
                }
              }
            });

          }
        }, 2000, 2000);
      }
    }
    /**
     * Reports the current player state to analytics.
     * @param playWhenReady Boolean indicating that the player should start playing media when it
     *                      is ready (has enough media to play)
     * @param exoPlayerState Target state to which player transitions to from current state
     * @see MMPlayerState
     */
    public void reportPlayerState(boolean playWhenReady, Integer exoPlayerState){
        if(logStackTrace){
            Log.v(StackTraceLogTag, "reportPlayerState - < " + Boolean.toString(playWhenReady) + ", " + exoPlayerState + " >");
        }
        switch (exoPlayerState){
            case Player.STATE_IDLE:
            break;
            case Player.STATE_BUFFERING:{
                MMSmartStreaming.getInstance().reportBufferingStarted();
                sendBufferingCompletionOnReady = true;
            }
            break;
            case Player.STATE_READY:{
                startPlaybackPolling();
                if (sendBufferingCompletionOnReady){
                    MMSmartStreaming.getInstance().reportBufferingCompleted();
                    sendBufferingCompletionOnReady = false;
                }
                if (playWhenReady){
                    MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.PLAYING);
                }else{
                    MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.PAUSED);
                }
            }
            break;
            case Player.STATE_ENDED:{
                MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.STOPPED);
                stopPlaybackPolling();
            }
            break;
        }
    }

    /**
     * Reports the ABR bitrate changes to the analytics. This API should be called when neither
     * getQBRChunk nor reportChunkRequest is called by the player.
     * @param prevBitrate Previous ABR bitrate in bits per second.
     * @param newBitrate New ABR bitrate in pers per second.
     */
    public void reportABRSwitch(Integer prevBitrate, Integer newBitrate){
        MMSmartStreaming.getInstance().reportABRSwitch(prevBitrate, newBitrate);
    }

    /**
     * Reports cumulative frame loss count to analytics.
     * @param lossCnt Cumulative count of frames lost in playback session.
     */
    public void reportFrameLoss(Integer lossCnt){
        if(logStackTrace){
            Log.v(StackTraceLogTag, "reportFrameLoss - " + lossCnt);
        }
        cumulativeFramesDropped += lossCnt;
        MMSmartStreaming.getInstance().reportFrameLoss(cumulativeFramesDropped);
    }

    /**
     * Reports an error encountered during playback.
     * @param error Error encountered during playback session.
     * @param playbackPosMilliSec Playback position in millisec when error occurred.
     */
    public void reportError(String error, Long playbackPosMilliSec){
        MMSmartStreaming.getInstance().reportError(error, playbackPosMilliSec);
    }

    /**
     * Reports that a seek event is complete, with the new playback starting position.
     * @param seekEndPos Playback position(in milliseconds) when seek completed. This is point from
     *                   which playback will start after the seek.
     */
    public void reportPlayerSeekCompleted(Long seekEndPos){
        MMSmartStreaming.getInstance().reportPlayerSeekCompleted(seekEndPos);
    }

    /**
     * Reports the WiFi Service Set Identifier (SSID).
     * @param ssid WiFi Service Set Identifier (SSID).
     */
    public void reportWifiSSID(String ssid){
        MMSmartStreaming.getInstance().reportWifiSSID(ssid);
    }

    /**
     * Reports the WiFi signal strength. This may be useful, if someone is analyzing a
     * back playback session using smartsight's microscope feature, and wants to know if Wifi signal
     * strength is the cause fo poor performance of that session. This API is relevant if Wifi is used
     * for the playback session.
     *
     * @param strength Strength of Wifi signal in %
     */
    public void reportWifiSignalStrengthPercentage(Double strength){
        MMSmartStreaming.getInstance().reportWifiSignalStrengthPercentage(strength);
    }

    /**
     * Reports the WiFi maximum data rate.
     * @param dataRate WiFi data rate (in kbps)
     */
    public void reportWifiDataRate(Integer dataRate){
        MMSmartStreaming.getInstance().reportWifiDataRate(dataRate);
    }

    /**
     * Reports advertisement playback state
     * @param adState State of the advertisement
     * @see MMAdState
     */
    public void reportAdState(MMAdState adState){
        MMSmartStreaming.getInstance().reportAdState(adState);
    }

    /**
     * Reports advertisement-related information
     *
     * @param adClient Client used to play the ad, eg: VAST
     * @param adURL Tag represented by the ad.
     * @param adDuration Length of the video ad (in milliseconds).
     * @param adPosition Position of the ad in the video  playback; one of "pre", "post" or "mid" 
     *                   that represents that the ad played before, after or during playback respectively.
     * @param adType Type of the ad (linear, non-linear etc).
     * @param adCreativeType Ad MIME type
     * @param adServer Ad server (ex. DoubleClick, YuMe, AdTech, Brightroll, etc.)
     * @param adResolution Advertisement video resolution
     */
//    public void reportAdInfo(String adClient, String adURL, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer, String adResolution){
//        MMSmartStreaming.getInstance().reportAdInfo(adClient, adURL, adDuration, adPosition, adType, adCreativeType, adServer, adResolution);
//
//    }

    public void reportAdInfo(String adClient, String adURL, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer, String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime) {

        MMSmartStreaming.getInstance().reportAdInfo(adClient, adURL, adDuration, adPosition, adType, adCreativeType, adServer, adResolution, adPodIndex, adPositionInPod, adPodLength, isBumper, adScheduledTime);

    }

        /**
         * Reports current advertisement playback position
         * @param playbackPosition Current playback position in the Ad (in milliseconds)
         */
    public void reportAdPlaybackTime(Long playbackPosition){
        MMSmartStreaming.getInstance().reportAdPlaybackTime(playbackPosition);
    }

    /**
     * Reports error encountered during the advertisement playback
     * @param error Error encountered during advertisement playback
     * @param pos Playback position (in milliseconds) where error occurred
     */
    public void reportAdError(String error, Long pos){
        MMSmartStreaming.getInstance().reportAdError(error, pos);
    }

    /**
     * Enables/Disables console logs for the SDK methods. This is to help in debugging and testing
     * of the player to SDK integration.
     * @param logStTrace True to enable console logs; false to disable console logs.
     */
    public static void enableLogTrace(boolean logStTrace){
        logStackTrace = logStTrace;
        MMSmartStreaming.enableLogTrace(logStTrace);
    }

    boolean playbackPollingStarted = false;
    private Timer timer;
    private Handler mainHandler;
    WeakReference<MMSmartStreamingObserver> obs = null;
    private boolean sendBufferingCompletionOnReady = false;
    private WeakReference<Context> ctx;
    private int cumulativeFramesDropped = 0;
    private MMSmartStreamingExo2_6(){}
    private static boolean logStackTrace = false;
    private static String StackTraceLogTag = "MMSmartStreamingIntgr";
    private static MMSmartStreamingExo2_6 myObj;
}


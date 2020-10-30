package com.zype.fire.api;

import com.google.gson.Gson;
import com.zype.fire.api.Model.AccessTokenInfoResponse;
import com.zype.fire.api.Model.AccessTokenResponse;
import com.zype.fire.api.Model.AppResponse;
import com.zype.fire.api.Model.Channel;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.Model.DevicePinResponse;
import com.zype.fire.api.Model.ChannelResponse;
import com.zype.fire.api.Model.PlanResponse;
import com.zype.fire.api.Model.PlaylistData;
import com.zype.fire.api.Model.PlaylistResponse;
import com.zype.fire.api.Model.Program;
import com.zype.fire.api.Model.ProgramResponse;
import com.zype.fire.api.Model.PlaylistsResponse;
import com.zype.fire.api.Model.VideoEntitlementsResponse;
import com.zype.fire.api.Model.VideoFavoritesResponse;
import com.zype.fire.api.Model.VideoResponse;
import com.zype.fire.api.Model.VideosResponse;
import com.zype.fire.api.Model.ZobjectContentResponse;
import com.zype.fire.api.Model.ZobjectTopPlaylistResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Evgeny Cherkasov on 10.04.2017.
 */

public class ZypeApi {
    private static final String BASE_URL = "https://api.zype.com/";

    // Parameters
    public static final String ACCESS_TOKEN = "access_token";
    public static final String APP_KEY = "app_key";
    private static final String CLIENT_GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    public static final String CONSUMER_EMAIL = "consumer[email]";
    public static final String CONSUMER_ID = "consumer_id";
    public static final String CONSUMER_PASSWORD = "consumer[password]";
    private static final String LINKED_DEVICE_ID = "linked_device_id";
    private static final String PAGE = "page";
    private static final String PASSWORD = "password";
    public static final String PER_PAGE = "per_page";
    private static final String PIN = "pin";
    public static final String PLAYLIST_ID_INCLUSIVE = "playlist_id.inclusive";
    public static final String QUERY = "q";
    private static final String REFRESH_TOKEN = "refresh_token";

    public static final String SUBSCRIPTION_CONSUMER_ID = "consumer_id";
    public static final String SUBSCRIPTION_DEVICE_TYPE = "device_type";
    public static final String SUBSCRIPTION_RECEIPT_ID = "receipt_id";
    public static final String SUBSCRIPTION_SHARED_SECRET = "shared_secret";
    public static final String SUBSCRIPTION_THIRD_PARTY_ID = "third_party_id";
    public static final String SUBSCRIPTION_USER_ID = "user_id";

    private static final String USERNAME = "username";
    public static final String UUID = "uuid";

    public static final int PER_PAGE_DEFAULT = 20;

    private static ZypeApi instance;
    private static Retrofit retrofit;
    private static IZypeApi apiImpl;

    private ZypeApi() {}

    public static synchronized ZypeApi getInstance() {
        if (instance == null) {
            instance = new ZypeApi();

            // Needs to log retrofit calls
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(new Gson()))
                    .build();
            apiImpl = retrofit.create(IZypeApi.class);
        }
        return instance;
    }

    public Retrofit retrofit() {
        return retrofit;
    }

    public IZypeApi getApi() {
        return apiImpl;
    }

    /*
     * OAuth
     */
    public AccessTokenResponse retrieveAccessToken(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put(USERNAME, username);
        params.put(PASSWORD, password);
        params.put(CLIENT_ID, ZypeSettings.CLIENT_ID);
        params.put(CLIENT_GRANT_TYPE, "password");
        try {
            Response response = apiImpl.retrieveAccessToken(params).execute();
            if (response.isSuccessful()) {
                return (AccessTokenResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AccessTokenResponse retrieveAccessTokenWithPin(String deviceId, String pin) {
        Map<String, String> params = new HashMap<>();
        params.put(LINKED_DEVICE_ID, deviceId);
        params.put(PIN, pin);
        params.put(CLIENT_ID, ZypeSettings.CLIENT_ID);
        params.put(CLIENT_GRANT_TYPE, "password");
        try {
            Response response = apiImpl.retrieveAccessToken(params).execute();
            if (response.isSuccessful()) {
                return (AccessTokenResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        Map<String, String> params = new HashMap<>();
        params.put(REFRESH_TOKEN, refreshToken);
        params.put(CLIENT_ID, ZypeSettings.CLIENT_ID);
        params.put(CLIENT_GRANT_TYPE, "refresh_token");
        try {
            Response response = apiImpl.retrieveAccessToken(params).execute();
            if (response.isSuccessful()) {
                return (AccessTokenResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AccessTokenInfoResponse getAccessTokenInfo(String accessToken) {
        try {
            Response response = apiImpl.getAccessTokenInfo(accessToken).execute();
            if (response.isSuccessful()) {
                return (AccessTokenInfoResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AppResponse getApp() {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            Response response = apiImpl.getApp(params).execute();
            if (response.isSuccessful()) {
                return (AppResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public DevicePinResponse getDevicePin(String deviceId) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(LINKED_DEVICE_ID, deviceId);
            Response response = apiImpl.getDevicePin(params).execute();
            if (response.isSuccessful()) {
                return (DevicePinResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public DevicePinResponse createDevicePin(String deviceId) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(LINKED_DEVICE_ID, deviceId);
            Response response = apiImpl.createDevicePin(params).execute();
            if (response.isSuccessful()) {
                return (DevicePinResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConsumerResponse unlinkDevicePin(String consumerId, String pin) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(CONSUMER_ID, consumerId);
            params.put(PIN, pin);
            Response response = apiImpl.unlinkDevicePin(params).execute();
            if (response.isSuccessful()) {
                return (ConsumerResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConsumerResponse getConsumer(String consumerId, String accessToken) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(ACCESS_TOKEN, accessToken);
            Response response = apiImpl.getConsumer(consumerId, params).execute();
            if (response.isSuccessful()) {
                return (ConsumerResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PlanResponse getPlan(String planId) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            Response response = apiImpl.getPlan(planId, params).execute();
            if (response.isSuccessful()) {
                return (PlanResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public VideoEntitlementsResponse getVideoEntitlements(String accessToken, int page, int perPage) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(ACCESS_TOKEN, accessToken);
            params.put(PER_PAGE, String.valueOf(perPage));
            params.put("sort", "created_at");
            params.put("order", "desc");
            Response response = apiImpl.getVideoEntitlements(page, params).execute();
            if (response.isSuccessful()) {
                return (VideoEntitlementsResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public VideoFavoritesResponse getVideoFavorites(String consumerId, String accessToken, int page) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(ACCESS_TOKEN, accessToken);
            params.put(PAGE, String.valueOf(page));
            params.put(PER_PAGE, String.valueOf(PER_PAGE_DEFAULT));
            params.put("sort", "created_at");
            params.put("order", "desc");
            Response response = apiImpl.getVideoFavorites(consumerId, params).execute();
            if (response.isSuccessful()) {
                return (VideoFavoritesResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PlaylistsResponse getPlaylists(int page) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(PER_PAGE, String.valueOf(300));
            Response response = apiImpl.getPlaylists(page, params).execute();
            if (response.isSuccessful()) {
                return (PlaylistsResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public VideosResponse getPlaylistVideos(String playlistId, int page) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(PER_PAGE, String.valueOf(PER_PAGE_DEFAULT));
            Response response = apiImpl.getPlaylistVideos(playlistId, page, params).execute();
            if (response.isSuccessful()) {
                return (VideosResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PlaylistData loadPlayList(String playlistId) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            Response response = apiImpl.loadPlaylist(playlistId, params).execute();
            if (response.isSuccessful()) {
                return ((PlaylistResponse) response.body()).response;
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public VideoResponse getVideo(String videoId) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            Response response = apiImpl.getVideo(videoId, params).execute();
            if (response.isSuccessful()) {
                return (VideoResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public VideosResponse searchVideos(String query) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(PER_PAGE, String.valueOf(PER_PAGE_DEFAULT));
            params.put(QUERY, query);
            Response response = apiImpl.getVideos(1, params).execute();
            if (response.isSuccessful()) {
                return (VideosResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ZobjectContentResponse getZobjectContents() {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(PER_PAGE, String.valueOf(PER_PAGE_DEFAULT));
            Response response = apiImpl.getZobjectContent(params).execute();
            if (response.isSuccessful()) {
                return (ZobjectContentResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ZobjectTopPlaylistResponse getZobjectTopPlayLists() {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            Response response = apiImpl.getZobjectsForTopPlaylist(params).execute();
            if (response.isSuccessful()) {
                return (ZobjectTopPlaylistResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ZobjectTopPlaylistResponse getZobjectAutoPlayHeroLists() {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            Response response = apiImpl.getZobjectsForAutoPlayHero(params).execute();
            if (response.isSuccessful()) {
                return (ZobjectTopPlaylistResponse) response.body();
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Channel> loadEpgChannels() {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(APP_KEY, ZypeSettings.APP_KEY);
            params.put(PAGE, String.valueOf(1));
            params.put(PER_PAGE, "100");
            Response response = apiImpl.epgChannels(params).execute();
            if (response.isSuccessful()) {
                return ((ChannelResponse) response.body()).response;
            }
            else {
              return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

  public ProgramResponse loadEpgEvents(Channel channel, String startDate, String endDate) {
    try {

      HashMap<String, String> params = new HashMap<>();
      params.put(APP_KEY, ZypeSettings.APP_KEY);
      params.put(PER_PAGE, "500");
      params.put("sort", "start_time");
      params.put("order", "asc");
      params.put("start_time.gte", startDate);
      params.put("end_time.lte", endDate);

      Response response = apiImpl.epgEvents(channel.id, params).execute();

      if (response.isSuccessful()) {
        return ((ProgramResponse) response.body());
      }
      else {
        return null;
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}

package com.zype.fire.api;

import com.zype.fire.api.Model.AccessTokenInfoResponse;
import com.zype.fire.api.Model.AccessTokenResponse;
import com.zype.fire.api.Model.AppResponse;
import com.zype.fire.api.Model.BifrostResponse;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.Model.DevicePinResponse;
import com.zype.fire.api.Model.MarketplaceConnectBody;
import com.zype.fire.api.Model.MarketplaceConnectResponse;
import com.zype.fire.api.Model.PlayerResponse;
import com.zype.fire.api.Model.PlaylistsResponse;
import com.zype.fire.api.Model.VideoData;
import com.zype.fire.api.Model.VideoEntitlementsResponse;
import com.zype.fire.api.Model.VideoFavoriteResponse;
import com.zype.fire.api.Model.VideoFavoritesResponse;
import com.zype.fire.api.Model.VideoResponse;
import com.zype.fire.api.Model.VideosResponse;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Created by Evgeny Cherkasov on 12.04.2017.
 */

public interface IZypeApi {
    String HEADER_USER_AGENT = "AmazonWebAppPlatform";

    String PARAM_PAGE = "page";

    @FormUrlEncoded
    @POST("https://login.zype.com/oauth/token")
    Call<AccessTokenResponse> retrieveAccessToken(@FieldMap Map<String, String> params);

    @GET("https://login.zype.com/oauth/token/info/")
    Call<AccessTokenInfoResponse> getAccessTokenInfo(@Query("access_token") String accessToken);

    @FormUrlEncoded
    @POST("https://bifrost.zype.com/api/v1/subscribe")
    Call<BifrostResponse> verifySubscription(@FieldMap Map<String, String> params);

    // App
    @GET("/app")
    Call<AppResponse> getApp(@QueryMap HashMap<String, String> params);

    // Consumers
    @GET("consumers/{consumer_id}")
    Call<ConsumerResponse> getConsumer(@Path("consumer_id") String consumerId,
                                       @QueryMap HashMap<String, String> params);

    @FormUrlEncoded
    @POST("/consumers")
    Call<ConsumerResponse> createConsumer(@QueryMap HashMap<String, String> queryParams,
                                          @FieldMap HashMap<String, String> fieldParams);

    // Device linking
    @GET("/pin/status")
    Call<DevicePinResponse> getDevicePin(@QueryMap HashMap<String, String> queryParams);

    @POST("/pin/acquire")
    Call<DevicePinResponse> createDevicePin(@QueryMap HashMap<String, String> queryParams);

    @PUT("pin/unlink")
    Call<ConsumerResponse> unlinkDevicePin(@QueryMap HashMap<String, String> queryParams);

    // Video entitlements
    @GET("/videos/{video_id}/entitled")
    Call<ResponseBody> checkVideoEntitlement(@Path("video_id") String videoId,
                                             @QueryMap HashMap<String, String> params);

    @GET("/consumer/videos")
    Call<VideoEntitlementsResponse> getVideoEntitlements(@Query(PARAM_PAGE) int page,
                                                         @QueryMap HashMap<String, String> params);

    // Video Favorites
    @GET("/consumers/{consumer_id}/video_favorites")
    Call<VideoFavoritesResponse> getVideoFavorites(@Path("consumer_id") String consumerId,
                                                   @QueryMap HashMap<String, String> params);

    @FormUrlEncoded
    @POST("/consumers/{consumer_id}/video_favorites")
    Call<VideoFavoriteResponse> addVideoFavorite(@Path("consumer_id") String consumerId,
                                                 @QueryMap HashMap<String, String> queryParams,
                                                 @FieldMap HashMap<String, String> fieldParams);

    @DELETE("/consumers/{consumer_id}/video_favorites/{video_favorite_id}")
    Call<ResponseBody> removeVideoFavorite(@Path("consumer_id") String consumerId,
                                           @Path("video_favorite_id") String videoFavoriteId,
                                           @QueryMap HashMap<String, String> queryParams);

    // Marketplace connect
    @POST("https://mkt.zype.com/v1/amazon/transactions")
    Call<MarketplaceConnectResponse> verifyPurchaseAmazon(@Body MarketplaceConnectBody body);

    // Playlist
    @GET("/playlists")
    Call<PlaylistsResponse> getPlaylists(@Query(PARAM_PAGE) int page, @QueryMap HashMap<String, String> params);

    @GET("/playlists/{playlist_id}/videos")
    Call<VideosResponse> getPlaylistVideos(@Path("playlist_id") String playlistId, @Query(PARAM_PAGE) int page, @QueryMap HashMap<String, String> params);

    // Videos
    @GET("/videos")
    Call<VideosResponse> getVideos(@Query(PARAM_PAGE) int page, @QueryMap HashMap<String, String> params);

    @GET("/videos/{video_id}")
    Call<VideoResponse> getVideo(@Path("video_id") String videoId, @QueryMap HashMap<String, String> params);

    @GET("https://player.zype.com/embed/{video_id}.json")
    Call<PlayerResponse> getPlayer(@Header("User-Agent") String userAgent, @Path("video_id") String videoId, @QueryMap HashMap<String, String> params);

}

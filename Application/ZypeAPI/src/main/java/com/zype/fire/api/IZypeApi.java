package com.zype.fire.api;

import com.zype.fire.api.Model.AccessTokenInfoResponse;
import com.zype.fire.api.Model.AccessTokenResponse;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.Model.PlayerResponse;
import com.zype.fire.api.Model.PlaylistsResponse;
import com.zype.fire.api.Model.VideosResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Created by Evgeny Cherkasov on 12.04.2017.
 */

public interface IZypeApi {
    String HEADER_USER_AGENT = "Dalvik/2.1.0 (Zype Android; Linux; U; Android 5.0.2; One X Build/LRX22G)";

    String PARAM_PAGE = "page";

    @FormUrlEncoded
    @POST("https://login.zype.com/oauth/token")
    Call<AccessTokenResponse> retrieveAccessToken(@FieldMap Map<String, String> params);

    @GET("https://login.zype.com/oauth/token/info/")
    Call<AccessTokenInfoResponse> getAccessTokenInfo(@Query("access_token") String accessToken);

    @GET("consumers/{consumer_id}")
    Call<ConsumerResponse> getConsumer(@Path("consumer_id") String consumerId, @QueryMap HashMap<String, String> params);

    @GET("https://player.zype.com/embed/{video_id}.json")
    Call<PlayerResponse> getPlayer(@Header("User-Agent") String header, @Path("video_id") String videoId, @QueryMap HashMap<String, String> params);

    @GET("/playlists")
    Call<PlaylistsResponse> getPlaylists(@Query(PARAM_PAGE) int page, @QueryMap HashMap<String, String> params);

    @GET("/playlists/{playlist_id}/videos")
    Call<VideosResponse> getPlaylistVideos(@Path("playlist_id") String playlistId, @Query(PARAM_PAGE) int page, @QueryMap HashMap<String, String> params);

    @GET("/videos")
    Call<VideosResponse> getVideos(@Query(PARAM_PAGE) int page, @QueryMap HashMap<String, String> params);
}

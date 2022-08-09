package com.amazon.dataloader.datadownloader;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.amazon.android.recipe.Recipe;
import com.zype.fire.api.Model.PlaylistData;
import com.zype.fire.api.Model.PlaylistsResponse;
import com.zype.fire.api.Model.VideoData;
import com.zype.fire.api.Model.VideoEntitlementData;
import com.zype.fire.api.Model.VideoFavoriteData;
import com.zype.fire.api.Model.VideoFavoritesResponse;
import com.zype.fire.api.Model.VideoResponse;
import com.zype.fire.api.Model.VideosResponse;
import com.zype.fire.api.Model.ZobjectContentData;
import com.zype.fire.api.Model.ZobjectContentResponse;
import com.zype.fire.api.ZypeApi;

import java.util.ArrayList;
import java.util.List;
import rx.Single;
import rx.Single.OnSubscribe;
import rx.schedulers.Schedulers;

/**
 * Created by Evgeny Cherkasov on 20.11.2017.
 */

public class ZypeDataDownloaderHelper {
    private static final String TAG = ZypeDataDownloaderHelper.class.getSimpleName();

    public static class VideosResult {
        public List<VideoData> videos;
        public int nextPage;
    }

    public static VideosResult loadVideos(List<String> videoIds, String playlistId) {
        Log.d(TAG, "loadVideos(): ");

        VideosResult result = new VideosResult();
        result.videos = new ArrayList<>();
        result.nextPage = -1;

        for (String videoId : videoIds) {
            VideoResponse responseVideo = ZypeApi.getInstance().getVideo(videoId);
            if (responseVideo != null) {
                VideoData videoData = responseVideo.videoData;
                if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
                    videoData.description = " ";
                }
                videoData.playlistId = playlistId;
                videoData.playerUrl = "null";
                videoData.videoFavoriteId = null;
                result.videos.add(videoData);
            }
            else {
                Log.e(TAG, "loadFavoriteVideos(): error loading video, id=" + videoId);
                return null;
            }
        }

        return result;
    }

    public static PlaylistData loadPlayList(String playlistId) {
        return ZypeApi.getInstance().loadPlayList(playlistId);
    }

    public static VideosResponse loadPlaylistVideos(String playlistId, int page) {
        Log.d(TAG, "loadPlaylistVideos(): id=" + playlistId);

        VideosResponse response = ZypeApi.getInstance().getPlaylistVideos(playlistId, page);
        if (response != null) {
            Log.d(TAG, "loadPlaylistVideos(): size=" + response.videoData.size());
            for (VideoData videoData : response.videoData) {
                // Put a space string for description if it is not specified to avoid crashing
                // because 'description' is mandatory field in the Content model
                if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
                    videoData.description = " ";
                }
                // Add reference to playlist
                videoData.playlistId = playlistId;
                // Set dummy player url. We get real url before switch to renderer screen
                videoData.playerUrl = "null";
            }
        }
        else {
            Log.e(TAG, "loadPlaylistVideos(): failed");
        }
        return response;
    }

    public static VideosResult loadFavoriteVideos(String favoritesPlaylistId, String consumerId, String accessToken, int page) {
        Log.d(TAG, "loadFavoriteVideos(): consumerId=" + consumerId);

        VideosResult result = new VideosResult();
        result.nextPage = page;

        boolean loadNext = true;

        while (loadNext) {
            VideoFavoritesResponse response = ZypeApi.getInstance()
                .getVideoFavorites(consumerId, accessToken, result.nextPage);
            if (response != null) {
                Log.d(TAG, "loadFavoriteVideos(): size=" + response.videoFavorites.size());

                if (result.videos == null) {
                    result.videos = new ArrayList<>();
                }

                if (response.pagination.current >= response.pagination.pages) {
                    result.nextPage = -1;
                }
                else {
                    result.nextPage = response.pagination.next;
                }

                for (VideoFavoriteData data : response.videoFavorites) {
                    VideoResponse responseVideo = ZypeApi.getInstance().getVideo(data.videoId);
                    if (responseVideo != null) {
                        VideoData videoData = responseVideo.videoData;
                        if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
                            videoData.description = " ";
                        }
                        videoData.playlistId = favoritesPlaylistId;
                        videoData.playerUrl = "null";
                        videoData.videoFavoriteId = data.id;
                        result.videos.add(videoData);
                    }
                    else {
                        Log.e(TAG, "loadFavoriteVideos(): error loading video, id=" + data.videoId);
                    }
                }
            }
            else {
                Log.e(TAG, "loadFavoriteVideos(): failed");
                return null;
            }

            // Load all favorites
            loadNext = !(result.nextPage == -1);
        }
        return result;
    }

    public static List<PlaylistData> loadPlaylists() {
        List<PlaylistData> result = new ArrayList<>();

        int page = 1;
        PlaylistsResponse playlistsResponse = ZypeApi.getInstance().getPlaylists(page);
        if (playlistsResponse != null && playlistsResponse.response != null) {
            result.addAll(playlistsResponse.response);
            if (playlistsResponse.pagination != null && playlistsResponse.pagination.pages > 1) {
                for (page = playlistsResponse.pagination.next; page <= playlistsResponse.pagination.pages; page++) {
                    playlistsResponse = ZypeApi.getInstance().getPlaylists(page);
                    if (playlistsResponse != null && playlistsResponse.response != null) {
                        result.addAll(playlistsResponse.response);
                    }
                }
            }
        }
        return result;
    }

    public static Single<Pair<PlaylistData, VideosResponse>> loadPlayListVideos(PlaylistData playlistData) {
        return Single.create((OnSubscribe<Pair<PlaylistData, VideosResponse>>) emitter -> {
            Log.d(TAG, "fetchData(): Loading videos for " + playlistData.title);

            VideosResponse videosResponse = loadPlaylistVideos(playlistData.id, 1);
            if(videosResponse == null) {
                videosResponse = new VideosResponse();
            }
            emitter.onSuccess(Pair.create(playlistData, videosResponse));
        }).subscribeOn(Schedulers.io());
    }
}

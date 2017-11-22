package com.amazon.dataloader.datadownloader;

import android.text.TextUtils;
import android.util.Log;

import com.zype.fire.api.Model.VideoData;
import com.zype.fire.api.Model.VideosResponse;
import com.zype.fire.api.ZypeApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 20.11.2017.
 */

public class ZypeDataDownloaderHelper {
    private static final String TAG = ZypeDataDownloaderHelper.class.getSimpleName();

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
}

package com.amazon.dataloader.datadownloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amazon.android.recipe.Recipe;
import com.amazon.android.utils.Preferences;
import com.amazon.dataloader.R;
import com.amazon.utils.model.Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zype.fire.api.Model.AppData;
import com.zype.fire.api.Model.AppResponse;
import com.zype.fire.api.Model.PlaylistData;
import com.zype.fire.api.Model.PlaylistsResponse;
import com.zype.fire.api.Model.VideoData;
import com.zype.fire.api.Model.VideosResponse;
import com.zype.fire.api.Model.ZobjectContentData;
import com.zype.fire.api.Model.ZobjectContentResponse;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 04.03.2017.
 */

public class ZypeDataDownloader extends ADataDownloader {
    private static final String TAG = ZypeDataDownloader.class.getSimpleName();

    // Key to locate the URL generator implementation.
    protected static final String URL_GENERATOR_IMPL = "url_generator_impl";
    // Key to locate the URL generator.
    protected static final String URL_GENERATOR_RECIPE = "url_generator";

    private static final String PREFERENCE_TERMS = "ZypeTerms";

    /**
     * {@link AUrlGenerator} instance.
     */
    private final AUrlGenerator urlGenerator;

    /**
     * Constructor for {@link ZypeDataDownloader}. It initializes the URL generator using
     * the URL generator implementation defined in the configuration.
     *
     * @param context The context.
     * @throws ObjectCreatorException Any exception generated while fetching this instance will be
     *                                wrapped in this exception.
     * @throws DataLoaderException    If there was an error while creating the URL generator.
     */
    public ZypeDataDownloader(Context context) throws ObjectCreatorException, DataLoaderException {
        super(context);
        try {
            String urlGeneratorClassPath = mConfiguration.getItemAsString(URL_GENERATOR_IMPL);
            this.urlGenerator = UrlGeneratorFactory.createUrlGenerator(mContext, urlGeneratorClassPath);
        }
        catch (UrlGeneratorFactory.UrlGeneratorInitializationFailedException e) {
            throw new DataLoaderException("Exception in initialization of " + "ZypeDataDownloader ", e);
        }
    }

    /**
     * Creates an instance of this class.
     *
     * @param context The context.
     * @return The {@link BasicHttpBasedDataDownloader} instance.
     * @throws ObjectCreatorException Any exception generated while fetching this instance will be
     *                                wrapped in this exception.
     */
    public static ADataDownloader createInstance(Context context) throws ObjectCreatorException {
        try {
            return new ZypeDataDownloader(context);
        }
        catch (DataLoaderException e) {
            throw new ObjectCreatorException("Exception while creating instance ", e);
        }
    }

    /**
     * Returns the configuration file path for this class relative to the assets folder.
     *
     * @param context The application context.
     * @return The path of the config file.
     */
    @Override
    protected String getConfigFilePath(Context context) {
        return mContext.getString(R.string.zype_downloader_config_file_path);
    }

    /**
     * Fetches the {@link Data} for this data downloader.
     *
     * @param dataLoadRecipe The data load recipe.
     * @return The downloaded {@link Data}.
     * @throws Exception if there was an error while fetching the data.
     */
    @Override
    protected Data fetchData(Recipe dataLoadRecipe) throws Exception {
        Log.d(TAG, "fetchData(): Started");

        AppData appData = loadAppConfiguration();
        Log.d(TAG, "fetchData(): App configuration loaded");
        ZypeConfiguration.update(appData, mContext);

        loadZobjectContents();

        List<PlaylistData> playlists = loadPlaylists();
        Log.d(TAG, "fetchData(): Playlists loaded");
        addFavoritesPlaylist(playlists);
        if (ZypeSettings.LIBRARY_ENABLED) {
            addMyLibraryPlaylists(playlists);
        }

        // Result data
        JSONArray jsonCategories = new JSONArray();
        JSONArray jsonContents = new JSONArray();

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        for (PlaylistData playlistData : playlists) {
            if (TextUtils.isEmpty(playlistData.description)) {
                playlistData.description = " ";
            }
            // Skip playlist that are not direct child of the root playlist
            if (TextUtils.isEmpty(playlistData.parentId)
                    || !playlistData.parentId.equals(ZypeConfiguration.getRootPlaylistId(mContext))) {
                continue;
            }

            if (playlistData.playlistItemCount > 0) {
                Log.d(TAG, "fetchData(): Loading videos for " + playlistData.title);

                VideosResponse videosResponse = ZypeDataDownloaderHelper.loadPlaylistVideos(playlistData.id, 1);
                if (videosResponse != null) {
                    for (VideoData videoData : videosResponse.videoData) {
                        jsonContents.put(new JSONObject(gson.toJson(videoData)));
                    }
                }
            }
        }
        Log.d(TAG, "fetchData(): Videos loaded");

        Collections.sort(playlists, (a, b) -> {
            Integer valA;
            Integer valB;
            try {
                valA = a.priority;
                valB = b.priority;
            }
            catch (Exception e) {
                return 0;
            }
            return valA.compareTo(valB);
        });

        for (PlaylistData playlistData : playlists) {
            String playlistId = playlistData.id;
            if (playlistId.equals(ZypeConfiguration.getRootPlaylistId(mContext))
                    || TextUtils.isEmpty(playlistData.parentId)) {
                continue;
            }
            jsonCategories.put(new JSONObject(gson.toJson(playlistData)));
        }

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("categories", jsonCategories);
        jsonResult.put("contents", jsonContents);

        Log.d(TAG, "fetchData(): finished");
        return Data.createDataForPayload(jsonResult.toString());
    }

    private AppData loadAppConfiguration() {
        AppData result = new AppData();

        AppResponse appResponse = ZypeApi.getInstance().getApp();
        if (appResponse != null && appResponse.data != null) {
            result = appResponse.data;
        }

        // TODO: Delete this for release build
        result.universalTVOD = null;

        return result;
    }

    private void loadZobjectContents() {
        ZobjectContentResponse response = ZypeApi.getInstance().getZobjectContents();
        if (response != null) {
            Log.d(TAG, "loadZobjectContents(): size=" + response.zobjectContents.size());
            for (ZobjectContentData item : response.zobjectContents) {
                if (item.friendlyTitle.equals("privacy_policy")) {
                    Preferences.setString(PREFERENCE_TERMS, item.description);
                    return;
                }
            }
            Preferences.setString(PREFERENCE_TERMS, null);
        }
        else {
            Log.e(TAG, "loadZobjectContents(): failed");
            Preferences.setString(PREFERENCE_TERMS, null);
        }
    }

    private List<PlaylistData> loadPlaylists() {
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

    private void addFavoritesPlaylist(List<PlaylistData> playlists) {
        PlaylistData item = new PlaylistData();
        item.id = ZypeSettings.ROOT_FAVORITES_PLAYLIST_ID;
        item.description = " ";
        item.parentId = ZypeConfiguration.getRootPlaylistId(mContext);
        item.thumbnailLayout = "landscape";
        item.title = ZypeSettings.ROOT_FAVORITES_PLAYLIST_ID;
        playlists.add(item);

        item = new PlaylistData();
        item.id = ZypeSettings.FAVORITES_PLAYLIST_ID;
        // TODO: Use string resources for description and title instead of hardcoded strings
        item.description = "Favorites";
        item.parentId = ZypeSettings.ROOT_FAVORITES_PLAYLIST_ID;
        item.thumbnailLayout = "landscape";
        item.title = ZypeSettings.FAVORITES_PLAYLIST_ID;
        playlists.add(item);
    }

    private void addMyLibraryPlaylists(List<PlaylistData> playlists) {
        PlaylistData item = new PlaylistData();
        item.id = ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID;
        item.description = " ";
        item.parentId = ZypeConfiguration.getRootPlaylistId(mContext);
        item.thumbnailLayout = "landscape";
        item.title = ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID;
        playlists.add(item);

        item = new PlaylistData();
        item.id = ZypeSettings.MY_LIBRARY_PLAYLIST_ID;
        item.description = " ";
        item.parentId = ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID;
        item.playlistItemCount = 1;
        item.thumbnailLayout = "landscape";
        item.title = "Library";
        playlists.add(item);
    }

//    private List<VideoData> loadPlaylistVideos(PlaylistData playlist) {
//        Log.d(TAG, "loadPlaylistVideos(): " + playlist.title);
//        List<VideoData> result = new ArrayList<>();
//
//        VideosResponse response = ZypeApi.getInstance().getPlaylistVideos(playlist.id, 1);
//        if (response != null) {
//            Log.d(TAG, "loadPlaylistVideos(): size=" + response.videoData.size());
//            for (VideoData videoData : response.videoData) {
//                // Put a space string for description if it is not specified to avoid crashing
//                // because 'description' is mandatory field in the Content model
//                if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
//                    videoData.description = " ";
//                }
//                // Add reference to playlist
//                videoData.playlistId = playlist.id;
//                // Set dummy player url. We get real url before switch to renderer screen
//                videoData.playerUrl = "null";
//            }
//            result.addAll(response.videoData);
//        }
//        return result;
//    }

//    public String getPlaylistFeed(PlaylistData playlist) {
//        List<VideoData> videos = loadPlaylistVideos(playlist);
//        if (videos == null || videos.isEmpty()) {
//            return null;
//        }
//        GsonBuilder builder = new GsonBuilder();
//        Gson gson = builder.create();
//        return gson.toJson(videos);
//    }
}

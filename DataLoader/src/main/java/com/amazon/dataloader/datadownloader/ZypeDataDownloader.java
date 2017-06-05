package com.amazon.dataloader.datadownloader;

import android.content.Context;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.amazon.android.recipe.Recipe;
import com.amazon.android.utils.Helpers;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.dataloader.R;
import com.amazon.utils.model.Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zype.fire.api.Model.PlaylistData;
import com.zype.fire.api.Model.PlaylistsResponse;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 04.03.2017.
 */

public class ZypeDataDownloader extends ADataDownloader {
    private static final String TAG = ZypeDataDownloader.class.getSimpleName();

    // Key to locate the URL generator implementation.
    protected static final String URL_GENERATOR_IMPL = "url_generator_impl";
    // Key to locate the URL generator.
    protected static final String URL_GENERATOR_RECIPE = "url_generator";

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

        List<PlaylistData> playlists = loadPlaylists();
        Log.d(TAG, "fetchData(): Playlists loaded");

//        // Starting with an empty map and replacing it with a map from recipe if one exists.
//        Map urlGeneratorRecipeMap = Collections.emptyMap();
//        if (dataLoadRecipe.getMap().containsKey(URL_GENERATOR_RECIPE)) {
//            urlGeneratorRecipeMap = (Map) dataLoadRecipe.getMap().get(URL_GENERATOR_RECIPE);
//        }
//        // Get the url.
//        String url = urlGenerator.getUrl(urlGeneratorRecipeMap);
//        Log.d(TAG, "url: " + url);
//        Data result = Data.createDataForPayload(NetworkUtils.getDataLocatedAtUrl(url));
//        JSONObject jsonResult = new JSONObject(result.getContent().getPayload());

        Map<String, Object> params;
        // Url to retrieve playlist videos
        params = new HashMap<>();
        params.put("url_index", "1");
        String urlPlaylistVideos = urlGenerator.getUrl(params);
//        // Url to retrieve player
//        params = new HashMap<>();
//        params.put("url_index", "3");
//        String urlPlayer = urlGenerator.getUrl(params);

        // Result data
        JSONArray jsonCategories = new JSONArray();
        JSONArray jsonContents = new JSONArray();

//        JSONArray jsonPlaylists = jsonResult.getJSONArray("response");
//        List<JSONObject> playlists = new ArrayList<>();
//        for (int i = 0; i < jsonPlaylists.length(); i++) {
        for (PlaylistData playlistData : playlists) {
            // Skip not direct root child playlist
            if (TextUtils.isEmpty(playlistData.parentId) || !playlistData.parentId.equals(ZypeSettings.ROOT_PLAYLIST_ID)) {
                continue;
            }
//            JSONObject jsonPlaylistData = jsonPlaylists.getJSONObject(i);
//            playlists.add(jsonPlaylistData);

//            String playlistId = jsonPlaylistData.getString("_id");
            String playlistId = playlistData.id;

//            if (jsonPlaylistData.getInt("playlist_item_count") > 0) {
            if (playlistData.playlistItemCount > 0) {
                Log.d(TAG, "fetchData(): Loading videos for " + playlistData.title);
                String url = String.format(urlPlaylistVideos, playlistId);
                try {
                    String playlistVideosResponse = NetworkUtils.getDataLocatedAtUrl(url);
                    JSONObject jsonPlaylistVideosResponse = new JSONObject(playlistVideosResponse);
                    JSONArray jsonPlaylistVideos = jsonPlaylistVideosResponse.getJSONArray("response");
                    for (int j = 0; j < jsonPlaylistVideos.length(); j++) {
                        JSONObject jsonVideoData = jsonPlaylistVideos.getJSONObject(j);
                        // Put a space string for description if it is not specified to avoid crashing
                        // because 'description' is mandatory field in the Content model
                        if (TextUtils.isEmpty(jsonVideoData.getString("description")) || jsonVideoData.getString("description").equals("null")) {
                            jsonVideoData.put("description", " ");
                        }
                        // Add reference to playlist
                        jsonVideoData.put("playlistId", playlistId);
                        // Set dummy player url. We get real url before switch to renderer screen
                        jsonVideoData.put("playerUrl", "null");

                        jsonContents.put(jsonVideoData);
                    }
                }
                catch (IOException e) {
//                    jsonPlaylistData.put("videoIds", new JSONArray());
                    Log.d(TAG, "Error get playlist videos. playlistId=" + playlistId);
                }
            }
            else {

            }
        }
        Log.d(TAG, "fetchData(): Videos loaded");

        Collections.sort(playlists, (a, b) -> {
            Integer valA;
            Integer valB;
            try {
//                valA = a.getInt("priority");
//                valB = b.getInt("priority");
                valA = a.priority;
                valB = b.priority;
            }
            catch (Exception e) {
                return 0;
            }
            return valA.compareTo(valB);
        });
//        for (JSONObject jsonPlaylistData : playlists) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        for (PlaylistData playlistData : playlists) {
//            String playlistId = jsonPlaylistData.getString("_id");
            String playlistId = playlistData.id;
            if (playlistId.equals(ZypeSettings.ROOT_PLAYLIST_ID) || TextUtils.isEmpty(playlistData.parentId)) {
                continue;
            }
            jsonCategories.put(new JSONObject(gson.toJson(playlistData)));
        }

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("categories", jsonCategories);
        jsonResult.put("contents", jsonContents);
//        result.getContent().setPayload(jsonResult.toString());

//        return result;
        Log.d(TAG, "fetchData(): finished");
        return Data.createDataForPayload(jsonResult.toString());
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

}

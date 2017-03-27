package com.amazon.dataloader.datadownloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amazon.android.recipe.Recipe;
import com.amazon.android.utils.Helpers;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.dataloader.R;
import com.amazon.utils.model.Data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
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

    private static final String PLAYLIST_ID = "playlist_id";
    private static final String API_KEY = "6vf5at5colfo7wtTR13GBDdigAUJOS1C_4BTdOOBHcLYjd1BiSfvRZwKggzbSUGF";
    private static final String API_HOST = "https://api.zype.com";

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
        // Starting with an empty map and replacing it with a map from recipe if one exists.
        Map urlGeneratorRecipeMap = Collections.emptyMap();
        if (dataLoadRecipe.getMap().containsKey(URL_GENERATOR_RECIPE)) {
            urlGeneratorRecipeMap = (Map) dataLoadRecipe.getMap().get(URL_GENERATOR_RECIPE);
        }
        // Get the url.
        String url = urlGenerator.getUrl(urlGeneratorRecipeMap);
        Log.d(TAG, "url: " + url);
        Data result = Data.createDataForPayload(NetworkUtils.getDataLocatedAtUrl(url));
//        Data result = Data.createDataForPayload(getDataLocatedAtUrl(url));

        int urlIndex = Integer.parseInt((String) urlGeneratorRecipeMap.get("url_index"));
        switch (urlIndex) {
            // Playlists
            case 0: {
                // Url to retrieve playlist videos
                Map<String, Object> params = new HashMap<>();
                params.put("url_index", "1");
                String urlPlaylistVideos = urlGenerator.getUrl(params);

                JSONObject jsonResult = new JSONObject(result.getContent().getPayload());
                JSONArray jsonPlaylists = jsonResult.getJSONArray("response");
                JSONArray jsonPlaylistsFiltered = new JSONArray();
                String rootPlaylistId = "577e65c85577de0d1000c1ee";
                for (int i = 0; i < jsonPlaylists.length(); i++) {
                    JSONObject jsonPlaylistData = jsonPlaylists.getJSONObject(i);
                    // Get playlist videos and add their ids to 'videoIds' parameter of result
                    if (jsonPlaylistData.getInt("playlist_item_count") > 0) {
                        String playlistId = jsonPlaylistData.getString("_id");
                        url = String.format(urlPlaylistVideos, playlistId);
                        try {
                            String playlistVideosResponse = NetworkUtils.getDataLocatedAtUrl(url);
                            JSONObject jsonPlaylistVideosResponse = new JSONObject(playlistVideosResponse);
                            JSONArray jsonPlaylistVideos = jsonPlaylistVideosResponse.getJSONArray("response");
//                            JSONArray jsonVideoIds = new JSONArray();
                            for (int j = 0; j < jsonPlaylistVideos.length(); j++) {
                                String videoId = jsonPlaylistVideos.getJSONObject(j).getString("_id");
                                // See ZypeContentTranslator for details of converting video id to its hash code
                                jsonPlaylistData.put("videoIds", String.valueOf(videoId.hashCode()));
                                jsonPlaylistsFiltered.put(jsonPlaylistData);
//                                jsonVideoIds.put(String.valueOf(videoId.hashCode()));
                            }
//                            jsonPlaylistData.put("videoIds", jsonVideoIds);
//                            jsonPlaylistsFiltered.put(jsonPlaylistData);
                        } catch (IOException e) {
                            jsonPlaylistData.put("videoIds", new JSONArray());
                            Log.d(TAG, "Error get playlist videos. playlistId=" + playlistId);
                        }
                    }
                    else {
                        jsonPlaylistData.put("videoIds", new JSONArray());
                    }
//                    if (jsonPlaylistData.getString("parent_id").equals(rootPlaylistId)) {
//                        jsonPlaylistsFiltered.put(jsonPlaylistData);
//                    }
                }
                jsonResult.put("response", jsonPlaylistsFiltered);
                result.getContent().setPayload(jsonResult.toString());
                break;
            }
            // Video data
            case 2: {
                // Url to retrieve player
                Map<String, Object> params = new HashMap<>();
                params.put("url_index", "3");
                String urlPlayer = urlGenerator.getUrl(params);
                JSONObject jsonResult = new JSONObject(result.getContent().getPayload());
                JSONArray jsonVideos = jsonResult.getJSONArray("response");
                for (int i = 0; i < jsonVideos.length(); i++) {
                    JSONObject jsonVideoData = jsonVideos.getJSONObject(i);
                    String videoId = jsonVideoData.getString("_id");
                    url = String.format(urlPlayer, videoId, videoId);
                    try {
                        String playerResponse = getDataLocatedAtUrl(url);
                        JSONObject jsonPlayer = new JSONObject(playerResponse);
                        JSONArray jsonFiles = jsonPlayer.getJSONObject("response").getJSONObject("body").getJSONArray("files");
                        if (jsonFiles != null && jsonFiles.length() > 0) {
                            String playerUrl = jsonFiles.getJSONObject(0).getString("url");
                            jsonVideoData.put("playerUrl", playerUrl);
                        } else {
                            jsonVideoData.put("playerUrl", "null");
                        }
                    } catch (IOException e) {
                        jsonVideoData.put("playerUrl", "null");
                        Log.d(TAG, "Error get url for videoId=" + videoId);
                    }
                }
                result.getContent().setPayload(jsonResult.toString());
                break;
            }
        }
        return result;

//        String playlistId = null;
//        if (dataLoadRecipe.getMap().containsKey(PLAYLIST_ID)) {
//            playlistId = (String) dataLoadRecipe.getMap().get(PLAYLIST_ID);
//        }
//        playlistId = "571a4dc68600bd07fb00431c";
//        if (playlistId == null) {
//            return null;
//        }
//        String url = API_HOST + String.format("/playlists/%1$s/videos?app_key=%2$s", playlistId, API_KEY);
//        Log.d(TAG, "url: " + url);
//
//        Data result = Data.createDataForPayload(NetworkUtils.getDataLocatedAtUrl(url));
//        return result;
    }


    public static String getDataLocatedAtUrl(String urlString) throws IOException {

        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Dalvik/2.1.0 (Zype Android; Linux; U; Android 5.0.2; One X Build/LRX22G)");
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), Helpers.getDefaultAppCharset()), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                }
                catch (IOException e) {
                    Log.e(TAG, "Closing input stream failed", e);
                }
            }
        }
    }


}

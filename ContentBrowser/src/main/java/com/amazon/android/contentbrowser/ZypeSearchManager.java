package com.amazon.android.contentbrowser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amazon.android.model.content.Content;
import com.amazon.android.model.translators.ZypeContentTranslator;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.search.ISearchResult;
import com.amazon.dynamicparser.DynamicParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zype.fire.api.Model.VideoData;
import com.zype.fire.api.Model.VideosResponse;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Evgeny Cherkasov on 25.05.2017.
 */

public class ZypeSearchManager implements ContentBrowser.ICustomSearchHandler {
    private static final String TAG = ZypeSearchManager.class.getSimpleName();

    private Context context;
    private Recipe recipeSearchContents;


    public ZypeSearchManager(Recipe recipeSearchContents, Context context) {
        this.recipeSearchContents = recipeSearchContents;
        this.context = context;
    }

    @Override
    public void onSearchRequested(String query, ISearchResult iSearchResult) {
        HashMap<String, String> params = new HashMap<>();
        params.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
        params.put(ZypeApi.PER_PAGE, String.valueOf(ZypeApi.PER_PAGE_DEFAULT));
        params.put(ZypeApi.PLAYLIST_ID_INCLUSIVE, ZypeConfiguration.getRootPlaylistId(context));
        params.put(ZypeApi.QUERY, query);

        ZypeApi.getInstance().getApi().getVideos(1, params).enqueue(new Callback<VideosResponse>() {
            @Override
            public void onResponse(Call<VideosResponse> call, Response<VideosResponse> response) {
                if (response.isSuccessful()) {
                    if (!response.body().videoData.isEmpty()) {
                        Log.d(TAG, "onResponse(): size=" + response.body().videoData.size());
                        for (VideoData videoData : response.body().videoData) {
                            if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
                                videoData.description = " ";
                            }
                            videoData.playlistId = "";
                            videoData.playerUrl = "null";
                        }
                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.create();
                        String feed = gson.toJson(response.body().videoData);
                        Subscription subscription = getSearchContentsObservable(feed, iSearchResult)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(result -> {
                                        },
                                        throwable -> {
                                        },
                                        () -> {
                                            iSearchResult.onSearchResult(null, true);
                                        });
                    }
                    else {
                        Log.d(TAG, "onResponse(): No videos found");
                        iSearchResult.onSearchResult(null, true);
                    }
                }
                else {
                    Log.d(TAG, "onResponse(): Error: " + response.message());
                    iSearchResult.onSearchResult(null, true);
                }
            }

            @Override
            public void onFailure(Call<VideosResponse> call, Throwable t) {
                iSearchResult.onSearchResult(null, true);
            }
        });
    }

    private Observable<Object> getSearchContentsObservable(String feed, ISearchResult iSearchResult) {
        DynamicParser parser = new DynamicParser();
        ZypeContentTranslator zypeContentTranslator = new ZypeContentTranslator();
        parser.addTranslatorImpl(zypeContentTranslator.getName(), zypeContentTranslator);

        String[] params = new String[] { (String) "" };
        return parser.cookRecipeObservable(recipeSearchContents, feed, null, params)
                .map(contentAsObject -> {
                    Content content = (Content) contentAsObject;
                    if (content != null) {
                        iSearchResult.onSearchResult(content, false);
                    }
                    return content;
                });
    }
}

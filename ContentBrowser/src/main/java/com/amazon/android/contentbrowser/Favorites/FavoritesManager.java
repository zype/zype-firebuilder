/**
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.android.contentbrowser.Favorites;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.ContentLoader;
import com.amazon.android.contentbrowser.database.helpers.VideoFavoritesHelper;
import com.amazon.android.contentbrowser.database.records.VideoFavoriteRecord;
import com.amazon.android.contentbrowser.helper.ErrorHelper;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.event.FavoritesLoadEvent;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.utils.ErrorUtils;
import com.google.gson.Gson;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Zype, Evgeny Cherkasov
 *
 * The video favorites manager contains the logic required to
 */
public class FavoritesManager {
    private static final String TAG = FavoritesManager.class.getName();

    private final Context context;
    private final ContentBrowser contentBrowser;
    private final ContentLoader contentLoader;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private EventBus eventBus;

    /**
     * Constructor. Initializes member variables and configures the purchase system.
     *
     * @param context        The context.
     */
    public FavoritesManager(Context context, ContentBrowser contentBrowser) {
        this.context = context;
        this.contentBrowser = contentBrowser;
        this.contentLoader = ContentLoader.getInstance(context);

        eventBus = EventBus.getDefault();
    }

    public void clearVideoFavorites() {
        if (ZypeConfiguration.isFavoritesViaApiEnabled(context)) {
            contentBrowser.setFavoritesLoaded(false);
            VideoFavoritesHelper.getInstance().clearDatabase(context);
        }
    }

    public void loadFavoritesVideos(ContentContainer contentContainer) {
        Observable<Object> observable = Observable.just(contentContainer.getContentContainers().get(0));
        Recipe recipeDynamicParserVideos = Recipe.newInstance(context, "recipes/ZypeSearchContentsRecipe.json");
        Subscription subscription = observable
                .subscribeOn(Schedulers.newThread())
                .concatMap(contentContainerAsObject -> {
                    return contentLoader.getLoadContentsObservable(Observable.just(contentContainerAsObject), recipeDynamicParserVideos);
                })
                .onBackpressureBuffer() // This must be right after concatMap.
                .doOnNext(o -> { })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                        },
                        throwable -> {
                            Log.e(TAG, "loadFavoritesVideos(): failed: ", throwable);
                            ErrorHelper.injectErrorFragment(
                                    contentBrowser.getNavigator().getActiveActivity(),
                                    ErrorUtils.ERROR_CATEGORY.FEED_ERROR,
                                    (errorDialogFragment, errorButtonType, errorCategory) -> {
                                        if (errorButtonType == ErrorUtils.ERROR_BUTTON_TYPE.EXIT_APP) {
                                            contentBrowser.getNavigator().getActiveActivity().finishAffinity();
                                        }
                                    });
                        },
                        () -> {
                            Log.v(TAG, "loadFavoritesVideos(): completed");
                            contentBrowser.setFavoritesLoaded(true);
                            eventBus.post(new FavoritesLoadEvent(contentBrowser.isFavoritesLoaded()));
                        });
        compositeSubscription.add(subscription);
    }

    // //////////
    // Actions
    //
    public void handleAddAction(Content content) {
        if (ZypeConfiguration.isFavoritesViaApiEnabled(context)) {
            contentLoader
                    .addVideoFavorite(content)
                    .subscribe(resultContent -> {
                        if (!TextUtils.isEmpty(resultContent.getExtraValueAsString(Content.EXTRA_VIDEO_FAVORITE_ID))) {
                            addLocalFavorite(resultContent);
                        }
                        else {
                            // TODO: Handke error
                        }
                    });
        }
        else {
            addLocalFavorite(content);
        }
    }

    public void handleRemoveAction(Content content) {
        if (ZypeConfiguration.isFavoritesViaApiEnabled(context)) {
//            ContentContainer favoritesContainer = contentLoader.getRootContentContainer()
//                    .findContentContainerById(ZypeSettings.FAVORITES_PLAYLIST_ID);
//            if (favoritesContainer == null) {
//                return;
//            }
//            Content favoriteVideo = favoritesContainer.findContentById(content.getId());
//            if (favoriteVideo == null) {
//                return;
//            }
//            String videoFavoriteId = favoriteVideo.getExtraValueAsString(Content.EXTRA_VIDEO_FAVORITE_ID);
            VideoFavoriteRecord videoFavorite = getVideoFavorite(content);
            String videoFavoriteId = videoFavorite.getVideoFavoriteId();
            contentLoader
                    .removeVideoFavorite(content, videoFavoriteId)
                    .subscribe(resultContent -> {
                        if (TextUtils.isEmpty(resultContent.getExtraValueAsString(Content.EXTRA_VIDEO_FAVORITE_ID))) {
                            deleteLocalFavorite(resultContent);
                        }
                        else {
                            // TODO: Handke error
                        }
                    });
        }
        else {
            deleteLocalFavorite(content);
        }
    }

    // //////////
    //
    //
    public void addVideoToFavoritesContentContainer(Content content) {
        ContentContainer favoritesContainer = contentLoader.getRootContentContainer()
                .findContentContainerById(ZypeSettings.FAVORITES_PLAYLIST_ID);
        if (favoritesContainer != null) {
            Gson gson = new Gson();
            String jsonFavoriteVideo = gson.toJson(content);
            Content favoriteVideo = gson.fromJson(jsonFavoriteVideo, content.getClass());
            favoriteVideo.setExtraValue(Content.EXTRA_PLAYLIST_ID, ZypeSettings.FAVORITES_PLAYLIST_ID);
            favoritesContainer.getContents().add(0, favoriteVideo);
        }
    }

    public void removeVideoFromFavoritesContentContainer(Content content) {
        ContentContainer favoritesContainer = contentLoader.getRootContentContainer()
                .findContentContainerById(ZypeSettings.FAVORITES_PLAYLIST_ID);
        if (favoritesContainer != null) {
            Content favoriteVideo = favoritesContainer.findContentById(content.getId());
            if (favoriteVideo == null) {
                return;
            }

            favoritesContainer.getContents().remove(favoriteVideo);
        }
    }

    // //////////
    // CRUD operations in local database for video favorites
    //
    public List<VideoFavoriteRecord> getVideoFavorites() {
        return VideoFavoritesHelper.getInstance().getVideoFavorites(context);
    }

    private VideoFavoriteRecord getVideoFavorite(Content content) {
        return (VideoFavoriteRecord) VideoFavoritesHelper.getInstance().getRecord(context, content.getId());
    }

    private void addLocalFavorite(Content content) {
        VideoFavoritesHelper videoFavoritesHelper = VideoFavoritesHelper.getInstance();
        if (!videoFavoritesHelper.recordExists(context, content.getId())) {
            String videoFavoriteId = content.getExtraValueAsString(Content.EXTRA_VIDEO_FAVORITE_ID);
            videoFavoritesHelper.addVideoFavorite(context, content.getId(), videoFavoriteId);
        }
        addVideoToFavoritesContentContainer(content);
        contentBrowser.updateContentActions();
    }

    public void deleteLocalFavorite(Content content) {
        VideoFavoritesHelper.getInstance().deleteRecord(context, content.getId());
        removeVideoFromFavoritesContentContainer(content);
        contentBrowser.updateContentActions();
    }
}
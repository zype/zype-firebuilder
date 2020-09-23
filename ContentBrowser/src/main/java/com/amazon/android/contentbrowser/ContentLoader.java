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
package com.amazon.android.contentbrowser;

import com.amazon.android.contentbrowser.database.helpers.RecentDatabaseHelper;
import com.amazon.android.contentbrowser.database.helpers.VideoFavoritesHelper;
import com.amazon.android.contentbrowser.database.records.RecentRecord;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.content.constants.ExtraKeys;
import com.amazon.android.model.translators.ContentContainerTranslator;
import com.amazon.android.model.translators.ContentTranslator;
import com.amazon.android.model.translators.ZypeContentContainerTranslator;
import com.amazon.android.model.translators.ZypeContentTranslator;
import com.amazon.android.navigator.Navigator;
import com.amazon.android.navigator.NavigatorModel;
import com.amazon.android.navigator.NavigatorModelParser;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.utils.Preferences;
import com.amazon.dataloader.datadownloader.ZypeDataDownloaderHelper;
import com.amazon.dataloader.dataloadmanager.DataLoadManager;
import com.amazon.dynamicparser.DynamicParser;
import com.amazon.utils.model.Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zype.fire.api.Model.PlaylistData;
import com.zype.fire.api.Model.VideoData;
import com.zype.fire.api.Model.VideoEntitlementData;
import com.zype.fire.api.Model.VideoEntitlementsResponse;
import com.zype.fire.api.Model.VideoFavoriteResponse;
import com.zype.fire.api.Model.VideoResponse;
import com.zype.fire.api.Model.VideosResponse;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;
import com.zype.fire.auth.ZypeAuthentication;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.amazon.android.contentbrowser.ContentBrowser.BROADCAST_DATA_LOADED;
import static com.amazon.android.contentbrowser.ContentBrowser.BROADCAST_VIDEO_DETAIL_DATA_LOADED;

/**
 * Class that initializes the content for the app. This includes running the recipes that download
 * the data and parses it into content model objects.
 */
public class ContentLoader {

    /**
     * Debug tag.
     */
    private final String TAG = ContentLoader.class.getSimpleName();

    /**
     * Debug recipe chain flag.
     */
    private static final boolean DEBUG_RECIPE_CHAIN = true;

    /**
     * Cause a feed error flag for debugging.
     */
    private static final boolean CAUSE_A_FEED_ERROR_FOR_DEBUGGING = false;

    /**
     * Singleton instance of ContentLoader.
     */
    private static ContentLoader sInstance;

    /**
     * The NavigatorModel that will contain the recipes.
     */
    private NavigatorModel mNavigatorModel;

    /**
     * The DataloadManager instance.
     */
    private DataLoadManager mDataLoadManager;

    /**
     * The DynamicParser instance.
     */
    private DynamicParser mDynamicParser;

    /**
     * Flag for if content reload is required.
     */
    private boolean mContentReloadRequired = false;

    /**
     * Flag for if the content is loaded.
     */
    private boolean mContentLoaded = false;

    /**
     * Root content container reference.
     */
    private ContentContainer mRootContentContainer = new ContentContainer("Root");

    /* Zype, Evgeny Cherkasov */
    private Context mContext;

    /**
     * Constructor. Initializes the {@link NavigatorModel}, {@link DataLoadManager}, and
     * {@link DynamicParser} that is required to load data.
     *
     * @param context The application context.
     */
    private ContentLoader(Context context) {

        mNavigatorModel = NavigatorModelParser.parse(context, Navigator.NAVIGATOR_FILE);

        try {
            mDataLoadManager = DataLoadManager.getInstance(context);
            mDynamicParser = new DynamicParser();

            // Register content translator in case parser recipes use translation.
            ContentTranslator contentTranslator = new ContentTranslator();
            mDynamicParser.addTranslatorImpl(contentTranslator.getName(), contentTranslator);

            // Register content container translator in case parser recipes use translation.
            ContentContainerTranslator containerTranslator = new ContentContainerTranslator();
            mDynamicParser.addTranslatorImpl(containerTranslator.getName(),
                                             containerTranslator);

            /* Zype, Evgeny Cherkasov */
            mContext = context;

            // Register Zype content translator parser recipes use translation.
            ZypeContentTranslator zypeContentTranslator = new ZypeContentTranslator();
            mDynamicParser.addTranslatorImpl(zypeContentTranslator.getName(), zypeContentTranslator);

            // Register content container translator in case parser recipes use translation.
            ZypeContentContainerTranslator zypeContainerTranslator = new ZypeContentContainerTranslator();
            mDynamicParser.addTranslatorImpl(zypeContainerTranslator.getName(), zypeContainerTranslator);

            // Set Zype playlist id to the root container
            mRootContentContainer.setExtraValue(Recipe.KEY_DATA_TYPE_TAG, ZypeConfiguration.getRootPlaylistId(context));

            mDataLoadManager.registerUpdateListener(new DataLoadManager.IDataUpdateListener() {
                @Override
                public void onSuccess(Data data) {

                    if (data != null) {
                        mContentReloadRequired = true;
                    }
                    else {
                        Log.i(TAG, "Data reload not required by data updater");
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {

                    Log.e(TAG, "registerUpdateListener onFailure!!!", throwable);
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, "DataLoadManager init failed.", e);
        }
    }

    /**
     * Get the singleton instance.
     *
     * @param context The context.
     * @return The content loader instance.
     */
    public static ContentLoader getInstance(Context context) {

        if (sInstance == null) {
            synchronized (ContentLoader.class) {
                if (sInstance == null) {
                    sInstance = new ContentLoader(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Get categories observable.
     *
     * @param root                             Content container.
     * @param dataLoaderRecipeForCategories    Data loader recipe for getting categories.
     * @param dynamicParserRecipeForCategories Dynamic parser recipe for getting categories.
     * @return RX Observable.
     */
    private Observable<Object> getCategoriesObservable(ContentContainer root,
                                                       Recipe dataLoaderRecipeForCategories,
                                                       Recipe dynamicParserRecipeForCategories) {

        /* Zype, Evgeny Cherkasov */
        // Set parent playlist id in receipt params to fetch only its child playlists
        String[] params = new String[] { root.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG) };

        return mDataLoadManager.cookRecipeObservable(
                dataLoaderRecipeForCategories,
                null,
                null,
                null).map(
                feedDataForCategories -> {
                    if (DEBUG_RECIPE_CHAIN) {
                        Log.d(TAG, "Feed download complete");
                    }

                    if (CAUSE_A_FEED_ERROR_FOR_DEBUGGING) {
                        return Observable.error(new Exception());
                    }
                    return feedDataForCategories;
                }).concatMap(
                feedDataForCategories -> mDynamicParser.cookRecipeObservable
                        (dynamicParserRecipeForCategories,
                         feedDataForCategories,
                         null,
                         /* Zype, Evgenty Cherkasov */
//                         null)).map(
                         params)).map(
                contentContainerAsObject -> {
                    ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;

                    ContentContainer alreadyAvailableContentContainer =
                            root.findContentContainerByName(contentContainer.getName());

                    if (alreadyAvailableContentContainer == null) {
                        root.addContentContainer(contentContainer);
                        alreadyAvailableContentContainer = contentContainer;
                    }
                        /* Zype, Evgeny Cherkasov */
                    if (alreadyAvailableContentContainer.getExtraValueAsInt(ContentContainer.EXTRA_PLAYLIST_ITEM_COUNT) > 0) {
                        alreadyAvailableContentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, 1);
                    }
                    else {
                        if (!alreadyAvailableContentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.FAVORITES_PLAYLIST_ID)) {
                            alreadyAvailableContentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, -1);
                        }
                    }

                    if (DEBUG_RECIPE_CHAIN) {
                        Log.d(TAG, "Dynamic parser got an container");
                    }
                    return alreadyAvailableContentContainer;
                })
                /* Zype, Evgeny Cherkasov */
                // Get all nested playlists for each playlist in the root
                .concatMap(contentContainer -> getSubCategoriesObservable(contentContainer, dataLoaderRecipeForCategories, dynamicParserRecipeForCategories));
    }

    /* Zype, Evgeny Cherkasov */
    private Observable<Object> getSubCategoriesObservable(ContentContainer parentContentContainer,
                                                          Recipe dataLoaderRecipeForCategories,
                                                          Recipe dynamicParserRecipeForCategories) {
        parentContentContainer.getContentContainers().clear();
        if (Integer.valueOf(parentContentContainer.getExtraStringValue("playlistItemCount")) > 0) {
            // If playlist contains videos just return itself and ignore nested playlists
            return Observable.just(parentContentContainer);
        }
        else {
            return Observable.concat(
                    Observable.just(parentContentContainer),
                    mDataLoadManager.cookRecipeObservable(dataLoaderRecipeForCategories, null, null, null)
                            .map(feedDataForCategories -> {
                                if (CAUSE_A_FEED_ERROR_FOR_DEBUGGING) {
                                    return Observable.error(new Exception());
                                }
                                return feedDataForCategories;
                            })
                            .concatMap(feedDataForCategories -> {
                                String[] params = new String[]{(String) parentContentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG)};
                                return mDynamicParser.cookRecipeObservable(dynamicParserRecipeForCategories, feedDataForCategories, null, params);
                            })
                            .filter(contentSubContainerAsObject -> contentSubContainerAsObject != null)
                            .map(contentSubContainerAsObject -> {
                                ContentContainer contentSubContainer = (ContentContainer) contentSubContainerAsObject;
                                if (DEBUG_RECIPE_CHAIN) {
                                    Log.d(TAG, "getSubCategoriesObservable(): " + contentSubContainer.getName());
                                }
                                if (contentSubContainer.getExtraValueAsInt(ContentContainer.EXTRA_PLAYLIST_ITEM_COUNT) > 0) {
                                    contentSubContainer.setExtraValue(ExtraKeys.NEXT_PAGE, 1);
                                }
                                else {
                                    contentSubContainer.setExtraValue(ExtraKeys.NEXT_PAGE, -1);
                                }
                                parentContentContainer.getContentContainers().add(contentSubContainer);
                                return parentContentContainer;
//                                if (Integer.valueOf(contentSubContainer.getExtraStringValue("playlistItemCount")) > 0) {
//                                    return parentContentContainer;
//                                }
//                                else {
//                                    return parentContentContainer;
//                                }
                            })
                            .distinct()
            );
        }
    }

    /**
     * Get contents observable.
     *
     * @param observable                     Rx Observable chain to continue on.
     * @param dataLoaderRecipeForContents    Data loader recipe for getting contents.
     * @param dynamicParserRecipeForContents Dynamic parser  recipe for getting contents.
     * @return RX Observable.
     */
    private Observable<Object> getContentsObservable(Observable<Object> observable,
                                                     Recipe dataLoaderRecipeForContents,
                                                     Recipe dynamicParserRecipeForContents) {

        Map<String, Content> parsedContent = new HashMap();
        return observable.concatMap(contentContainerAsObject -> {
            ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
            if (DEBUG_RECIPE_CHAIN) {
                Log.d(TAG, "ContentContainer:" + contentContainer.getName());
            }
            return mDataLoadManager.cookRecipeObservable(
                    dataLoaderRecipeForContents,
                    null,
                    null,
                    null).map(
                    feedDataForContent -> {
                        if (DEBUG_RECIPE_CHAIN) {
                            Log.d(TAG, "Feed for container complete");
                        }
                        return Pair.create(contentContainerAsObject, feedDataForContent);
                    });
        }).concatMap(objectPair -> {
            ContentContainer contentContainer = (ContentContainer) objectPair.first;
            /* Zype, Evgeny Cherkasov */
            // Clear content list to avoid duplicate contents for nested playlist (subcategory)
            contentContainer.getContents().clear();

            String feed = (String) objectPair.second;

            String[] params = new String[]{(String) contentContainer
                    .getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG)
            };

            return mDynamicParser.cookRecipeObservable(
                    dynamicParserRecipeForContents,
                    feed,
                    null,
                    params).map(contentAsObject -> {
                if (DEBUG_RECIPE_CHAIN) {
                    Log.d(TAG, "Parser got an content");
                }
                Content content = (Content) contentAsObject;
                if (content != null) {
                    //check if this content has already been parsed for some other container
//                    content = checkForParsedContent(parsedContent, content);
                    //Add information of free content available with container
                    if (contentContainer.getExtraStringValue(Recipe.CONTENT_TYPE_TAG) != null) {
                        content.setExtraValue(Recipe.CONTENT_TYPE_TAG, contentContainer
                                .getExtraStringValue(Recipe.CONTENT_TYPE_TAG));
                    }
                    /* Zype, Evgeny Cherkasov */
                    if (ZypeConfiguration.displayWatchedBarOnVideoThumbnails()) {
                        content.setExtraValue(Content.EXTRA_PLAYBACK_POSITION_PERCENTAGE,
                                getContentPlaybackPositionPercentage(content));
                    }
                    contentContainer.addContent(content);
                }
                return Pair.create(contentContainer, contentAsObject);
            });
        });
    }

    /**
     * Get content chain observable.
     *
     * @param hardCodedCategoryName            Hard coded category name.
     * @param dataLoaderRecipeForCategories    Data loader recipe for getting categories.
     * @param dataLoaderRecipeForContents      Data loader recipe for getting contents.
     * @param dynamicParserRecipeForCategories Dynamic parser recipe for getting categories.
     * @param dynamicParserRecipeForContents   Dynamic parser  recipe for getting contents.
     * @param root                             Content container.
     * @return RX Observable.
     */
    private Observable<Object> getContentChainObservable(String hardCodedCategoryName,
                                                         Recipe dataLoaderRecipeForCategories,
                                                         Recipe dataLoaderRecipeForContents,
                                                         Recipe dynamicParserRecipeForCategories,
                                                         Recipe dynamicParserRecipeForContents,
                                                         ContentContainer root) {

        Observable<Object> observable;

        if (hardCodedCategoryName == null) {
            observable = getCategoriesObservable(root, dataLoaderRecipeForCategories,
                                                 dynamicParserRecipeForCategories);
        }
        else {
            observable = Observable.just(hardCodedCategoryName)
                                   .map(s -> {
                                       ContentContainer contentContainer =
                                               new ContentContainer(hardCodedCategoryName);
                                       root.addContentContainer(contentContainer);
                                       return contentContainer;
                                   });
        }

        return getContentsObservable(observable, dataLoaderRecipeForContents,
                                     dynamicParserRecipeForContents);
    }

    /**
     * Run global recipes at index.
     *
     * @param index Index.
     * @param root  Content container.
     * @return RX Observable.
     */
    public Observable<Object> runGlobalRecipeAtIndex(int index, ContentContainer root) {


        NavigatorModel.GlobalRecipes recipe = mNavigatorModel.getGlobalRecipes().get(index);

        Recipe dataLoaderRecipeForCategories = recipe.getCategories().dataLoaderRecipe;
        Recipe dataLoaderRecipeForContents = recipe.getContents().dataLoaderRecipe;

        Recipe dynamicParserRecipeForCategories = recipe.getCategories().dynamicParserRecipe;
        Recipe dynamicParserRecipeForContents = recipe.getContents().dynamicParserRecipe;

        // Add any extra configurations that the parser recipe needs from the navigator recipe.
        if (recipe.getRecipeConfig() != null) {
            // Add if the recipe is for live feed data.
            dynamicParserRecipeForContents.getMap().put(Recipe.LIVE_FEED_TAG,
                                                        recipe.getRecipeConfig().liveContent);
        }

        String hardCodedCategoryName = recipe.getCategories().name;

        return getContentChainObservable(hardCodedCategoryName,
                                         dataLoaderRecipeForCategories,
                                         dataLoaderRecipeForContents,
                                         dynamicParserRecipeForCategories,
                                         dynamicParserRecipeForContents,
                                         root);
    }

    /**
     * Run recommendation recipe at index.
     *
     * @param index Index.
     * @param root  Content container.
     * @return RX Observable.
     */
    public Observable<Object> runRecommendationRecipeAtIndex(int index, ArrayList<String> root) {


        NavigatorModel.RecommendationRecipes recipe = mNavigatorModel.getRecommendationRecipes()
                                                                     .get(index);

        Recipe dataLoaderRecipeForContents = recipe.getContents().dataLoaderRecipe;
        Recipe dynamicParserRecipeForContents = recipe.getContents().dynamicParserRecipe;


        return getRecommendationsChainObservable(dataLoaderRecipeForContents,
                                                 dynamicParserRecipeForContents,
                                                 root);
    }

    /**
     * Get recommendations chain observable.
     *
     * @param dataLoaderRecipe    Data loader recipe for getting recommendation content ids.
     * @param dynamicParserRecipe Parser recipe for translating the content ids to strings.
     * @param root                Content container.
     * @return RX Observable.
     */
    public Observable<Object> getRecommendationsChainObservable(Recipe dataLoaderRecipe,
                                                                Recipe dynamicParserRecipe,
                                                                ArrayList<String> root) {

        Observable<Object> observable;

        observable = getRecommendationsObservable(root, dataLoaderRecipe, dynamicParserRecipe);

        return observable;
    }

    /**
     * @param root                Content container.
     * @param dataLoaderRecipe    Data loader recipe for getting recommendation content ids.
     * @param dynamicParserRecipe Parser recipe for translating the content ids to strings.
     * @return RX Observable.
     */
    public Observable<Object> getRecommendationsObservable(ArrayList<String> root,
                                                           Recipe dataLoaderRecipe,
                                                           Recipe dynamicParserRecipe) {

        return mDataLoadManager.cookRecipeObservable(dataLoaderRecipe, null, null, null)
                               .map(feedDataForRecommendations -> {
                                   if (DEBUG_RECIPE_CHAIN) {
                                       Log.d(TAG, "Recommendation Feed download complete");
                                   }
                                   return feedDataForRecommendations;
                               }).concatMap(feedDataForRecommendations -> mDynamicParser
                        .cookRecipeObservable(dynamicParserRecipe,
                                              feedDataForRecommendations,
                                              null, null)).map(contentIdAsObject -> {
                    String contentId = (String) contentIdAsObject;
                    if (!root.contains(contentId)) {
                        root.add(contentId);
                    }
                    if (DEBUG_RECIPE_CHAIN) {
                        Log.d(TAG, "Dynamic parser got a content id: " + contentId);
                    }
                    return contentId;
                });
    }

    /**
     * Check if this content has already been parsed for some other container.
     *
     * @param parsedContent Map of previously parsed content objects.
     * @param content       Content object which need to be checked.
     * @return previously created content object or current object.
     */
    private Content checkForParsedContent(Map<String, Content> parsedContent, Content content) {

        if (parsedContent.containsKey(content.getId())) {
            return parsedContent.get(content.getId());
        }
        //Add current content in parsedContent map
        parsedContent.put(content.getId(), content);
        return content;
    }

    /**
     * Is content reloading required?
     *
     * @return True if content should be reloaded; false otherwise.
     */
    public boolean isContentReloadRequired() {

        return mContentReloadRequired;
    }

    /**
     * Set if content reload is required.
     *
     * @param contentReloadRequired True if content should be reloaded; false otherwise.
     */
    public void setContentReloadRequired(boolean contentReloadRequired) {

        mContentReloadRequired = contentReloadRequired;
    }

    /**
     * Is the content loaded?
     *
     * @return True if the content is loaded; false otherwise.
     */
    public boolean isContentLoaded() {

        return mContentLoaded;
    }

    /**
     * Set if the content has been loaded.
     *
     * @param contentLoaded True if the content is loaded; false otherwise.
     */
    public void setContentLoaded(boolean contentLoaded) {

        mContentLoaded = contentLoaded;
    }

    /**
     * Get the Navigator Model.
     *
     * @return The Navigator Model.
     */
    public NavigatorModel getNavigatorModel() {

        return mNavigatorModel;
    }

    /**
     * Get the root content container.
     *
     * @return The root.
     */
    public ContentContainer getRootContentContainer() {

        return mRootContentContainer;
    }

    /**
     * Set the root content container.
     *
     * @param rootContentContainer The root.
     */
    public void setRootContentContainer(ContentContainer rootContentContainer) {

        mRootContentContainer = rootContentContainer;
    }

    /**
     * Get the number of global recommendations that should be sent, as specified in the
     * navigator configuration file.
     *
     * @return The number of global recommendations to send.
     */
    public int getNumberOfGlobalRecommendations() {

        return mNavigatorModel.getConfig().numberOfGlobalRecommendations;
    }

    /**
     * Get the number of related recommendations that should be sent, as specified in the
     * navigator configuration file.
     *
     * @return The number of related recommendations to send.
     */
    public int getNumberOfRelatedRecommendations() {

        return mNavigatorModel.getConfig().numberOfRelatedRecommendations;
    }

    /*
     * Zype, Evgeny Cherkasov
     */
    public Observable<Object> getLoadContentsObservable(Observable<Object> observable, Recipe recipeDynamicParser) {
        return observable
                // Clear contents of the content container for initial loading (extra parameter
                // NEXT_PAGE value is 1)
                .map(contentContainerAsObject -> {
                    ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
                    if (contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.MY_LIBRARY_PLAYLIST_ID)) {
                        ContentContainer rootMyLibrary = getRootContentContainer().findContentContainerByName(ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID);
                        if (rootMyLibrary.getExtraValueAsInt(ExtraKeys.NEXT_PAGE) == 1) {
                            contentContainer.getContents().clear();
                        }
                    }
                    else {
                        if (contentContainer.getExtraValueAsInt(ExtraKeys.NEXT_PAGE) == 1) {
                            contentContainer.getContents().clear();
                        }
                    }
                    return contentContainerAsObject;
                })
                // Load videos via Zype API and convert the result to JSON feed
                .concatMap(contentContainerAsObject -> {
                    ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
                    if (DEBUG_RECIPE_CHAIN) {
                        Log.d(TAG, "getLoadContentsObservable:" + contentContainer.getName());
                    }
                    if (contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.MY_LIBRARY_PLAYLIST_ID)) {
                        // TODO: Move videos loading code to ZypeDataDownloader and use its method here
                        // Loading My Library videos
                        return getMyLibraryVideosObservable(contentContainerAsObject);
                    }
                    else if (contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.FAVORITES_PLAYLIST_ID)) {
                        // Load favorites videos
                        return getFavoriteVideosFeedObservable(contentContainerAsObject);
                    }
                    else {
                        LocalBroadcastManager.getInstance(mContext)
                                .sendBroadcast(new Intent(BROADCAST_VIDEO_DETAIL_DATA_LOADED));
                        // Loading playlist videos
                        return getPlaylistVideosFeedObservable(contentContainerAsObject);
                    }
                })
                // Parse videos feed to Content objects
                .concatMap(objectPair -> {
                    ContentContainer contentContainer = (ContentContainer) objectPair.first;
                    String feed = (String) objectPair.second;
                    String[] params = new String[] { contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG) };

                    if (TextUtils.isEmpty(feed)) {
                        return Observable.just(Pair.create(contentContainer, null));
                    }
                    else {
                        return mDynamicParser
                                .cookRecipeObservable(recipeDynamicParser, feed, null, params)
                                .map(contentAsObject -> {
                                    if (DEBUG_RECIPE_CHAIN) {
                                        Log.d(TAG, "Parser got an content");
                                    }
                                    Content content = (Content) contentAsObject;
                                    if (content != null) {
                                        contentContainer.addContent(content);
                                        if (ZypeConfiguration.displayWatchedBarOnVideoThumbnails()) {
                                            content.setExtraValue(Content.EXTRA_PLAYBACK_POSITION_PERCENTAGE,
                                                    getContentPlaybackPositionPercentage(content));
                                        }
                                    }
                                    return Pair.create(contentContainer, contentAsObject);
                                });
                    }
                });
    }

    public Observable<Object> getLoadContentsByVideoIdsObservable(Observable<Object> observable,
                                                                  Recipe recipeDynamicParser,
                                                                  List<String> videoIds) {
        return observable
                // Clear contents of the content container
                .map(contentContainerAsObject -> {
                    ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
                    contentContainer.getContents().clear();
                    contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, -1);
                    return contentContainerAsObject;
                })
                // Load videos via Zype API and convert the result to JSON feed
                .concatMap(contentContainerAsObject -> {
                    ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
                    if (DEBUG_RECIPE_CHAIN) {
                        Log.d(TAG, "getLoadContentsByVideoIdsObservable(): " + contentContainer.getName());
                    }
                    // Loading videos
                    return getVideosFeedObservable(contentContainerAsObject, videoIds);
                })
                // Parse videos feed to Content objects
                .concatMap(objectPair -> {
                    ContentContainer contentContainer = (ContentContainer) objectPair.first;
                    String feed = (String) objectPair.second;
                    String[] params = new String[] { contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG) };

                    if (TextUtils.isEmpty(feed)) {
                        return Observable.just(Pair.create(contentContainer, null));
                    }
                    else {
                        return mDynamicParser
                                .cookRecipeObservable(recipeDynamicParser, feed, null, params)
                                .map(contentAsObject -> {
                                    if (DEBUG_RECIPE_CHAIN) {
                                        Log.d(TAG, "Parser got an content");
                                    }
                                    Content content = (Content) contentAsObject;
                                    if (content != null) {
                                        contentContainer.addContent(content);
                                    }
                                    return Pair.create(contentContainer, contentAsObject);
                                });
                    }
                });
    }

    public Observable<Pair> getVideosFeedObservable(Object contentContainerAsObject, List<String> videoIds) {
        ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;

        ZypeDataDownloaderHelper.VideosResult videosResult = ZypeDataDownloaderHelper
                .loadVideos(videoIds, contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG));
        if (videosResult != null) {
            contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, videosResult.nextPage);

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String feed = gson.toJson(videosResult.videos);
            Log.d(TAG, "getVideosFeedObservable(): size=" + videosResult.videos.size());
            return Observable.just(Pair.create(contentContainerAsObject, feed));
        }
        else {
            Log.e(TAG, "getVideosFeedObservable(): no videos found");
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }
    }

    public Observable<ContentContainer> loadPlayList(ContentContainer root, String playListId) {
        return Observable.just(playListId).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).flatMap(id -> {
            PlaylistData playlistData = ZypeDataDownloaderHelper.loadPlayList(playListId);

            if(playlistData != null) {
                //need to convert this to a content container
                playlistData.parentId = playListId;
                HashMap map = new HashMap();
                map.put("categories", Arrays.asList(playlistData));
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                String feed = gson.toJson(map);
                String[] params = new String[] { playListId };
                Recipe recipeDynamicParserContainer = Recipe.newInstance(mContext, "recipes/ZypeCategoriesRecipe.json");

                return  mDynamicParser.cookRecipeObservable
                    (recipeDynamicParserContainer,
                        feed,
                        null, params).flatMap(o -> {

                            ContentContainer contentContainer = (ContentContainer)o;
                            return Observable.just(contentContainer);
                });
            }

            return Observable.error(new Exception("unable to load the playlist"));
        });
    }


    public Observable<Pair> getPlaylistVideosFeedObservable(Object contentContainerAsObject) {
        ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;

        int nextPage = contentContainer.getExtraValueAsInt(ExtraKeys.NEXT_PAGE);
        if (nextPage <= 0) {
            Log.e(TAG, "getPlaylistVideosFeedObservable(): incorrect page: " + nextPage);
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }

        VideosResponse response = ZypeDataDownloaderHelper.loadPlaylistVideos(contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG), nextPage);
        if (response != null) {
            if (response.pagination.current == response.pagination.pages) {
                contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, -1);
            }
            else {
                contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, response.pagination.next);
            }

            Log.d(TAG, "getPlaylistVideosFeedObservable(): size=" + response.videoData.size());
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String feed = gson.toJson(response.videoData);
            return Observable.just(Pair.create(contentContainerAsObject, feed));
        }
        else {
            Log.e(TAG, "getPlaylistVideosFeedObservable(): no videos found");
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }
    }

    public Observable<Pair> getFavoriteVideosFeedObservable(Object contentContainerAsObject) {
        ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;

        int nextPage = contentContainer.getExtraValueAsInt(ExtraKeys.NEXT_PAGE);
        if (nextPage <= 0) {
            Log.e(TAG, "getFavoriteVideosFeedObservable(): incorrect page: " + nextPage);
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }

        String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
        String consumerId = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_ID);
        ZypeDataDownloaderHelper.VideosResult videosResult = ZypeDataDownloaderHelper.loadFavoriteVideos(
                contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG), consumerId, accessToken, nextPage);
        if (videosResult != null) {
            // Currently 'loadFavoriteVideos' loads all video favorites, so 'nextPage' value
            // of the 'videoResult' will be -1, that means there is no more data.
            contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, videosResult.nextPage);

            // Update local database
            VideoFavoritesHelper.getInstance().clearDatabase(mContext);
            for (VideoData data : videosResult.videos) {
                VideoFavoritesHelper.getInstance().addVideoFavorite(mContext, data.Id, data.videoFavoriteId);
            }

            // Prepare feed
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String feed = gson.toJson(videosResult.videos);
            return Observable.just(Pair.create(contentContainerAsObject, feed));
        }
        else {
            Log.e(TAG, "getFavoriteVideosFeedObservable(): no videos found");
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }
    }

    public Observable<Content> addVideoFavorite(Content content) {
        return Observable.create(subscriber -> {
            String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
            String consumerId = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_ID);
            HashMap<String, String> queryParams = new HashMap<>();
            queryParams.put(ZypeApi.ACCESS_TOKEN, accessToken);
            HashMap<String, String> fieldParams = new HashMap<>();
            fieldParams.put("video_id", content.getId());
            ZypeApi.getInstance().getApi().addVideoFavorite(consumerId, queryParams, fieldParams)
                    .enqueue(new Callback<VideoFavoriteResponse>() {
                        @Override
                        public void onResponse(Call<VideoFavoriteResponse> call, Response<VideoFavoriteResponse> response) {
                            Content resultContent = null;
                            if (response.isSuccessful()) {
                                resultContent = content;
                                resultContent.setExtraValue(Content.EXTRA_VIDEO_FAVORITE_ID, response.body().data.id);
                            }
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(resultContent);
                            }
                            subscriber.onCompleted();
                        }

                        @Override
                        public void onFailure(Call<VideoFavoriteResponse> call, Throwable t) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(t);
                            }
                            subscriber.onCompleted();
                        }
                    });
        });
    }

    public Observable<Content> removeVideoFavorite(Content content, String videoFavoriteId) {
        return Observable.create(subscriber -> {
            String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
            String consumerId = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_ID);
            HashMap<String, String> queryParams = new HashMap<>();
            queryParams.put(ZypeApi.ACCESS_TOKEN, accessToken);
            ZypeApi.getInstance().getApi().removeVideoFavorite(consumerId, videoFavoriteId, queryParams)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            Content resultContent = null;
                            if (response.isSuccessful()) {
                                resultContent = content;
                                resultContent.setExtraValue(Content.EXTRA_VIDEO_FAVORITE_ID, null);
                            }
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(resultContent);
                            }
                            subscriber.onCompleted();
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(t);
                            }
                            subscriber.onCompleted();
                        }
                    });
        });
    }

    public Observable<Pair> getMyLibraryVideosObservable(Object contentContainerAsObject) {
        ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;

        ContentContainer rootMyLibrary = getRootContentContainer().findContentContainerByName(ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID);
        int nextPage = rootMyLibrary.getExtraValueAsInt(ExtraKeys.NEXT_PAGE);
        if (nextPage <= 0) {
            Log.e(TAG, "getMyLibraryVideosObservable(): incorrect page: " + nextPage);
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }

        String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
        VideoEntitlementsResponse response = ZypeApi.getInstance().getVideoEntitlements(accessToken, nextPage, ZypeApi.PER_PAGE_DEFAULT);
        if (response != null) {
            if (response.pagination.current == response.pagination.pages) {
                rootMyLibrary.setExtraValue(ExtraKeys.NEXT_PAGE, -1);
            }
            else {
                rootMyLibrary.setExtraValue(ExtraKeys.NEXT_PAGE, response.pagination.next);
            }
            Log.d(TAG, "getMyLibraryVideosObservable(): size=" + response.videoEntitlements.size());
            List<VideoData> videos = new ArrayList<>();
            for (VideoEntitlementData data : response.videoEntitlements) {
                VideoResponse responseVideo = ZypeApi.getInstance().getVideo(data.videoId);
                if (responseVideo != null) {
                    VideoData videoData = responseVideo.videoData;
                    if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
                        videoData.description = " ";
                    }
                    videoData.playlistId = contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG);
                    videoData.playerUrl = "null";
                    videos.add(videoData);
                }
                else {
                    Log.e(TAG, "getMyLibraryVideosObservable(): error loading video, id=" + data.videoId);
                }
            }

            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String feed = gson.toJson(videos);
            return Observable.just(Pair.create(contentContainerAsObject, feed));
        }
        else {
            Log.e(TAG, "getMyLibraryVideosObservable(): no videos found");
            return Observable.just(Pair.create(contentContainerAsObject, ""));
        }
    }

    public Observable<Object> runZypeGlobalRecipeAtIndex(NavigatorModel.GlobalRecipes recipe, Recipe recipeDynamicParserVideos,
                                                         int index, ContentContainer root) {
        Recipe dataLoaderRecipeForCategories = recipe.getCategories().dataLoaderRecipe;
        Recipe dataLoaderRecipeForContents = recipe.getContents().dataLoaderRecipe;

        Recipe dynamicParserRecipeForCategories = recipe.getCategories().dynamicParserRecipe;
        Recipe dynamicParserRecipeForContents = recipe.getContents().dynamicParserRecipe;

        // Add any extra configurations that the parser recipe needs from the navigator recipe.
        if (recipe.getRecipeConfig() != null) {
            // Add if the recipe is for live feed data.
            dynamicParserRecipeForContents.getMap().put(Recipe.LIVE_FEED_TAG,
                    recipe.getRecipeConfig().liveContent);
        }

        String hardCodedCategoryName = recipe.getCategories().name;

        return getLoadContentChainObservable(hardCodedCategoryName,
                dataLoaderRecipeForCategories,
                dynamicParserRecipeForCategories,
                recipeDynamicParserVideos,
                root);
    }

    private Observable<Object> getLoadContentChainObservable(String hardCodedCategoryName,
                                                             Recipe dataLoaderRecipeForCategories,
                                                             Recipe dynamicParserRecipeForCategories,
                                                             Recipe dynamicParserRecipeForContents,
                                                             ContentContainer root) {

        Observable<Object> observable;

        if (hardCodedCategoryName == null) {
            observable = getCategoriesObservable(root, dataLoaderRecipeForCategories,
                    dynamicParserRecipeForCategories);
        }
        else {
            observable = Observable.just(hardCodedCategoryName)
                    .map(s -> {
                        ContentContainer contentContainer =
                                new ContentContainer(hardCodedCategoryName);
                        root.addContentContainer(contentContainer);
                        return contentContainer;
                    });
        }

        return getLoadContentsObservable(observable, dynamicParserRecipeForContents);
    }

    public interface ILoadContentForContentContainer {
        void onContentsLoaded();
    }

    // TODO: Move 'loadPlaylistVideos()' from 'ContentBrowser' to here,
    // - update definition of 'loadPlaylistVideos' to use listener
    // - update all calls of 'loadPlaylistVideos' with listeners instead of listening to broadcast receivers
    // for updating content with result of this function
    // - replace all calls of 'loadContentForContentContainer' function with 'loadPlaylistVideos'
    public void loadContentForContentContainer(ContentContainer contentContainer, Context context, ILoadContentForContentContainer callback) {
//        NavigatorModel.GlobalRecipes recipe = mNavigator.getNavigatorModel().getGlobalRecipes().get(0);
//        Recipe dataLoaderRecipeForContents = recipe.getContents().dataLoaderRecipe;
//        Recipe dynamicParserRecipeForContents = recipe.getContents().dynamicParserRecipe;
//
        HashMap<String, String> params = new HashMap<>();
        params.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
        params.put(ZypeApi.PER_PAGE, String.valueOf(ZypeApi.PER_PAGE_DEFAULT));
        ZypeApi.getInstance().getApi().getPlaylistVideos(contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG), 1, params).enqueue(new Callback<VideosResponse>() {
            @Override
            public void onResponse(Call<VideosResponse> call, Response<VideosResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().pagination.current == response.body().pagination.pages) {
                        contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, -1);
                    }
                    else {
                        contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, response.body().pagination.next);
                    }

                    if (!response.body().videoData.isEmpty()) {
                        Log.d(TAG, "loadContentForContentContainer(): onResponse(): size=" + response.body().videoData.size());
                        for (VideoData videoData : response.body().videoData) {
                            if (TextUtils.isEmpty(videoData.description) || videoData.description.equals("null")) {
                                videoData.description = " ";
                            }
                            videoData.playlistId = (String) contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG);
                            videoData.playerUrl = "null";
                        }
                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.create();
                        String feed = gson.toJson(response.body().videoData);
                        // TODO: Rename the recipe file
                        Recipe recipe = Recipe.newInstance(context, "recipes/ZypeSearchContentsRecipe.json");
                        Subscription subscription = getContentsForContentContainerObservable(feed, recipe, contentContainer)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(result -> {
                                        },
                                        throwable -> {
                                            if(contentContainer.getContentCount() > 0) {
                                                callback.onContentsLoaded();
                                            }
                                        },
                                        () -> {
                                            callback.onContentsLoaded();
                                        });

//                        mCompositeSubscription.add(subscription);
                    }
                }
                else {
                    // TODO: Handle error
                }
            }

            @Override
            public void onFailure(Call<VideosResponse> call, Throwable t) {
                // TODO: Handle exception
            }
        });
    }

    public void loadContentForMyLibraryContentContainer(ContentContainer contentContainer, Context context, ILoadContentForContentContainer callback) {
        HashMap<String, String> params = new HashMap<>();
        String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
        params.put(ZypeApi.ACCESS_TOKEN, accessToken);
        params.put(ZypeApi.PER_PAGE, String.valueOf(ZypeApi.PER_PAGE_DEFAULT));
        ZypeApi.getInstance().getApi().getVideoEntitlements(1, params).enqueue(new Callback<VideoEntitlementsResponse>() {
            @Override
            public void onResponse(Call<VideoEntitlementsResponse> call, Response<VideoEntitlementsResponse> response) {
                if (response.isSuccessful()) {
                    if (!response.body().videoEntitlements.isEmpty()) {
                        Log.d(TAG, "loadContentForMyLibraryContentContainer(): size=" + response.body().videoEntitlements.size());
//                        for (VideoEntitlementDataData data : response.body().videoEntitlements) {
//                            if (TextUtils.isEmpty(data.description) || videoData.description.equals("null")) {
//                                videoData.description = " ";
//                            }
//                            videoData.playlistId = (String) contentContainer.getExtraStringValue("keyDataType");
//                            videoData.playerUrl = "null";
//                        }
//                        GsonBuilder builder = new GsonBuilder();
//                        Gson gson = builder.create();
//                        String feed = gson.toJson(response.body().videoData);
//                        // TODO: Rename the recipe file
//                        Recipe recipe = Recipe.newInstance(context, "recipes/ZypeSearchContentsRecipe.json");
//                        Subscription subscription = getContentsForContentContainerObservable(feed, recipe, contentContainer)
//                                .subscribeOn(Schedulers.newThread())
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(result -> {
//                                        },
//                                        throwable -> {
//                                        },
//                                        () -> {
//                                            callback.onContentsLoaded();
//                                        });

//                        mCompositeSubscription.add(subscription);
                    }
                    else {

                    }
                }
                else {
                    Log.e(TAG, "loadContentForMyLibraryContentContainer(): error: " + response.errorBody());
                    // TODO: Handle error
                }
            }

            @Override
            public void onFailure(Call<VideoEntitlementsResponse> call, Throwable t) {
                Log.e(TAG, "loadContentForMyLibraryContentContainer(): failed. " + t.toString());
                // TODO: Handle exception
            }
        });
    }

    private Observable<Object> getContentsForContentContainerObservable(String feed, Recipe recipeDynamicParserVideos,
                                                                        ContentContainer contentContainer) {
        String[] params = new String[] { contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG) };
        return Observable
                .just(contentContainer)
                .concatMap(o -> mDynamicParser.cookRecipeObservable(recipeDynamicParserVideos, feed, null, params)
                        .map(contentAsObject -> {
                            Content content = (Content) contentAsObject;
                            if (content != null) {
                                if (DEBUG_RECIPE_CHAIN) {
                                    Log.d(TAG, "getContentsForContentContainerObservable(): " + content.getTitle());
                                }
                                contentContainer.addContent(content);
                            }
                            return content;
                        }));
    }


    /**
     * Get content playback position percentage for progress bar.
     *
     * @param content Content.
     * @return Percentage playback complete.
     */
    public double getContentPlaybackPositionPercentage(Content content) {

        RecentRecord record = getRecentRecord(content);
        // Calculate the playback position percentage as the current playback position
        // over the entire video duration
        if (record != null && !record.isPlaybackComplete()) {

            // Calculate time remaining as duration minus playback location
            long duration = record.getDuration();
            long currentPlaybackPosition = record.getPlaybackLocation();

            if ((duration > 0) && (currentPlaybackPosition > 0)
                    && (duration > currentPlaybackPosition)) {
                return (((double) currentPlaybackPosition) / duration);
            }
        }

        return 0;
    }

    /**
     * Get Recent Record from database based on content id
     *
     * @param content Content.
     * @return Recent Record.
     */
    public RecentRecord getRecentRecord(Content content) {

        RecentRecord record = null;
        RecentDatabaseHelper databaseHelper = RecentDatabaseHelper.getInstance();
        if (databaseHelper != null) {
            if (databaseHelper.recordExists(mContext, content.getId())) {
                record = databaseHelper.getRecord(mContext, content.getId());
            }
        }
        else {
            Log.e(TAG, "Unable to load content because database is null");
        }

        return record;
    }

}

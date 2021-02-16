/**
 * This file was modified by Amazon:
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
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.amazon.android.tv.tenfoot.ui.fragments;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.ContentLoader;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.model.Action;
import com.amazon.android.model.PlaylistAction;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.content.constants.ExtraKeys;
import com.amazon.android.model.event.ContentUpdateEvent;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.presenter.CustomListRowPresenter;
import com.amazon.android.tv.tenfoot.presenter.StubItemPresenter;
import com.amazon.android.tv.tenfoot.ui.sliders.HeroSlider;
import com.amazon.android.utils.Preferences;
import com.zype.fire.api.ZypeSettings;
import com.amazon.android.tv.tenfoot.ui.activities.ContentBrowseActivity;
import com.amazon.android.tv.tenfoot.utils.BrowseHelper;
import com.amazon.android.ui.constants.PreferencesConstants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import static androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_NONE;
import static com.amazon.android.contentbrowser.ContentBrowser.BROADCAST_DATA_LOADED;
import static com.zype.fire.api.ZypeSettings.SHOW_TITLE;

/**
 * This fragment displays content in horizontal rows for browsing. Each row has its title displayed
 * above it.
 */
public class ContentBrowseFragment extends RowsFragment {

    private static final String TAG = ContentBrowseFragment.class.getSimpleName();
    private static final int WAIT_BEFORE_FOCUS_REQUEST_MS = 500;
    private OnBrowseRowListener mCallback;
    private ArrayObjectAdapter mSettingsAdapter = null;
    private ListRow mRecentListRow = null;
    private ListRow mWatchlistListRow = null;
    private int mLoginButtonIndex;
    /* Zype, Evgeny Cherkasov */
    ArrayObjectAdapter mRowsAdapter = null;
    private BroadcastReceiver receiver;

    private boolean contentUpdationInProgress = false;
    private boolean userAuthenticated;

    // Container Activity must implement this interface.
    public interface OnBrowseRowListener {
        void onItemSelected(Object item, Row row, boolean isLastContentRow);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);
        // This makes sure that the container activity has implemented the callback interface.
        // If not, it throws an exception.
        try {
            mCallback = (OnBrowseRowListener) getActivity();
        }
        catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() +
                                                 " must implement OnBrowseRowListener: " + e);
        }

        CustomListRowPresenter customListRowPresenter = new CustomListRowPresenter();
        customListRowPresenter.setHeaderPresenter(new RowHeaderPresenter());

        // Uncomment this code to remove shadow from the cards
        customListRowPresenter.setShadowEnabled(!SHOW_TITLE);

        /* Zype, Evgney Cherkasov */
//        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(customListRowPresenter);
        mRowsAdapter = new ArrayObjectAdapter(customListRowPresenter);

        BrowseHelper.loadRootContentContainer(getActivity(), mRowsAdapter);
        if (ZypeSettings.SETTINGS_PLAYLIST_ENABLED){
            mSettingsAdapter = BrowseHelper.addSettingsActionsToRowAdapter(getActivity(), mRowsAdapter);
            mLoginButtonIndex = BrowseHelper.getLoginButtonIndex(mSettingsAdapter);
        }
        else {
            addStubRow(mRowsAdapter);
        }

        setAdapter(mRowsAdapter);

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());

        // Wait for WAIT_BEFORE_FOCUS_REQUEST_MS for the data to load before requesting focus.

        if(!HeroSlider.getInstance().isSliderPresent()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (getView() != null) {
                    VerticalGridView verticalGridView = findGridViewFromRoot(getView());
                    if (verticalGridView != null) {
                        verticalGridView.requestFocus();
                    }
                }
            }, WAIT_BEFORE_FOCUS_REQUEST_MS);
        }


        /* Zype, Evgeny Cherkasov */
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                contentUpdationInProgress = false;
                updateContents(mRowsAdapter);
            }
        };

    }

    /* Zype, Evgeny Cherkasov */
    @Override
    public void onPause() {
        super.onPause();
        if (receiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {

        super.onResume();

        userAuthenticated = ContentBrowser.getInstance(getActivity()).isUserLoggedIn();

        /* Zype, Evgeny Cherkasov */
        if (receiver != null) {
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(receiver, new IntentFilter(BROADCAST_DATA_LOADED));
        }
        /*if (mRowsAdapter != null) {
            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
        }*/

        ArrayObjectAdapter rowsAdapter = (ArrayObjectAdapter) getAdapter();

        if (ContentBrowser.getInstance(getActivity()).isRecentRowEnabled()) {
            mRecentListRow = BrowseHelper.updateContinueWatchingRow(getActivity(),
                                                                    mRecentListRow, rowsAdapter);
        }
        if (ContentBrowser.getInstance(getActivity()).isWatchlistRowEnabled()) {
            mWatchlistListRow = BrowseHelper.updateWatchlistRow(getActivity(), mWatchlistListRow,
                                                                mRecentListRow, rowsAdapter);
        }
    }

    /**
     * Event bus listener method to listen for authentication updates from AuthHelper and update
     * the login action status in settings.
     *
     * @param authenticationStatusUpdateEvent Broadcast event for update in authentication status.
     */
    @Subscribe
    public void onAuthenticationStatusUpdateEvent(AuthHelper.AuthenticationStatusUpdateEvent
                                                          authenticationStatusUpdateEvent) {

        if(userAuthenticated == authenticationStatusUpdateEvent.isUserAuthenticated()) {
            return;
        }

        userAuthenticated = authenticationStatusUpdateEvent.isUserAuthenticated();


        if (mSettingsAdapter != null) {
            if (mLoginButtonIndex != -1) {
                mSettingsAdapter.notifyArrayItemRangeChanged(mLoginButtonIndex, 1);

                // Update the details preview if the action occurred from the home screen.
                if (Preferences.getString(PreferencesConstants.LAST_ACTIVITY)
                        .equals(ContentBrowser.CONTENT_HOME_SCREEN)) {
                    if (authenticationStatusUpdateEvent.isUserAuthenticated()) {
                        ((ContentBrowseActivity) getActivity()).callImageLoadSubscription(
                                getString(R.string.logout_label),
                                getString(R.string.logout_description),
                                null);
                    }
                    else {
                        ((ContentBrowseActivity) getActivity()).callImageLoadSubscription(
                                getString(R.string.login_label),
                                getString(R.string.login_description),
                                null);
                    }
                }
            }
        }
        /* Zype, Evgney Cherkasov */
        if (mRowsAdapter != null) {
            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
        }
    }

    @Subscribe
    public void onContentUpdateEvent(ContentUpdateEvent event) {
        for (int rowIndex = 0; rowIndex < mRowsAdapter.size(); rowIndex++) {
            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) ((ListRow) mRowsAdapter.get(rowIndex)).getAdapter();
            for (int i = 0; i < rowAdapter.size(); i++) {
                Content content = (Content) rowAdapter.get(i);
                if (content.getId().equals(event.videoId)) {
                    content.setExtraValue(Content.EXTRA_PLAYBACK_POSITION_PERCENTAGE,
                            ContentBrowser.getInstance(getActivity()).getContentLoader().getContentPlaybackPositionPercentage(content));
                }
                rowAdapter.notifyArrayItemRangeChanged(i, i + 1);
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            /* Zype, Evgeny Cherkasov */
            // If selected playlist contains videos then open video details screen of the first video
            // in the playlist
            if (item instanceof ContentContainer) {
                ContentContainer contentContainer = (ContentContainer) item;
                if (!contentContainer.getContents().isEmpty()) {
                    item = contentContainer.getContents().get(0);
                }
                else {
                    if (Integer.valueOf(contentContainer.getExtraStringValue(ContentContainer.EXTRA_PLAYLIST_ITEM_COUNT)) > 0) {
                        // Playlist has  videos, but they is not loaded yet.
                        // Load videos and then open video detail screen of the first video in the playlist
                        ContentLoader.ILoadContentForContentContainer listener = new ContentLoader.ILoadContentForContentContainer() {
                            @Override
                            public void onContentsLoaded() {
                                ContentBrowser.getInstance(getActivity())
                                        .setLastSelectedContent(contentContainer.getContents().get(0))
                                        .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN);
                            }
                        };
                        // TODO: Add mCompositeSubscription parameter from ContentBrowser
                        ContentLoader.getInstance(getActivity()).loadContentForContentContainer(contentContainer, getActivity(), listener);
                        return;
                    }
                }
            }

            if (item instanceof Content) {
                Content content = (Content) item;
                Log.d(TAG, "Content with title " + content.getTitle() + " was clicked");

                /* Zype, Evgeny Cherkasov */
//                // Get video entitlement for purchase required videos
//                if (ZypeConfiguration.isUniversalTVODEnabled(getActivity())) {
//                    if (content.getExtraValueAsBoolean(Content.EXTRA_PURCHASE_REQUIRED)
//                            && !content.getExtras().containsKey(Content.EXTRA_ENTITLED)) {
//                        String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
//                        HashMap<String, String> params = new HashMap<>();
//                        params.put(ZypeApi.ACCESS_TOKEN, accessToken);
//                        ZypeApi.getInstance().getApi().checkVideoEntitlement(content.getId(), params).enqueue(new Callback<ResponseBody>() {
//                            @Override
//                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                                Log.e(TAG, "onItemClicked(): check video entitlement: code=" + response.code());
//                                if (response.isSuccessful()) {
//                                    content.setExtraValue(Content.EXTRA_ENTITLED, true);
//                                }
//                                else {
//                                    content.setExtraValue(Content.EXTRA_ENTITLED, false);
//                                }
//                                ContentBrowser.getInstance(getActivity())
//                                        .setLastSelectedContent(content)
//                                        .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
//                            }
//
//                            @Override
//                            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                                Log.e(TAG, "onItemClicked(): check video entitlement: failed");
//                                content.setExtraValue(Content.EXTRA_ENTITLED, false);
//                                ContentBrowser.getInstance(getActivity())
//                                        .setLastSelectedContent(content)
//                                        .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
//                            }
//                        });
//                    }
//                    else {
//                        ContentBrowser.getInstance(getActivity())
//                                .setLastSelectedContent(content)
//                                .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
//                    }
//                }
//                else {
//                    ContentBrowser.getInstance(getActivity())
//                            .setLastSelectedContent(content)
//                            .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
//                }
                ContentBrowser.getInstance(getActivity())
                        .setLastSelectedContent(content)
                        .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);
            }
            else if (item instanceof ContentContainer) {
                ContentContainer contentContainer = (ContentContainer) item;
                Log.d(TAG, "ContentContainer with name " + contentContainer.getName() + " was " +
                        "clicked");

                ContentBrowser.getInstance(getActivity())
                              .setLastSelectedContentContainer(contentContainer)
                              .switchToScreen(ContentBrowser.CONTENT_SUBMENU_SCREEN);
            }
            /* Zype, Evgeny Cherkasov */
            else if (item instanceof PlaylistAction) {
                PlaylistAction action = (PlaylistAction) item;
                if (action.getAction().equals(ContentBrowser.NEXT_PAGE)) {
                    Log.d(TAG, "Next page button was clicked");
                    ContentBrowser.getInstance(getActivity()).loadPlaylistVideos(action.getExtraValueAsString(PlaylistAction.EXTRA_PLAYLIST_ID));
                }
            }
            else if (item instanceof Action) {
                Action settingsAction = (Action) item;
                Log.d(TAG, "Settings with title " + settingsAction.getAction() + " was clicked");
                ContentBrowser.getInstance(getActivity())
                              .settingsActionTriggered(getActivity(),
                                                       settingsAction);
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            boolean isLastContentRow = false;
            if (!ZypeSettings.SETTINGS_PLAYLIST_ENABLED) {
                if (mRowsAdapter.indexOf(row) == mRowsAdapter.size() - 2) {
                    isLastContentRow = true;
                }
                else {
                    isLastContentRow = false;
                }
            }
            mCallback.onItemSelected(item, row, isLastContentRow);

            if (item instanceof Content) {
                Content content = (Content) item;
                if (content.getExtras().containsKey(ContentBrowser.NEXT_PAGE) /*&& content.getExtraValueAsBoolean(ContentBrowser.NEXT_PAGE)*/ && !contentUpdationInProgress) {
                    contentUpdationInProgress = true;
                    Log.d(TAG, "Next page item was selected");
                    String playlistId = content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID);
                    ContentBrowser.getInstance(getActivity()).loadPlaylistVideos(playlistId);
                }
            }
        }
    }

    /* Zype, Evgeny Cherkasov */
    private void updateContents(ArrayObjectAdapter rowsAdapter) {

        ContentContainer rootContentContainer = ContentBrowser.getInstance(getActivity())
                .getRootContentContainer();

        int index = 0;
        for (ContentContainer contentContainer : rootContentContainer.getContentContainers()) {
            // Skip 'My Library' and 'Favorites' content containers
            if (contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID)
                    || contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.ROOT_FAVORITES_PLAYLIST_ID)) {
                continue;
            }

            ListRow row = (ListRow) rowsAdapter.get(index);
            ArrayObjectAdapter listRowAdapter = (ArrayObjectAdapter) row.getAdapter();

            // Remove 'Load more' action button    (No need now)
            if (listRowAdapter.size() > 0 && listRowAdapter.get(listRowAdapter.size() - 1) instanceof PlaylistAction) {
                listRowAdapter.remove(listRowAdapter.get(listRowAdapter.size() - 1));
            }

            if (listRowAdapter.size() > 0 && listRowAdapter.get(listRowAdapter.size() - 1) instanceof Content && listRowAdapter.size() < contentContainer.getContentCount()) {
                Content content = (Content) listRowAdapter.get(listRowAdapter.size() - 1);
                content.setExtraValue(ContentBrowser.NEXT_PAGE, false);
            }
            // Add new contents
            for (int i = listRowAdapter.size() - contentContainer.getContentContainerCount(); i < contentContainer.getContentCount(); i++) {
                Content content = contentContainer.getContents().get(i);

                if (contentContainer.getExtraValueAsInt(ExtraKeys.NEXT_PAGE) > 0 &&
                        contentContainer.getContents().indexOf(content) == contentContainer.getContentCount()-1){
                    content.setExtraValue(ContentBrowser.NEXT_PAGE, true);
                    content.setExtraValue(Content.EXTRA_PLAYLIST_ID, contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG));
                }
                listRowAdapter.add(content);
            }

            index++;
        }
    }


    private void addStubRow(ArrayObjectAdapter rowsAdapter) {
        StubItemPresenter presenter = new StubItemPresenter();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        adapter.add("Item 1");
        rowsAdapter.add(new ListRow(null, adapter));
    }
}

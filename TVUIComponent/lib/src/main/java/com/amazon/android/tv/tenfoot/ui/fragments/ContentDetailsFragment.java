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
import com.amazon.android.model.Action;
import com.amazon.android.model.PlaylistAction;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.content.constants.ExtraKeys;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.tv.tenfoot.presenter.CustomDetailsOverviewRowPresenter;
import com.amazon.android.utils.GlideHelper;
import com.amazon.android.utils.Helpers;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.presenter.CardPresenter;
import com.amazon.android.tv.tenfoot.presenter.DetailsDescriptionPresenter;
import com.amazon.android.tv.tenfoot.ui.activities.ContentDetailsActivity;
import com.amazon.android.utils.LeanbackHelpers;
import com.amazon.android.utils.Preferences;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;
import com.zype.fire.auth.ZypeAuthentication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.DetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.TenFootActionPresenterSelector;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.amazon.android.contentbrowser.ContentBrowser.BROADCAST_DATA_LOADED;
import static com.zype.fire.api.ZypeSettings.DETAIL_BACKGROUND_IMAGE;


/**
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback
 * content_details_activity_layout screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class ContentDetailsFragment extends androidx.leanback.app.DetailsFragment {

    private static final String TAG = ContentDetailsFragment.class.getSimpleName();

    private static final int DETAIL_THUMB_WIDTH = 264;
    private static final int DETAIL_THUMB_HEIGHT = 198;

    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = 60 * 60;

    private Content mSelectedContent;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private boolean mShowRelatedContent;

    SparseArrayObjectAdapter mActionAdapter = new SparseArrayObjectAdapter();

    // Decides whether the action button should be enabled or not.
    private boolean mActionInProgress = false;

    private ContentBrowser.IContentActionListener mActionCompletedListener =
            new ContentBrowser.IContentActionListener() {
                @Override
                public void onContentAction(Activity activity, Content content, int actionId) {

                }

                @Override
                public void onContentActionCompleted(Activity activity, Content content,
                                                     int actionId) {

                    mActionInProgress = false;
                }

            };

    /* Zype, Evgeny Cherkasov */
    private BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

//        prepareBackgroundManager();
//
//        mSelectedContent = ContentBrowser.getInstance(getActivity()).getLastSelectedContent();
//        mShowRelatedContent = ContentBrowser.getInstance(getActivity()).isShowRelatedContent();

        /* Zype, Evgeny Cherkasov */
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateRelatedContentRow();
            }
        };

    }

    @Override
    public void onStart() {

        Log.v(TAG, "onStart called.");
        super.onStart();

        prepareBackgroundManager();

        mSelectedContent = ContentBrowser.getInstance(getActivity()).getLastSelectedContent();
        mShowRelatedContent = ContentBrowser.getInstance(getActivity()).isShowRelatedContent();

        if (mSelectedContent != null || checkGlobalSearchIntent()) {

            setupAdapter();
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            if (mShowRelatedContent) {
                setupRelatedContentRow();
            }
            setupContentListRowPresenter();

            if (DETAIL_BACKGROUND_IMAGE){
                updateBackground(mSelectedContent.getBackgroundImageUrl());
            }
            setOnItemViewClickedListener(new ItemViewClickedListener());
            setOnItemViewSelectedListener(new ItemViewSelectedListener());
        }
        else {
            Log.v(TAG, "Start CONTENT_HOME_SCREEN.");
            ContentBrowser.getInstance(getActivity())
                          .switchToScreen(ContentBrowser.CONTENT_HOME_SCREEN);
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item != null) {
                if (item instanceof Content) {
                    Content content = (Content) item;
                    if (!content.getId().equals(ContentBrowser.getInstance(getActivity()).getLastSelectedContent().getId())) {
                        ContentBrowser.getInstance(getActivity()).setLastSelectedContent(content);
                        mSelectedContent = ContentBrowser.getInstance(getActivity()).getLastSelectedContent();
                        updateDetailsOverviewRow();
                    }
                }
            }
        }
    }

    /* Zype, Evgeny Cherkasov */
    @Override
    public void onPause() {
        super.onPause();
        if (receiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }
    }

    /**
     * Overriding this method to return null since we do not want the title view to be available
     * in ContentDetails page.
     * {@inheritDoc}
     */
    protected View inflateTitle(LayoutInflater inflater, ViewGroup parent,
                                Bundle savedInstanceState) {

        return null;
    }

    /**
     * Check if there is a global search intent.
     */
    private boolean checkGlobalSearchIntent() {

        Log.v(TAG, "checkGlobalSearchIntent called.");
        Intent intent = getActivity().getIntent();
        String intentAction = intent.getAction();
        String globalSearch = getString(R.string.global_search);
        if (globalSearch.equalsIgnoreCase(intentAction)) {
            Uri intentData = intent.getData();
            Log.d(TAG, "action: " + intentAction + " intentData:" + intentData);
            int selectedIndex = Integer.parseInt(intentData.getLastPathSegment());

            ContentContainer contentContainer = ContentBrowser.getInstance(getActivity())
                                                              .getRootContentContainer();

            int contentTally = 0;
            if (contentContainer == null) {
                return false;
            }

            for (Content content : contentContainer) {
                ++contentTally;
                if (selectedIndex == contentTally) {
                    mSelectedContent = content;
                    return true;
                }
            }
        }
        return false;
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        // BackgroundManager has default dim drawable set to 'R.color.lb_background_protection'. This
        // makes the background image dark. We should set the dim drawable to be transparent for
        // light theme.
        mBackgroundManager.setDimLayer(ContextCompat.getDrawable(getActivity(), R.color.transparent));
        mDefaultBackground = ContextCompat.getDrawable(getActivity(), android.R.color.transparent);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {

        Log.v(TAG, "updateBackground called");
        if (Helpers.DEBUG) {
            Log.v(TAG, "updateBackground called: " + uri);
        }

        SimpleTarget<Bitmap> bitmapTarget = new SimpleTarget<Bitmap>(mMetrics.widthPixels,
                                                                     mMetrics.heightPixels) {
            @Override
            public void onResourceReady(Bitmap resource,
                                        GlideAnimation<? super Bitmap> glideAnimation) {

//                Bitmap bitmap = Helpers.adjustOpacity(resource, getResources().getInteger(
//                        R.integer.content_details_fragment_bg_opacity));
//
                Bitmap bitmap = Helpers.adjustOpacityAndBackground(resource,
                        getResources().getInteger(R.integer.content_details_fragment_bg_opacity),
                        ContextCompat.getColor(getActivity(), R.color.background));
                mBackgroundManager.setBitmap(bitmap);
            }
        };

        GlideHelper.loadImageIntoSimpleTargetBitmap(getActivity(), uri,
                                                    new GlideHelper.LoggingListener(),
                                                    android.R.color.transparent, bitmapTarget);
    }

    private void setupAdapter() {

        Log.v(TAG, "setupAdapter called.");
        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    public void updateActions() {

        List<Action> contentActionList = ContentBrowser.getInstance(getActivity())
                                                       .getContentActionList(mSelectedContent);

        int i = 0;
        mActionAdapter.clear();
        for (Action action : contentActionList) {
            mActionAdapter.set(i++, LeanbackHelpers.translateActionToLeanBackAction(action));
        }

        mActionInProgress = false;
    }

    private void setupDetailsOverviewRow() {

        Log.d(TAG, "doInBackground");
        if (Helpers.DEBUG) {
            Log.d(TAG, "Selected content is: " + mSelectedContent.toString());
        }
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedContent);
        row.setActionsAdapter(new ArrayObjectAdapter(new TenFootActionPresenterSelector()));
        row.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                                       android.R.color.transparent));
        int width = Helpers.convertDpToPixel(getActivity().getApplicationContext(),
                                             DETAIL_THUMB_WIDTH);
        int height = Helpers.convertDpToPixel(getActivity().getApplicationContext(),
                                              DETAIL_THUMB_HEIGHT);

        long timeRemaining = ContentBrowser.getInstance(getActivity())
                                           .getContentTimeRemaining(mSelectedContent);
        double playbackPercentage = ContentBrowser.getInstance(getActivity())
                                                  .getContentPlaybackPositionPercentage
                                                          (mSelectedContent);

        Log.d(TAG, "Time Remaining: " + timeRemaining);
        Log.d(TAG, "Playback Percentage: " + playbackPercentage);

        SimpleTarget<Bitmap> bitmapTarget = new SimpleTarget<Bitmap>(width, height) {
            @Override
            public void onResourceReady(Bitmap resource,
                                        GlideAnimation<? super Bitmap> glideAnimation) {

                Log.d(TAG,
                      "content_details_activity_layout overview card image url ready: " + resource);

                int cornerRadius =
                        getResources().getInteger(R.integer.details_overview_image_corner_radius);

                Bitmap bitmap = Helpers.roundCornerImage(getActivity(), resource, cornerRadius);

                if (playbackPercentage > 0) {
                    bitmap = Helpers.addProgress(getActivity(), bitmap, playbackPercentage);
                }

                long secondsRemaining = timeRemaining / MILLISECONDS_IN_SECOND;

                if (secondsRemaining > 0) {

                    long hours = 0;
                    long minutes = 0;
                    long seconds = 0;

                    if (secondsRemaining >= SECONDS_IN_HOUR) {
                        hours = secondsRemaining / SECONDS_IN_HOUR;
                        secondsRemaining -= hours * SECONDS_IN_HOUR;
                    }

                    if (secondsRemaining >= SECONDS_IN_MINUTE) {
                        minutes = secondsRemaining / SECONDS_IN_MINUTE;
                        secondsRemaining -= minutes * SECONDS_IN_MINUTE;
                    }

                    seconds = secondsRemaining;

                    Resources res = getResources();

                    String durationText = res.getString(R.string.duration, hours, minutes, seconds);
                    String timeRemainingText = res.getString(R.string.time_remaining, durationText);

                    bitmap = Helpers.addTimeRemaining(getActivity(), bitmap, timeRemainingText);

                }

                row.setImageBitmap(getActivity(), bitmap);

                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
            }
        };

        GlideHelper.loadImageDetailIntoSimpleTargetBitmap(getActivity(),
                                                    mSelectedContent.getCardImageUrl(),
                                                    new GlideHelper.LoggingListener<>(),
                                                    android.R.color.transparent,
                                                    bitmapTarget);

        updateActions();
        row.setActionsAdapter(mActionAdapter);

        mAdapter.add(row);
    }

    private void updateDetailsOverviewRow() {
        final DetailsOverviewRow row = (DetailsOverviewRow) mAdapter.get(0);
        row.setItem(mSelectedContent);
        row.setActionsAdapter(new ArrayObjectAdapter(new TenFootActionPresenterSelector()));
        row.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                android.R.color.transparent));
        int width = Helpers.convertDpToPixel(getActivity().getApplicationContext(),
                DETAIL_THUMB_WIDTH);
        int height = Helpers.convertDpToPixel(getActivity().getApplicationContext(),
                DETAIL_THUMB_HEIGHT);

        long timeRemaining = ContentBrowser.getInstance(getActivity())
                .getContentTimeRemaining(mSelectedContent);
        double playbackPercentage = ContentBrowser.getInstance(getActivity())
                .getContentPlaybackPositionPercentage
                        (mSelectedContent);

        Log.d(TAG, "Time Remaining: " + timeRemaining);
        Log.d(TAG, "Playback Percentage: " + playbackPercentage);

        SimpleTarget<Bitmap> bitmapTarget = new SimpleTarget<Bitmap>(width, height) {
            @Override
            public void onResourceReady(Bitmap resource,
                                        GlideAnimation<? super Bitmap> glideAnimation) {

                Log.d(TAG,
                        "content_details_activity_layout overview card image url ready: " + resource);

                int cornerRadius =
                        getResources().getInteger(R.integer.details_overview_image_corner_radius);

                Bitmap bitmap = Helpers.roundCornerImage(getActivity(), resource, cornerRadius);

                if (playbackPercentage > 0) {
                    bitmap = Helpers.addProgress(getActivity(), bitmap, playbackPercentage);
                }

                long secondsRemaining = timeRemaining / MILLISECONDS_IN_SECOND;

                if (secondsRemaining > 0) {

                    long hours = 0;
                    long minutes = 0;
                    long seconds = 0;

                    if (secondsRemaining >= SECONDS_IN_HOUR) {
                        hours = secondsRemaining / SECONDS_IN_HOUR;
                        secondsRemaining -= hours * SECONDS_IN_HOUR;
                    }

                    if (secondsRemaining >= SECONDS_IN_MINUTE) {
                        minutes = secondsRemaining / SECONDS_IN_MINUTE;
                        secondsRemaining -= minutes * SECONDS_IN_MINUTE;
                    }

                    seconds = secondsRemaining;

                    Resources res = getResources();

                    String durationText = res.getString(R.string.duration, hours, minutes, seconds);
                    String timeRemainingText = res.getString(R.string.time_remaining, durationText);

                    bitmap = Helpers.addTimeRemaining(getActivity(), bitmap, timeRemainingText);

                }

                row.setImageBitmap(getActivity(), bitmap);
            }
        };

        GlideHelper.loadImageDetailIntoSimpleTargetBitmap(getActivity(),
                mSelectedContent.getCardImageUrl(),
                new GlideHelper.LoggingListener<>(),
                android.R.color.transparent,
                bitmapTarget);

        updateActions();
        row.setActionsAdapter(mActionAdapter);
    }

    private void setupDetailsOverviewRowPresenter() {

        DetailsDescriptionPresenter detailsDescPresenter = new DetailsDescriptionPresenter();

        // Set detail background and style.
        CustomDetailsOverviewRowPresenter detailsPresenter = new CustomDetailsOverviewRowPresenter(detailsDescPresenter);
//        DetailsOverviewRowPresenter detailsPresenter =
//                new DetailsOverviewRowPresenter(detailsDescPresenter) {
//                    @Override
//                    protected void initializeRowViewHolder(RowPresenter.ViewHolder vh) {
//
//                        super.initializeRowViewHolder(vh);
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            vh.view.findViewById(R.id.details_overview_image)
//                                   .setTransitionName(ContentDetailsActivity.SHARED_ELEMENT_NAME);
//                        }
//                    }
//                };
        detailsPresenter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        detailsPresenter.setActionsBackgroundColor(getResources().getColor(android.R.color.transparent));
//        detailsPresenter.setStyleLarge(true);

//        // Hook up transition element.
//        detailsPresenter.setSharedElementEnterTransition(getActivity(),
//                                                         ContentDetailsActivity
//                                                                 .SHARED_ELEMENT_NAME);

        detailsPresenter.setOnActionClickedListener(action -> {
            try {
                if (mActionInProgress) {
                    return;
                }
                mActionInProgress = true;

                int actionId = (int) action.getId();
                Log.v(TAG, "detailsPresenter.setOnActionClicked:" + actionId);

                ContentBrowser.getInstance(getActivity()).actionTriggered(getActivity(),
                                                                          mSelectedContent,
                                                                          actionId,
                                                                          mActionAdapter,
                                                                          mActionCompletedListener);
            }
            catch (Exception e) {
                Log.e(TAG, "caught exception while clicking action", e);
                mActionInProgress = false;
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    /**
     * Builds the related content row. Uses contents from the selected content's category.
     */
    private void setupRelatedContentRow() {

        ContentContainer recommended =
                ContentBrowser.getInstance(getActivity())
                              .getRecommendedListOfAContentAsAContainer(mSelectedContent);
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());

        for (Content c : recommended) {
            listRowAdapter.add(c);
        }

        /* Zype, Evgeny Cherkasov */
        String playlistId = mSelectedContent.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID);
        // Update header for Favorites
        if (!TextUtils.isEmpty(playlistId) && playlistId.equals(ZypeSettings.FAVORITES_PLAYLIST_ID)) {
            recommended.setName(getString(R.string.content_details_recommended_header_favorites));
        }
        // Add a button for loading next page of playlist videos
        ContentContainer contentContainer = ContentBrowser.getInstance(getActivity()).getRootContentContainer()
                .findContentContainerById(playlistId);

        if (contentContainer != null) {
            if (contentContainer.getExtraValueAsInt(ExtraKeys.NEXT_PAGE) > 0) {
                PlaylistAction action = new PlaylistAction();
                action.setAction(ContentBrowser.NEXT_PAGE)
                        .setIconResourceId(com.amazon.android.contentbrowser.R.drawable.ic_add_white_48dp)
                        .setLabel1(getString(R.string.action_load_more));
                action.setExtraValue(PlaylistAction.EXTRA_PLAYLIST_ID, contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG));
                listRowAdapter.add(action);
            }
        }

        // Only add the header and row for recommendations if there are any recommended content.
        if (listRowAdapter.size() > 0) {
            HeaderItem header = new HeaderItem(0, recommended.getName());
            mAdapter.add(new ListRow(header, listRowAdapter));
        }
    }

    private void setupContentListRowPresenter() {

        ListRowPresenter presenter = new ListRowPresenter();
        presenter.setSelectEffectEnabled(false);
        presenter.setHeaderPresenter(new RowHeaderPresenter());
        mPresenterSelector.addClassPresenter(ListRow.class, presenter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Content) {
                Content content = (Content) item;
                if (Helpers.DEBUG) {
                    Log.d(TAG, "Item: " + content.getId());
                }
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        ContentDetailsActivity.SHARED_ELEMENT_NAME).toBundle();

                ContentBrowser.getInstance(getActivity())
                              .setLastSelectedContent(content)
                              .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content,
                                              bundle);
            }
            /* Zype, Evgeny Cherkasov */
            else if (item instanceof PlaylistAction) {
                PlaylistAction action = (PlaylistAction) item;
                if (action.getAction().equals(ContentBrowser.NEXT_PAGE)) {
                    Log.d(TAG, "Next page button was clicked");
                    ContentBrowser.getInstance(getActivity()).loadPlaylistVideos(action.getExtraValueAsString(PlaylistAction.EXTRA_PLAYLIST_ID));
                }
                else {
                    Log.d(TAG, "Settings with title " + action.getAction() + " was clicked");
                    ContentBrowser.getInstance(getActivity()).settingsActionTriggered(getActivity(),action);
                }
            }
        }
    }

    @Override
    public void onResume() {

        Log.v(TAG, "onResume called.");
        super.onResume();
        updateActionsProperties();
        mActionInProgress = false;
        /* Zype, Evgeny Cherkasov */
        if (receiver != null) {
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(receiver, new IntentFilter(BROADCAST_DATA_LOADED));
        }
        checkVideoEntitlement();
    }

    /**
     * Since we do not have direct access to the details overview actions row, we are adding a
     * delayed handler that waits for some time, searches for the row and then updates the
     * properties. This is not a fool-proof method,
     * > In slow devices its possible that this does not succeed in achieving the desired result.
     * > In fast devices its possible that the update is clearly visible to the user.
     * TODO: Find a better approach to update action properties
     */
    private void updateActionsProperties() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            View view = getView();
            if (view != null) {
                HorizontalGridView horizontalGridView =
                        (HorizontalGridView) view.findViewById(R.id.details_overview_actions);

                if (horizontalGridView != null) {
                    // This is required to make sure this button gets the focus whenever
                    // detailsFragment is resumed.
                    horizontalGridView.requestFocus();
                    for (int i = 0; i < horizontalGridView.getChildCount(); i++) {
                        final Button button = (Button) horizontalGridView.getChildAt(i);
                        if (button != null) {
                            // Button objects are recreated every time MovieDetailsFragment is
                            // created or restored, so we have to bind OnKeyListener to them on
                            // resuming the Fragment.
                            button.setOnKeyListener((v, keyCode, keyEvent) -> {
                                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                                        keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                                    button.performClick();
                                }
                                return false;
                            });
                        }
                    }
                }
            }
        }, 400);
    }

    /* Zype, Evgeny Cherkasov */
    private void updateRelatedContentRow() {

        ContentContainer recommended = ContentBrowser.getInstance(getActivity())
                .getRecommendedListOfAContentAsAContainer(mSelectedContent);

        // Find a row for related content
        ListRow row = null;
        for (int i = 0; i < mAdapter.size(); i++) {
            Object item = mAdapter.get(i);
            if (item instanceof ListRow) {
                row = (ListRow) item;
                break;
            }
        }
        if (row == null) {
            return;
        }

        ArrayObjectAdapter listRowAdapter = (ArrayObjectAdapter) row.getAdapter();
        // Remove 'Load more' action button
        if (listRowAdapter.size() > 0 && listRowAdapter.get(listRowAdapter.size() - 1) instanceof PlaylistAction) {
            listRowAdapter.remove(listRowAdapter.get(listRowAdapter.size() - 1));
        }
        // Add new contents
        for (int i = listRowAdapter.size(); i < recommended.getContentCount(); i++) {
            listRowAdapter.add(recommended.getContents().get(i));
        }
        // Add a button for loading next page of playlist videos
        ContentContainer contentContainer = ContentBrowser.getInstance(getActivity())
                .getRootContentContainer()
                .findContentContainerById(mSelectedContent.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
        if (contentContainer.getExtraValueAsInt(ExtraKeys.NEXT_PAGE) > 0) {
            PlaylistAction action = new PlaylistAction();
            action.setAction(ContentBrowser.NEXT_PAGE)
                    .setIconResourceId(com.amazon.android.contentbrowser.R.drawable.ic_add_white_48dp)
                    .setLabel1(getString(R.string.action_load_more));
            action.setExtraValue(PlaylistAction.EXTRA_PLAYLIST_ID, contentContainer.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG));
            listRowAdapter.add(action);
        }
    }

    private void checkVideoEntitlement() {
        if (ContentBrowser.getInstance(getActivity()).getPurchaseHelper()
                .isVideoPaywalled(mSelectedContent)) {
//        if (ZypeConfiguration.isUniversalTVODEnabled(getActivity())) {
//            ContentContainer playlist = ContentBrowser.getInstance(getActivity())
//                    .getRootContentContainer()
//                    .findContentContainerById(mSelectedContent.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
//            if (mSelectedContent.getExtraValueAsBoolean(Content.EXTRA_PURCHASE_REQUIRED)
//                || (playlist != null && playlist.getExtraValueAsBoolean(ContentContainer.EXTRA_PURCHASE_REQUIRED))) {
                String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
                HashMap<String, String> params = new HashMap<>();
                params.put(ZypeApi.ACCESS_TOKEN, accessToken);
                ZypeApi.getInstance().getApi().checkVideoEntitlement(mSelectedContent.getId(), params).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.i(TAG, "checkVideoEntitlement(): code=" + response.code());
                        if (response.isSuccessful()) {
                            mSelectedContent.setExtraValue(Content.EXTRA_ENTITLED, true);
                        }
                        else {
                            mSelectedContent.setExtraValue(Content.EXTRA_ENTITLED, false);
                        }
                        updateActions();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "checkVideoEntitlement(): failed");
                    }
                });
//            }
        }

    }

    @Override
    protected void onSetRowStatus(RowPresenter presenter, RowPresenter.ViewHolder viewHolder, int adapterPosition, int selectedPosition, int selectedSubPosition) {
        if (selectedPosition == 0) {
            updateActionsProperties();
        }
    }
}

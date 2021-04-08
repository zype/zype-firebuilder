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

package com.amazon.android.tv.tenfoot.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Row;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amazon.android.adapters.ActionWidgetAdapter;
import com.amazon.android.adapters.ContentActionWidgetAdapter;
import com.amazon.android.configuration.ConfigurationManager;
import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.event.ActionUpdateEvent;
import com.amazon.android.model.event.FavoritesLoadEvent;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;
import com.amazon.android.tv.tenfoot.ui.fragments.ContentBrowseFragment;
import com.amazon.android.tv.tenfoot.ui.fragments.MenuFragment;
import com.amazon.android.tv.tenfoot.ui.fragments.ZypeContentDetailsPlaylistFragment;
import com.amazon.android.tv.tenfoot.ui.fragments.ZypePlaylistContentBrowseFragment;
import com.amazon.android.tv.tenfoot.utils.BrowseHelper;
import com.amazon.android.tv.tenfoot.utils.ContentHelper;
import com.amazon.android.ui.constants.ConfigurationConstants;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.ui.fragments.LogoutSettingsFragment;
import com.amazon.android.ui.utils.BackgroundImageUtils;
import com.amazon.android.ui.widget.EllipsizedTextView;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.GlideHelper;
import com.amazon.android.utils.Helpers;
import com.amazon.android.utils.LeanbackHelpers;
import com.zype.fire.api.ZypeSettings;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import uk.co.chrisjenx.calligraphy.CalligraphyUtils;

import static com.amazon.android.contentbrowser.ContentBrowser.BROADCAST_DATA_LOADED;
import static com.amazon.android.contentbrowser.ContentBrowser.BROADCAST_VIDEO_DETAIL_DATA_LOADED;

/* Zype, Evgeny Cherkasov */

/**
 * Display current selected video details and current playlist videos
 */
public class ZypeContentDetailsActivity extends BaseActivity
        implements ZypeContentDetailsPlaylistFragment.OnBrowseRowListener,
                    ErrorDialogFragment.ErrorDialogFragmentListener,
                    MenuFragment.IMenuFragmentListener {


    private final String TAG = ZypeContentDetailsActivity.class.getSimpleName();

    private static final int CONTENT_IMAGE_CROSS_FADE_DURATION = 1000;
    private static final int ACTIVITY_ENTER_TRANSITION_FADE_DURATION = 1500;
    private static final int UI_UPDATE_DELAY_IN_MS = 0;

    private TextView mContentTitle;
    private EllipsizedTextView mContentDescription;
    private TextView mContentEpisode;
    private ImageView mContentImage;
    private HorizontalGridView mActionsRow;
    private ProgressBar progressBar;
    private Subscription mContentImageLoadSubscription;

    // View that contains the background
    private View mMainFrame;
    private Drawable mBackgroundWithPreview;

//    private boolean isMenuOpened = false;

    private boolean lastRowSelected = false;

    Content mSelectedContent;

    ContentActionWidgetAdapter mActionAdapter;
    private boolean mActionInProgress = false;
    private ContentBrowser.IContentActionListener mActionCompletedListener =
            new ContentBrowser.IContentActionListener() {
                @Override
                public void onContentAction(Activity activity, Content content, int actionId) {
                }

                @Override
                public void onContentActionCompleted(Activity activity, Content content, int actionId) {
                    mActionInProgress = false;
                }
            };


    private Row lastSelectedRow = null;
    private boolean lastSelectedRowChanged = false;
    private int lastSelectedItemIndex = -1;
    private int lastSelectedActionIndex = -1;
    private boolean restoreActionsFocus = false;

    private BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_zype_content_details);

        Helpers.handleActivityEnterFadeTransition(this, ACTIVITY_ENTER_TRANSITION_FADE_DURATION);

        mContentTitle = (TextView) findViewById(R.id.content_detail_title);
        CalligraphyUtils.applyFontToTextView(this, mContentTitle, ConfigurationManager
                .getInstance(this).getTypefacePath(ConfigurationConstants.BOLD_FONT));

        mContentDescription = (EllipsizedTextView) findViewById(R.id.content_detail_description);
        CalligraphyUtils.applyFontToTextView(this, mContentDescription, ConfigurationManager
                .getInstance(this).getTypefacePath(ConfigurationConstants.LIGHT_FONT));

        mContentEpisode = (TextView) findViewById(R.id.content_episode);
        CalligraphyUtils.applyFontToTextView(this, mContentEpisode, ConfigurationManager
                .getInstance(this).getTypefacePath(ConfigurationConstants.LIGHT_FONT));

        mContentImage = (ImageView) findViewById(R.id.content_image);

        mContentImage.setImageURI(Uri.EMPTY);

        mActionsRow = (HorizontalGridView) findViewById(R.id.listActions);
        mActionAdapter = new ContentActionWidgetAdapter(mActionsRow);
        mSelectedContent = ContentBrowser.getInstance(this).getLastSelectedContent();
        updateActions(mSelectedContent);
        mActionsRow.requestFocus();

//        // Get display/background size
//        Display display = getWindowManager().getDefaultDisplay();
//        Point windowSize = new Point();
//        display.getSize(windowSize);
//        int imageWidth = (int) getResources().getDimension(R.dimen.content_image_width);
//        int imageHeight = (int) getResources().getDimension(R.dimen.content_image_height);
//        int gradientSize = (int) getResources().getDimension(R.dimen.content_image_gradient_size_zype);
//        // Create the background
//        Bitmap background =
//                BackgroundImageUtils.createBackgroundWithPreviewWindow(
//                        windowSize.x,
//                        windowSize.y,
//                        imageWidth,
//                        imageHeight,
//                        gradientSize,
//                        ContextCompat.getColor(this, R.color.browse_background_color));
//        mBackgroundWithPreview = new BitmapDrawable(getResources(), background);
//        // Set the background
//        mMainFrame = findViewById(R.id.main_frame);
//        mMainFrame.setBackground(mBackgroundWithPreview);

        hideMenu();
        if (ZypeSettings.SHOW_TOP_MENU) {
            showActions(false);
            hideTopMenu();
        }

        progressBar = (ProgressBar) findViewById(R.id.feed_progress);
        progressBar.setVisibility(View.VISIBLE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        };

    }

    /**
     * {@inheritDoc}
     * Called by the browse fragment ({@link ContentBrowseFragment}. Switches the content
     * title, description, and image.
     */
    @Override
    public void onItemSelected(Object item, Row row, int rowIndex, int rowsNumber) {
        if (row != lastSelectedRow && item != null) {
            lastSelectedRow = row;
            lastSelectedRowChanged = true;
        }
        else {
            lastSelectedRowChanged = false;
        }
        if (row != null) {
            lastSelectedItemIndex = ((ArrayObjectAdapter) ((ListRow) row).getAdapter()).indexOf(item);
        }

        if (item instanceof Content) {
            Content content = (Content) item;
            mSelectedContent = content;
            if (!ContentBrowser.getInstance(this).getLastSelectedContent().getId().equals(content.getId())) {
                ContentBrowser.getInstance(this).setLastSelectedContent(content);
            }
            callImageLoadSubscription(content.getTitle(),
                    content.getDescription(),
                    content.getBackgroundImageUrl());

            if (ZypeSettings.SHOW_EPISODE_NUMBER
                    && !TextUtils.isEmpty(ContentHelper.getEpisodeSubTitle(this, content))) {
                mContentEpisode.setVisibility(View.VISIBLE);
                mContentEpisode.setText(ContentHelper.getEpisodeSubTitle(this, content));
            }
            else{
                mContentEpisode.setVisibility(View.GONE);
            }
            updateActions(content);
        }
        else if (item instanceof ContentContainer) {
            ContentContainer contentContainer = (ContentContainer) item;
            callImageLoadSubscription(contentContainer.getName(),
                    contentContainer.getExtraStringValue("description"),
                    contentContainer.getExtraStringValue(Content.BACKGROUND_IMAGE_URL_FIELD_NAME));
        }
        else if (item instanceof Action) {
            Action settingsAction = (Action) item;
            // Terms of use action.
            if (ContentBrowser.TERMS.equals(settingsAction.getAction())) {
                callImageLoadSubscription(getString(R.string.terms_title),
                        getString(R.string.terms_description),
                        null);
            }
            // Login and logout action.
            else if (ContentBrowser.LOGIN_LOGOUT.equals(settingsAction.getAction())) {

                if (settingsAction.getState() == LogoutSettingsFragment.TYPE_LOGOUT) {
                    callImageLoadSubscription(getString(R.string.logout_label),
                            getString(R.string.logout_description),
                            null);
                }
                else {
                    callImageLoadSubscription(getString(R.string.login_label),
                            getString(R.string.login_description),
                            null);
                }
            }
        }
    }

    @Override
    public void onItemClicked(Object item) {
        Content content = (Content) item;
        ContentBrowser.getInstance(ZypeContentDetailsActivity.this)
                .actionTriggered(ZypeContentDetailsActivity.this,
                        content,
                        (int) findDefaultAction().getId(),
                        null,
                        mActionCompletedListener);
    }

    /**
     * Helper method to subscribe the selected item to the observable that will load the content
     * image into the background. It is okay for the background image URL to be null. A null URL
     * will result in showing the default background.
     *
     * @param title       The title to display.
     * @param description The description to display.
     * @param bgImageUrl  The URL of the image to display.
     */
    private void callImageLoadSubscription(String title, String description, String bgImageUrl) {

        mContentImageLoadSubscription = Observable
                .timer(UI_UPDATE_DELAY_IN_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread()) // This is a must for timer.
                .subscribe(c -> {
                    mContentTitle.setText(title);
                    mContentDescription.setText(description);

                    GlideHelper.loadImageWithCrossFadeTransition(this,
                            mContentImage,
                            bgImageUrl,
                            CONTENT_IMAGE_CROSS_FADE_DURATION,
                            R.color.browse_background_color);

//                    // If there is no image, remove the preview window
//                    if (bgImageUrl != null && !bgImageUrl.isEmpty()) {
//                        mMainFrame.setBackground(mBackgroundWithPreview);
//                    }
//                    else {
//                        mMainFrame.setBackgroundColor(Color.TRANSPARENT);
//                    }
                });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(receiver, new IntentFilter(BROADCAST_VIDEO_DETAIL_DATA_LOADED));
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(receiver, new IntentFilter(BROADCAST_DATA_LOADED));
        }
        if (ContentBrowser.getInstance(this).getAuthHelper() != null) {
            ContentBrowser.getInstance(this).getAuthHelper()
                    .loadPoweredByLogo(this, (ImageView) findViewById(R.id.mvpd_logo));
        }

        if (mSelectedContent != null) {
            updateActions(mSelectedContent);
        }

        reportFullyDrawn();
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (mContentImageLoadSubscription != null) {
            mContentImageLoadSubscription.unsubscribe();
        }
        if (receiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }

    }

    @Override
    protected void onStart() {

        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {

        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (mContentImageLoadSubscription != null) {
            mContentImageLoadSubscription.unsubscribe();
        }
    }

    //
    // ErrorDialogFragmentListener
    //
    /**
     * Callback method to define the button behaviour for this activity.
     *
     * @param errorDialogFragment The fragment listener.
     * @param errorButtonType     The display text on the button
     * @param errorCategory       The error category determined by the client.
     */
    @Override
    public void doButtonClick(ErrorDialogFragment errorDialogFragment, ErrorUtils.ERROR_BUTTON_TYPE errorButtonType, ErrorUtils.ERROR_CATEGORY errorCategory) {
        if (errorDialogFragment != null) {
            errorDialogFragment.dismiss();
            finish();
        }
    }

    //
    // BaseActivity abstract methods
    //
    @Override
    public void setRestoreActivityValues() {
        BrowseHelper.saveBrowseActivityState(this);
    }


    private void updateActions(Content content) {
        List<Action> contentActionList = ContentBrowser.getInstance(this)
                .getContentActionList(content);

        mActionAdapter.removeActions();
        mActionAdapter.addActions(contentActionList);
        ContentActionWidgetAdapter.IContentActionWidgetAdapterListener actionListener = new ContentActionWidgetAdapter.IContentActionWidgetAdapterListener() {
            @Override
            public void onActionClicked(Action action) {
                try {
                    if (mActionInProgress) {
                        return;
                    }
                    mActionInProgress = true;

                    int actionId = (int) action.getId();
                    Log.v(TAG, "onActionClicked():" + actionId);

                    ContentBrowser.getInstance(ZypeContentDetailsActivity.this)
                            .actionTriggered(ZypeContentDetailsActivity.this,
                                    mSelectedContent,
                                    actionId,
                                    null,
                                    mActionCompletedListener);
                }
                catch (Exception e) {
                    Log.e(TAG, "caught exception while clicking action", e);
                    mActionInProgress = false;
                }
            }

            @Override
            public void onActionSelected(int position) {
                lastSelectedActionIndex = position;
            }
        };
        mActionAdapter.setListener(actionListener);
    }

    private Action findDefaultAction() {
        Action result = null;
        for (int i = 0; i < mActionAdapter.getItemCount(); i++) {
            Action action = mActionAdapter.getAction(i);
            if (result == null) {
                result = action;
            }
            else {
                if (action.getState() != 0) {
                    if (action.getState() < result.getState()) {
                        result = action;
                    }
                 }
            }
        }
        return result;
    }

    private void showMenu() {
        MenuFragment fragment = (MenuFragment) getFragmentManager().findFragmentById(R.id.fragmentMenu);
        if (fragment != null) {
            isMenuOpened = true;
            fragment.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.left_menu_background_color));
            int paddingTop = (int) getResources().getDimension(R.dimen.lb_browse_padding_top);
            fragment.getView().setPadding(0, paddingTop, 0, 0);
            getFragmentManager().beginTransaction()
                    .show(fragment)
                    .commit();
            fragment.getView().requestFocus();
        }
    }

    private void hideMenu() {
        MenuFragment fragment = (MenuFragment) getFragmentManager().findFragmentById(R.id.fragmentMenu);
        if (fragment != null) {
            isMenuOpened = false;
            getFragmentManager().beginTransaction()
                    .hide(fragment)
                    .commit();
        }
    }

    @Override
    public void showMenuFragment() {
        if (!isMenuOpened){
            showMenu();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "event=" + event.toString());

        switch (event.getKeyCode()) {

            case KeyEvent.KEYCODE_MENU:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (ZypeSettings.SHOW_TOP_MENU) {
                        Log.d(TAG, "Menu button pressed");
                        if (!isMenuOpened) {
                            showTopMenu();
                        }
                        return true;
                    }
                    if (ZypeSettings.SHOW_LEFT_MENU) {
                        Log.d(TAG, "Menu button pressed");
                        if (!isMenuOpened) {
                            showMenu();
                        }
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK: {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Back button pressed");
                    if (isMenuOpened) {
                        if (ZypeSettings.SHOW_LEFT_MENU) {
                            hideMenu();
                        }
                        else if (ZypeSettings.SHOW_TOP_MENU) {
                            hideTopMenu();
                        }
                        if (restoreActionsFocus) {
                            mActionsRow.requestFocus();
                        }
                        else {
                            findViewById(R.id.full_content_browse_fragment).requestFocus();
                        }
                        return true;
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.d(TAG, "Up button pressed");
                if (isMenuOpened && ZypeSettings.SHOW_LEFT_MENU) {
                    MenuFragment fragment = (MenuFragment) getFragmentManager().findFragmentById(R.id.fragmentMenu);
                    if (fragment != null) {
                        ArrayObjectAdapter menuAdapter = (ArrayObjectAdapter) fragment.getAdapter();
                        if (fragment.getSelectedMenuItemIndex() == 0) {
                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                return true;
                            }
                        }
                    }
                }
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (!isMenuOpened && ZypeSettings.SHOW_TOP_MENU) {
                        if (mActionsRow.hasFocus()) {
                            showTopMenu();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d(TAG, "Down button pressed");
                if (isMenuOpened) {
                    if (ZypeSettings.SHOW_LEFT_MENU) {
                        MenuFragment fragment = (MenuFragment) getFragmentManager().findFragmentById(R.id.fragmentMenu);
                        if (fragment != null) {
                            ArrayObjectAdapter menuAdapter = (ArrayObjectAdapter) fragment.getAdapter();
                            if (fragment.getSelectedMenuItemIndex() + 1 >= menuAdapter.size()) {
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    return true;
                                }
                            }
                        }
                    }
                    else if (ZypeSettings.SHOW_TOP_MENU) {
                        hideTopMenu();
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d(TAG, "Right button pressed");
                if (isMenuOpened) {
                    if (ZypeSettings.SHOW_LEFT_MENU) {
                        hideMenu();
                        if (restoreActionsFocus) {
                            mActionsRow.requestFocus();
                        } else {
                            findViewById(R.id.full_content_browse_fragment).requestFocus();
                        }
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d(TAG, "Left button pressed");
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (!isMenuOpened && ZypeSettings.SHOW_LEFT_MENU) {
                        if (mActionsRow.hasFocus()) {
                            if (lastSelectedActionIndex == 0) {
                                lastSelectedActionIndex = -1;
                            } else if (lastSelectedActionIndex == -1) {
                                restoreActionsFocus = true;
                                showMenu();
                            }
                        }
                        else {
                            if (lastSelectedItemIndex == 0) {
                                lastSelectedItemIndex = -1;
                                if (lastSelectedRowChanged) {
                                    restoreActionsFocus = false;
                                    showMenu();
                                }
                            } else if (lastSelectedItemIndex == -1) {
                                restoreActionsFocus = false;
                                showMenu();
                            }
                        }
                    }
                }
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onItemSelected(Action item) {
        hideMenu();
        ContentBrowser.getInstance(this)
                .settingsActionTriggered(this, item);
    }

    @Subscribe
    public void onActionUpdateRequired(ActionUpdateEvent actionUpdateEvent) {
        updateActions(mSelectedContent);
    }

    @Subscribe
    public void onFavoritesLoadEvent(FavoritesLoadEvent event) {
        updateActions(mSelectedContent);
    }

}

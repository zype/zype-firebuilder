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


import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;
import com.amazon.android.tv.tenfoot.ui.fragments.ContentSearchFragment;
import com.zype.fire.api.ZypeSettings;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.VisibleForTesting;
import androidx.leanback.widget.SearchEditText;

/**
 * An activity class for the {@link ContentSearchFragment} fragment.
 */
public class ContentSearchActivity extends BaseActivity {

private static final String TAG = ContentSearchActivity.class.getSimpleName();

    @VisibleForTesting
    // This is the local ContentSearchFragment variable.
    public ContentSearchFragment mFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_search_layout);

        // Set the ContentSearchFragment.
        mFragment = (ContentSearchFragment) getFragmentManager()
                .findFragmentById(R.id.content_search_fragment);

        if (ZypeSettings.SHOW_TOP_MENU) {
            hideTopMenu();
        }
    }

    @Override
    public void setRestoreActivityValues() {
        // not saving this state.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "event=" + event.toString());

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MENU:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (!isMenuOpened) {
                        if (ZypeSettings.SHOW_TOP_MENU) {
                            showTopMenu();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK: {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Back button pressed");
                    if (isMenuOpened) {
                        if (ZypeSettings.SHOW_TOP_MENU) {
                            hideTopMenu();
                            return true;
                        }
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.d(TAG, "Up button pressed");
                if (!isMenuOpened && ZypeSettings.SHOW_TOP_MENU) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        final SearchEditText searchEditText = findViewById(R.id.lb_search_text_editor);
                        if (searchEditText != null && searchEditText.hasFocus()) {
                            showTopMenu();
                            return true;
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d(TAG, "Down button pressed");
                if (isMenuOpened) {
                    if (ZypeSettings.SHOW_TOP_MENU) {
                        hideTopMenu();
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mFragment.showKeyboard();
                }
                break;
        }
        return super.dispatchKeyEvent(event);
    }

}

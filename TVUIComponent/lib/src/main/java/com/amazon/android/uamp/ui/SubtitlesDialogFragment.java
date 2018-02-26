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
package com.amazon.android.uamp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.amazon.android.tv.tenfoot.R;

/**
 * Alert dialog fragment class.
 */
public class SubtitlesDialogFragment extends DialogFragment {

    /**
     * Fragment TAG.
     */
    private static final String TAG = SubtitlesDialogFragment.class.getSimpleName();

    /**
     * Subtitles dialog listener.
     */
    public interface ISubtitlesDialogListener {
        /**
         * On item selectedmethod.
         *
         * @param dialog The alert dialog fragment to listen on.
         */
        void onItemSelected(SubtitlesDialogFragment dialog, int selectedItem);
    }

    /**
     * Dialog title.
     */
    private String mTitle;

    /**
     * List of subtitles tracks
     */
    private CharSequence[] items;

    /**
     * Selected subtitle track
     */
    private int selectedItem = 0;

    /**
     * Dialog listener reference.
     */
    private ISubtitlesDialogListener mDialogListener;

    /**
     * Create and show alert dialog fragment.
     *
     * @param activity             Activity.
     * @param title                Dialog title.
     * @param items                List of tracks to select.
     * @param selectedItem         Selected track
     * @param listener             Dialog listener reference.
     */
    public static void createAndShowSubtitlesDialogFragment(Activity activity,
                                                        String title,
                                                        CharSequence[] items,
                                                        int selectedItem,
                                                        ISubtitlesDialogListener listener) {

        SubtitlesDialogFragment dialog = new SubtitlesDialogFragment(title,
                                                                      items,
                                                                      selectedItem,
                                                                      listener);
//        dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.selection_dialog);
        FragmentManager fragmentManager = activity.getFragmentManager();
        dialog.setCancelable(true);
        dialog.show(fragmentManager, TAG);
    }

    /**
     * Default constructor.
     */
    public SubtitlesDialogFragment() {

    }

    /**
     * Constructor.
     *
     * @param title                Dialog title.
     * @param items                List of tracks to select.
     * @param selectedItem         Selected track
     * @param listener             Dialog listener reference.
     */
    public SubtitlesDialogFragment(String title,
                                   CharSequence[] items,
                                   int selectedItem,
                                   ISubtitlesDialogListener listener) {

        mTitle = title;
        this.items = items;
        this.selectedItem = selectedItem;
        mDialogListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getActivity(), R.layout.closed_captions_list_item, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle);
        builder.setSingleChoiceItems(adapter, selectedItem,
                (dialog, which) -> mDialogListener.onItemSelected(this, which));

        Dialog result = builder.create();
        return result;
    }
}

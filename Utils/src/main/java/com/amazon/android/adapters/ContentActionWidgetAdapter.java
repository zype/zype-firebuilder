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
package com.amazon.android.adapters;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.MainThread;
import androidx.leanback.widget.HorizontalGridView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.amazon.android.configuration.ConfigurationManager;
import com.amazon.android.model.Action;
import com.amazon.android.ui.constants.ConfigurationConstants;
import com.amazon.utils.R;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import uk.co.chrisjenx.calligraphy.CalligraphyUtils;

/**
 * This class extends the {@link RecyclerView.Adapter}
 * It is used to display Actions items.
 */
public class ContentActionWidgetAdapter extends RecyclerView.Adapter {

    // A list of action items that are presented in the view.
    private final ArrayList<Action> mActionsList;

    private static final int WINDOW_ALIGNMENT_OFFSET_PERCENT = 35;

    // The HorizontalGridView that is used to display the items.
    private HorizontalGridView mHorizontalGridView;

    private IContentActionWidgetAdapterListener listener;

    public interface IContentActionWidgetAdapterListener {
        void onActionClicked(Action action);
        void onActionSelected(int position);
    }

    /**
     * This constructor will setup the connections from the view and the widget adapter.
     *
     * @param inputHorizontalGridView The {@link HorizontalGridView}.
     */
    public ContentActionWidgetAdapter(HorizontalGridView inputHorizontalGridView) {

        // Call the specific constructor with a new ArrayList.
        this(inputHorizontalGridView, new ArrayList<>());

    }

    /**
     * This constructor will setup the connections from the view and the widget adapter.
     *
     * @param inputHorizontalGridView The {@link HorizontalGridView}.
     * @param actions                 The actions items that will be displayed.
     */
    public ContentActionWidgetAdapter(HorizontalGridView inputHorizontalGridView,
                                      ArrayList<Action> actions) {

        // Set the horizontal grid view to the input view.
        mHorizontalGridView = inputHorizontalGridView;

        // Set the horizontal grid view alignment.
        mHorizontalGridView.setWindowAlignment(HorizontalGridView.WINDOW_ALIGN_BOTH_EDGE);
        mHorizontalGridView.setWindowAlignmentOffsetPercent(WINDOW_ALIGNMENT_OFFSET_PERCENT);

        // Set the adapter of the Horizontal grid view.
        mHorizontalGridView.setAdapter(this);

        mActionsList = new ArrayList<>();

        // Set the actions.
        if (actions != null) {

            addActions(actions);
        }
    }

    public void setListener(IContentActionWidgetAdapterListener listener) {
        this.listener = listener;
    }

    /**
     * This method will add an {@link ArrayList} of actions to the action widget.
     * This method must be called on the main UI thread.
     *
     * @param inputActions An {@link ArrayList} of actions that needs to be added.
     */
    @MainThread
    public void addActions(List<Action> inputActions) {

        mActionsList.addAll(inputActions);

//        setHorizontalGridViewSizeBasedOnActions();

        // Notify the adapter that new items have been added.
        // This call needs to be on the main UI thread.
        notifyDataSetChanged();
    }

    /**
     * This method will add a single action to the action widget.
     * This method must be called on the main UI thread.
     *
     * @param inputAction The action that needs to be added.
     */
    @MainThread
    public void addAction(Action inputAction) {

        mActionsList.add(inputAction);

//        setHorizontalGridViewSizeBasedOnActions();

        // Notify the adapter that a new item has been added.
        notifyDataSetChanged();
    }

    /**
     * This method returns the action at a position in the action array.
     *
     * @param position The position of the action we want.
     * @return The found action.
     */
    public Action getAction(int position) {

        return mActionsList.get(position);
    }

    /**
     * This method will return and remove an action by its ID.
     *
     * @param inputID The id of the action.
     * @return The found {@link Action}.
     * @throws NoSuchElementException if the {@link Action} is not found.
     */
    public Action removeAction(long inputID) throws NoSuchElementException {

        Action returnAction = null;

        // Iterate through the list to find and remove the action.
        for (int i = 0; i < mActionsList.size(); i++) {

            if (mActionsList.get(i).getId() == inputID) {
                // Set the return action.
                returnAction = mActionsList.get(i);

                // Remove the action from the list.
                mActionsList.remove(i);

                // Notify this adapter that this data set has changed.
                notifyDataSetChanged();
                break;
            }
        }

        if (returnAction != null) {
            return returnAction;
        }
        else {
            throw new NoSuchElementException("Action was not found");
        }
    }

    public void removeActions() {
        mActionsList.clear();
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new {@link RecyclerView
     * .ViewHolder} of the given type to represent an item.
     *
     * @param parent   The parent view.
     * @param viewType The type of view.
     * @return A {@link ContentActionWidgetAdapter.ViewHolder}.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout.
        View contactView = inflater.inflate(R.layout.content_action_item, parent, false);

        // Return a new holder instance.
        return new ViewHolder(contactView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param baseHolder The view holder.
     * @param position   The position of the view.
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {
        ViewHolder holder = (ViewHolder) baseHolder;
        Action action = mActionsList.get(position);

        // Set item views based on the data model.
//        holder.actionButton.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(),
//                R.drawable.action_button_background));
        holder.actionButton.setText(action.getLabel1() + "\n" + action.getLabel2());
        CalligraphyUtils.applyFontToTextView(holder.itemView.getContext(), holder.actionButton,
                ConfigurationManager.getInstance(holder.itemView.getContext())
                        .getTypefacePath(ConfigurationConstants.REGULAR_FONT));

//        LinearLayout.LayoutParams params =
//                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT);
//        int margin = (int) getResources().getDimension(R.dimen.error_button_side_margin);
//        params.setMargins(margin, 0, margin, 0);
//        holder.actionButton.setLayoutParams(params);

        holder.actionButton.setTextColor(ContextCompat
                .getColorStateList(holder.itemView.getContext(),
                        R.color.action_button_text_color_selector));
        holder.actionButton.setFocusable(true);
        holder.actionButton.setFocusableInTouchMode(true);
        holder.actionButton.setTag(action.getName());

        holder.actionButton.setOnClickListener(v -> {
            mHorizontalGridView.performClick();
            if (listener != null) {
                listener.onActionClicked(action);
            }
        });

        holder.actionButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                listener.onActionSelected(position);
            }
//            if (hasFocus) {
//                // TODO: Fix up app theme and custom.xml colors. DEVTECH-3009
//                int color = v.getContext().getResources().getColor(R.color.search_orb);
//                v.getBackground().setColorFilter(color,
//                                                 PorterDuff.Mode.SRC_ATOP);
//                v.invalidate();
//            }
//            else {
//                v.getBackground().clearColorFilter();
//                v.invalidate();
//            }
        });
    }

    /**
     * Returns the total number of items in the data set hold by the adapter.
     *
     * @return int, that represents the number of items in the list.
     */
    @Override
    public int getItemCount() {

        return mActionsList.size();
    }

    /**
     * Sets the horizontal grid view layout params based on the number of actions registered.
     */
    private void setHorizontalGridViewSizeBasedOnActions() {

        Resources res = mHorizontalGridView.getContext().getResources();

        int width = res.getDimensionPixelSize(R.dimen.action_widget_width);
        int actionPadding = res.getDimensionPixelSize(R.dimen.action_widget_padding);
        int gridViewPadding = res.getDimensionPixelSize(R.dimen.grid_view_left_right_padding);

        ViewGroup.LayoutParams params = mHorizontalGridView.getLayoutParams();

        params.width = (width + actionPadding + gridViewPadding) * mActionsList.size();
        params.height = res.getDimensionPixelSize(R.dimen.action_widget_height);
        mHorizontalGridView.setLayoutParams(params);
    }

    /**
     * This class describes an item view and metadata about its place within the RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        public final Button actionButton;

        public ViewHolder(View v) {

            super(v);
            actionButton = (Button) itemView.findViewById(R.id.action_button);
        }
    }
}
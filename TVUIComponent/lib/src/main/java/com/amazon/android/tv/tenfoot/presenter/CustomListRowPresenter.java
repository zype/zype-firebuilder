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
package com.amazon.android.tv.tenfoot.presenter;

import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amazon.android.tv.tenfoot.R;

/**
 * Customized ListRowPresenter to get each category to correspond to a 2-row horizontal grid.
 */
public class CustomListRowPresenter extends ListRowPresenter {

    private int initialSelectedPosition;

    private int headerColor = -1;

    public CustomListRowPresenter(int position) {
        super();
        this.initialSelectedPosition = position;
    }
    /**
     * Constructor.
     */
    public CustomListRowPresenter() {
        super();
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {

        ListRowPresenter.ViewHolder viewHolder = (ListRowPresenter.ViewHolder) super
                .createRowViewHolder(parent);
        return viewHolder;
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        if (initialSelectedPosition != -1) {
            ViewHolder vh = (ListRowPresenter.ViewHolder) holder;
            vh.getGridView().setSelectedPosition(initialSelectedPosition);
        }
        // Set custom header text color, if it is defined.
        TextView viewHeader = (TextView) holder.getHeaderViewHolder().view.findViewById(R.id.row_header);
        if (headerColor != -1) {
            viewHeader.setTextColor(headerColor);
        }
    }

    public void setHeaderColor(int color) {
        headerColor = color;
    }
}

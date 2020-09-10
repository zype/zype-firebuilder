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

import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.purchase.Model.SubscriptionItem;

/**
 * A CardPresenter used to generate Views and bind SettingsItems to them on demand.
 */
public class SubscriptionCardPresenter extends Presenter {
    private static final String TAG = SubscriptionCardPresenter.class.getSimpleName();

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscriptions_list_item, parent, false);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        return new ViewHolder(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.item = (SubscriptionItem) item;
        holder.textTitle.setText(holder.item.title);
        holder.textPrice.setText(holder.item.priceText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public class ViewHolder extends Presenter.ViewHolder {
        public final View view;
        public SubscriptionItem item;
        public TextView textTitle;
        public TextView textPrice;
        public TextView textDescription;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            textTitle = (TextView) view.findViewById(R.id.textTitle);
            textPrice = (TextView) view.findViewById(R.id.textPrice);
        }
    }

}


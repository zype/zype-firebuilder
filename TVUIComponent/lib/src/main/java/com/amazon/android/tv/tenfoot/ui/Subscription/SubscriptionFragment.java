package com.amazon.android.tv.tenfoot.ui.Subscription;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.Log;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.ContentLoader;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.model.Action;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.event.SubscriptionProductsUpdateEvent;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.presenter.CustomListRowPresenter;
import com.amazon.android.tv.tenfoot.presenter.SettingsCardPresenter;
import com.amazon.android.tv.tenfoot.ui.Subscription.Model.SubscriptionItem;
import com.amazon.android.tv.tenfoot.ui.fragments.ContentBrowseFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 25.08.2017.
 */

public class SubscriptionFragment extends RowsFragment {
    private static final String TAG = SubscriptionFragment.class.getSimpleName();

    private ArrayObjectAdapter subscriptionsAdapter = null;
    ArrayObjectAdapter rowsAdapter = null;

    private static final int WAIT_BEFORE_FOCUS_REQUEST_MS = 500;

    public interface ISubscriptionSelectedListener {
        void onSubscriptionSelected(SubscriptionItem item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);

//        // This makes sure that the container activity has implemented the callback interface.
//        // If not, it throws an exception.
//        try {
//            mCallback = (ContentBrowseFragment.OnBrowseRowListener) getActivity();
//        }
//        catch (ClassCastException e) {
//            throw new ClassCastException(getActivity().toString() +
//                    " must implement " +
//                    "OnBrowseRowListener: " + e);
//        }

        CustomListRowPresenter customListRowPresenter = new CustomListRowPresenter();
//        customListRowPresenter.setHeaderPresenter(new RowHeaderPresenter());

        customListRowPresenter.setShadowEnabled(false);

        /* Zype, Evgney Cherkasov */
//        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(customListRowPresenter);
        rowsAdapter = new ArrayObjectAdapter(customListRowPresenter);
        setAdapter(rowsAdapter);

        setOnItemViewClickedListener(new SubscriptionFragment.ItemViewClickedListener());

        // Wait for WAIT_BEFORE_FOCUS_REQUEST_MS for the data to load before requesting focus.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (getView() != null) {
                VerticalGridView verticalGridView = findGridViewFromRoot(getView());
                if (verticalGridView != null) {
                    verticalGridView.requestFocus();
                }
            }
        }, WAIT_BEFORE_FOCUS_REQUEST_MS);

        ContentBrowser.getInstance(getActivity()).getPurchaseHelper().handleProductsChain(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
//        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void updateSubscriptionOptions(ArrayObjectAdapter arrayObjectAdapter, ArrayList<HashMap<String, String>> products) {
        SubscriptionCardPresenter cardPresenter = new SubscriptionCardPresenter();
        subscriptionsAdapter = new ArrayObjectAdapter(cardPresenter);

        if (products == null || products.isEmpty()) {
            // TODO: Comment adding dummy item for release build
            SubscriptionItem item = new SubscriptionItem();
            item.title = "Monthly";
            item.description = "";
            item.price = 4.99f;
            item.priceText = String.valueOf(item.price);
            item.sku = "com.zype.aftv.testsubscriptionmonthly";
            subscriptionsAdapter.add(item);

            item = new SubscriptionItem();
            item.title = "Yearly";
            item.description = "";
            item.price = 7.99f;
            item.priceText = String.valueOf(item.price);
            item.sku = "com.zype.aftv.testsubscriptionyearly";
            subscriptionsAdapter.add(item);
        }
        else {
            for (HashMap<String, String> productData : products) {
                SubscriptionItem item = new SubscriptionItem();
                // TODO: Use constants for keys
                item.title = productData.get("Title");
                item.description = productData.get("Description");
                item.priceText = productData.get("Price");
                item.sku = productData.get("SKU");
                subscriptionsAdapter.add(item);
            }
        }

        arrayObjectAdapter.clear();
        if (subscriptionsAdapter != null) {
            arrayObjectAdapter.add(new ListRow(null, subscriptionsAdapter));
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            ((ISubscriptionSelectedListener) getActivity()).onSubscriptionSelected((SubscriptionItem) item);
        }
    }

    /**
     * Event bus event listener method to detect products broadcast.
     *
     * @param event Broadcast event for progress overlay dismiss.
     */
    @Subscribe
    public void onProgressOverlayDismissEvent(SubscriptionProductsUpdateEvent event) {
        ArrayList<HashMap<String, String>> products = (ArrayList<HashMap<String, String>>) event.getExtras().getSerializable(PurchaseHelper.RESULT_PRODUCTS);
        updateSubscriptionOptions(rowsAdapter, products);
    }

}

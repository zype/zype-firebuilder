package com.amazon.android.tv.tenfoot.ui.purchase;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridView;
import android.util.Log;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.model.event.ProductsUpdateEvent;
import com.amazon.android.tv.tenfoot.presenter.CustomListRowPresenter;
import com.amazon.android.tv.tenfoot.presenter.SubscriptionCardPresenter;
import com.amazon.android.tv.tenfoot.ui.purchase.Model.SubscriptionItem;
import com.zype.fire.api.MarketplaceGateway;
import com.zype.fire.api.Model.PlanData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;

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

    private ISubscriptionSelectedListener listenerSubscriptionSelected;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);

        // This makes sure that the container activity has implemented the callback interface.
        // If not, it throws an exception.
        try {
            listenerSubscriptionSelected = (ISubscriptionSelectedListener) getActivity();
        }
        catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + "ISubscriptionSelectedListener: " + e);
        }

        CustomListRowPresenter customListRowPresenter = new CustomListRowPresenter();
        customListRowPresenter.setShadowEnabled(false);

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
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void updateSubscriptionOptions(ArrayObjectAdapter arrayObjectAdapter,
                                           ArrayList<HashMap<String, String>> products) {
        SubscriptionCardPresenter cardPresenter = new SubscriptionCardPresenter();
        subscriptionsAdapter = new ArrayObjectAdapter(cardPresenter);

        if (products != null && !products.isEmpty()) {
            for (HashMap<String, String> productData : products) {
                SubscriptionItem item = new SubscriptionItem();
                // TODO: Use constants for keys
                item.title = productData.get("Title");
                item.description = productData.get("Description");
                item.priceText = productData.get("Price");
                item.sku = productData.get("SKU");
                PlanData plan = MarketplaceGateway.getInstance(getActivity()).findPlanBySku(item.sku);
                if (plan == null) {
                    Log.e(TAG, "updateSubscriptionOptions(): Plan not found for sku " + item.sku);
                    continue;
                }
                item.planId = plan.id;
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
            listenerSubscriptionSelected.onSubscriptionSelected((SubscriptionItem) item);
        }
    }

    /**
     * Event bus event listener method to detect products broadcast.
     *
     * @param event Broadcast event for progress overlay dismiss.
     */
    @Subscribe
    public void onProductsUpdateEvent(ProductsUpdateEvent event) {
        ArrayList<HashMap<String, String>> products = (ArrayList<HashMap<String, String>>) event.getExtras().getSerializable(PurchaseHelper.RESULT_PRODUCTS);
        updateSubscriptionOptions(rowsAdapter, products);
    }

}

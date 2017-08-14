package com.amazon.android.tv.tenfoot.ui.Subscription;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.Subscription.Model.SubscriptionItem;
import com.amazon.android.utils.Preferences;
import com.zype.fire.auth.ZypeAuthentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 07.08.2017.
 */

public class SubscriptionActivity extends Activity {
    private static final String TAG = SubscriptionActivity.class.getName();

    public static final String PARAMETERS_MODE = "Mode";
    public static final String PARAMETERS_PRODUCTS = "Products";

    private static final int REQUEST_CREATE_LOGIN = 110;

    private LinearLayout layoutChoosePlan;
    private LinearLayout layoutConfirm;

    private TextView textDescription;
    private RecyclerView listSubscriptions;
    private LinearLayout layoutLogin;
    private Button buttonLogin;
    private LinearLayout layoutLoggedIn;
    private TextView textConsumerEmail;

    private Button buttonConfirm;
    private Button buttonCancel;

    public static final int MODE_CHOOSE_PLAN = 1;
    public static final int MODE_CONFIRM = 2;
    private int mode;
    private ArrayList<HashMap<String, String>> products;

    private SubscriptionsAdapter adapter;
    private SubscriptionItem selectedSubscription = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        initParameters(savedInstanceState);

        layoutChoosePlan = (LinearLayout) findViewById(R.id.layoutChoosePlan);
        layoutConfirm = (LinearLayout) findViewById(R.id.layoutConfirm);

        textDescription = (TextView) findViewById(R.id.textDescription);

        listSubscriptions = (RecyclerView) findViewById(R.id.listSubscription);
        adapter = new SubscriptionsAdapter();
        listSubscriptions.setAdapter(adapter);

        layoutLogin = (LinearLayout) findViewById(R.id.layoutLogin);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogin();
            }
        });

        layoutLoggedIn = (LinearLayout) findViewById(R.id.layoutLoggeIn);
        textConsumerEmail = (TextView) findViewById(R.id.textConsumerEmail);

        buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConfirm();
            }
        });

        buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });

        updateViews();
        bindViews();
        getSubscriptions();
    }

    private void initParameters(Bundle savedInstanceState) {
        Bundle args;
        if (savedInstanceState != null) {
            args = savedInstanceState;
        }
        else {
            args = getIntent().getExtras();
        }
        if (args != null) {
            mode = args.getInt(PARAMETERS_MODE, MODE_CHOOSE_PLAN);
            products = (ArrayList<HashMap<String, String>>) args.getSerializable(PARAMETERS_PRODUCTS);
        }
        else {
            mode = MODE_CHOOSE_PLAN;
        }
        if (products == null) {
            products = new ArrayList<>();
        }
    }

    // //////////
    // UI
    //
    private void bindViews() {
        textDescription.setText(R.string.app_name);
    }

    private void updateViews() {
        if (mode == MODE_CHOOSE_PLAN) {
            layoutChoosePlan.setVisibility(View.VISIBLE);
            layoutConfirm.setVisibility(View.GONE);
            if (ContentBrowser.getInstance(this).isUserLoggedIn()) {
                layoutLoggedIn.setVisibility(View.VISIBLE);
                layoutLogin.setVisibility(View.GONE);
                textConsumerEmail.setText(Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL));
            }
            else {
                layoutLoggedIn.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.VISIBLE);
            }
        }
        else if (mode == MODE_CONFIRM) {
            layoutChoosePlan.setVisibility(View.GONE);
            layoutConfirm.setVisibility(View.VISIBLE);
        }
    }

    private void showConfirm() {
        mode = MODE_CONFIRM;
        updateViews();
    }

    //
    // Actions
    //
    private void onLogin() {
        // TODO: Open Login screen
        finish();
    }

    private void onConfirm() {
        purchaseSubscription(selectedSubscription);
        finish();
    }

    private void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CREATE_LOGIN:
                if (resultCode == RESULT_OK) {
                    showConfirm();
                }
                break;
        }
    }

    // //////////
    // Data
    //
    public class SubscriptionsAdapter extends RecyclerView.Adapter<SubscriptionsAdapter.ViewHolder> {
        private List<SubscriptionItem> items;

        public SubscriptionsAdapter() {
            items = new ArrayList<>();
        }

        public void setData(List<SubscriptionItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscriptions_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.item = items.get(position);
            holder.textTitle.setText(holder.item.title);
            holder.textPrice.setText(String.format(getString(R.string.subscription_item_price), String.valueOf(holder.item.price), "month"));
//            holder.textDescription.setText(holder.item.description);

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedSubscription = holder.item;
                    if (ContentBrowser.getInstance(SubscriptionActivity.this).isUserLoggedIn()) {
                        showConfirm();
                    }
                    else {
                        Intent intent = new Intent(SubscriptionActivity.this, CreateLoginActivity.class);
                        startActivityForResult(intent, REQUEST_CREATE_LOGIN);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
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
//                textDescription = (TextView) view.findViewById(R.id.textDescription);
            }
        }
    }

    private void getSubscriptions() {
        List<SubscriptionItem> items = new ArrayList<>();

        for (HashMap<String, String> productData : products) {
            SubscriptionItem item = new SubscriptionItem();
            // TODO: Use constants for keys
            item.title = productData.get("Title");
            item.description = productData.get("Description");
            item.price = Float.valueOf(productData.get("Price"));
            item.sku = productData.get("SKU");
            items.add(item);
        }

        // Comment adding dummy item for release build
        if (items.isEmpty()) {
            SubscriptionItem item = new SubscriptionItem();
            item.title = "Monthly";
            item.description = "";
            item.price = 4.99f;
            item.sku = "com.zype.aftv.testsubscriptionmonthly";
            items.add(item);

            item = new SubscriptionItem();
            item.title = "Yearly";
            item.description = "";
            item.price = 7.99f;
            item.sku = "com.zype.aftv.testsubscriptionyearly";
            items.add(item);
        }

        adapter.setData(items);
    }

    private void purchaseSubscription(SubscriptionItem item) {
        ContentBrowser contentBrowser = ContentBrowser.getInstance(this);
        contentBrowser.updateSubscriptionSku(item.sku);
        contentBrowser.actionTriggered(this, contentBrowser.getLastSelectedContent(), ContentBrowser.CONTENT_ACTION_SUBSCRIPTION);
    }

}

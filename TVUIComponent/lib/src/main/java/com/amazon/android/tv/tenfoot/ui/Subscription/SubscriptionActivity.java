package com.amazon.android.tv.tenfoot.ui.Subscription;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.model.event.ProgressOverlayDismissEvent;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.Subscription.Model.SubscriptionItem;
import com.amazon.android.utils.Preferences;
import com.zype.fire.auth.ZypeAuthentication;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 07.08.2017.
 */

public class SubscriptionActivity extends Activity implements SubscriptionFragment.ISubscriptionSelectedListener {
    private static final String TAG = SubscriptionActivity.class.getName();

    public static final String PARAMETERS_MODE = "Mode";

    private static final int REQUEST_CREATE_LOGIN = 110;

    private LinearLayout layoutChoosePlan;
    private LinearLayout layoutConfirm;

    private TextView textDescription;
    private LinearLayout layoutLogin;
    private Button buttonLogin;
    private LinearLayout layoutLoggedIn;
    private TextView textConsumerEmail;

    private Button buttonConfirm;
    private Button buttonCancel;

    public static final int MODE_CHOOSE_PLAN = 1;
    public static final int MODE_CONFIRM = 2;
    private int mode;

    private SubscriptionItem selectedSubscription = null;

    private ContentBrowser contentBrowser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        contentBrowser = ContentBrowser.getInstance(this);

        initParameters(savedInstanceState);

        layoutChoosePlan = (LinearLayout) findViewById(R.id.layoutChoosePlan);
        layoutConfirm = (LinearLayout) findViewById(R.id.layoutConfirm);

        textDescription = (TextView) findViewById(R.id.textDescription);

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
        }
        else {
            mode = MODE_CHOOSE_PLAN;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
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
            if (contentBrowser.isUserLoggedIn()) {
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

    // //////////
    // Actions
    //
    private void onLogin() {
        contentBrowser.getAuthHelper()
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        if (Preferences.getLong(ZypeAuthentication.PREFERENCE_SUBSCRIPTION_COUNT) > 0) {
                            finish();
                        }
                        else {
                            updateViews();
                        }
                    }
                    else {
                        contentBrowser.getAuthHelper()
                                .authenticateWithActivity()
                                .subscribe(resultBundle -> {
                                    if (resultBundle != null) {
                                        if (resultBundle.getBoolean(AuthHelper.RESULT)) {
                                            if (Preferences.getLong(ZypeAuthentication.PREFERENCE_SUBSCRIPTION_COUNT) > 0) {
                                                finish();
                                            }
                                            else {
                                                updateViews();
                                            }
                                        }
                                        else {
                                            contentBrowser.getNavigator().runOnUpcomingActivity(() -> contentBrowser.getAuthHelper()
                                                    .handleErrorBundle(resultBundle));
                                        }
                                    }
                                    if (resultBundle != null && !resultBundle.getBoolean(AuthHelper.RESULT)) {
                                        contentBrowser.getNavigator().runOnUpcomingActivity(() -> contentBrowser.getAuthHelper()
                                                .handleErrorBundle(resultBundle));
                                    }
                                    else {
                                        updateViews();
                                    }
                                });
                    }
                });
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

        contentBrowser.handleOnActivityResult(this, requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CREATE_LOGIN:
                updateViews();
                if (resultCode == RESULT_OK) {
                    showConfirm();
                }
                break;
        }
    }

    @Override
    public void onSubscriptionSelected(SubscriptionItem item) {
        selectedSubscription = item;
        if (contentBrowser.isUserLoggedIn()) {
            showConfirm();
        }
        else {
            Intent intent = new Intent(SubscriptionActivity.this, CreateLoginActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_LOGIN);
        }
    }

    private void purchaseSubscription(SubscriptionItem item) {
        contentBrowser.updateSubscriptionSku(item.sku);
        contentBrowser.actionTriggered(this, contentBrowser.getLastSelectedContent(), ContentBrowser.CONTENT_ACTION_SUBSCRIPTION);
    }

}

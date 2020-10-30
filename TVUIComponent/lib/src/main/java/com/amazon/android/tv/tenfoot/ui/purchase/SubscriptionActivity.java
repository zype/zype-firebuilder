package com.amazon.android.tv.tenfoot.ui.purchase;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.model.event.ProgressOverlayDismissEvent;
import com.amazon.android.model.event.PurchaseEvent;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.purchase.Model.Consumer;
import com.amazon.android.tv.tenfoot.ui.purchase.Model.SubscriptionItem;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.android.utils.Preferences;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.auth.ZypeAuthentication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 07.08.2017.
 */

public class SubscriptionActivity extends Activity implements SubscriptionFragment.ISubscriptionSelectedListener,
                                                    ErrorDialogFragment.ErrorDialogFragmentListener {
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
    private ErrorDialogFragment dialogError = null;

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

//        Button buttonCancelSelectPlan = (Button) findViewById(R.id.buttonCancelSelectPlan);
//        buttonCancelSelectPlan.setOnClickListener(v -> closeScreen());

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
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
        super.onStop();
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
            if (ZypeConfiguration.isNativeSubscriptionEnabled(this)) {
                layoutLoggedIn.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.GONE);
            }
            else {
                if (contentBrowser.isUserLoggedIn()) {
                    layoutLoggedIn.setVisibility(View.VISIBLE);
                    layoutLogin.setVisibility(View.GONE);
                    textConsumerEmail.setText(Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL));
                } else {
                    layoutLoggedIn.setVisibility(View.GONE);
                    layoutLogin.setVisibility(View.VISIBLE);
                }
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

    private void closeScreen() {
        contentBrowser.onAuthenticationStatusUpdateEvent(new AuthHelper.AuthenticationStatusUpdateEvent(true));
        EventBus.getDefault().post(new AuthHelper.AuthenticationStatusUpdateEvent(true));

        setResult(RESULT_OK);
        buttonLogin.setEnabled(true);
        finish();
    }

    private void showSubscriptionSuccessDialog() {
        dialogError = ErrorDialogFragment.newInstance(this,
                ErrorUtils.ERROR_CATEGORY.ZYPE_CUSTOM,
            "", "",
//                getString(R.string.subscription_dialog_success_title),
//                String.format(getString(R.string.subscription_dialog_success_message), selectedSubscription.title),
                false,
                (errorDialogFragment, errorButtonType, errorCategory) -> {
                    if (errorDialogFragment != null) {
                        errorDialogFragment.dismiss();
                    }
                    if (ZypeConfiguration.isNativeSubscriptionEnabled(SubscriptionActivity.this)) {
                        closeScreen();
                    }
                    else {
                        // Relogin required after successful purchase validation with Marketplace connect
                        Consumer consumer = new Consumer();
                        consumer.email = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL);
                        consumer.password = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_PASSWORD);
                        login(consumer);
                    }
                });
        dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
    }

    // //////////
    // Actions
    //
    private void onLogin() {
        contentBrowser.getAuthHelper()
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        if (Preferences.getLong(ZypeAuthentication.PREFERENCE_CONSUMER_SUBSCRIPTION_COUNT) > 0) {
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
                                            if (Preferences.getLong(ZypeAuthentication.PREFERENCE_CONSUMER_SUBSCRIPTION_COUNT) > 0) {
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
                                    else {
                                        updateViews();
                                    }
                                });
                    }
                });
    }

    private void onConfirm() {
        purchaseSubscription(selectedSubscription);
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
        onConfirm();
//        if (ZypeConfiguration.isUniversalSubscriptionEnabled(SubscriptionActivity.this)
//            || ZypeConfiguration.marketplaceConnectSvodEnabled(SubscriptionActivity.this)) {
//            if (!contentBrowser.isUserLoggedIn()) {
//                Intent intent = new Intent(SubscriptionActivity.this, CreateLoginActivity.class);
//                startActivityForResult(intent, REQUEST_CREATE_LOGIN);
//                return;
//            }
//        }
//        showConfirm();
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
        if (dialogError  != null) {
            dialogError .dismiss();
        }
    }

    private void purchaseSubscription(SubscriptionItem item) {
        Log.i(TAG, "purchaseSubscription(): sku=" + item.sku + ", planId=" + item.planId);
        Bundle extras = new Bundle();
        extras.putString("PlanId", item.planId);
        contentBrowser.getPurchaseHelper().setPurchaseExtras(extras);
        contentBrowser.updateSubscriptionSku(item.sku);
        contentBrowser.actionTriggered(this,
                contentBrowser.getLastSelectedContent(),
                ContentBrowser.CONTENT_ACTION_SUBSCRIPTION,
                null, null);
    }

    /**
     * Event bus listener method to detect purchase broadcast.
     *
     * @param event Broadcast event for progress overlay dismiss.
     */
    @Subscribe
    public void onSubscriptionPurchaseEvent(PurchaseEvent event) {
        if (event.getExtras().getBoolean(PurchaseHelper.RESULT)) {
            if (event.getExtras().getBoolean(PurchaseHelper.RESULT_VALIDITY)) {
                showSubscriptionSuccessDialog();
//                if (ZypeConfiguration.isNativeSubscriptionEnabled(SubscriptionActivity.this)) {
//                    closeScreen();
//                } else {
//                    // Relogin required after successful purchase validation with Marketplace connect
//                    Consumer consumer = new Consumer();
//                    consumer.email = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL);
//                    consumer.password = Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_PASSWORD);
//                    login(consumer);
//                }
            } else {
                dialogError = ErrorDialogFragment.newInstance(SubscriptionActivity.this, ErrorUtils.ERROR_CATEGORY.ZYPE_VERIFY_SUBSCRIPTION_ERROR, SubscriptionActivity.this);
                dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
            }
        }
    }

    private void login(Consumer consumer) {
        if (NetworkUtils.isConnectedToNetwork(this)) {
            (new AsyncTask<Void, Void, Map>() {
                @Override
                protected Map<String, Object> doInBackground(Void... params) {
                    return ZypeAuthentication.getAccessToken(consumer.email, consumer.password);
                }

                @Override
                protected void onPostExecute(Map response) {
                    super.onPostExecute(response);
                    if (response != null) {
                        // Successful login.
                        ZypeAuthentication.saveAccessToken(response);
                        closeScreen();
                    }
                    else {
                        dialogError = ErrorDialogFragment.newInstance(SubscriptionActivity.this, ErrorUtils.ERROR_CATEGORY.ZYPE_VERIFY_SUBSCRIPTION_ERROR, SubscriptionActivity.this);
                        dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                    }
                }
            }).execute();
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(SubscriptionActivity.this, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR, SubscriptionActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }
}

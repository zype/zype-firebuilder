package com.amazon.android.tv.tenfoot.ui.Subscription;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.Subscription.Model.Consumer;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.android.utils.Preferences;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.auth.ZypeAuthentication;
import com.zype.fire.auth.ZypeLoginActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.amazon.android.ui.fragments.LogoutSettingsFragment.LOGOUT_BUTTON_BROADCAST_INTENT_ACTION;

/**
 * Created by Evgeny Cherkasov on 24.09.2018.
 */

public class SubscriptionSplashActivity extends Activity implements
        ErrorDialogFragment.ErrorDialogFragmentListener,
        ZypeAuthentication.IAuthenticationActivityParameters {
    private static final String TAG = SubscriptionSplashActivity.class.getName();

    private static final int REQUEST_CREATE_LOGIN = 110;

    private Button buttonSubscribe;
    private Button buttonSignIn;
    private LinearLayout layoutSignedIn;
    private TextView textConsumerEmail;

    private ContentBrowser contentBrowser;
    private ErrorDialogFragment dialogError = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_splash);

        contentBrowser = ContentBrowser.getInstance(this);

        buttonSubscribe = (Button) findViewById(R.id.buttonSubscribe);
        buttonSubscribe.setOnClickListener(v -> onSubscribe());

        buttonSignIn = (Button) findViewById(R.id.buttonSignIn);
        buttonSignIn.setOnClickListener(v -> onSignIn());

        layoutSignedIn = (LinearLayout) findViewById(R.id.layoutSignedIn);
        textConsumerEmail = (TextView) findViewById(R.id.textConsumerEmail);
        Button buttonSignOut = (Button) findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(v -> {
            onSignOut();
        });

        updateViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
//        EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
        super.onStop();
    }

    private void updateViews() {
        if (contentBrowser.isUserLoggedIn()) {
            layoutSignedIn.setVisibility(View.VISIBLE);
            buttonSignIn.setVisibility(View.GONE);
            textConsumerEmail.setText(Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL));
        }
        else {
            layoutSignedIn.setVisibility(View.GONE);
            buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void closeScreen() {
//        EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
//        contentBrowser.onAuthenticationStatusUpdateEvent(new AuthHelper.AuthenticationStatusUpdateEvent(true));
//        EventBus.getDefault().post(new AuthHelper.AuthenticationStatusUpdateEvent(true));
        setResult(RESULT_OK);
        finish();
    }

    private void createAccount() {
        Intent intent = new Intent(SubscriptionSplashActivity.this, CreateLoginActivity.class);
        startActivityForResult(intent, REQUEST_CREATE_LOGIN);
    }

    private void switchToSubscriptionScreen() {
        ContentBrowser.getInstance(this).switchToSubscriptionScreen(new Bundle());
        finish();
    }

    @Override
    public Bundle getAuthenticationActivityParameters() {
        Bundle parameters = new Bundle();
        parameters.putString(ZypeLoginActivity.PARAMETERS_MESSAGE, getString(R.string.sign_in_message));
        return parameters;
    }

    private void showSignInSuccessDialog() {
        dialogError = ErrorDialogFragment.newInstance(this,
                ErrorUtils.ERROR_CATEGORY.ZYPE_CUSTOM,
                getString(R.string.sign_in_dialog_success_title),
                String.format(getString(R.string.sign_in_dialog_success_message), Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL)),
                false,
                (errorDialogFragment, errorButtonType, errorCategory) -> {
                    if (errorDialogFragment != null) {
                        errorDialogFragment.dismiss();
                    }
                    if (contentBrowser.isUserSubscribed()) {
                        closeScreen();
                    }
                    else {
                        updateViews();
                    }
                });
        dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
    }

    // //////////
    // Actions
    //
    private void onSubscribe() {
        if (!contentBrowser.isUserLoggedIn()) {
            createAccount();
        }
        else {
            switchToSubscriptionScreen();
        }
    }

    private void onSignIn() {
        contentBrowser.getAuthHelper()
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        showSignInSuccessDialog();
//                        closeScreen();
                    }
                    else {
                        contentBrowser.getAuthHelper()
                                .authenticateWithActivity()
                                .subscribe(resultBundle -> {
                                    if (resultBundle != null) {
                                        if (resultBundle.getBoolean(AuthHelper.RESULT)) {
                                            showSignInSuccessDialog();
//                                            closeScreen();
                                        }
                                        else {
                                            if (resultBundle.containsKey("SignUp")) {
                                                onSubscribe();
                                            }
                                            else {
                                                contentBrowser.getNavigator().runOnUpcomingActivity(() -> contentBrowser.getAuthHelper()
                                                        .handleErrorBundle(resultBundle,
                                                                (errorDialogFragment, errorButtonType, errorCategory) -> onSignIn()));
                                            }
                                        }
                                    }
                                    else {
                                    }
                                });
                    }
                });
    }

    private void onSignOut() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent().setAction
                        (LOGOUT_BUTTON_BROADCAST_INTENT_ACTION));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        contentBrowser.handleOnActivityResult(this, requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CREATE_LOGIN:
                if (resultCode == RESULT_OK) {
                    switchToSubscriptionScreen();
                }
                break;
        }
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

    private void checkVideoEntitlement() {
        if (ZypeConfiguration.isUniversalTVODEnabled(this)) {
            Content content = contentBrowser.getLastSelectedContent();
            ContentContainer playlist = ContentBrowser.getInstance(this)
                    .getRootContentContainer()
                    .findContentContainerById(content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
            if (content.getExtraValueAsBoolean(Content.EXTRA_PURCHASE_REQUIRED)
                    || (playlist != null && playlist.getExtraValueAsBoolean(ContentContainer.EXTRA_PURCHASE_REQUIRED))) {
                String accessToken = Preferences.getString(ZypeAuthentication.ACCESS_TOKEN);
                HashMap<String, String> params = new HashMap<>();
                params.put(ZypeApi.ACCESS_TOKEN, accessToken);
                ZypeApi.getInstance().getApi().checkVideoEntitlement(content.getId(), params).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.i(TAG, "checkVideoEntitlement(): code=" + response.code());
                        if (response.isSuccessful()) {
                            content.setExtraValue(Content.EXTRA_ENTITLED, true);
                            closeScreen();
                            contentBrowser.actionTriggered(SubscriptionSplashActivity.this,
                                    contentBrowser.getLastSelectedContent(),
                                    ContentBrowser.CONTENT_ACTION_WATCH_NOW,
                                    null,
                                    null);
                        }
                        else {
                            content.setExtraValue(Content.EXTRA_ENTITLED, false);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "checkVideoEntitlement(): failed");
                    }
                });
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
                        dialogError = ErrorDialogFragment.newInstance(SubscriptionSplashActivity.this, ErrorUtils.ERROR_CATEGORY.ZYPE_VERIFY_SUBSCRIPTION_ERROR, SubscriptionSplashActivity.this);
                        dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                    }
                }
            }).execute();
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(SubscriptionSplashActivity.this, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR, SubscriptionSplashActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }

    /**
     * Event bus listener method to detect sign in status
     *
     * @param event
     */
    @Subscribe
    public void onAuthenticationStatusUpdateEvent(AuthHelper.AuthenticationStatusUpdateEvent event) {
        contentBrowser.onAuthenticationStatusUpdateEvent(event);
        updateViews();
    }

}

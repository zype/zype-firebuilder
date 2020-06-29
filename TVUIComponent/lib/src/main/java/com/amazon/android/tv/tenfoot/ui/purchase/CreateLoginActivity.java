package com.amazon.android.tv.tenfoot.ui.purchase;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.auth.AuthenticationConstants;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeSettings;
import com.amazon.android.tv.tenfoot.ui.purchase.Model.Consumer;
import com.zype.fire.auth.ZypeAuthentication;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Evgeny Cherkasov on 07.08.2017.
 */

public class CreateLoginActivity extends Activity implements ErrorDialogFragment.ErrorDialogFragmentListener {
    private static final String TAG = CreateLoginActivity.class.getName();

    public static final String PARAMETERS_SKU = "SKU";

    private CheckBox checkboxTermsOfService;
    private EditText editEmail;
    private EditText editPassword;
    private Button buttonSignUp;
    private Button buttonLogin;

    private ContentBrowser contentBrowser;
    private ErrorDialogFragment dialogError = null;
    private boolean registration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_login);

        contentBrowser = ContentBrowser.getInstance(this);

        editEmail = (EditText) findViewById(R.id.editEmail);
        editPassword = (EditText) findViewById(R.id.editPassword);

        checkboxTermsOfService = (CheckBox) findViewById(R.id.checkboxTermsOfService);

        buttonSignUp = (Button) findViewById(R.id.buttonSignUp);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignUp();
            }
        });
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogin();
            }
        });

        registration = getIntent().getBooleanExtra("registration", false);

        updateViews();
    }

    // //////////
    // UI
    //
    private void bindViews() {
    }

    private void updateViews() {
        if (contentBrowser.isCreateAccountTermsOfServiceRequired()) {
            checkboxTermsOfService.setVisibility(View.VISIBLE);
        }
        else {
            checkboxTermsOfService.setVisibility(View.GONE);
        }
    }

    //
    // Actions
    //
    private void onSignUp() {
        createConsumer();
    }

    private void onLogin() {
        ContentBrowser contentBrowser = ContentBrowser.getInstance(this);
        contentBrowser.getAuthHelper()
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        contentBrowser.updateUserSubscribed();
                        finish();
                    }
                    else {
                        contentBrowser.getAuthHelper()
                                .authenticateWithActivity()
                                .subscribe(resultBundle -> {
                                    if (resultBundle != null) {
                                        if (!resultBundle.getBoolean(AuthHelper.RESULT)) {
                                            contentBrowser.getNavigator().runOnUpcomingActivity(() -> contentBrowser.getAuthHelper()
                                                    .handleErrorBundle(resultBundle));
                                        } else {
                                            contentBrowser.onAuthenticationStatusUpdateEvent(new AuthHelper.AuthenticationStatusUpdateEvent(true, registration));
                                            EventBus.getDefault().post(new AuthHelper.AuthenticationStatusUpdateEvent(true));
                                            setResult(RESULT_OK);
                                            finish();
                                        }
                                    }
                                });
                    }
                });
    }

    private void createConsumer() {
        if (contentBrowser.isCreateAccountTermsOfServiceRequired()) {
            if (!checkboxTermsOfService.isChecked()) {
                dialogError = ErrorDialogFragment.newInstance(CreateLoginActivity.this,
                        ErrorUtils.ERROR_CATEGORY.ZYPE_CUSTOM, getString(R.string.create_login_error_tos),
                        CreateLoginActivity.this);
                dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                return;
            }
        }
        Consumer consumer = getViewModel();
        if (validate(consumer)) {
            requestCreateConsumer(consumer);
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(CreateLoginActivity.this,
                    ErrorUtils.ERROR_CATEGORY.ZYPE_CUSTOM, getString(R.string.create_login_error_credentials),
                    CreateLoginActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        contentBrowser.handleOnActivityResult(this, requestCode, resultCode, data);
        switch (requestCode) {
        }
    }

    /**
     * Set the corresponding extras & finish this activity
     *
     * @param throwable contains detailed info about the cause of error
     * @param category  error cause
     */
    private void setResultAndReturn(Throwable throwable, String category) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(AuthenticationConstants.ERROR_CATEGORY, category);
        bundle.putSerializable(AuthenticationConstants.ERROR_CAUSE, throwable);
        setResult(RESULT_CANCELED, intent.putExtra(AuthenticationConstants.ERROR_BUNDLE, bundle));
        finish();
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
        if (dialogError != null) {
            dialogError.dismiss();
        }
    }

    // //////////
    // Data
    //
    private Consumer getViewModel() {
        Consumer result = new Consumer();
        result.email = editEmail.getText().toString();
        result.password = editPassword.getText().toString();
        return result;
    }

    private boolean validate(Consumer consumer) {
        boolean result = true;
        if (TextUtils.isEmpty(consumer.email)) {
            result = false;
//            layoutEmail.setError(getString(R.string.create_login_email_error_empty));
//            layoutEmail.setErrorEnabled(true);
        }
        else {
            if (!isEmailValid(consumer.email)) {
                result = false;
//                layoutEmail.setError(getString(R.string.create_login_email_error_invalid));
//                layoutEmail.setErrorEnabled(true);
            }
            else {
//                layoutEmail.setErrorEnabled(false);
            }
        }
        if (TextUtils.isEmpty(consumer.password)) {
            result = false;
//            layoutPassword.setError(getString(R.string.create_login_password_error_empty));
//            layoutPassword.setErrorEnabled(true);
        }
        else {
//            layoutPassword.setErrorEnabled(false);
        }
        return result;
    }

    private boolean isEmailValid(String value) {
        // TODO: Add implementation
        return true;
    }

    private void requestCreateConsumer(Consumer consumer) {
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
        HashMap<String, String> fieldParams = new HashMap<>();
        fieldParams.put(ZypeApi.CONSUMER_EMAIL, consumer.email);
        fieldParams.put(ZypeApi.CONSUMER_PASSWORD, consumer.password);
        ZypeApi.getInstance().getApi().createConsumer(queryParams, fieldParams).enqueue(new Callback<ConsumerResponse>() {
            @Override
            public void onResponse(Call<ConsumerResponse> call, Response<ConsumerResponse> response) {
                if (response.isSuccessful()) {
                    login(consumer);
                    Log.d(TAG, "requestCreateConsumer(): success");
                }
                else {
                    Log.e(TAG, "requestCreateConsumer(): failed");
                    dialogError = ErrorDialogFragment.newInstance(CreateLoginActivity.this, ErrorUtils.ERROR_CATEGORY.ZYPE_CREATE_CONSUMER_ERROR, CreateLoginActivity.this);
                    dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                }
            }

            @Override
            public void onFailure(Call<ConsumerResponse> call, Throwable t) {
                Log.e(TAG, "requestCreateConsumer(): failed");
                dialogError = ErrorDialogFragment.newInstance(CreateLoginActivity.this, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR, CreateLoginActivity.this);
                dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
            }
        });
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

                        contentBrowser.onAuthenticationStatusUpdateEvent(new AuthHelper.AuthenticationStatusUpdateEvent(true, registration));
                        EventBus.getDefault().post(new AuthHelper.AuthenticationStatusUpdateEvent(true));
                        setResult(RESULT_OK);
                        buttonLogin.setEnabled(true);
                        finish();
                    }
                    else {
                        buttonLogin.setEnabled(true);
                        // There was an error authenticating the user entered token.
                        setResultAndReturn(null, AuthenticationConstants.AUTHENTICATION_ERROR_CATEGORY);
                    }
                }
            }).execute();
        }
        else {
            setResultAndReturn(null, AuthenticationConstants.NETWORK_ERROR_CATEGORY);
        }
    }
}

package com.zype.fire.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.utils.NetworkUtils;
import com.amazon.auth.AuthenticationConstants;
import com.zype.fire.api.Model.DevicePinResponse;
import com.zype.fire.api.Util.AdMacrosHelper;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;

import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 12.04.2017.
 */

public class ZypeLoginActivity extends Activity {
    private final static String TAG = ZypeLoginActivity.class.getSimpleName();

    public static final String PARAMETERS_MESSAGE = "Message";

    private LinearLayout layoutMethod;
    private Button buttonLinkDevice;
    private Button buttonEmail;

    private LinearLayout layoutPin;
    private TextView textDeviceLinkingUrl;
    private TextView textPin;
    private Button buttonDeviceLinked;

    private LinearLayout layoutEmail;
    private TextView textMessage;
    private EditText editUsername;
    private EditText editPassword;
    private Button buttonLogin;

    private String message;
    private String pin;

    private int mode;
    private static final int MODE_SELECT_METHOD = 0;
    private static final int MODE_DEVICE_LINKING = 1;
    private static final int MODE_SIGN_IN_WITH_EMAIL = 2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_zype_login);

        layoutMethod = (LinearLayout) findViewById(R.id.layoutMethod);
        buttonLinkDevice = (Button) findViewById(R.id.buttonLinkDevice);
        buttonLinkDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = MODE_DEVICE_LINKING;
                getDevicePin();
                updateViews();
            }
        });
        buttonEmail = (Button) findViewById(R.id.buttonEmail);
        buttonEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = MODE_SIGN_IN_WITH_EMAIL;
                updateViews();
            }
        });

        layoutPin = (LinearLayout) findViewById(R.id.layoutPin);
        textDeviceLinkingUrl = (TextView) findViewById(R.id.textDeviceLinkingUrl);
        textPin = (TextView) findViewById(R.id.textPin);
        buttonDeviceLinked = (Button) findViewById(R.id.buttonDeviceLinked);
        buttonDeviceLinked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccessTokenWithPin();
            }
        });

        layoutEmail = (LinearLayout) findViewById(R.id.layoutEmail);
        textMessage = (TextView) findViewById(R.id.textMessage);
        editUsername = (EditText) findViewById(R.id.editUsername);
        editPassword = (EditText) findViewById(R.id.editPassword);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccessToken();
            }
        });
//        Button buttonSignUp = (Button) findViewById(R.id.buttonSignUp);
//        buttonSignUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onSignUp();
//            }
//        });

        initParameters(savedInstanceState);
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
            message = args.getString(PARAMETERS_MESSAGE);
        }
        if (ZypeConfiguration.isDeviceLinkingEnabled(this)) {
            mode = MODE_SELECT_METHOD;
        }
        else {
            mode = MODE_SIGN_IN_WITH_EMAIL;
        }
    }

    // Actions

    private void onSignUp() {
        Intent data = new Intent();
        data.putExtra("SignUp", true);
        setResult(RESULT_CANCELED, data);
        finish();
    }

    // //////////
    // UI
    //
    private void bindViews() {
//        textMessage.setText(getString(R.string.sign_in_message));
        textPin.setText(pin);

        updateViews();
    }

    private void updateViews() {
        switch (mode) {
            case MODE_SELECT_METHOD:
                layoutMethod.setVisibility(View.VISIBLE);
                layoutPin.setVisibility(View.GONE);
                layoutEmail.setVisibility(View.GONE);
                break;
            case MODE_DEVICE_LINKING:
                layoutMethod.setVisibility(View.GONE);
                layoutPin.setVisibility(View.VISIBLE);
                layoutEmail.setVisibility(View.GONE);
                break;
            case MODE_SIGN_IN_WITH_EMAIL:
                layoutMethod.setVisibility(View.GONE);
                layoutPin.setVisibility(View.GONE);
                layoutEmail.setVisibility(View.VISIBLE);
                break;
        }
    }

    // //////////
    // Data
    //
    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void getAccessToken() {
        buttonLogin.setEnabled(false);
        final String username = editUsername.getText().toString();
        final String password = editPassword.getText().toString();

        // Validation
        if (!isEmailValid(username)) {
            buttonLogin.setEnabled(true);
        }

        if (NetworkUtils.isConnectedToNetwork(this)) {
            (new AsyncTask<Void, Void, Map>() {
                @Override
                protected Map<String, Object> doInBackground(Void... params) {
                    return ZypeAuthentication.getAccessToken(username, password);
                }

                @Override
                protected void onPostExecute(Map response) {
                    super.onPostExecute(response);
                    if (response != null) {
                        // Successful login.
                        ZypeAuthentication.saveAccessToken(response);

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

    public void getAccessTokenWithPin() {
        if (NetworkUtils.isConnectedToNetwork(this)) {
            (new AsyncTask<Void, Void, Map>() {
                @Override
                protected Map<String, Object> doInBackground(Void... params) {
                    String deviceId = AdMacrosHelper.getAdvertisingId(ZypeLoginActivity.this);
                    // Check if the device is linked
                    DevicePinResponse responseDevicePin = ZypeApi.getInstance().getDevicePin(deviceId);
                    if (responseDevicePin != null) {
                        if (responseDevicePin.data.linked) {
                            // If linked get access token with device pin
                            return ZypeAuthentication.getAccessTokenWithPin(deviceId, pin);
                        }
                        else {
                            return null;
                        }
                    }
                    else {
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Map response) {
                    super.onPostExecute(response);
                    if (response != null) {
                        // Successful login.
                        ZypeAuthentication.saveAccessToken(response);

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

    public void getDevicePin() {
        if (NetworkUtils.isConnectedToNetwork(this)) {
            (new AsyncTask<Void, Void, DevicePinResponse>() {
                @Override
                protected DevicePinResponse doInBackground(Void... params) {
                    return ZypeApi.getInstance().createDevicePin(AdMacrosHelper.getAdvertisingId(ZypeLoginActivity.this));
                }

                @Override
                protected void onPostExecute(DevicePinResponse response) {
                    super.onPostExecute(response);
                    if (response != null) {
                        pin = response.data.pin;
                        bindViews();
                    }
                    else {
                        // Error getting device pin.
                        setResultAndReturn(null, AuthenticationConstants.AUTHENTICATION_ERROR_CATEGORY);
                    }
                }
            }).execute();
        }
        else {
            setResultAndReturn(null, AuthenticationConstants.NETWORK_ERROR_CATEGORY);
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

}

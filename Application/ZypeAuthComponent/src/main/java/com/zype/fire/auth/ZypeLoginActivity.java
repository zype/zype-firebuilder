package com.zype.fire.auth;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazon.android.module.*;
import com.amazon.android.module.BuildConfig;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.android.utils.Preferences;
import com.amazon.auth.AuthenticationConstants;
import com.amazon.auth.IAuthentication;
import com.zype.fire.api.Model.AccessTokenInfoResponse;
import com.zype.fire.api.Model.AccessTokenResponse;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.ZypeApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 12.04.2017.
 */

public class ZypeLoginActivity extends Activity {
    private final static String TAG = ZypeLoginActivity.class.getSimpleName();

//    private TextInputLayout layoutUsername;
//    private TextInputLayout layoutPassword;
    private EditText editUsername;
    private EditText editPassword;
    private Button buttonLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_zype_login);

//        layoutUsername = (TextInputLayout) findViewById(R.id.layoutUsername);
//        layoutPassword = (TextInputLayout) findViewById(R.id.layoutPassword);
        editUsername = (EditText) findViewById(R.id.editUsername);
        editPassword = (EditText) findViewById(R.id.editPassword);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAuthenticationToken();
            }
        });
    }

    // //////////
    // Data
    //
    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void getAuthenticationToken() {
        buttonLogin.setEnabled(false);
//        final String username = layoutUsername.getEditText().getText().toString();
//        final String password = layoutPassword.getEditText().getText().toString();
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

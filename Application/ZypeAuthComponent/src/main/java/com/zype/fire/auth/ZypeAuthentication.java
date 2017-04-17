package com.zype.fire.auth;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.amazon.android.module.*;
import com.amazon.android.utils.Preferences;
import com.amazon.auth.AuthenticationConstants;
import com.amazon.auth.IAuthentication;
import com.zype.fire.api.Model.AccessTokenInfoResponse;
import com.zype.fire.api.Model.AccessTokenResponse;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.ZypeApi;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 11.04.2017.
 */

public class ZypeAuthentication implements IAuthentication {
    private static final String TAG = ZypeAuthentication.class.getSimpleName();

    public static final String PREFERENCE_ACCESS_TOKEN_CREATED_AT = "ZypeAccessTokenCreatedAt";
    public static final String PREFERENCE_ACCESS_TOKEN_EXPIRES_IN = "ZypeAccessTokenExpiresIn";
    public static final String PREFERENCE_REFRESH_TOKEN = "ZypeRefreshToken";
    public static final String PREFERENCE_RESOURCE_OWNER_ID = "ZypeResourceOwnerId";
    public static final String PREFERENCE_SUBSCRIPTION_COUNT = "ZypeSubscriptionCount";

    private static final String RESPONSE_ACCESS_TOKEN = "ResponseAccessToken";
    private static final String RESPONSE_ACCESS_TOKEN_INFO = "ResponseAccessTokenInfo";
    private static final String RESPONSE_CONSUMER = "ResponseConsumer";

    /**
     * The access token is used to see if the user is authenticated or not.
     */
    private String accessToken;

    /**
     * Initialize the authentication framework.
     *
     * @param context Context The application context.
     */
    @Override
    public void init(Context context) {
        accessToken = Preferences.getString(IAuthentication.ACCESS_TOKEN);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Previous access token is: " + accessToken);
        }
    }

    /**
     * Check if authentication can be done later.
     *
     * @return Return true if login process can be skipped.
     */
    @Override
    public boolean isAuthenticationCanBeDoneLater() {
        return true;
    }

    /**
     * This returns the intent that should be started for authentication of the user.
     * Start the intent from an activity for result.
     * If activity result is RESULT_OK, start playback.
     *
     * @param context The context required to create the intent.
     * @return The intent for the authentication activity.
     */
    @Override
    public Intent getAuthenticationActivityIntent(Context context) {
        return new Intent(context, ZypeLoginActivity.class);
    }

    /**
     * This method checks if the user is already logged in.
     *
     * @param context         The context to check if user is logged in.
     * @param responseHandler The callback interface
     */
    @Override
    public void isUserLoggedIn(Context context, ResponseHandler responseHandler) {
        accessToken = Preferences.getString(IAuthentication.ACCESS_TOKEN);
        final Bundle bundle = new Bundle();
        if (TextUtils.isEmpty(accessToken)) {
            // Access token is null or empty so we know its not valid.
            Log.d(TAG, "isUserLoggedIn(): false");
            bundle.putString(ResponseHandler.MESSAGE, "Access token is empty");
            responseHandler.onFailure(bundle);
        }
        else {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime - Preferences.getLong(PREFERENCE_ACCESS_TOKEN_CREATED_AT) * 1000 > Preferences.getLong(PREFERENCE_ACCESS_TOKEN_EXPIRES_IN) * 1000) {
                handleRefreshToken(responseHandler);
            }
            else {
                responseHandler.onSuccess(bundle);
            }
        }
    }

    /**
     * This method checks if the resource is authorized for playback.
     *
     * @param context         The context to check for authorization.
     * @param resourceId      The id of the resource to verify authorization.
     * @param responseHandler The callback interface
     */
    @Override
    public void isResourceAuthorized(Context context, String resourceId, ResponseHandler responseHandler) {
        responseHandler.onSuccess(new Bundle());
    }

    /**
     * This method will log out the user. Resets or deletes any user login information.
     *
     * @param context         The context to logout the user.
     * @param responseHandler The callback interface
     */
    @Override
    public void logout(Context context, ResponseHandler responseHandler) {
        accessToken = "";
        Preferences.setString(IAuthentication.ACCESS_TOKEN, accessToken);
        Preferences.setLong(ZypeAuthentication.PREFERENCE_ACCESS_TOKEN_CREATED_AT, 0);
        Preferences.setLong(ZypeAuthentication.PREFERENCE_ACCESS_TOKEN_EXPIRES_IN, 0);
        Preferences.setString(ZypeAuthentication.PREFERENCE_REFRESH_TOKEN, "");
        Preferences.setString(ZypeAuthentication.PREFERENCE_RESOURCE_OWNER_ID, "");
        Preferences.setLong(ZypeAuthentication.PREFERENCE_SUBSCRIPTION_COUNT, 0);
        responseHandler.onSuccess(new Bundle());
    }

    /**
     * This method cancels all requests
     */
    @Override
    public void cancelAllRequests() {
    }

    public static Map<String, Object> getAccessToken(String username, String password) {
        AccessTokenResponse responseAccessToken = ZypeApi.getInstance().retrieveAccessToken(username, password);
        AccessTokenInfoResponse responseAccessTokenInfo = null;
        ConsumerResponse responseConsumer = null;
        if (responseAccessToken != null) {
            responseAccessTokenInfo = ZypeApi.getInstance().getAccessTokenInfo(responseAccessToken.getAccessToken());
            if (responseAccessTokenInfo != null) {
                responseConsumer = ZypeApi.getInstance().getConsumer(responseAccessTokenInfo.resourceOwnerId, responseAccessToken.getAccessToken());
            }
        }
        else {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put(RESPONSE_ACCESS_TOKEN, responseAccessToken);
        result.put(RESPONSE_ACCESS_TOKEN_INFO, responseAccessTokenInfo);
        result.put(RESPONSE_CONSUMER, responseConsumer);
        return result;
    }

    private void handleRefreshToken(final ResponseHandler responseHandler) {
        (new AsyncTask<Void, Void, Map>() {
            @Override
            protected void onPostExecute(Map response) {
                super.onPostExecute(response);
                if (response != null) {
                    // Successful refresh token.
                    saveAccessToken(response);
                    responseHandler.onSuccess(new Bundle());
                }
                else {
                    responseHandler.onFailure(new Bundle());
                }
            }

            @Override
            protected Map<String, Object> doInBackground(Void... params) {
                return refreshToken(responseHandler);
            }
        }).execute();
    }

    private Map<String, Object> refreshToken(ResponseHandler responseHandler) {
        AccessTokenResponse responseAccessToken = ZypeApi.getInstance().refreshAccessToken(Preferences.getString(PREFERENCE_REFRESH_TOKEN));
        AccessTokenInfoResponse responseAccessTokenInfo = null;
        ConsumerResponse responseConsumer = null;
        if (responseAccessToken != null) {
            responseAccessTokenInfo = ZypeApi.getInstance().getAccessTokenInfo(responseAccessToken.getAccessToken());
            if (responseAccessTokenInfo != null) {
                responseConsumer = ZypeApi.getInstance().getConsumer(responseAccessTokenInfo.resourceOwnerId, responseAccessToken.getAccessToken());
            }
        }
        else {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put(RESPONSE_ACCESS_TOKEN, responseAccessToken);
        result.put(RESPONSE_ACCESS_TOKEN_INFO, responseAccessTokenInfo);
        result.put(RESPONSE_CONSUMER, responseConsumer);
        return result;
    }

    public static void saveAccessToken(Map<String, Object> data) {
        String accessToken = ((AccessTokenResponse) data.get(RESPONSE_ACCESS_TOKEN)).getAccessToken();
        String refreshToken = ((AccessTokenResponse) data.get(RESPONSE_ACCESS_TOKEN)).getRefreshToken();
        long createdAt = 0;
        long expiresIn = 0;
        String resourceOwnerId = "";
        if (data.get(RESPONSE_ACCESS_TOKEN_INFO) != null) {
            createdAt = ((AccessTokenInfoResponse) data.get(RESPONSE_ACCESS_TOKEN_INFO)).createdAt;
            expiresIn = ((AccessTokenInfoResponse) data.get(RESPONSE_ACCESS_TOKEN_INFO)).expiresInSeconds;
            resourceOwnerId = ((AccessTokenInfoResponse) data.get(RESPONSE_ACCESS_TOKEN_INFO)).resourceOwnerId;
        }
        int subscriptionCount = 0;
        if (data.get(RESPONSE_CONSUMER) != null) {
            subscriptionCount = ((ConsumerResponse) data.get(RESPONSE_CONSUMER)).consumerData.subscriptionCount;
        }

        if (com.amazon.android.module.BuildConfig.DEBUG) {
            Log.d(TAG, "Storing access token: " + accessToken);
        }
        Preferences.setString(IAuthentication.ACCESS_TOKEN, accessToken);
        Preferences.setLong(ZypeAuthentication.PREFERENCE_ACCESS_TOKEN_CREATED_AT, createdAt);
        Preferences.setLong(ZypeAuthentication.PREFERENCE_ACCESS_TOKEN_EXPIRES_IN, expiresIn);
        Preferences.setString(ZypeAuthentication.PREFERENCE_REFRESH_TOKEN, refreshToken);
        Preferences.setString(ZypeAuthentication.PREFERENCE_RESOURCE_OWNER_ID, resourceOwnerId);
        Preferences.setLong(ZypeAuthentication.PREFERENCE_SUBSCRIPTION_COUNT, subscriptionCount);
    }
}

package com.zype.fire.api;

import android.content.Context;

import com.zype.fire.api.Model.AccessTokenInfoResponse;
import com.zype.fire.api.Model.AccessTokenResponse;
import com.zype.fire.api.Model.ConsumerResponse;
import com.zype.fire.api.Model.PlayerResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Evgeny Cherkasov on 10.04.2017.
 */

public class ZypeApi {
    private static final String BASE_URL = "https://api.zype.com/";
    private static final String CLIENT_GRAND_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";

    private static ZypeApi instance;
    private static IZypeApi apiImpl;

    private ZypeApi() {}

    public static synchronized ZypeApi getInstance() {
        if (instance == null) {
            instance = new ZypeApi();

            // Needs to log retrofit calls
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            apiImpl = retrofit.create(IZypeApi.class);
        }
        return instance;
    }

    public IZypeApi getApi() {
        return apiImpl;
    }

    public AccessTokenResponse retrieveAccessToken(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        // TODO: Move client id and secret values to configuration file
        params.put("client_id", "62f1d247b4c5e77b6111d9a9ed8b3b64bab6be66cc8b7513a928198083cd1c72");
        params.put("client_secret", "06f45687da00bbe3cf51dddc7dbd7a288d1c852cf0b9a6e76e25bb115dcf872c");
        params.put("grant_type", "password");
        try {
            Response response = apiImpl.retrieveAccessToken(params).execute();
            if (response.isSuccessful()) {
                return (AccessTokenResponse) response.body();
            }
            else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        Map<String, String> params = new HashMap<>();
        params.put("refresh_token", refreshToken);
        // TODO: Move client id and secret values to configuration file
        params.put("client_id", "62f1d247b4c5e77b6111d9a9ed8b3b64bab6be66cc8b7513a928198083cd1c72");
        params.put("client_secret", "06f45687da00bbe3cf51dddc7dbd7a288d1c852cf0b9a6e76e25bb115dcf872c");
        params.put("grant_type", "refresh_token");
        try {
            Response response = apiImpl.retrieveAccessToken(params).execute();
            if (response.isSuccessful()) {
                return (AccessTokenResponse) response.body();
            }
            else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AccessTokenInfoResponse getAccessTokenInfo(String accessToken) {
        try {
            Response response = apiImpl.getAccessTokenInfo(accessToken).execute();
            if (response.isSuccessful()) {
                return (AccessTokenInfoResponse) response.body();
            }
            else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConsumerResponse getConsumer(String consumerId, String accessToken) {
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            Response response = apiImpl.getConsumer(consumerId, params).execute();
            if (response.isSuccessful()) {
                return (ConsumerResponse) response.body();
            }
            else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

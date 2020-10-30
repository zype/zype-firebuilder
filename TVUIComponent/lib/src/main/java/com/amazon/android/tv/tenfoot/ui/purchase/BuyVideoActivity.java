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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.content.constants.ExtraKeys;
import com.amazon.android.model.event.ProductsUpdateEvent;
import com.amazon.android.model.event.ProgressOverlayDismissEvent;
import com.amazon.android.model.event.PurchaseEvent;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.purchase.Model.Consumer;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.android.utils.Preferences;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.auth.ZypeAuthentication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Evgeny Cherkasov on 24.09.2018.
 */

public class BuyVideoActivity extends Activity implements ErrorDialogFragment.ErrorDialogFragmentListener {
    private static final String TAG = BuyVideoActivity.class.getName();

    private static final int REQUEST_CREATE_LOGIN = 110;

    private LinearLayout layoutConfirm;

    private TextView textVideo;
    private LinearLayout layoutLogin;
    private Button buttonLogin;
    private LinearLayout layoutLoggedIn;
    private TextView textConsumerEmail;

    private Button buttonConfirm;
    private Button buttonRestore;
    private Button buttonCancel;

    private String sku = null;
    private String price = null;
    private boolean isVideoPurchased = false;

    //
    private int mode;

    private static final int MODE_VIDEO = 1;
    private static final int MODE_PLAYLIST = 2;

    private ContentBrowser contentBrowser;
    private ErrorDialogFragment dialogError = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_video);

        contentBrowser = ContentBrowser.getInstance(this);

        layoutConfirm = (LinearLayout) findViewById(R.id.layoutConfirm);

        textVideo = (TextView) findViewById(R.id.textVideo);

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

        buttonRestore = (Button) findViewById(R.id.buttonRestore);
        buttonRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRestore();
            }
        });

        buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });

        Set<String> skuSet;
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(ExtraKeys.PLAYLIST_ID)) {
            // Buy playlist
            mode = MODE_PLAYLIST;
            try {
                Content content = ContentBrowser.getInstance(this).getLastSelectedContent();
                ContentContainer playlist = contentBrowser.getRootContentContainer()
                        .findContentContainerById(content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
                skuSet = ContentBrowser.getInstance(this).getPurchaseHelper().getPlaylistSKU(playlist);
                ContentBrowser.getInstance(this).getPurchaseHelper().handleProductsChain(this, skuSet);
            }
            catch (Exception e) {
                // TODO: Handle error
            }
        }
        else {
            // Buy video
            mode = MODE_VIDEO;
//            sku = "com.zumba.zumbaathome.singleVideo";
//            skuSet = new HashSet<>();
//            skuSet.add(sku);
//            ContentBrowser.getInstance(this).getPurchaseHelper().handleProductsChain(this, skuSet);
            try {
                Content video = ContentBrowser.getInstance(this).getLastSelectedContent();
                skuSet = ContentBrowser.getInstance(this).getPurchaseHelper().getVideoSku(video);
                ContentBrowser.getInstance(this).getPurchaseHelper().handleProductsChain(this, skuSet);
            }
            catch (Exception e) {
                // TODO: Handle error
            }
        }

        updateViews();
        bindViews();
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
        Content content = ContentBrowser.getInstance(this).getLastSelectedContent();
        if (mode == MODE_VIDEO) {
            textVideo.setText(content.getTitle());
        }
        else if (mode == MODE_PLAYLIST ){
            ContentContainer playlist = contentBrowser.getRootContentContainer()
                    .findContentContainerById(content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
            textVideo.setText(playlist.getName());
        }
    }

    private void updateViews() {
        if (isVideoPurchased) {
            buttonConfirm.setVisibility(View.GONE);
            buttonRestore.setVisibility(View.VISIBLE);
        }
        else {
            buttonConfirm.setVisibility(View.VISIBLE);
            buttonRestore.setVisibility(View.GONE);
            if (mode == MODE_VIDEO) {
                if (!TextUtils.isEmpty(price)) {
                    buttonConfirm.setText(String.format(getString(R.string.buy_video_button_confirm_price), price));
                }
                else {
                    buttonConfirm.setText(getString(R.string.buy_video_button_confirm));
                }
            }
            else if (mode == MODE_PLAYLIST) {
                Content content = ContentBrowser.getInstance(this).getLastSelectedContent();
                ContentContainer playlist = contentBrowser.getRootContentContainer()
                        .findContentContainerById(content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
                if (!TextUtils.isEmpty(price)) {
                    buttonConfirm.setText(String.format(getString(R.string.buy_playlist_button_confirm_price),
                            String.valueOf(playlist.getExtraValueAsInt(ContentContainer.EXTRA_PLAYLIST_ITEM_COUNT)),
                            price));
                }
                else {
                    buttonConfirm.setText(String.format(getString(R.string.buy_playlist_button_confirm),
                            String.valueOf(playlist.getExtraValueAsInt(ContentContainer.EXTRA_PLAYLIST_ITEM_COUNT))));
                }
            }
        }
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

    private void closeScreen() {
        EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
        contentBrowser.onAuthenticationStatusUpdateEvent(new AuthHelper.AuthenticationStatusUpdateEvent(true));
        EventBus.getDefault().post(new AuthHelper.AuthenticationStatusUpdateEvent(true));

        setResult(RESULT_OK);
        finish();
    }

    // //////////
    // Actions
    //
    private void onLogin() {
        contentBrowser.getAuthHelper()
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        updateViews();
                    }
                    else {
                        contentBrowser.getAuthHelper()
                                .authenticateWithActivity()
                                .subscribe(resultBundle -> {
                                    if (resultBundle != null) {
                                        if (resultBundle.getBoolean(AuthHelper.RESULT)) {
                                            updateViews();
                                            checkVideoEntitlement();
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
        buyVideo();
    }

    private void onRestore() {
        restorePurchase();
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
                            contentBrowser.actionTriggered(BuyVideoActivity.this,
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

    private void buyVideo() {
//        contentBrowser.getPurchaseHelper().setBuyVideoSKU(sku);
        if (mode == MODE_VIDEO) {
            Bundle extras = new Bundle();
            extras.putString("VideoId", contentBrowser.getLastSelectedContent().getId());
            contentBrowser.getPurchaseHelper().setPurchaseExtras(extras);
//            contentBrowser.getPurchaseHelper().setVideoId(contentBrowser.getLastSelectedContent().getId());
        }
        else if (mode == MODE_PLAYLIST) {
            Bundle extras = new Bundle();
            extras.putString("PlaylistId", contentBrowser.getLastSelectedContent()
                    .getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
            contentBrowser.getPurchaseHelper().setPurchaseExtras(extras);
        }
        if (contentBrowser.isUserLoggedIn()) {
            contentBrowser.actionTriggered(this, contentBrowser.getLastSelectedContent(),
                    ContentBrowser.CONTENT_ACTION_BUY, null, null);
        }
        else {
            Intent intent = new Intent(BuyVideoActivity.this, CreateLoginActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_LOGIN);
        }

    }

    private void restorePurchase() {

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
                        dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this, ErrorUtils.ERROR_CATEGORY.ZYPE_VERIFY_SUBSCRIPTION_ERROR, BuyVideoActivity.this);
                        dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                    }
                }
            }).execute();
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR, BuyVideoActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
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
        if (products != null && !products.isEmpty()) {
            sku = products.get(0).get("SKU");
            price = products.get(0).get("Price");
//            if (mode == MODE_VIDEO) {
//                for (HashMap<String, String> product : products) {
//                    if (product.get("SKU").equals(sku)) {
//                        price = products.get(0).get("Price");
//                        // TODO: Check if the product already purchased and update action button - Buy ot Restore
//                        break;
//                    }
//                }
//            }
//            else if (mode == MODE_PLAYLIST) {
//                sku = products.get(0).get("SKU");
//                price = products.get(0).get("Price");
//            }
            updateViews();
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this,
                    ErrorUtils.ERROR_CATEGORY.ZYPE_BUY_VIDEO_ERROR_PRODUCT,
                    BuyVideoActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }

    /**
     * Event bus listener method to detect purchase broadcast.
     *
     * @param event Broadcast event for progress overlay dismiss.
     */
    @Subscribe
    public void onPurchaseEvent(PurchaseEvent event) {
        contentBrowser.getPurchaseHelper().setPurchaseExtras(null);
//        contentBrowser.getPurchaseHelper().setVideoId(null);
        if (event.getExtras().getBoolean(PurchaseHelper.RESULT)) {
            boolean validity = event.getExtras().getBoolean(PurchaseHelper.RESULT_VALIDITY);
            Log.i(TAG, "onPurchaseEvent(): " + validity);
            if (validity) {
                isVideoPurchased = true;
                contentBrowser.getEntitlementsManager().loadVideoEntitlements(getApplicationContext());
                closeScreen();
                contentBrowser.actionTriggered(this,
                        contentBrowser.getLastSelectedContent(),
                        ContentBrowser.CONTENT_ACTION_WATCH_NOW,
                        null,
                        null);
            }
            else {
                isVideoPurchased = false;
                updateViews();
                dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this,
                        ErrorUtils.ERROR_CATEGORY.ZYPE_BUY_VIDEO_ERROR_VERIFY,
                        BuyVideoActivity.this);
                dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
            }
        }
        else {
            isVideoPurchased = false;
            updateViews();
            // TODO: Update error message
            dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this,
                    ErrorUtils.ERROR_CATEGORY.ZYPE_BUY_VIDEO_ERROR_VERIFY,
                    BuyVideoActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }

}

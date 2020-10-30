/**
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.android.contentbrowser.helper;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.R;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.content.constants.ExtraKeys;
import com.amazon.android.model.event.ProgressOverlayDismissEvent;
import com.amazon.android.model.event.ProductsUpdateEvent;
import com.amazon.android.model.event.PurchaseEvent;
import com.amazon.android.module.ModuleManager;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.ui.fragments.ProgressDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.FileHelper;
import com.amazon.android.utils.Preferences;
import com.amazon.purchase.IPurchase;
import com.amazon.purchase.PurchaseManager;
import com.amazon.purchase.PurchaseManagerListener;
import com.amazon.purchase.model.Product;
import com.amazon.purchase.model.Response;
import com.zype.fire.api.MarketplaceGateway;
import com.zype.fire.api.ZypeConfiguration;
import com.zype.fire.api.ZypeSettings;

import org.greenrobot.eventbus.EventBus;
import org.w3c.dom.Text;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Helper class to perform purchase related actions.
 */
public class PurchaseHelper {

    /**
     * Event class to represent Purchase events.
     */
    public static class PurchaseUpdateEvent {

        /**
         * Purchase valid flag.
         */
        private boolean mPurchaseValid = false;

        /**
         * Constructor
         *
         * @param flag Purchase valid flag.
         */
        public PurchaseUpdateEvent(boolean flag) {

            mPurchaseValid = flag;
        }

        /**
         * Method to check if the purchase is valid after this event
         *
         * @return is purchase valid after this event
         */
        public boolean isPurchaseValid() {

            return mPurchaseValid;
        }
    }


    public static final String CONFIG_PURCHASE_VERIFIED = "CONFIG_PURCHASE_VERIFIED";
    public static final String CONFIG_PURCHASED_SKU = "CONFIG_PURCHASED_SKU";

    private static final String TAG = PurchaseHelper.class.getName();
    public static final String ACTIONS = "actions";
    /* Zype, Evgeny Cherkasov */
    public static final String SKUS_LIST = "skusList";

    private final ContentBrowser mContentBrowser;
    private PurchaseManager mPurchaseManager;
    private final Context mContext;
    private final Map<String, String> mActionsMap;
    private String mDailyPassSKU;
    private String mSubscriptionSKU;

    /* Zype, Evgeny Cherkasov
     * begin */
    private String mBuyVideoSKU;
    private String videoId;
    private Bundle purchaseExtras;
    /* Zype
     * end */

    /**
     * Event bus reference.
     */
    private final EventBus mEventBus = EventBus.getDefault();


    /**
     * Result key.
     */
    public static final String RESULT = "RESULT";

    /**
     * Result SKU key.
     */
    public static final String RESULT_SKU = "RESULT_SKU";

    /**
     * Result validity key.
     */
    public static final String RESULT_VALIDITY = "RESULT_VALIDITY";

    /* Zype, Evgeny Cherkasov */
    /**
     * Result products.
     */
    public static final String RESULT_PRODUCTS = "RESULT_PRODUCTS";

    /**
     * Constructor. Initializes member variables and configures the purchase system.
     *
     * @param context        The context.
     * @param contentBrowser The content browser.
     */
    public PurchaseHelper(Context context, ContentBrowser contentBrowser) {

        this.mContentBrowser = contentBrowser;
        this.mContext = context;
        this.mActionsMap = new HashMap<>();

        //If Iap is disabled, do not proceed with initializing purchase system
        if (!contentBrowser.isIapDisabled()) {
            initializePurchaseSystem();
        }
    }

    /**
     * Method to initialize purchase system
     */
    private void initializePurchaseSystem() {

        // The purchase system should be initialized by the module initializer, if there is no
        // initializer available that means the purchase system is not needed.
        IPurchase purchaseSystem = (IPurchase) ModuleManager.getInstance()
                .getModule(
                        IPurchase.class.getSimpleName())
                .getImpl(true);
        if (purchaseSystem == null) {
            Log.i(TAG, "Purchase system not registered.");
            return;
        }

        try {
            registerSkuForActions();
        }
        catch (Exception e) {
            Log.e(TAG, "Could not register actions!!", e);
        }

        // Register the purchase system received via ModuleManager and configure the purchase
        // listener.
        this.mPurchaseManager = PurchaseManager.getInstance(mContext.getApplicationContext());
        try {
            mPurchaseManager.init(purchaseSystem, new PurchaseManagerListener() {
                @Override
                public void onRegisterSkusResponse(Response response) {

                    if (response == null || !Response.Status.SUCCESSFUL.equals(response.getStatus
                            ())) {
                        Log.e(TAG, "Register products failed " + response);
                    }
                    else {
                        // If there is a valid receipt available in the system, set content browser
                        // variable as true.
                        String sku = mPurchaseManager.getPurchasedSku();
                        if (sku == null) {
                            setSubscription(false, null);
                        }
                        else {
                            setSubscription(true, sku);
                        }
                        Log.d(TAG, "Register products complete.");
                    }
                    mContentBrowser.updateContentActions();
                }

                @Override
                public void onValidPurchaseResponse(Response response, boolean validity,
                                                    String sku) {

                    Log.e(TAG, "You should not hit here!!!");
                }

                /* Zype, Evgeny Cherkasov
                 * begin */
                @Override
                public void onProductDataResponse(Response response, Map<String, Product> products) {
                    Log.e(TAG, "You should not hit here!!!");
                }

//                @Override
//                public String getVideoId() {
//                    Log.e(TAG, "You should not hit here!!!");
//                    return null;
//                }

                @Override
                public Bundle getPurchaseExtras() {
                    Log.e(TAG, "You should not hit here!!!");
                    return null;
                }
                /* Zype
                 * end */

            });
        }
        catch (Exception e) {
            Log.e(TAG, "Could not configure the purchase system. ", e);
        }
    }

    /**
     * Reads the SKUs from the configuration file and registers them in the system.
     */
    private void registerSkuForActions() throws IOException {

        Recipe recipe = Recipe.newInstance(
                FileHelper.readFile(mContext, mContext.getString(R.string.skus_file)));
        Map<String, String> actions = (Map<String, String>) recipe.getMap().get(ACTIONS);
        for (String key : actions.keySet()) {
            mActionsMap.put(key, actions.get(key));
        }
        Log.d(TAG, "actions registered " + mActionsMap);
        mSubscriptionSKU = mActionsMap.get("CONTENT_ACTION_SUBSCRIPTION");
        mDailyPassSKU = mActionsMap.get("CONTENT_ACTION_DAILY_PASS");
        /* Zype, Evgeny Cherkasov */
        mBuyVideoSKU = mActionsMap.get("CONTENT_ACTION_BUY_VIDEO");
    }


    /**
     * Sets the subscription data in Preferences.
     */
    private void setSubscription(boolean subscribe, String sku) {

        /* Zype, Evgeny Cherkasov */
//        mContentBrowser.setSubscribed(subscribe);
        //Send purchase status update event
        mEventBus.post(new PurchaseUpdateEvent(subscribe));
        Preferences.setBoolean(PurchaseHelper.CONFIG_PURCHASE_VERIFIED, subscribe);
        if (sku != null) {
            Preferences.setString(PurchaseHelper.CONFIG_PURCHASED_SKU, sku);
        }
        /* Zype, Evgeny Cherkasov */
        mContentBrowser.updateUserSubscribed();
    }

    /**
     * Handle success case of subscriber.
     *
     * @param subscriber Subscriber.
     * @param extras     Result bundle.
     */
    private void handleSuccessCase(Subscriber subscriber, Bundle extras) {

        extras.putBoolean(RESULT, true);
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(extras);
        }
        subscriber.onCompleted();
    }

    /**
     * Handle failure case of subscriber.
     *
     * @param subscriber Subscriber.
     * @param extras     Result bundle.
     */
    private void handleFailureCase(Subscriber subscriber, Bundle extras) {

        extras.putBoolean(RESULT, false);
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(extras);
        }
        subscriber.onCompleted();
    }

    /**
     * Handles the response for a valid purchase.
     *
     * @param subscriber Rx subscriber.
     * @param response   IAP response.
     * @param validity   Validity of the SKU.
     * @param sku        SKU name.
     */
    private void handleOnValidPurchaseResponse(Subscriber subscriber,
                                               Response response,
                                               boolean validity,
                                               String sku) {

        Bundle resultBundle = new Bundle();

        if (response != null && Response.Status.SUCCESSFUL.equals(response.getStatus())) {
            Log.d(TAG, "purchase succeeded " + response);
            setSubscription(validity, sku);
            resultBundle.putString(RESULT_SKU, sku);
            resultBundle.putBoolean(RESULT_VALIDITY, validity);
            AnalyticsHelper.trackPurchaseResult(sku, validity);
            handleSuccessCase(subscriber, resultBundle);
        }
        else {
            AnalyticsHelper.trackError(TAG, "Purchase failed "+response);
            resultBundle.putBoolean(RESULT_VALIDITY, false);
            handleFailureCase(subscriber, resultBundle);
        }
    }

    /**
     * Create observable purchase manager listener.
     *
     * @param subscriber Rx subscriber.
     * @return Purchase manager listener instance.
     */
    private PurchaseManagerListener createObservablePurchaseManagerListener(Subscriber subscriber) {

        return new PurchaseManagerListener() {
            @Override
            public void onRegisterSkusResponse(Response response) {

                Log.e(TAG, "You should not hit here!!!");
            }

            @Override
            public void onValidPurchaseResponse(Response response,
                                                boolean validity,
                                                String sku) {

                handleOnValidPurchaseResponse(subscriber,
                        response,
                        validity,
                        sku);
            }

            /* Zype, Evgeny Cherkasov
             * begin */
            public void onProductDataResponse(Response response, Map<String, Product> products) {
                handleProductsResponse(subscriber, response, products);
            }

//            public String getVideoId() {
//                return videoId;
//            }

            public Bundle getPurchaseExtras() {
                return purchaseExtras;
            }
            /* Zype
             * end */
        };
    }

    /**
     * Purchase SKU observable.
     *
     * @param sku SKU name.
     * @return Purchase SKU observable result.
     */
    public Observable<Bundle> purchaseSkuObservable(String sku) {

        Log.v(TAG, "purchaseSku called:" + sku);

        return Observable.create(subscriber -> mPurchaseManager
                .purchaseSku(sku,
                        createObservablePurchaseManagerListener(
                                subscriber))
        );
    }

    /**
     * Is purchase valid observable.
     *
     * @param purchasedSku Purchased sku name.
     * @return Purchase valid observable result.
     */
    public Observable<Bundle> isPurchaseValidObservable(String purchasedSku) {

        Log.v(TAG, "isPurchaseValid called:" + purchasedSku);

        return Observable.create(subscriber -> mPurchaseManager
                .isPurchaseValid(purchasedSku,
                        createObservablePurchaseManagerListener(
                                subscriber))
        );
    }

    /**
     * Is subscription valid observable.
     *
     * @return Subscription valid observable result.
     */
    public Observable<Bundle> isSubscriptionValidObservable() {

        return isPurchaseValidObservable(mSubscriptionSKU)
                .concatMap(resultBundle -> {
                    if (resultBundle.getBoolean(RESULT) &&
                            !resultBundle.getBoolean(RESULT_VALIDITY)) {
                        return isPurchaseValidObservable(mDailyPassSKU);
                    }
                    else {
                        return Observable.just(resultBundle);
                    }
                });
    }

    /**
     * Handle purchase chain.
     *
     * @param activity Activity.
     * @param sku      Sku name.
     */
    private void handlePurchaseChain(Activity activity, String sku) {

        purchaseSkuObservable(sku)
                .subscribeOn(Schedulers.newThread()) //this needs to be first make sure
                .observeOn(AndroidSchedulers.mainThread()) //this needs to be last to
                // make sure rest is running on separate thread.
                .subscribe(resultBundle -> {
                    Log.e(TAG, "isPurchaseValid subscribe called");
                    /* Zype, Evgeny Cherkasov */
//                    mContentBrowser.updateContentActions();
                    EventBus.getDefault().post(new PurchaseEvent(resultBundle));

                    EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
                }, throwable -> {
                    EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));

                    ErrorHelper.injectErrorFragment(activity, ErrorUtils.ERROR_CATEGORY
                            .NETWORK_ERROR, (errorDialogFragment, errorButtonType, errorCategory)
                            -> {
                        errorDialogFragment.dismiss();
                    });
                });
    }

    /**
     * Handles the action corresponding to the purchase buttons.
     *
     * @param activity Activity instance that triggered the action.
     * @param content  The content triggered by the action.
     * @param actionId The id of the action.
     */
    public void handleAction(Activity activity, Content content, int actionId) {

//        triggerProgress(activity);
        if (actionId == ContentBrowser.CONTENT_ACTION_DAILY_PASS) {
            triggerProgress(activity);
            handlePurchaseChain(activity, mDailyPassSKU);
        }
        else if (actionId == ContentBrowser.CONTENT_ACTION_SUBSCRIPTION) {
            triggerProgress(activity);
            handlePurchaseChain(activity, mSubscriptionSKU);
        }
        /* Zype, Evgeny Cherkasov */
        else if (actionId == ContentBrowser.CONTENT_ACTION_BUY) {
            handlePurchaseChain(activity, mBuyVideoSKU);
        }
        else if (actionId == ContentBrowser.CONTENT_ACTION_CHOOSE_PLAN) {
            mContentBrowser.switchToSubscriptionScreen(new Bundle());
        }
        else if (actionId == ContentBrowser.CONTENT_ACTION_CONFIRM_PURCHASE) {
            mContentBrowser.switchToBuyVideoScreen(new Bundle());
        }
        else if (actionId == ContentBrowser.CONTENT_ACTION_CONFIRM_PURCHASE_PLAYLIST) {
            Bundle extras = new Bundle();
            extras.putString(ExtraKeys.PLAYLIST_ID, content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
            mContentBrowser.switchToBuyVideoScreen(extras);
        }
    }

    /**
     * Triggers the progress fragment.
     *
     * @param activity Activity instance.
     */
    private void triggerProgress(Activity activity) {

        ProgressDialogFragment.createAndShow(activity, mContext.getString(R.string.loading));
    }

    /* Zype,  Evgeny Cherkasov
     * begin */
    public void handleProductsChain(Activity activity) {
        triggerProgress(activity);
        Set<String> skuSet = null;
//        try {
            skuSet = MarketplaceGateway.getInstance(activity).getSubscriptionSkus();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            ErrorHelper.injectErrorFragment(activity, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR,
//                    (errorDialogFragment, errorButtonType, errorCategory) -> {
//                        errorDialogFragment.dismiss();
//                    });
//        }
        handleProductsChain(activity, skuSet);
    }

    public void handleProductsChain(Activity activity, Set<String> skuSet) {
        triggerProgress(activity);
        productsObservable(skuSet)
                .subscribeOn(Schedulers.newThread()) //this needs to be first make sure
                .observeOn(AndroidSchedulers.mainThread()) //this needs to be last to
                // make sure rest is running on separate thread.
                .subscribe(resultBundle -> {
                            EventBus.getDefault().post(new ProductsUpdateEvent(resultBundle));
                            EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
                        },
                        throwable -> {
                            EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));

                            ErrorHelper.injectErrorFragment(activity, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR,
                                    (errorDialogFragment, errorButtonType, errorCategory) -> {
                                        errorDialogFragment.dismiss();
                                    });
                        });
    }

    public Observable<Bundle> productsObservable(Set<String> skuSet) {

        Log.v(TAG, "productsObservable()");

        return Observable.create(subscriber -> mPurchaseManager
                .getProducts(skuSet, createObservablePurchaseManagerListener(subscriber))
        );
    }

    private void handleProductsResponse(Subscriber subscriber, Response response, Map<String, Product> products) {
        Bundle resultBundle = new Bundle();
        if (response != null && Response.Status.SUCCESSFUL.equals(response.getStatus())) {
            Log.d(TAG, "handleProductsResponse(): successful,  " + response);
            ArrayList<HashMap<String, String>> productsList = new ArrayList<>();
            for (String key : products.keySet()) {
                HashMap<String, String> productDetails = new HashMap<>();
                // TODO: Use constants for keys
                productDetails.put("Title", products.get(key).getTitle());
                productDetails.put("Price", products.get(key).getPrice());
                productDetails.put("SKU", products.get(key).getSku());
                productsList.add(productDetails);
            }
            resultBundle.putSerializable(RESULT_PRODUCTS, productsList);
//            AnalyticsHelper.trackPurchaseResult(sku, validity);
            handleSuccessCase(subscriber, resultBundle);
        }
        else {
            Log.e(TAG, "handleProductsResponse(): failed, " + response);
//            AnalyticsHelper.trackError(TAG, "Purchase failed "+response);
            handleFailureCase(subscriber, resultBundle);
        }
    }

    public Set<String> getSubscriptionSKUs() throws IOException {

        Set<String> result = new HashSet<>();

        Recipe recipe = Recipe.newInstance(FileHelper.readFile(mContext, mContext.getString(R.string.skus_file)));
        List<Map<String, String>> skuList = (List<Map<String, String>>) recipe.getMap().get(SKUS_LIST);
        for (Map<String, String> item : skuList) {
            if (item.get("productType").equals("SUBSCRIBE")) {
                result.add(item.get("sku"));
            }
        }

        return result;
    }

    public void setSubscriptionSKU(String sku) {
        mSubscriptionSKU = sku;
    }

    public String getSubscriptionId(String sku) throws IOException {
        Recipe recipe = Recipe.newInstance(FileHelper.readFile(mContext, mContext.getString(R.string.skus_file)));
        List<Map<String, String>> skuList = (List<Map<String, String>>) recipe.getMap().get(SKUS_LIST);
        for (Map<String, String> item : skuList) {
            if (item.get("sku").equals(sku)) {
                return item.get("id");
            }
        }
        return null;
    }

    public void setBuyVideoSKU(String sku) {
        mBuyVideoSKU = sku;
    }

    /**
     * If 'playlist' parameter is not empty, returns marketplace id extra attribute
     * of specified playlist
     *
     * Otherwise, search in configuration file the SKU that is used for 'Buy Playlist' action.
     * Returns first found SKU with `BuyPlaylist` id
     */
    public Set<String> getPlaylistSKU(ContentContainer playlist) throws IOException {
        Set<String> result = new HashSet<>();
        String sku = null;

        if (playlist != null) {
            sku = playlist.getExtraStringValue(ContentContainer.EXTRA_MARKETPLACE_ID);
        }
        if (TextUtils.isEmpty(sku)) {
            Recipe recipe = Recipe.newInstance(FileHelper.readFile(mContext, mContext.getString(R.string.skus_file)));
            List<Map<String, String>> skuList = (List<Map<String, String>>) recipe.getMap().get(SKUS_LIST);
            for (Map<String, String> item : skuList) {
                if (item.containsKey("id") && item.get("id").equals("BuyPlaylist")) {
                    sku = item.get("sku");
                    break;
                }
            }
        }
        Log.i(TAG, "getPlaylistSKU(): sku=" + sku);
        result.add(sku);
        mBuyVideoSKU = sku;

        return result;
    }

    /**
     * If 'video' parameter is not empty, returns marketplace id extra attribute
     * of specified video
     *
     * Otherwise, search in configuration file the SKU that is used for 'Buy Video' action.
     * Returns first found SKU with `BuyVideo` id
     */
    public Set<String> getVideoSku(Content video) throws IOException {
        Set<String> result = new HashSet<>();
        String sku = null;

        if (video != null) {
            sku = video.getExtraValueAsString(Content.EXTRA_MARKETPLACE_ID);
        }
        if (TextUtils.isEmpty(sku)) {
            Recipe recipe = Recipe.newInstance(FileHelper.readFile(mContext, mContext.getString(R.string.skus_file)));
            List<Map<String, String>> skuList = (List<Map<String, String>>) recipe.getMap().get(SKUS_LIST);
            for (Map<String, String> item : skuList) {
                if (item.containsKey("id") && item.get("id").equals("BuyVideo")) {
                    sku = item.get("sku");
                    break;
                }
            }
        }
        if (sku == null) {
            sku = "";
        }
        Log.i(TAG, "getVideoSku(): sku=" + sku);
        result.add(sku);
        mBuyVideoSKU = sku;

        return result;
    }

//    public void setVideoId(String videoId) {
//        this.videoId = videoId;
//    }

    public void setPurchaseExtras(Bundle extras) {
        this.purchaseExtras = extras;
    }

    public boolean isVideoPaywalled(Content content) {
        if (ZypeConfiguration.isNativeSubscriptionEnabled(mContext)
                || ZypeConfiguration.isUniversalSubscriptionEnabled(mContext)) {
            if (content.isSubscriptionRequired()) {
                return true;
            }
        }
        if (ZypeConfiguration.isNativeTVODEnabled(mContext)
                || ZypeConfiguration.isUniversalTVODEnabled(mContext)) {
            boolean purchaseRequired = content.getExtraValueAsBoolean(Content.EXTRA_PURCHASE_REQUIRED);
            ContentContainer playlist = mContentBrowser.getRootContentContainer()
                    .findContentContainerById(content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
            if (playlist != null) {
                if (!playlist.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG)
                        .equals(ZypeSettings.MY_LIBRARY_PLAYLIST_ID)) {
                    boolean playlistPurchaseRequired = (playlist != null)
                            && playlist.getExtraValueAsBoolean(ContentContainer.EXTRA_PURCHASE_REQUIRED)
                            && ZypeSettings.PLAYLIST_PURCHASE_ENABLED;
                    if (purchaseRequired || playlistPurchaseRequired) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isVideoLocked(Content content) {
        if (ZypeConfiguration.isNativeSubscriptionEnabled(mContext)
            || ZypeConfiguration.isUniversalSubscriptionEnabled(mContext)) {
            if (content.isSubscriptionRequired()) {
                if (!mContentBrowser.isUserSubscribed()) {
                    return true;
                }
            }
        }
        if (ZypeConfiguration.isNativeTVODEnabled(mContext)
            || ZypeConfiguration.isUniversalTVODEnabled(mContext)) {
            boolean purchaseRequired = content.getExtraValueAsBoolean(Content.EXTRA_PURCHASE_REQUIRED);
            ContentContainer playlist = mContentBrowser.getRootContentContainer()
                    .findContentContainerById(content.getExtraValueAsString(Content.EXTRA_PLAYLIST_ID));
            boolean playlistPurchaseRequired = (playlist != null)
                    && playlist.getExtraValueAsBoolean(ContentContainer.EXTRA_PURCHASE_REQUIRED)
                    && ZypeSettings.PLAYLIST_PURCHASE_ENABLED;
            if (purchaseRequired || playlistPurchaseRequired) {
//                boolean entitled = content.getExtraValueAsBoolean(Content.EXTRA_ENTITLED);
                boolean entitled = mContentBrowser.getEntitlementsManager()
                    .isVideoEntitled(content);
                if (!entitled) {
                    return true;
                }
            }
        }
        return false;
    }

    /* Zype
     * end */
}
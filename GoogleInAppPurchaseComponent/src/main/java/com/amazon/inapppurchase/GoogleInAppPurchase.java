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
package com.amazon.inapppurchase;

//import com.amazon.device.iap.PurchasingListener;
//import com.amazon.device.iap.PurchasingService;
//import com.amazon.device.iap.model.FulfillmentResult;
//import com.amazon.device.iap.model.ProductDataResponse;
//import com.amazon.device.iap.model.PurchaseResponse;
//import com.amazon.device.iap.model.PurchaseUpdatesResponse;
//import com.amazon.device.iap.model.UserDataResponse;
import com.amazon.android.navigator.Navigator;
import com.amazon.purchase.IPurchase;
import com.amazon.purchase.model.Product;
import com.amazon.purchase.model.Receipt;
import com.amazon.purchase.model.Response;
import com.amazon.purchase.model.UserData;
import com.amazon.utils.ObjectVerification;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.zype.fire.api.ZypeSettings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of {@link IPurchase} interface backed by Amazon IAP.
 */
public class GoogleInAppPurchase implements IPurchase {

    private static final String TAG = GoogleInAppPurchase.class.getName();

    /**
     * {@link IPurchase} implementation name.
     */
    static final String IMPL_CREATOR_NAME = GoogleInAppPurchase.class.getSimpleName();

    /**
     * The context.
     */
    protected Context mContext;

    /**
     * Bundle for extra data.
     */
    protected Bundle mExtras;

    /**
     * The purchase listener.
     */
    protected PurchaseListener mPurchaseListener;

    /**
     * The receipt verifier.
     */
    protected AReceiptVerifier mReceiptVerifier;


    private Navigator navigator;
    private PurchasesUpdatedListener purchasesUpdatedListener;
    private BillingClient billingClient;
    private final Map<String, SkuDetails> skuDetailsMap = new HashMap<>();

    private final static String REQUEST_PRODUCTS = "getProducts";
    private final static String REQUEST_PURCHASE = "purchase";
    private final static String REQUEST_PURCHASES_DATA = "getUserPurchaseData";
    private final static String REQUEST_USER_DATA = "getUserData";

    /**
     * Constructor that initializes the default variable values.
     */
    public GoogleInAppPurchase() {

        super();
    }

    /**
     * Register custom a receipt verifier.
     *
     * @param receiptVerifier The verifier to be used by AmazonInAppPurchase.
     */
    public void registerReceiptVerification(AReceiptVerifier receiptVerifier) {

        this.mReceiptVerifier = receiptVerifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Context context, Navigator navigator, Bundle extras) {

        this.mContext = ObjectVerification.notNull(context, "Context cannot be null")
                .getApplicationContext();
        this.navigator = navigator;
        this.mExtras = extras;

        String receiptVerificationClassPath =
                context.getString(R.string.receipt_verification_service_iap);

        try {
            this.mReceiptVerifier = createReceiptVerifier(mContext, receiptVerificationClassPath);
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to create custom ReceiptVerifier for path " +
                    receiptVerificationClassPath, e);
            // Assigning the default Receipt verifier.
            this.mReceiptVerifier = DefaultReceiptVerificationService.createInstance(context);
        }

        purchasesUpdatedListener = createPurchasesUpdatedListener();
        billingClient = BillingClient.newBuilder(mContext)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        connectBillingClient();
    }

    /**
     * Creates an instance of {@link AReceiptVerifier} based on param receiptVerificationClassPath
     *
     * @param context                      The application context.
     * @param receiptVerificationClassPath The fully qualified path of the class.
     * @return The instance of the class.
     * @throws ClassNotFoundException           if the class was not found.
     * @throws java.util.NoSuchElementException if the method was not found on the reflection
     *                                          object.
     * @throws IllegalAccessException           if an illegal access attempt was made.
     * @throws InvocationTargetException        if there was an error invoking the method with
     *                                          reflection.
     * @throws NoSuchMethodException            if the method was not found.
     */
    private AReceiptVerifier createReceiptVerifier(Context context, String
            receiptVerificationClassPath) throws NoSuchMethodException, ClassNotFoundException,
            InvocationTargetException, IllegalAccessException {

        Log.d(TAG, "Creating instance of " + receiptVerificationClassPath);
        long startTime = System.currentTimeMillis();
        Class<?> clazz = Class.forName(receiptVerificationClassPath);
        Method method = clazz.getMethod(AReceiptVerifier.CREATE_INSTANCE_METHOD_NAME, Context
                .class);
        AReceiptVerifier receiptVerifierImpl = (AReceiptVerifier) method.invoke(null, context);
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Time taken in createReceiptVerifier " + (endTime - startTime));
        return receiptVerifierImpl;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerDefaultPurchaseListener(PurchaseListener purchaseListener) {

        this.mPurchaseListener = purchaseListener;
//        PurchasingListener iapListener = createIapPurchasingListener(purchaseListener);
//        PurchasingService.registerListener(mContext, iapListener);
//        Log.d(TAG, PurchasingService.IS_SANDBOX_MODE + "IS_SANDBOX_MODE");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserData() {
        Log.d(TAG, "getUserData(): Not implemented");
        return REQUEST_USER_DATA;
//        String requestId = PurchasingService.getUserData().toString();
//        Log.d(TAG, "calling PurchaseService getUserData with requestId " + requestId);
//        return requestId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProducts(final Set<String> skuSet) {
        Log.d(TAG, "getProducts()");
        final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(new ArrayList<String>(skuSet))
                .setType(BillingClient.SkuType.SUBS);
        SkuDetailsResponseListener subsListener = new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                final Map<String, SkuDetails> skuDetailsMapTemp = new HashMap<>();
                final Response response = createResponse(isSuccessful(billingResult), REQUEST_PRODUCTS);
                final Map<String, Product> productMap = createProductMapFromProductDataResponse(skuDetailsList);
                for (SkuDetails skuDetails : skuDetailsList) {
                    skuDetailsMapTemp.put(skuDetails.getSku(), skuDetails);
                }
                final SkuDetailsResponseListener inappListener = new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        if (isSuccessful(billingResult)) {
                            response.setStatus(Response.Status.SUCCESSFUL);
                        }
                        productMap.putAll(createProductMapFromProductDataResponse(skuDetailsList));
                        for (SkuDetails skuDetails : skuDetailsList) {
                            skuDetailsMapTemp.put(skuDetails.getSku(), skuDetails);
                        }
                        skuDetailsMap.clear();
                        skuDetailsMap.putAll(skuDetailsMapTemp);
                        for (String sku : productMap.keySet()) {
                            if (skuSet.contains(sku)) {
                                skuSet.remove(sku);
                            }
                        }
                        mPurchaseListener.onProductDataResponse(response, productMap, skuSet);
                    }
                };

                params.setType(BillingClient.SkuType.INAPP);
                billingClient.querySkuDetailsAsync(params.build(), inappListener);
            }
        };
        billingClient.querySkuDetailsAsync(params.build(), subsListener);
        return REQUEST_PRODUCTS;
//        String requestId = PurchasingService.getProductData(skuSet).toString();
//        Log.d(TAG, "calling PurchaseService getProducts with requestId " + requestId);
//        return requestId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserPurchaseData(boolean reset) {
        Log.d(TAG, "getUserPurchaseData()");
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        Response response = createResponse(isSuccessful(purchasesResult.getBillingResult()), REQUEST_PURCHASES_DATA);
        List<Receipt> receipts = createReceiptList(purchasesResult.getPurchasesList());
        UserData userData = createUserData();
        purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (isSuccessful(purchasesResult.getBillingResult())) {
            response.setStatus(Response.Status.SUCCESSFUL);
        }
        receipts.addAll(createReceiptList(purchasesResult.getPurchasesList()));
        mPurchaseListener.onUserDataResponse(response, receipts, userData, false);

//        String requestId = PurchasingService.getPurchaseUpdates(reset).toString();
//        Log.d(TAG, "calling PurchaseService getUserPurchaseData with requestId " + requestId);
        return REQUEST_PURCHASES_DATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String purchase(String sku) {
        Log.d(TAG, "purchase()");
        SkuDetails skuDetails = skuDetailsMap.get(sku);
        if (skuDetails != null) {
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build();
            int responseCode = billingClient.launchBillingFlow(navigator.getActiveActivity(),
                    billingFlowParams).getResponseCode();
            if (responseCode != BillingClient.BillingResponseCode.OK) {
                Response response = createResponse(false, REQUEST_PURCHASE);
                mPurchaseListener.onPurchaseResponse(response, sku, null, null);
            }
        }
        else {
            Response response = createResponse(false, REQUEST_PURCHASE);
            mPurchaseListener.onPurchaseResponse(response, sku, null, null);
        }
//        String requestId = PurchasingService.purchase(sku).toString();
//        Log.d(TAG, "calling PurchaseService purchase with requestId " + requestId);
        return REQUEST_PURCHASE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String isPurchaseValid(String sku, UserData userData, Receipt receipt) {
        Log.d(TAG, "isPurchaseValid()");

        String requestId = createRandomString(receipt.getReceiptId(), 10);
        mReceiptVerifier.validateReceipt(mContext, requestId, sku, userData, receipt,
                mPurchaseListener);

        return requestId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyFulfillment(String sku, UserData userData, Receipt receipt,
                                  Receipt.FulfillmentStatus fulfillmentResult) {
        Log.d(TAG, "notifyFulfillment(): " + receipt);

        if (receipt.getProductType() == Product.ProductType.BUY) {
            ConsumeParams consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(receipt.getReceiptId())
                    .build();
            ConsumeResponseListener listener = new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "notifyFulfillment(): Purchase consumed.");
                    }
                }
            };
            billingClient.consumeAsync(consumeParams, listener);
        }
    }

    @Override
    public String getMarketplace() {
        return IPurchase.GOOGLE;
    }

    // Google Billing Library

    private PurchasesUpdatedListener createPurchasesUpdatedListener() {
        return new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                Response response = createResponse(isSuccessful(billingResult), REQUEST_PURCHASE);
                Purchase purchase = null;
                if (purchases != null && !purchases.isEmpty()) {
                    purchase = purchases.get(0);
                }
                if (purchase != null) {
                    mPurchaseListener.onPurchaseResponse(response, purchase.getSku(),
                            createReceipt(purchase), createUserData());
                }
                else {
                    mPurchaseListener.onPurchaseResponse(response, null, null, null);
                }
            }
        };
    }

    private void connectBillingClient() {
        if (!billingClient.isReady()) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // TODO: onBillingClientSetupFinished
                        // The BillingClient is ready. You can query purchases here.
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            });
        }
    }

//    /**
//     * Creates IAP purchase listener from PurchaseListener.
//     */
//    /*package private*/
//    PurchasingListener createIapPurchasingListener(final PurchaseListener listener) {
//
//        Log.d(TAG, "PurchasingListener registered");
//        return new PurchasingListener() {
//            @Override
//            public void onUserDataResponse(UserDataResponse userDataResponse) {
//
//                Log.d(TAG, "UserDataResponse received " + userDataResponse.toString());
//                Response response = createResponse(isSuccessful(userDataResponse),
//                        userDataResponse.getRequestId().toString());
//
//                UserData userData = createUserDataFromIapUserData(userDataResponse.getUserData());
//                listener.onGetUserDataResponse(response, userData);
//            }
//
//            @Override
//            public void onProductDataResponse(ProductDataResponse productDataResponse) {
//
//                Log.d(TAG, "ProductDataResponse received " + productDataResponse.toString());
//                Response response = createResponse(isSuccessful(productDataResponse),
//                        productDataResponse.getRequestId().toString());
//
//                Map<String, Product> productMap = createProductMapFromProductDataResponse
//                        (productDataResponse.getProductData());
//
//                listener.onProductDataResponse(response, productMap,
//                        productDataResponse.getUnavailableSkus());
//            }
//
//            @Override
//            public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
//
//                Log.d(TAG, "purchaseResponse received " + purchaseResponse.toString());
//                Response response = createResponse(isSuccessful(purchaseResponse),
//                        purchaseResponse.getRequestId().toString());
//
//                com.amazon.device.iap.model.Receipt iapReceipt = purchaseResponse.getReceipt();
//
//                String sku = null;
//                if (iapReceipt != null) {
//                    sku = iapReceipt.getSku();
//                }
//                listener.onPurchaseResponse(response, sku, createReceipt(iapReceipt),
//                        createUserDataFromIapUserData(purchaseResponse
//                                .getUserData()));
//            }
//
//            @Override
//            public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
//
//                Log.d(TAG, "purchaseUpdatesResponse received " + purchaseUpdatesResponse.toString
//                        ());
//                Response response = createResponse(isSuccessful(purchaseUpdatesResponse),
//                        purchaseUpdatesResponse.getRequestId()
//                                .toString());
//
//                List<Receipt> receipts = createReceiptList(purchaseUpdatesResponse.getReceipts());
//                UserData userData = createUserDataFromIapUserData(purchaseUpdatesResponse
//                        .getUserData());
//
//                listener.onUserDataResponse(response, receipts, userData, purchaseUpdatesResponse
//                        .hasMore());
//            }
//        };
//    }

    /**
     * Converts a list of IAP receipts to purchase receipts.
     *
     * @param purchases The IAP receipts to be converted.
     * @return The purchase receipts.
     */
    private List<Receipt> createReceiptList(List<Purchase> purchases) {

        List<Receipt> receipts = new ArrayList<>();
        if (purchases == null) {
            return receipts;
        }
        for (Purchase purchase : purchases) {
            receipts.add(createReceipt(purchase));
        }
        return receipts;
    }

    /**
     * Converts a single IAP receipt to a purchase receipt.
     *
     * @param purchase The IAP receipt to be converted.
     * @return The purchase receipt.
     */
    private Receipt createReceipt(Purchase purchase) {
        if (purchase == null) {
            return null;
        }
        Receipt receipt = new Receipt();
        receipt.setSku(purchase.getSku());
        Date purchaseDate = new Date();
        purchaseDate.setTime(purchase.getPurchaseTime());
        receipt.setPurchasedDate(purchaseDate);
        receipt.setExpiryDate(null);
        receipt.setReceiptId(purchase.getPurchaseToken());
        receipt.setProductType(isSubscription(purchase) ? Product.ProductType.SUBSCRIBE : Product.ProductType.BUY);
        Bundle extras = new Bundle();
        extras.putString("OriginalReceipt", purchase.getOriginalJson());
        extras.putDouble("Price", 0.0);
        extras.putString("Signature", purchase.getSignature());
        receipt.setExtras(extras);
        Log.i(TAG, "createReceipt(): receipt=" + receipt.toString());
        return receipt;
    }

    private boolean isSubscription(Purchase purchase) {
        return Arrays.asList(ZypeSettings.PLAN_IDS).contains(purchase.getSku());
    }

    /**
     * Converts a list of IAP products to purchase products.
     *
     * @param skuDetailsList The IAP products to be converted.
     * @return The purchase products.
     */
    private Map<String, Product> createProductMapFromProductDataResponse(List<SkuDetails> skuDetailsList) {

        Map<String, Product> productMap = new HashMap<>();
        if (skuDetailsList == null) {
            return productMap;
        }
        for (SkuDetails skuDetails : skuDetailsList) {
            Product product = createProductFromIapProduct(skuDetails);
            if (product != null) {
                productMap.put(skuDetails.getSku(), product);
            }
        }

        return productMap;
    }

    /**
     * Converts an IAP product to a purchase product.
     *
     * @param skuDetails The IAP product to be converted.
     * @return The purchase product.
     */
    private Product createProductFromIapProduct(SkuDetails skuDetails) {

        if (skuDetails == null) {
            return null;
        }
        Product product = new Product();
        product.setPrice(skuDetails.getPrice());
        product.setSku(skuDetails.getSku());
        product.setTitle(skuDetails.getTitle());
        product.setDescription(skuDetails.getDescription());
        product.setIconUrl("");
        product.setProductType(getProductType(skuDetails.getType()));
        return product;
    }

    /**
     * Converts an IAP product type to a purchase product type.
     *
     * @param skuType The IAP product to be converted.
     * @return The purchase product.
     */
    private Product.ProductType getProductType(String skuType) {

        if (skuType == null) {
            return null;
        }
        if (BillingClient.SkuType.INAPP.equals(skuType)) {
            return Product.ProductType.BUY;
        } else if (BillingClient.SkuType.SUBS.equals(skuType)) {
            return Product.ProductType.SUBSCRIBE;
        }
        throw new RuntimeException("product type " + skuType + " not supported");
    }

    /**
     * Converts an IAP user data to a purchase user data object.
     *
     * @return The purchase user data.
     */
    private UserData createUserData() {

        return new UserData("", "GooglePlay");
    }

    /**
     * Creates a {@link Response} object based on parameters.
     *
     * @param successful True if the response was successful; false otherwise.
     * @param requestId  The request id of the response.
     * @return response
     */
    private Response createResponse(boolean successful, String requestId) {

        if (successful) {
            return new Response(requestId, Response.Status.SUCCESSFUL, null);
        }
        else {
            return new Response(requestId, Response.Status.FAILED,
                    new Exception("Could not retrieve data from IAP"));
        }
    }

//    /**
//     * Decides whether the IAP user data response is a successful one or not.
//     *
//     * @param response The response to be checked.
//     * @return Whether the IAP response is a successful one or not.
//     */
//    private boolean isSuccessful(UserDataResponse response) {
//
//        return UserDataResponse.RequestStatus.SUCCESSFUL.equals(response.getRequestStatus());
//    }


    /**
     * Decides whether the IAP request result is a successful one or not.
     *
     * @param result The response to be checked.
     * @return Whether the IAP response is of successful one or not.
     */
    private boolean isSuccessful(BillingResult result) {

        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }


//    /**
//     * Decides whether the IAP purchase response is a successful one or not.
//     *
//     * @param response The response to be checked.
//     * @return Whether the IAP response is a successful one or not.
//     */
//    private boolean isSuccessful(PurchaseResponse response) {
//
//        return PurchaseResponse.RequestStatus.SUCCESSFUL.equals(response.getRequestStatus()) ||
//                PurchaseResponse.RequestStatus.ALREADY_PURCHASED
//                        .equals(response.getRequestStatus());
//    }
//
//    /**
//     * Decides whether the IAP purchase updates response is a successful one or not.
//     *
//     * @param response The response to be checked.
//     * @return Whether the IAP response is a successful one or not.
//     */
//    private boolean isSuccessful(PurchaseUpdatesResponse response) {
//
//        return PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL.equals(response.getRequestStatus());
//    }

    /**
     * Creates a random string of the given length and appends the prefix to the front of it.
     *
     * @param prefix The prefix of the string to append.
     * @param length The length of the random part of the string.
     * @return The string.
     */
    public String createRandomString(String prefix, int length) {

        StringBuilder builder;
        if (prefix != null) {
            builder = new StringBuilder(prefix);
        }
        else {
            builder = new StringBuilder();
        }
        Random r = new Random();
        for (int i = 0; i < length; i++) {
            char randomChar = (char) (r.nextInt(26) + 'a');
            builder.append(randomChar);
        }
        return builder.toString();
    }
}

package com.amazon.inapppurchase;

import android.content.Context;
import android.util.Log;

import com.amazon.android.utils.Preferences;
import com.amazon.purchase.IPurchase;
import com.amazon.purchase.model.Receipt;
import com.amazon.purchase.model.Response;
import com.amazon.purchase.model.UserData;
import com.google.gson.Gson;
import com.zype.fire.api.Model.MarketplaceConnectBody;
import com.zype.fire.api.Model.MarketplaceConnectBodyData;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeConfiguration;

import java.io.IOException;

/**
 * Created by Evgeny Cherkasov on 30.08.2017.
 *
 * Zype implementation of {@link AReceiptVerifier} interface.
 */

public class ZypeReceiptVerificationService extends AReceiptVerifier {
    private static final String TAG = ZypeReceiptVerificationService.class.getSimpleName();

    /**
     * {@inheritDoc}
     */
    @Override
    public String validateReceipt(Context context, String requestId, String sku, UserData
            userData, Receipt receipt, IPurchase.PurchaseListener listener) {

//        if (ZypeConfiguration.marketplaceConnectSvodEnabled(context)) {
//            Map<String, String> fieldParams = new HashMap<>();
//            // TODO: Get preference id from ZypeAuthentication
//            fieldParams.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
//            fieldParams.put(ZypeApi.SUBSCRIPTION_CONSUMER_ID, Preferences.getString("ZypeConsumerId"));
//            fieldParams.put(ZypeApi.SUBSCRIPTION_DEVICE_TYPE, "amazon");
//            fieldParams.put(ZypeApi.SUBSCRIPTION_RECEIPT_ID, receipt.getReceiptId());
//            fieldParams.put(ZypeApi.SUBSCRIPTION_SHARED_SECRET, ZypeSettings.AMAZON_SHARED_KEY);
//            // Need to remove all dots from sku because Bifrost service does not allows any symbols
//            // except letters and numbers
//            fieldParams.put(ZypeApi.SUBSCRIPTION_THIRD_PARTY_ID, sku.replaceAll("\\.", ""));
//            fieldParams.put(ZypeApi.SUBSCRIPTION_USER_ID, userData.getUserId());
//            // TODO: Comment 3 below lines for release build
////        Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
////        listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
////        return requestId;
//            try {
//                retrofit2.Response response = ZypeApi.getInstance().getApi().verifySubscription(fieldParams).execute();
//                if (response.isSuccessful()) {
//                    BifrostResponse bifrostResponse = (BifrostResponse) response.body();
//                    if (bifrostResponse.success) {
//                        Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
//                        listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
//                        return requestId;
//                    } else {
//                        Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
//                        listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
//                        return requestId;
//                    }
//                } else {
//                    Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
//                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
//                    return requestId;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
//                listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
//                return requestId;
//            }
//        }
        if (ZypeConfiguration.marketplaceConnectSvodEnabled(context)) {
            Log.i(TAG, "validateReceipt(): Subscription");
            MarketplaceConnectBody body = new MarketplaceConnectBody();
            body.amount = "";
            body.appId = ZypeConfiguration.getAppId(context);
            body.consumerId = Preferences.getString("ZypeConsumerId");
            body.planId = receipt.getExtras().getString("PlanId");
            body.siteId = ZypeConfiguration.getSiteId(context);
            body.transactionType = "subscription";
            MarketplaceConnectBodyData bodyData = new MarketplaceConnectBodyData();
            bodyData.receiptId = receipt.getReceiptId();
            bodyData.userId = userData.getUserId();
            body.data = bodyData;
            Log.i(TAG, "validateReceipt(): body=" + (new Gson()).toJson(body));
            try {
                retrofit2.Response response = ZypeApi.getInstance().getApi().verifySubscriptionPurchaseAmazon(body).execute();
                if (response.isSuccessful()) {
                    Log.i(TAG, "validateReceipt(): Receipt is valid");
                    Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
                    return requestId;
                }
                else {
                    Log.i(TAG, "validateReceipt(): Receipt is not valid");
                    Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
                    return requestId;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "validateReceipt(): Error marketplace connect call");
                Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
                listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
                return requestId;
            }
        }
        else if (ZypeConfiguration.isUniversalTVODEnabled(context)) {
            MarketplaceConnectBody body = new MarketplaceConnectBody();
            body.amount = "";
            body.appId = ZypeConfiguration.getAppId(context);
            body.consumerId = Preferences.getString("ZypeConsumerId");
            body.playlistId = receipt.getExtras().getString("PlaylistId");
            body.siteId = ZypeConfiguration.getSiteId(context);
            body.transactionType = "purchase";
            body.productId = receipt.getExtras().getString("VideoId");
            body.videoId = receipt.getExtras().getString("VideoId");
            MarketplaceConnectBodyData bodyData = new MarketplaceConnectBodyData();
            bodyData.receiptId = receipt.getReceiptId();
            bodyData.userId = userData.getUserId();
            body.data = bodyData;
            Log.i(TAG, "validateReceipt(): body=" + (new Gson()).toJson(body));
            try {
                retrofit2.Response response = ZypeApi.getInstance().getApi().verifyPurchaseAmazon(body).execute();
                if (response.isSuccessful()) {
                    Log.i(TAG, "validateReceipt(): Receipt is valid");
                    Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
                    return requestId;
                }
                else {
                    Log.i(TAG, "validateReceipt(): Receipt is not valid");
//                    Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
//                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
                    Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
                    return requestId;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "validateReceipt(): Error marketplace connect call");
                Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
                listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
                return requestId;
            }
        }
        else {
            Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
            listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
            return requestId;
        }
    }

    /**
     * Creates an instance of {@link AReceiptVerifier}.
     *
     * @return An instance of {@link AReceiptVerifier}.
     */
    public static AReceiptVerifier createInstance(Context context) {
        return new ZypeReceiptVerificationService();
    }

}

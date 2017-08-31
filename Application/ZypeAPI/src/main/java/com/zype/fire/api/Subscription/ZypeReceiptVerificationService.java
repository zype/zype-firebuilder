package com.zype.fire.api.Subscription;

import android.content.Context;

import com.amazon.android.utils.Preferences;
import com.amazon.inapppurchase.AReceiptVerifier;
import com.amazon.purchase.IPurchase;
import com.amazon.purchase.model.Receipt;
import com.amazon.purchase.model.Response;
import com.amazon.purchase.model.UserData;
import com.zype.fire.api.Model.BifrostResponse;
import com.zype.fire.api.ZypeApi;
import com.zype.fire.api.ZypeSettings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Evgeny Cherkasov on 30.08.2017.
 *
 * Zype implementation of {@link AReceiptVerifier} interface.
 */

public class ZypeReceiptVerificationService extends AReceiptVerifier {
    /**
     * {@inheritDoc}
     */
    @Override
    public String validateReceipt(Context context, String requestId, String sku, UserData
            userData, Receipt receipt, IPurchase.PurchaseListener listener) {

        Map<String, String> fieldParams = new HashMap<>();
        // TODO: Get preference id from ZypeAuthentication
        fieldParams.put(ZypeApi.APP_KEY, ZypeSettings.APP_KEY);
        fieldParams.put(ZypeApi.SUBSCRIPTION_CONSUMER_ID, Preferences.getString("ZypeConsumerId"));
        fieldParams.put(ZypeApi.SUBSCRIPTION_DEVICE_TYPE, "amazon");
        fieldParams.put(ZypeApi.SUBSCRIPTION_RECEIPT_ID, receipt.getReceiptId());
        fieldParams.put(ZypeApi.SUBSCRIPTION_SHARED_SECRET, ZypeSettings.AMAZON_SHARED_KEY);
        // This hack is removing all dots from sku because Bifrost service does not allows any symbols
        // except letters and numbers
        fieldParams.put(ZypeApi.SUBSCRIPTION_THIRD_PARTY_ID, sku.replaceAll("\\.", ""));
        fieldParams.put(ZypeApi.SUBSCRIPTION_USER_ID, userData.getUserId());
        try {
            retrofit2.Response response = ZypeApi.getInstance().getApi().verifySubscription(fieldParams).execute();
            if (response.isSuccessful()) {
                BifrostResponse bifrostResponse = (BifrostResponse) response.body();
                if (bifrostResponse.success) {
                    Response purchaseResponse = new Response(requestId, Response.Status.SUCCESSFUL, null);
                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, true, userData);
                    return requestId;
                }
                else {
                    Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
                    listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
                    return requestId;
                }
            }
            else {
                Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
                listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
                return requestId;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            Response purchaseResponse = new Response(requestId, Response.Status.FAILED, null);
            listener.isPurchaseValidResponse(purchaseResponse, sku, receipt, false, userData);
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

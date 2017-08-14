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
package com.amazon.purchase;

import android.os.AsyncTask;
import android.util.Log;

import com.amazon.purchase.model.Product;
import com.amazon.purchase.model.Receipt;
import com.amazon.purchase.model.Response;

import java.util.Map;
import java.util.Set;

/**
 * Class to trigger the products call.
 * The {@link #doInBackground} triggers the call and saves the mPurchaseManagerListener in a map.
 * The response of this call searches the map for the mPurchaseManagerListener and communicates the
 * response via this mPurchaseManagerListener.
 *
 * The {@link IPurchase#purchase}  call can be either synchronous
 * or asynchronous. If its sync, the call response is triggered before the listener map is actually
 * updated. This is because the response call is unable to find the listener in the listener map so
 * its unable to communicate the response. In this case the response call saves the response in a
 * response map and returns. The {@link #onPostExecute(Object)} will then find the response in the
 * response map and communicate via the listener.
 */
class ProductsAction extends AsyncTask<Void, Void, String> {
    private static final String TAG = ProductsAction.class.getName();

    private PurchaseManager mPurchaseManager;
    private Set<String> mSkuSet;
    private PurchaseManagerListener mPurchaseManagerListener;

    /**
     * Constructor.
     *
     * @param purchaseManager The instance of the purchase manager.
     * @param skuSet          The SKU list to get product data for.
     * @param listener        The listener for the response.
     */
    public ProductsAction(PurchaseManager purchaseManager, Set<String> skuSet, PurchaseManagerListener listener) {
        this.mPurchaseManager = purchaseManager;
        this.mSkuSet = skuSet;
        this.mPurchaseManagerListener = listener;
    }

    /**
     * {@inheritDoc}
     * Triggers the products call and stores the listener in the map keyed with the request id.
     *
     * @return The request id received from the purchase call.
     */
    @Override
    protected String doInBackground(Void... params) {
        String requestId = mPurchaseManager.mPurchaseSystem.getProducts(mSkuSet);
        Log.d(TAG, "Products called with " + requestId);
        mPurchaseManager.productsObjectMap.put(requestId, this);
        return requestId;
    }

    /**
     * {@inheritDoc}
     * Triggers the onResponse logic if products response is available.
     *
     * @param requestId The request id received from the purchase call.
     */
    @Override
    protected void onPostExecute(String requestId) {
        if (!mPurchaseManager.responseMap.containsKey(requestId)) {
            Log.d(TAG, "Products call not complete yet");
            return;
        }
        onResponse(requestId);
    }

    /**
     * Utility to execute logic required on products call to inform the user of the result.
     *
     * @param requestId The request id of this request.
     */
    public void informUser(String requestId) {
        onResponse(requestId);
    }

    /**
     * Inform the user of the request completion.
     *
     * @param requestId The request id of this call.
     */
    private void onResponse(String requestId) {
        Log.d(TAG, "onResponse():  request=" + requestId);

        Response response = mPurchaseManager.responseMap.get(requestId);
        Map<String, Product> products = mPurchaseManager.productsDataMap.get(requestId);

        if (mPurchaseManagerListener != null) {
            mPurchaseManagerListener.onProductDataResponse(response, products);
        }

        cleanUp(requestId);
    }

    /**
     * Removes all objects stored for this call.
     *
     * @param requestId The request id.
     */
    private void cleanUp(String requestId) {
        mPurchaseManager.productsObjectMap.remove(requestId);
        mPurchaseManager.responseMap.remove(requestId);
        mPurchaseManager.purchaseReceiptMap.remove(requestId);
    }

}

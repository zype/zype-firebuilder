package com.zype.fire.api;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.zype.fire.api.AppConfiguration;
import com.zype.fire.api.Model.PlanData;
import com.zype.fire.api.Model.PlanResponse;
import com.zype.fire.api.R;
import com.zype.fire.api.Util.FileHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarketplaceGateway {
    private static final String TAG = MarketplaceGateway.class.getSimpleName();

    private static MarketplaceGateway INSTANCE;

    private static AppConfiguration appConfiguration;
    private Context context;
    private static List<PlanData> plans;

    public static MarketplaceGateway getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MarketplaceGateway.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MarketplaceGateway(context);
                }
            }
        }
        return INSTANCE;
    }

    private MarketplaceGateway(Context context) {
        this.context = context.getApplicationContext();
        appConfiguration = readAppConfiguration();
    }

    // Plans

    public List<PlanData> getPlans() {
        return plans;
    }

    public interface ILoadPlansTask {
        void onComplete();
    }

    public static class LoadPlansTask extends AsyncTask<Void, Void, List<PlanData>> {
        private ILoadPlansTask listener;

        public LoadPlansTask(ILoadPlansTask listener) {
            this.listener = listener;
        }

        @Override
        protected List<PlanData> doInBackground(Void... voids) {
            return loadPlans(appConfiguration.planIds);
        }

        @Override
        protected void onPostExecute(List<PlanData> result) {
            plans = result;
            listener.onComplete();
        }

        @Override
        protected void onCancelled(List<PlanData> planData) {
            plans = new ArrayList<>();
            listener.onComplete();
        }

        @Override
        protected void onCancelled() {
            plans = new ArrayList<>();
            listener.onComplete();
        }
    }

    public static List<PlanData> loadPlans(List<String> planIds) {
        List<PlanData> result = new ArrayList<>();
        for (String planId : planIds) {
            PlanData planData = loadPlan(planId);
            if (planData != null) {
                result.add(planData);
            }
        }
        return result;
    }

    private static PlanData loadPlan(String planId) {
        PlanResponse response = ZypeApi.getInstance().getPlan(planId);
        if (response != null) {
            Log.d(TAG, "loadPlan(): success, id=" + response.response.id);
            return response.response;
        }
        else {
            Log.e(TAG, "loadPlan(): failed");
            return null;
        }
    }

    public PlanData findPlanBySku(String sku) {
        for (PlanData plan : plans) {
              if (getSubscriptionSku(plan).equals(sku)) {
                return plan;
            }
        }
        return null;
    }

    // SKU data

    public List<Map<String, String>> getSkuData() {
        List<Map<String, String>> result = new ArrayList<>();
        for (PlanData plan : plans) {
            Map<String, String> skuData = new HashMap<>();
            skuData.put("productType", "SUBSCRIBE");
            skuData.put("sku", getSubscriptionPartialSku(plan));
            skuData.put("purchaseSku", getSubscriptionSku(plan));
            skuData.put("id", getSubscriptionSku(plan));
            result.add(skuData);
        }
        return result;
    }

    private String getSubscriptionSku(PlanData plan) {
        if (ZypeConfiguration.getMarketplace().equals(Marketplace.GOOGLE)) {
            return plan.marketplaceIds.google;
        }
        else if (ZypeConfiguration.getMarketplace().equals(Marketplace.AMAZON)) {
            return plan.marketplaceIds.amazon;
        }
        throw new IllegalStateException("Invalid 'Marketplace' value: " + ZypeConfiguration.getMarketplace());
    }

    private String getSubscriptionPartialSku(PlanData plan) {
        if (ZypeConfiguration.getMarketplace().equals(Marketplace.GOOGLE)) {
            return plan.marketplaceIds.google;
        }
        else if (ZypeConfiguration.getMarketplace().equals(Marketplace.AMAZON)) {
            return plan.marketplaceIds.amazon.substring(0, plan.marketplaceIds.amazon.lastIndexOf("."));
        }
        throw new IllegalStateException("Invalid 'Marketplace' value: " + ZypeConfiguration.getMarketplace());
    }

    public Set<String> getSubscriptionSkus() {
        Set<String> result = new HashSet<>();
        for (PlanData plan : plans) {
            result.add(getSubscriptionSku(plan));
        }
        return result;
    }

    // Util

    private AppConfiguration readAppConfiguration() {
        AppConfiguration result = null;

        String jsonAppConfiguration = FileHelper.readAssetsFile(context, R.raw.zype_app_configuration);
        if (!TextUtils.isEmpty(jsonAppConfiguration)) {
            Gson gson = new Gson();
            result = gson.fromJson(jsonAppConfiguration, AppConfiguration.class);
        }

        return result;
    }
}

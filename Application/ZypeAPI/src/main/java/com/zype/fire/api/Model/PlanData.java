package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlanData {

    @Expose
    public boolean active;

    @SerializedName("_id")
    @Expose
    public String id;

    @SerializedName("_keywords")
    @Expose
    public List<String> keywords;

    @SerializedName("third_party_id")
    @Expose
    public String thirdPartyId;

    @SerializedName("marketplace_ids")
    @Expose
    public MarketplaceIds marketplaceIds;
}

package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MarketplaceIds {
    @SerializedName("amazon_fire_tv")
    @Expose
    public String amazon;

    @SerializedName("googleplay")
    @Expose
    public String google;
}

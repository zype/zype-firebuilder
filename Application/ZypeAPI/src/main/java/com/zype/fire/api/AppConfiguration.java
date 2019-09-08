package com.zype.fire.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AppConfiguration {
    @SerializedName("lockIcons")
    public boolean lockIcons;

    @SerializedName("marketplaceConnectSVOD")
    boolean marketplaceConnectSVOD;

    @SerializedName("planIds")
    public List<String> planIds;

    @SerializedName("showItemTitles")
    public boolean showItemTitles;
}

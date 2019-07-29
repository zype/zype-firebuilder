package com.zype.fire.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AppConfiguration {
    @SerializedName("planIds")
    public List<String> planIds;
}

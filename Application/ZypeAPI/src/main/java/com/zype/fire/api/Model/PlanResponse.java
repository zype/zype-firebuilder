package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlanResponse {
  @SerializedName("response")
  @Expose
  public PlanData response;
}

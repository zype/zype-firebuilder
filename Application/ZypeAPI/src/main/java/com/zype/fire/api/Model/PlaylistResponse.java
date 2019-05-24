package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlaylistResponse {
  @SerializedName("response")
  @Expose
  public PlaylistData response = new PlaylistData();

}

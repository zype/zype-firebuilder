package com.zype.fire.api.Model;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Channel implements Serializable {
  @SerializedName("_id")
  @Expose
  public String id;

  @SerializedName("name")
  @Expose
  public String name;

  @SerializedName("status")
  @Expose
  public String status;

  @SerializedName("program_guide_entry_count")
  public int programGuideEntryCount;
  private List<Program> programs = new ArrayList<>();

  public int getPerPageCount() {
    return 500;
  }

  public boolean isActive() {
    if (!TextUtils.isEmpty(status)) {
      return status.equalsIgnoreCase("synced");
    }

    return false;
  }

  public List<Program> getPrograms() {
    return programs;
  }

  public void addProgram(List<Program> programs) {

    for(Program program : programs) {
      if(program.getStartTime() > (System.currentTimeMillis() - 24 * 60 * 60 * 1000)) {
        this.programs.add(program);
      }
    }

  }
  public void setPrograms(List<Program> programs) {
    this.programs = programs;
  }


}

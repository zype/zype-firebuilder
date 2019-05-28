package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Channel implements Serializable {

  private static final int TIME_BLOCK = 60 * 60 * 1000;

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

  @SerializedName("video_ids")
  private List<String> videoIds = new ArrayList<>();

  public boolean isActive() {
    /*if (!TextUtils.isEmpty(status)) {
      return status.equalsIgnoreCase("synced");
    }*/

    return true;
  }

  public List<Program> getPrograms() {
    return programs;
  }

  public void setPrograms(List<Program> programs) {
    this.programs = programs;
  }

  public void addProgram(List<Program> programs) {

    for (Program program : programs) {
      this.programs.add(program);
    }
  }

  public String getVideoId() {
    if (videoIds != null && videoIds.size() > 0) {
      return videoIds.get(0);
    }
    return "";
  }

  private long getMostRecentHourTime() {
    long currentTime = DateTime.now().getMillis() - DateTimeZone.getDefault().getOffset(DateTime.now());

    DateTime dateTime = DateTime.now().withMillis(currentTime).withSecondOfMinute(0);

    if (dateTime.getMinuteOfHour() >= 30) {
      dateTime = dateTime.withMinuteOfHour(30);
    } else {
      dateTime = dateTime.withMinuteOfHour(0);
    }

    return dateTime.getMillis();
  }

  public void addCurrentTimeProgramIfMissing(String text) {
    boolean currentProgramAvailable = false;
    long currentTime = getMostRecentHourTime();

    for (Program program : programs) {
      if (program.getStartTime() <= currentTime && program.getEndTime() >= currentTime) {
        currentProgramAvailable = true;
        break;
      }
    }

    if (currentProgramAvailable) {
      return;
    }

    if (programs.size() == 0) {
      //add a 1 hr block
      Program program = new Program();
      program.setStartTime(currentTime);
      program.setEndTime(currentTime + TIME_BLOCK);
      program.name = text;
      addProgram(Arrays.asList(program));
    } else {
      Program beforeCurrenTime = null;
      Program afterCurrentTime = null;

      for (Program program : programs) {
        if (program.getStartTime() < currentTime) {
          beforeCurrenTime = program;
        } else {
          if (afterCurrentTime == null) {
            afterCurrentTime = program;
          }
        }
      }

      long startTime = currentTime;

      if (beforeCurrenTime.getStartTime() > startTime) {
        startTime = beforeCurrenTime.getStartTime();
      }

      long endTime = currentTime + TIME_BLOCK;

      if (afterCurrentTime != null) {
        if (afterCurrentTime.getStartTime() < endTime) {
          endTime = afterCurrentTime.getStartTime();
        }
      }

      Program program = new Program();
      program.setStartTime(startTime);
      program.setEndTime(endTime);
      program.name = text;
      addProgram(Arrays.asList(program));
    }
  }
}

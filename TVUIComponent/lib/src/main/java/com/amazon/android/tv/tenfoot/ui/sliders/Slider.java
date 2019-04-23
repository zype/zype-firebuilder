package com.amazon.android.tv.tenfoot.ui.sliders;

import java.io.Serializable;

public class Slider implements Serializable {
  private String id;
  private String videoId;

  public String getId() {
    return id;
  }

  public String getVideoId() {
    return videoId;
  }

  public String getPlayListId() {
    return playListId;
  }

  public String getUrl() {
    return url;
  }

  public String getName() {
    return name;
  }

  private String playListId;
  private String url;
  private String name;

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  private int position;

  public static Slider create(String id, String videoId, String playListId, String url, String name) {
    Slider slider = new Slider();
    slider.id = id;
    slider.name = name;
    slider.videoId = videoId;
    slider.playListId = playListId;
    slider.url = url;
    return slider;
  }
}
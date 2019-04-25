package se.kmdev.tvepg.epg.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kristoffer.
 */
public class EPGChannel implements Serializable {

  private final String name;
  private final String imageURL;
  private int channelID;
  private List<EPGEvent> events = new ArrayList();
  private EPGChannel previousChannel;
  private EPGChannel nextChannel;
  private String link = "";

  public EPGChannel(String imageURL, String name, int channelID) {
    this.imageURL = imageURL;
    this.name = name;
    this.channelID = channelID;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public int getChannelID() {
    return channelID;
  }

  public void setChannelID(int channelID) {
    this.channelID = channelID;
  }

  public String getName() {
    return name;
  }

  public String getImageURL() {
    return imageURL;
  }

  public List<EPGEvent> getEvents() {
    return events;
  }

  public EPGChannel getPreviousChannel() {
    return previousChannel;
  }

  public void setPreviousChannel(EPGChannel previousChannel) {
    this.previousChannel = previousChannel;
  }

  public EPGChannel getNextChannel() {
    return nextChannel;
  }

  public void setNextChannel(EPGChannel nextChannel) {
    this.nextChannel = nextChannel;
  }

  public EPGEvent addEvent(EPGEvent event) {
    this.events.add(event);
    return event;
  }
}

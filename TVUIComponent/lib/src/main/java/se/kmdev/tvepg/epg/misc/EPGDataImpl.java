package se.kmdev.tvepg.epg.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.kmdev.tvepg.epg.EPGData;
import se.kmdev.tvepg.epg.domain.EPGChannel;
import se.kmdev.tvepg.epg.domain.EPGEvent;

/**
 * TODO: Add null check when fetching at position etc.
 * Created by Kristoffer on 15-05-23.
 */
public class EPGDataImpl implements EPGData {

  private List<EPGChannel> channels = new ArrayList();
  private Map<String, EPGChannel> channelsByName = new HashMap<>();
  //private List<List<EPGEvent>> events = Lists.newArrayList();

  public EPGDataImpl(Map<EPGChannel, List<EPGEvent>> data) {
    channels = new ArrayList(data.keySet());
    //events = Lists.newArrayList(data.values());
    indexChannels();
  }

  public EPGChannel getChannel(int position) {
    return channels.get(position);
  }


  public List<EPGEvent> getEvents(int channelPosition) {
    //return events.get(channelPosition);
    return channels.get(channelPosition).getEvents();
  }

  public EPGEvent getEvent(int channelPosition, int programPosition) {
    //return events.get(channelPosition).get(programPosition);
    return channels.get(channelPosition).getEvents().get(programPosition);
  }

  public int getChannelCount() {
    return channels.size();
  }

  @Override
  public boolean hasData() {
    return !channels.isEmpty();
  }

  public EPGChannel addNewChannel(String channelName) {
    int newChannelID = channels.size();
    EPGChannel newChannel = new EPGChannel("http://resolvethis.com/epg/be/" + channelName + ".png", channelName, newChannelID);
    if (newChannelID > 0) {
      EPGChannel previousChannel = channels.get(newChannelID - 1);
      previousChannel.setNextChannel(newChannel);
      newChannel.setPreviousChannel(previousChannel);
    }
    channels.add(newChannel);
    channelsByName.put(newChannel.getName(), newChannel);
    return newChannel;
  }

  private void indexChannels() {
    channelsByName = new HashMap<>();
    for (int j = 0; j < channels.size(); j++) {
      EPGChannel channel = channels.get(j);
      channelsByName.put(channel.getName(), channel);
    }
  }
}

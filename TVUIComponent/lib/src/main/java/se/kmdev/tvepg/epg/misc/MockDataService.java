package se.kmdev.tvepg.epg.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.kmdev.tvepg.epg.EPG;
import se.kmdev.tvepg.epg.domain.EPGChannel;
import se.kmdev.tvepg.epg.domain.EPGEvent;

/**
 * Created by Kristoffer on 15-05-24.
 */
public class MockDataService {

  private static Random rand = new Random();

  private static List<Integer> availableEventLength = new ArrayList<Integer>(Arrays.asList(
      1000 * 60 * 15,  // 15 minutes
      1000 * 60 * 30,  // 30 minutes
      1000 * 60 * 45,  // 45 minutes
      1000 * 60 * 60,  // 60 minutes
      1000 * 60 * 120  // 120 minutes
  ));

  private static List<String> availableEventTitles = new ArrayList<String>(Arrays.asList(
      "Avengers",
      "How I Met Your Mother",
      "Silicon Valley",
      "Late Night with Jimmy Fallon",
      "The Big Bang Theory",
      "Leon",
      "Die Hard"
  ));

  private static List<String> channelName = new ArrayList<String>(Arrays.asList(
      "Channel1",
      "Channel2",
      "Channel3",
      "Channel4",
      "Channel5"
  ));

  private static List<String> availableChannelLogos = new ArrayList<String>(Arrays.asList(
      "http://tvfiles.alphacoders.com/100/hdclearart-10.png",
      "http://tvfiles.alphacoders.com/100/hdclearart-10.png",
      "http://tvfiles.alphacoders.com/100/hdclearart-10.png",
      "https://pbs.twimg.com/profile_images/630285593268752384/iD1MkFQ0.png",
      "https://pbs.twimg.com/profile_images/630285593268752384/iD1MkFQ0.png"
  ));

  public static Map<EPGChannel, List<EPGEvent>> getMockData() {
    HashMap<EPGChannel, List<EPGEvent>> result = new LinkedHashMap<>();

    long nowMillis = System.currentTimeMillis();
    EPGChannel prevChannel = null;

    for (int i = 0; i < 20; i++) {
      EPGChannel epgChannel = new EPGChannel(channelName.get(i % 5),
          "Channel " + (i + 1), i);
      if (prevChannel != null) {
        prevChannel.setNextChannel(epgChannel);
        epgChannel.setPreviousChannel(prevChannel);
      }
      result.put(epgChannel, createEvents(epgChannel, nowMillis));
      prevChannel = epgChannel;
    }

    return result;
  }

  private static List<EPGEvent> createEvents(EPGChannel epgChannel, long nowMillis) {
    List<EPGEvent> result = new ArrayList();

    long epgStart = nowMillis - EPG.DAYS_BACK_MILLIS;
    long epgEnd = nowMillis + EPG.DAYS_FORWARD_MILLIS;
    EPGEvent prevEvent = null;
    long currentTime = epgStart;

    while (currentTime <= epgEnd) {
      long eventEnd = getEventEnd(currentTime);
      EPGEvent epgEvent = new EPGEvent(epgChannel, currentTime, eventEnd, availableEventTitles.get(randomBetween(0, 6)), null);
      if (prevEvent != null) {
        prevEvent.setNextEvent(epgEvent);
        epgEvent.setPreviousEvent(prevEvent);
      }
      prevEvent = epgEvent;
      result.add(epgEvent);
      currentTime = eventEnd;
      epgChannel.addEvent(epgEvent);
    }

    return result;
  }

  private static long getEventEnd(long eventStartMillis) {
    long length = availableEventLength.get(randomBetween(0, 4));
    return eventStartMillis + length;
  }

  private static int randomBetween(int start, int end) {
    return start + rand.nextInt((end - start) + 1);
  }
}

package com.amazon.android.tv.tenfoot.ui.epg;

import java.util.List;

public interface EPGData {

    EPGChannel getChannel(int position);

    List<EPGEvent> getEvents(int channelPosition);

    EPGEvent getEvent(int channelPosition, int programPosition);

    int getChannelCount();

    boolean hasData();

}

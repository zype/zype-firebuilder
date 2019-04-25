package se.kmdev.tvepg.epg;

import se.kmdev.tvepg.epg.domain.EPGChannel;
import se.kmdev.tvepg.epg.domain.EPGEvent;

/**
 * Created by Kristoffer on 15-05-25.
 */
public interface EPGClickListener {

    void onChannelClicked(int channelPosition, EPGChannel epgChannel);

    void onEventClicked(EPGEvent epgEvent);

    void onEventSelected(EPGEvent epgEvent);

    void onResetButtonClicked();
}

package com.amazon.android.tv.tenfoot.ui.epg;

public interface EPGClickListener {

    void onChannelClicked(int channelPosition, EPGChannel epgChannel);

    void onEventClicked(EPGEvent epgEvent);

    void onEventSelected(EPGEvent epgEvent);

    void onResetButtonClicked();
}

package com.amazon.android.tv.tenfoot.ui.epg;

import com.zype.fire.api.Model.Channel;
import com.zype.fire.api.Model.ChannelResponse;
import com.zype.fire.api.Model.Program;
import com.zype.fire.api.Model.ProgramResponse;
import com.zype.fire.api.ZypeApi;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

public class EPGDataManager {
  private static EPGDataManager instance;
  public final BehaviorSubject<EPGData> epgDataSubject = BehaviorSubject.create();
  private CompositeSubscription compositeSubscription = new CompositeSubscription();

  private EPGDataManager() {

  }

  public static synchronized EPGDataManager getInstance() {
    if (instance == null) {
      instance = new EPGDataManager();
    }

    return instance;
  }

  public void load() {
    compositeSubscription.clear();

    compositeSubscription.add(Observable.just(true).subscribeOn(Schedulers.io())
        .observeOn(Schedulers.newThread()).subscribe(aBoolean -> {
          ZypeApi zypeApi = ZypeApi.getInstance();

          List<Channel> channels = new ArrayList<>();

          int pageIndex = 1;

          ChannelResponse epgChannelResponse = zypeApi.loadEpgChannels(pageIndex);
          channels.addAll(epgChannelResponse.response);

          if (epgChannelResponse.pagination != null) {
            if (epgChannelResponse.pagination.pages != null) {
              pageIndex++;

              for (int i = pageIndex; i <= epgChannelResponse.pagination.pages; i++) {
                epgChannelResponse = zypeApi.loadEpgChannels(i);
                channels.addAll(epgChannelResponse.response);
              }
            }
          }

          compositeSubscription.add(Observable.just(channels).flatMapIterable(channelList -> channelList)
              .filter(epgChannel -> epgChannel.isActive()).flatMap(epgChannel -> {
                int eventPageIndex = 1;
                ProgramResponse programResponse = zypeApi.loadEpgEvents(epgChannel, eventPageIndex);
                epgChannel.addProgram(programResponse.response);

                if (programResponse.pagination != null) {
                  if (programResponse.pagination.pages != null) {
                    eventPageIndex++;

                    for (int i = eventPageIndex; i <= programResponse.pagination.pages; i++) {
                      programResponse = zypeApi.loadEpgEvents(epgChannel, i);

                      if(programResponse != null) {
                        epgChannel.addProgram(programResponse.response);
                      }
                    }
                  }
                }

                return Observable.just(epgChannel);
              }, 3).filter(epgChannel -> epgChannel.getPrograms().size() > 0)
              .toSortedList((epgChannel1, epgChannel2) -> epgChannel1.name.compareToIgnoreCase(epgChannel2.name))

              .subscribe(epgChannels -> {
                buildEpg(epgChannels);

              }, throwable -> {
                throwable.printStackTrace();
              }));

        }, throwable -> {

        }));
  }

  private void buildEpg(List<Channel> channels) {
    EPGChannel prevChannel = null;
    int pos = 0;

    List<EPGChannel> epgChannels = new ArrayList<>();

    for (Channel channel : channels) {
      EPGChannel epgChannel = new EPGChannel("", channel.name, pos++, channel.id);
      epgChannel.setPreviousChannel(prevChannel);

      EPGEvent prevEpgEvent = null;

      for (Program program : channel.getPrograms()) {
        EPGEvent epgEvent = new EPGEvent(epgChannel, program.getStartTime(), program.getEndTime(), program.name,
            "");

        epgEvent.setPreviousEvent(prevEpgEvent);

        if (prevEpgEvent != null) {
          prevEpgEvent.setNextEvent(epgEvent);
        }

        prevEpgEvent = epgEvent;
        epgChannel.addEvent(epgEvent);
      }

      if (prevChannel != null) {
        prevChannel.setNextChannel(epgChannel);
      }
      epgChannels.add(epgChannel);
      prevChannel = epgChannel;
    }

    epgDataSubject.onNext(new EPGDataImpl(epgChannels));
  }
}

package com.amazon.android.tv.tenfoot.ui.epg;

import com.zype.fire.api.Model.Channel;
import com.zype.fire.api.Model.Program;
import com.zype.fire.api.Model.ProgramResponse;
import com.zype.fire.api.ZypeApi;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

public class EPGDataManager {
  private static final int EPG_INTERVAL_IN_HOURS = 4;
  private static EPGDataManager instance;
  public final BehaviorSubject<EPGData> epgDataSubject = BehaviorSubject.create();
  private CompositeSubscription compositeSubscription = new CompositeSubscription();

  private EPGDataManager() {

  }

  public void setNowPlayingText(String nowPlayingText) {
    this.nowPlayingText = nowPlayingText;
  }

  private String nowPlayingText = "";

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

          List<Channel> channelsList = zypeApi.loadEpgChannels();
          channels.addAll(channelsList);

          compositeSubscription.add(Observable.just(channels).flatMapIterable(channelList -> channelList)
              .filter(epgChannel -> epgChannel.isActive()).flatMap(epgChannel -> {
                String startDate = DateTimeFormat.forPattern("yyyy-MM-dd").print(DateTime.now().minusDays(3));
                String endDate = DateTimeFormat.forPattern("yyyy-MM-dd").print(DateTime.now().plusDays(1));
                ProgramResponse programResponse = zypeApi.loadEpgEvents(epgChannel, startDate, endDate);
                epgChannel.addProgram(programResponse.response);
                return Observable.just(epgChannel);
              }, 3).filter(epgChannel -> epgChannel.getPrograms().size() > 0)
              .toSortedList((epgChannel1, epgChannel2) -> epgChannel1.name.compareToIgnoreCase(epgChannel2.name))

              .subscribe(epgChannels -> {
                buildEpg(epgChannels);
                loadAgain();

              }, throwable -> {
                loadAgain();
              }));

        }, throwable -> {
          loadAgain();
        }));
  }

  private void loadAgain() {
    compositeSubscription.clear();
    compositeSubscription.add(Observable.timer(EPG_INTERVAL_IN_HOURS, TimeUnit.HOURS).subscribe(aLong -> {
      load();
    }));
  }

  private void buildEpg(List<Channel> channels) {
    EPGChannel prevChannel = null;
    int pos = 0;

    List<EPGChannel> epgChannels = new ArrayList<>();

    for (Channel channel : channels) {

      channel.addCurrentTimeProgramIfMissing(nowPlayingText);

      EPGChannel epgChannel = new EPGChannel("", channel.name, pos++, channel.id, channel.getVideoId());
      epgChannel.setPreviousChannel(prevChannel);

      EPGEvent prevEpgEvent = null;

      for (Program program : channel.getPrograms()) {
        EPGEvent epgEvent = new EPGEvent(epgChannel, program.getStartTime(), program.getEndTime(), program.name,
            "", program.startDateTime, program.endDateTime);

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

package com.amazon.android.tv.tenfoot.ui.epg;

import android.os.Bundle;
import android.view.KeyEvent;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;

import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class EpgActivity extends BaseActivity {
  private EPG epg;
  private CompositeSubscription compositeSubscription = new CompositeSubscription();

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.epg_activity_layout);
    initListener();
  }

  private void initListener() {
    epg = (EPG) findViewById(R.id.epg);
    epg.setEPGClickListener(new EPGClickListener() {
      @Override
      public void onChannelClicked(int channelPosition, EPGChannel epgChannel) {

      }

      @Override
      public void onEventClicked(EPGEvent epgEvent) {
        //launch player activity
      }

      @Override
      public void onEventSelected(EPGEvent epgEvent) {
        //dataSet(epgEvent);
      }


      @Override
      public void onResetButtonClicked() {
        // Reset button clicked
        epg.recalculateAndRedraw(null, true);
      }
    });

    compositeSubscription.add(EPGDataManager.getInstance().epgDataSubject
        .observeOn(AndroidSchedulers.mainThread()).subscribe(epgData -> {
          epg.setEPGData(epgData);
        }, throwable -> {

        }));
  }

  @Override
  public void setRestoreActivityValues() {

  }

  @Override
  protected void onDestroy() {
    if (epg != null) {
      epg.clearEPGImageCache();
    }

    compositeSubscription.clear();
    super.onDestroy();
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    switch (event.getKeyCode()) {
      case KeyEvent.KEYCODE_BACK:
        if (event.getAction() == KeyEvent.ACTION_UP) {
          finish();
        }
    }
    return super.dispatchKeyEvent(event);
  }
}

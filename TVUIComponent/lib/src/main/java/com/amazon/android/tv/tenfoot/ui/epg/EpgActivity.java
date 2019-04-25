package com.amazon.android.tv.tenfoot.ui.epg;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;

import se.kmdev.tvepg.epg.EPG;
import se.kmdev.tvepg.epg.EPGClickListener;
import se.kmdev.tvepg.epg.EPGData;
import se.kmdev.tvepg.epg.domain.EPGChannel;
import se.kmdev.tvepg.epg.domain.EPGEvent;
import se.kmdev.tvepg.epg.misc.EPGDataImpl;
import se.kmdev.tvepg.epg.misc.MockDataService;

public class EpgActivity extends BaseActivity {
  private EPG epg;

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


    // Do initial load of data.
    new AsyncLoadEPGData(epg).execute();
  }

  @Override
  public void setRestoreActivityValues() {

  }

  @Override
  protected void onDestroy() {
    if (epg != null) {
      epg.clearEPGImageCache();
    }
    super.onDestroy();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
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

  private static class AsyncLoadEPGData extends AsyncTask<Void, Void, EPGData> {

    EPG epg;

    public AsyncLoadEPGData(EPG epg) {
      this.epg = epg;
    }

    @Override
    protected EPGData doInBackground(Void... voids) {
      return new EPGDataImpl(MockDataService.getMockData());
    }

    @Override
    protected void onPostExecute(EPGData epgData) {
      epg.setEPGData(epgData);
      epg.recalculateAndRedraw(null, false);
    }
  }

}

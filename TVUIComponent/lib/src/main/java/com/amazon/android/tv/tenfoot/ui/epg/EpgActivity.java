package com.amazon.android.tv.tenfoot.ui.epg;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.content.Content;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;
import com.amazon.android.ui.fragments.AlertDialogFragment;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import static com.amazon.android.model.content.Content.EXTRA_PREVIEW_IDS;

public class EpgActivity extends BaseActivity {
  private EPG epg;
  private CompositeSubscription compositeSubscription = new CompositeSubscription();
  private ProgressBar progressBar;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.epg_activity_layout);
    progressBar = (ProgressBar) findViewById(R.id.progressView);
    progressBar.setVisibility(View.VISIBLE);
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
        if (TextUtils.isEmpty(epgEvent.getChannel().getVideoId()) || epgEvent.getStart() > DateTime.now().getMillis()) {

          AlertDialogFragment.createAndShowAlertDialogFragment(EpgActivity.this,
              getString(com.amazon.android.contentbrowser.R.string.alert),
              getString(com.amazon.android.contentbrowser.R.string.alert_msg),
              getString(com.amazon.android.contentbrowser.R.string.ok),
              null, new AlertDialogFragment.IAlertDialogListener() {
                @Override
                public void onDialogPositiveButton(AlertDialogFragment alertDialogFragment) {

                }

                @Override
                public void onDialogNegativeButton(AlertDialogFragment alertDialogFragment) {

                }
              });
          return;
        }

        Content content = new Content();
        List<String> list = new ArrayList<>();
        list.add(epgEvent.getChannel().getVideoId());
        content.setExtraValue(EXTRA_PREVIEW_IDS, list);

        if (epgEvent.getEnd() < DateTime.now().getMillis()) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append("&start=" + epgEvent.getStartDateTime());
          stringBuilder.append("&end=" + epgEvent.getEndDateTime());
          ContentBrowser.getInstance(EpgActivity.this).switchToPlayTrailerScreen(content, stringBuilder.toString());
        } else {
          ContentBrowser.getInstance(EpgActivity.this).switchToPlayTrailerScreen(content);
        }
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
        .delay(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(epgData -> {
          epg.setEPGData(epgData);
          progressBar.setVisibility(View.GONE);
        }, throwable -> {
          progressBar.setVisibility(View.GONE);
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

  public void onBackPressed() {
    if (!epg.backPressed()) {
      finish();
    }
  }
}

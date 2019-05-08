package com.amazon.android.tv.tenfoot.ui.epg;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.base.BaseActivity;

import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

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

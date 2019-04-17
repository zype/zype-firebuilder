package com.amazon.android.tv.tenfoot.ui.sliders;

import android.os.Bundle;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;

import com.zype.fire.api.Model.Image;
import com.zype.fire.api.Model.ZobjectContentData;

import java.util.List;

public class HeroSliderFragment extends RowsFragment {

  private ArrayObjectAdapter rowsAdapter;
  private HeroCardPresenter cardPresenter;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    loadRows();
  }

  private void loadRows() {
    ListRowPresenter presenter = new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL, true);
    presenter.setShadowEnabled(false);
    presenter.setSelectEffectEnabled(false);

    rowsAdapter = new ArrayObjectAdapter(presenter);
    cardPresenter = new HeroCardPresenter();

    List<ZobjectContentData> sliderList = HeroSlider.getInstance().getSliders();

    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);

    for (ZobjectContentData sliderData : sliderList) {

      for (Image image : sliderData.images) {
        Slider slider = Slider.create(sliderData.id, "", sliderData.playlistid, image.url,
            sliderData.friendlyTitle);
        listRowAdapter.add(slider);
      }
    }

    rowsAdapter.add(new ListRow(listRowAdapter));
    setAdapter(rowsAdapter);
  }


}
package com.amazon.android.tv.tenfoot.ui.sliders;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.VerticalGridView;

import com.zype.fire.api.Model.Image;
import com.zype.fire.api.Model.ZobjectContentData;

import java.util.List;

public class HeroSliderFragment extends RowsFragment {

  private ArrayObjectAdapter rowsAdapter;
  private HeroCardPresenter cardPresenter;
  private OnHeroSliderSelected mCallback;
  private static final int WAIT_BEFORE_FOCUS_REQUEST_MS = 500;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    try {
      mCallback = (OnHeroSliderSelected) getActivity();
    } catch (ClassCastException e) {
      throw new ClassCastException(getActivity().toString() +
          " must implement OnBrowseRowListener: " + e);
    }
    loadRows();
  }

  private void loadRows() {
    ListRowPresenter presenter = new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, true);
    presenter.setShadowEnabled(false);
    presenter.setSelectEffectEnabled(true);

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

    setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
      mCallback.onSliderSelected((Slider) item);
    });

    if (sliderList.size() > 0) {
      //run the timer to toggle the positions
      Handler handler = new Handler(Looper.getMainLooper());
      handler.postDelayed(() -> {
        if (getView() != null) {
          VerticalGridView verticalGridView = findGridViewFromRoot(getView());
          if (verticalGridView != null) {
            verticalGridView.requestFocus();

            verticalGridView.setOnFocusChangeListener((view, b) -> {
              if(b) {

              }
            });
          }
        }
      }, WAIT_BEFORE_FOCUS_REQUEST_MS);

    }
  }


  public interface OnHeroSliderSelected {

    void onSliderSelected(Slider slider);
  }


}
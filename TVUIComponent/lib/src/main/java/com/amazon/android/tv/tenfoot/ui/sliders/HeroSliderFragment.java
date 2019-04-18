package com.amazon.android.tv.tenfoot.ui.sliders;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.View;

import com.zype.fire.api.Model.Image;
import com.zype.fire.api.Model.ZobjectContentData;

import java.util.ArrayList;
import java.util.List;

public class HeroSliderFragment extends RowsFragment {

  private static final int WAIT_BEFORE_FOCUS_REQUEST_MS = 500;
  private static final int WAIT_NEXT_SCROLL_MS = 10000;

  private ArrayObjectAdapter rowsAdapter;
  private HeroCardPresenter cardPresenter;
  private OnHeroSliderSelected mCallback;
  private int selectedIndex;
  private Handler mHandler = new Handler(Looper.getMainLooper());
  private HeroCardAdapter listRowAdapter = null;

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

  private void registerNextScroll() {
    mHandler.removeCallbacksAndMessages(null);

    mHandler.postDelayed(() -> {

      if (hasFocus()) {
        scrollToNextItem(true);
      }

      registerNextScroll();

    }, WAIT_NEXT_SCROLL_MS);
  }

  private void scrollToNextItem(boolean smooth) {
    if (getView() != null) {
      VerticalGridView verticalGridView = findGridViewFromRoot(getView());

      if (verticalGridView != null) {
        verticalGridView.requestFocus();
      }

      View view = verticalGridView.getLayoutManager().getChildAt(0);
      ItemBridgeAdapter.ViewHolder ibvh = (ItemBridgeAdapter.ViewHolder)
          verticalGridView.getChildViewHolder(view);
      ListRowPresenter rowPresenter = (ListRowPresenter) ibvh.getPresenter();
      ListRowPresenter.ViewHolder vh = (ListRowPresenter.ViewHolder)
          rowPresenter.getRowViewHolder(ibvh.getViewHolder());

      if(smooth) {
        vh.getGridView().setSelectedPositionSmooth(selectedIndex + 1);
      }
      else {
        vh.getGridView().setSelectedPosition(selectedIndex + 1);
      }
    }
  }

  private void loadRows() {
    ListRowPresenter presenter = new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, true);
    presenter.setShadowEnabled(false);
    presenter.setSelectEffectEnabled(true);

    rowsAdapter = new ArrayObjectAdapter(presenter);
    cardPresenter = new HeroCardPresenter();

    List<ZobjectContentData> sliderList = HeroSlider.getInstance().getSliders();

    listRowAdapter = new HeroCardAdapter(cardPresenter);

    List<Slider> sliders = new ArrayList<>();

    for (ZobjectContentData sliderData : sliderList) {

      for (Image image : sliderData.images) {
        Slider slider = Slider.create(sliderData.id, "", sliderData.playlistid, image.url,
            sliderData.friendlyTitle);
        sliders.add(slider);
      }
    }

    listRowAdapter.addAll(0, sliders);

    final ListRow listRow = new ListRow(listRowAdapter);
    rowsAdapter.add(listRow);
    setAdapter(rowsAdapter);

    setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
      if (item != null) {
        selectedIndex = ((HeroCardPresenter.ViewHolder) itemViewHolder).getIndex();
        registerNextScroll();
      }
    });

    if (sliderList.size() > 0) {
      //run the timer to toggle the positions
      Handler handler = new Handler(Looper.getMainLooper());
      handler.postDelayed(() -> {
        int index = listRowAdapter.realSize() % 2 == 0 ? 1 : 0;
        selectedIndex = (listRowAdapter.size() / 2 + index) - 1;
        scrollToNextItem(false);
      }, WAIT_BEFORE_FOCUS_REQUEST_MS);

    }
  }

  public boolean hasFocus() {
    if (getView() != null) {
      VerticalGridView verticalGridView = findGridViewFromRoot(getView());
      if (verticalGridView != null) {
        return verticalGridView.hasFocus();
      }
    }

    return false;
  }


  public interface OnHeroSliderSelected {

    void onSliderSelected(Slider slider);
  }


}
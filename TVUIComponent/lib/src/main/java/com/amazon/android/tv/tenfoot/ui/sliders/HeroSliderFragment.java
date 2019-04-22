package com.amazon.android.tv.tenfoot.ui.sliders;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.VerticalGridView;
import android.text.TextUtils;
import android.view.View;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.tv.tenfoot.presenter.CustomListRowPresenter;
import com.zype.fire.api.Model.Image;
import com.zype.fire.api.Model.ZobjectContentData;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class HeroSliderFragment extends RowsFragment {

  private static final int WAIT_BEFORE_FOCUS_REQUEST_MS = 500;
  private static final int WAIT_NEXT_SCROLL_MS = 10000;

  private ArrayObjectAdapter rowsAdapter;
  private HeroCardPresenter cardPresenter;
  private OnHeroSliderSelected mCallback;
  private int selectedIndex;
  private Handler mHandler = new Handler(Looper.getMainLooper());
  private HeroCardAdapter listRowAdapter = null;
  private CompositeSubscription mCompositeSubscription = new CompositeSubscription();


  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    try {
      mCallback = (OnHeroSliderSelected) getActivity();
    } catch (ClassCastException e) {
      throw new ClassCastException(getActivity().toString() +
          " must implement OnBrowseRowListener: " + e);
    }

    if (HeroSlider.getInstance().isSliderPresent()) {
      loadRows();
    }

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

      View view = verticalGridView.getLayoutManager().getChildAt(0);
      ItemBridgeAdapter.ViewHolder ibvh = (ItemBridgeAdapter.ViewHolder)
          verticalGridView.getChildViewHolder(view);
      CustomListRowPresenter rowPresenter = (CustomListRowPresenter) ibvh.getPresenter();
      CustomListRowPresenter.ViewHolder vh = (CustomListRowPresenter.ViewHolder)
          rowPresenter.getRowViewHolder(ibvh.getViewHolder());

      if (smooth) {
        vh.getGridView().setSelectedPositionSmooth(selectedIndex + 1);
      } else {
        vh.getGridView().setSelectedPosition(selectedIndex + 1);
      }
    }
  }

  private void loadRows() {
    CustomListRowPresenter customListRowPresenter = new CustomListRowPresenter();

    rowsAdapter = new ArrayObjectAdapter(customListRowPresenter);
    cardPresenter = new HeroCardPresenter();

    List<ZobjectContentData> sliderList = HeroSlider.getInstance().getSliders();

    listRowAdapter = new HeroCardAdapter(cardPresenter);

    List<Slider> sliders = new ArrayList<>();

    String videoId;
    for (ZobjectContentData sliderData : sliderList) {

      videoId = sliderData.videoIds.size() > 0 ? sliderData.videoIds.get(0).toString() : "";
      for (Image image : sliderData.images) {

        Slider slider = Slider.create(sliderData.id, videoId, sliderData.playlistid, image.url,
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

    setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {

      if (item != null && item instanceof Slider) {
        ContentBrowser contentBrowser = ContentBrowser.getInstance(getActivity());

        Slider slider = (Slider) item;

        if (TextUtils.isEmpty(slider.getVideoId())) {
          //load the playlist
          ContentContainer contentContainer = contentBrowser.getPlayList(slider.getPlayListId());

          if (contentContainer != null) {
            contentBrowser
                .setLastSelectedContentContainer(contentContainer)
                .switchToScreen(ContentBrowser.CONTENT_SUBMENU_SCREEN);
          }

        } else {
          //load the videoId

          mCompositeSubscription.add(contentBrowser.getContentById(slider.getVideoId()).observeOn(AndroidSchedulers.mainThread())
              .subscribe(content -> {
                //move the user to the detail screen
                contentBrowser
                    .setLastSelectedContent(content)
                    .switchToScreen(ContentBrowser.CONTENT_DETAILS_SCREEN, content);

              }, throwable -> {

              }));

        }
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


  public void requestFocus() {
    if (getView() != null) {
      VerticalGridView verticalGridView = findGridViewFromRoot(getView());
      if (verticalGridView != null) {
        verticalGridView.requestFocus();
      }
    }
  }

  public interface OnHeroSliderSelected {

    void onSliderSelected(Slider slider);
  }

}
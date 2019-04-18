package com.amazon.android.tv.tenfoot.ui.sliders;

import com.zype.fire.api.Model.ZobjectContentData;
import com.zype.fire.api.Model.ZobjectContentResponse;
import com.zype.fire.api.ZypeApi;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

public class HeroSlider {

  private static HeroSlider heroSlider;

  public List<ZobjectContentData> getSliders() {
    return sliders;
  }

  private List<ZobjectContentData> sliders = new ArrayList();
  private HeroSlider() {

  }

  public static synchronized HeroSlider getInstance() {
    if (heroSlider == null) {
      heroSlider = new HeroSlider();
    }

    return heroSlider;
  }

  public Observable<Boolean> loadContent() {
    return Observable.just(true).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).flatMap(s -> {
      ZobjectContentResponse response = ZypeApi.getInstance().getZobjectTopPlayLists();

      if (response != null) {
        sliders.clear();
        sliders.addAll(response.zobjectContents);
      }

      return Observable.just(true);
    });
  }

  public boolean isSliderPresent() {
    return false;// sliders.size() > 0;
  }
}

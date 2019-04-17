package com.amazon.android.tv.tenfoot.ui.sliders;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.utils.Helpers;
import com.bumptech.glide.Glide;

public class HeroCardPresenter extends Presenter {
  private static final String TAG = "HeroCardPresenter";

  private Context mContext;
  private int mCardWidthDp;
  private int mCardHeightDp;
  private View mInfoField;
  private int mSelectedBackgroundColor = -1;
  private int mDefaultBackgroundColor = -1;

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent) {
    Log.d(TAG, "onCreateViewHolder");
    mContext = parent.getContext();
    mDefaultBackgroundColor =
        ContextCompat.getColor(parent.getContext(), R.color.transparent);
    mSelectedBackgroundColor =
        ContextCompat.getColor(parent.getContext(), R.color.transparent);

    ImageCardView cardView = new ImageCardView(mContext);
    cardView.setFocusable(true);
    cardView.setFocusableInTouchMode(true);
    cardView.setTitleText(null);

    mInfoField = cardView.findViewById(R.id.info_field);
    updateCardBackgroundColor(cardView, false);
    return new ViewHolder(cardView);
  }

  private Point getSize() {
    Point size = new Point();

    if(mContext instanceof Activity) {
      Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
      display.getSize(size);
    }

    return size;
  }

  private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
    int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

    // Both background colors should be set because the view's
    // background is temporarily visible during animations.
    view.setBackgroundColor(color);
    view.findViewById(R.id.info_field).setBackgroundColor(color);
  }

  @Override
  public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
    Slider slider = (Slider) item;
    ((ViewHolder) viewHolder).setSlider(slider);
    int CARD_WIDTH_PX = 20;
    mCardWidthDp = getSize().x - Helpers.convertPixelToDp(mContext, CARD_WIDTH_PX);

    if (!TextUtils.isEmpty(slider.getUrl())) {

      ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(mCardWidthDp, ViewGroup.LayoutParams.MATCH_PARENT);

      Glide.with(((ViewHolder) viewHolder).mCardView.getContext())
          .load(slider.getUrl())
          .into(((ViewHolder) viewHolder).mCardView.getMainImageView());

      mInfoField.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
    }

  }

  @Override
  public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    Log.d(TAG, "onUnbindViewHolder");
    ImageCardView cardView = (ImageCardView) viewHolder.view;
    // Remove references to images so that the garbage collector can free up memory.
    cardView.setBadgeImage(null);
    cardView.setMainImage(null);
  }

  @Override
  public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    Log.d(TAG, "onViewAttachedToWindow");
  }

  static class ViewHolder extends Presenter.ViewHolder {
    private Slider slider;
    private ImageCardView mCardView;

    public ViewHolder(View view) {
      super(view);
      mCardView = (ImageCardView) view;
    }

    public Slider getSlider() {
      return slider;
    }

    public void setSlider(Slider m) {
      slider = m;
    }

    public ImageCardView getCardView() {
      return mCardView;
    }

  }

}
package com.amazon.android.tv.tenfoot.ui.sliders;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.utils.Helpers;
import com.bumptech.glide.Glide;

import static androidx.leanback.widget.ImageCardView.CARD_TYPE_FLAG_IMAGE_ONLY;

public class HeroCardPresenter extends Presenter {
  private static final String TAG = "HeroCardPresenter";

  private Context mContext;
  private int mCardWidthDp;
  private int mCardHeightDp;
  private View mInfoField;
  private int mSelectedBackgroundColor = -1;
  private int mDefaultBackgroundColor = -1;
  private static final int CARD_WIDTH_PX = 320;
  private static final int SINGLE_CARD_WIDTH_PX = 110;
  private static final int CARD_HEIGHT_PX = 250;

    public void setSingleImage(boolean singleImage) {
        this.singleImage = singleImage;
    }

    private boolean singleImage=false;


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
    cardView.setCardType(CARD_TYPE_FLAG_IMAGE_ONLY);
    //cardView.setMainImageScaleType(ScaleType.FIT_CENTER);

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
    ((ViewHolder) viewHolder).setIndex(slider.getPosition());

    mCardWidthDp = getSize().x - Helpers.convertPixelToDp(mContext, singleImage ? SINGLE_CARD_WIDTH_PX : CARD_WIDTH_PX);

    mCardHeightDp = Helpers.convertPixelToDp(mContext, CARD_HEIGHT_PX);

    if (!TextUtils.isEmpty(slider.getUrl())) {

      ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(mCardWidthDp, mCardHeightDp);

      Glide.with(((ViewHolder) viewHolder).mCardView.getContext())
          .load(slider.getUrl())
              .placeholder(mContext.getResources().getColor(R.color.image_card_place_holder))
              .into(((ViewHolder) viewHolder).mCardView.getMainImageView());

      mInfoField.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
    }

    ImageView imageView = ((ViewHolder) viewHolder).mCardView.getMainImageView();

    int color = slider.isSelected() ? mContext.getResources().getColor(R.color.transparent)
                  : mContext.getResources().getColor(R.color.hero_slider_overlay_color);
    float alpha = slider.isSelected() ? 1.0f : (color >> 24) / -255f;
    imageView.setAlpha(alpha);
    imageView.setColorFilter(color);
  }

  @Override
  public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    Log.d(TAG, "onUnbindViewHolder");
    ImageCardView cardView = (ImageCardView) viewHolder.view;
    // Remove references to images so that the garbage collector can free up memory.
    cardView.setBadgeImage(null);
    cardView.getMainImageView().setImageDrawable(null);
  }

  @Override
  public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    Log.d(TAG, "onViewAttachedToWindow");
  }

  public static class ViewHolder extends Presenter.ViewHolder {
    private Slider slider;
    private ImageCardView mCardView;

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    private int index;

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
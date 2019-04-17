package com.amazon.android.tv.tenfoot.ui.sliders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.utils.Helpers;
import com.bumptech.glide.Glide;

public class HeaderCardPresenter extends Presenter {
  private static final String TAG = "HeaderCardPresenter";

  private static Context mContext;
  private int mCardWidthDp;
  private int mCardHeightDp;
  private View mInfoField;
  private Drawable mDefaultCardImage;
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

  private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
    int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

    // Both background colors should be set because the view's
    // background is temporarily visible during animations.
    view.setBackgroundColor(color);
    view.findViewById(R.id.info_field).setBackgroundColor(color);
  }

  @Override
  public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
    Movie movie = (Movie) item;
    ((ViewHolder) viewHolder).setMovie(movie);
    int CARD_WIDTH_PX = 800;
    mCardWidthDp = Helpers.convertPixelToDp(mContext, CARD_WIDTH_PX);
    int CARD_HEIGHT_PX = 160;
    mCardHeightDp = Helpers.convertPixelToDp(mContext, CARD_HEIGHT_PX);

    Log.d(TAG, "onBindViewHolder");
    if (movie.getCardImageUrl() != null) {
      ((ViewHolder) viewHolder).mCardView.setMainImageDimensions(mCardWidthDp, mCardHeightDp);
      Glide.with(((ViewHolder) viewHolder).mCardView.getContext())
          .load(movie.getCardImageUrl())
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
    private Movie mMovie;
    private ImageCardView mCardView;

    public ViewHolder(View view) {
      super(view);
      mCardView = (ImageCardView) view;
    }

    public Movie getMovie() {
      return mMovie;
    }

    public void setMovie(Movie m) {
      mMovie = m;
    }

    public ImageCardView getCardView() {
      return mCardView;
    }

  }

}
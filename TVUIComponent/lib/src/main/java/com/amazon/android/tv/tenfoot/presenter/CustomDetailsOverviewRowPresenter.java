package com.amazon.android.tv.tenfoot.presenter;

import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import com.amazon.android.tv.tenfoot.R;

public class CustomDetailsOverviewRowPresenter extends FullWidthDetailsOverviewRowPresenter {
    public CustomDetailsOverviewRowPresenter(Presenter detailsPresenter) {
        super(detailsPresenter);
    }

    @Override
    protected void onLayoutLogo(ViewHolder viewHolder, int oldState, boolean logoChanged) {
        View v = viewHolder.getLogoViewHolder().view;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)
                v.getLayoutParams();
        switch (getAlignmentMode()) {
            case ALIGN_MODE_START:
            default:
                lp.setMarginStart(v.getResources().getDimensionPixelSize(
                        R.dimen.lb_details_v2_logo_margin_start));
                break;
            case ALIGN_MODE_MIDDLE:
                lp.setMarginStart(v.getResources().getDimensionPixelSize(R.dimen.lb_details_v2_left)
                        - lp.width);
                break;
        }

        switch (viewHolder.getState()) {
            case STATE_FULL:
            default:
//                lp.topMargin =
//                        v.getResources().getDimensionPixelSize(R.dimen.lb_details_v2_blank_height)
//                                - lp.height / 2;
                lp.topMargin = v.getResources().getDimensionPixelSize(R.dimen.lb_details_v2_blank_height);
                break;
            case STATE_HALF:
//                lp.topMargin = v.getResources().getDimensionPixelSize(
//                        R.dimen.lb_details_v2_blank_height) + v.getResources()
//                        .getDimensionPixelSize(R.dimen.lb_details_v2_actions_height) + v
//                        .getResources().getDimensionPixelSize(
//                                R.dimen.lb_details_v2_description_margin_top);
                lp.topMargin = v.getResources().getDimensionPixelSize(R.dimen.lb_details_v2_blank_height);
                break;
            case STATE_SMALL:
                lp.topMargin = 0;
                break;
        }
        v.setLayoutParams(lp);
    }
}

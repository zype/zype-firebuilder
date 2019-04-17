package com.amazon.android.tv.tenfoot.ui.sliders;

import android.os.Bundle;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public class HeaderFragment extends RowsFragment {


  private final int NUM_ROWS = 1;
  private final int NUM_COLS = 15;

  private ArrayObjectAdapter rowsAdapter;
  private HeaderCardPresenter cardPresenter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = super.onCreateView(inflater, container, savedInstanceState);
    return v;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    loadRows();
  }

  private void loadRows() {
    ListRowPresenter presenter = new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false);
    presenter.setShadowEnabled(false);
    presenter.setSelectEffectEnabled(false);
    rowsAdapter = new ArrayObjectAdapter(presenter);
    cardPresenter = new HeaderCardPresenter();

    List<Movie> list = MovieList.setupMovies();

    int i;
    for (i = 0; i < NUM_ROWS; i++) {
      if (i != 0) {
        Collections.shuffle(list);
      }
      ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
      for (int j = 0; j < NUM_COLS; j++) {
        listRowAdapter.add(list.get(j % 5));
      }

      rowsAdapter.add(new ListRow(listRowAdapter));
    }

    setAdapter(rowsAdapter);
  }


}
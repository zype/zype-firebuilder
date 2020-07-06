package com.amazon.android.tv.tenfoot.ui.menu;

import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.tv.tenfoot.presenter.MenuItemPresenter;

import java.util.List;

import static androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_MEDIUM;

/**
 * Zype, Evgeny Cherkasov
 */

public class TopMenuFragment extends RowsFragment {
    private static final String TAG = TopMenuFragment.class.getSimpleName();

    private ArrayObjectAdapter adapter;
    private int selectedMenuItemIndex;

    public interface ITopMenuFragmentListener {
        void onTopMenuItemSelected(Action item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                selectedMenuItemIndex = adapter.indexOf(row);
                Log.i(TAG, "onItemSelected(): item=" + item + ", row=" + row + ", index=" + selectedMenuItemIndex);
            }
        });

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            setupMenu();
            if (getView() != null) {
                getView().requestFocus();
            }
        }
    }

    private void setupMenu() {
        List<Action> settings = ContentBrowser.getInstance(getActivity()).getSettingsActions();
        if (settings == null || settings.isEmpty()) {
            Log.d(TAG, "No settings were found");
            return;
        }

        ListRowPresenter presenter = new ListRowPresenter(ZOOM_FACTOR_MEDIUM,false);
        presenter.setShadowEnabled(false);
        presenter.setSelectEffectEnabled(false);
        presenter.setRowHeight(120);
        adapter = new ArrayObjectAdapter(presenter);

        final TopMenuItemPresenter menuItemPresenter = new TopMenuItemPresenter();
        ArrayObjectAdapter menuItemAdapter = new ArrayObjectAdapter(menuItemPresenter);
        for (Action item : settings) {
            menuItemAdapter.add(item);
        }

        adapter.add(new ListRow(null, menuItemAdapter));
        setAdapter(adapter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Action) {
                Action menuAction = (Action) item;
                Log.d(TAG, "Menu item " + menuAction.getAction() + " was clicked");
                ((ITopMenuFragmentListener) getActivity()).onTopMenuItemSelected(menuAction);
            }
        }
    }

    public int getSelectedMenuItemIndex() {
        return selectedMenuItemIndex;
    }
}

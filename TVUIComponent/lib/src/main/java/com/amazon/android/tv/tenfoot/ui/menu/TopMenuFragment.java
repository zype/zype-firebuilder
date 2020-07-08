package com.amazon.android.tv.tenfoot.ui.menu;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.presenter.MenuItemPresenter;

import java.util.List;

import static androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_MEDIUM;

/**
 * Zype, Evgeny Cherkasov
 */

public class TopMenuFragment extends Fragment {
    private static final String TAG = TopMenuFragment.class.getSimpleName();

    private Action selectedMenuItem;

    private TopMenuAdapter adapter;
    private ITopMenuListener listener;

    private HorizontalGridView menuContainer;

    public interface ITopMenuListener {
        void onTopMenuItemSelected(Action item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_menu, container);
        menuContainer = rootView.findViewById(R.id.gridTopMenu);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (listener == null) {
            if (getActivity() instanceof ITopMenuListener) {
                listener = (ITopMenuListener) getActivity();
            }
            else {
                throw new ClassCastException("Activity must implement 'ITopMenuListener'");
            }
        }
        adapter = new TopMenuAdapter(getTopMenuActions(), listener);
        menuContainer.setAdapter(adapter);
    }

    private List<Action> getTopMenuActions() {
        List<Action> actions = ContentBrowser.getInstance(getActivity()).getSettingsActions();
        if (actions == null || actions.isEmpty()) {
            Log.d(TAG, "No settings were found");
        }
        return actions;
    }
}

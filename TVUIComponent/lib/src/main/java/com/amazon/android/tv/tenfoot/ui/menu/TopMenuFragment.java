package com.amazon.android.tv.tenfoot.ui.menu;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.leanback.widget.HorizontalGridView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.tv.tenfoot.R;

import java.util.List;

/**
 * Zype, Evgeny Cherkasov
 */

public class TopMenuFragment extends Fragment {
    private static final String TAG = TopMenuFragment.class.getSimpleName();

    private Action selectedMenuItem;

    private TopMenuAdapter adapter;
    private ITopMenuListener listener;

    private HorizontalGridView menuContainer;
    private int itemWidthTotal = 0;
    private boolean menuWidthEvaluated = false;

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
        menuContainer.setItemSpacing(getResources().getInteger(R.integer.top_navigation_item_spacing));
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
        itemWidthTotal = 0;
        int topMenuMaxWidth = getResources().getDimensionPixelSize(R.dimen.top_navigation_max_width);
        menuContainer.setOnChildLaidOutListener((parent, view, position, id) -> {
            Log.d(TAG, "ChildLaidOutListener(): id=" + id + ", width=" + view.getWidth() + ", position=" + position);
            if (!menuWidthEvaluated) {
                itemWidthTotal += view.getWidth();
                if (position + 1 != adapter.getItemCount()) {
                    itemWidthTotal += getResources().getInteger(R.integer.top_navigation_item_spacing);
                }
                if (position + 1 == adapter.getItemCount() || itemWidthTotal > topMenuMaxWidth) {
                    menuWidthEvaluated = true;
                    ViewGroup.LayoutParams layoutParams = menuContainer.getLayoutParams();
                    Log.d(TAG, "ChildLaidOutListener(): current menu width: " + layoutParams.width);
                    layoutParams.width = Math.min(itemWidthTotal, topMenuMaxWidth);
                    Handler handler = new Handler();
                    handler.post(() -> {
                        menuContainer.setLayoutParams(layoutParams);
                        menuContainer.invalidate();
                        Log.d(TAG, "ChildLaidOutListener(): menu width updated: " + layoutParams.width);
                    });
                }
            }
        });
    }

    private List<Action> getTopMenuActions() {
        List<Action> actions = ContentBrowser.getInstance(getActivity()).getSettingsActions();
        if (actions == null || actions.isEmpty()) {
            Log.d(TAG, "No settings were found");
        }
        return actions;
    }
}

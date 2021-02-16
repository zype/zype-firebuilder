package com.amazon.android.tv.tenfoot.ui.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amazon.android.model.Action;
import com.amazon.android.tv.tenfoot.R;

import java.util.ArrayList;
import java.util.List;

public class TopMenuAdapter extends RecyclerView.Adapter {

    private List<Action> items;

    private TopMenuFragment.ITopMenuListener listener;

    public TopMenuAdapter(List<Action> items, TopMenuFragment.ITopMenuListener listener) {
        this.items = new ArrayList<>();
        setItems(items);
        this.listener = listener;
    }

    @MainThread
    public void setItems(List<Action> items) {
        this.items.clear();
        this.items.addAll(items);
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.top_menu_item, parent, false);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        return new TopMenuAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        TopMenuAdapter.ViewHolder holder = (TopMenuAdapter.ViewHolder) viewHolder;
        holder.item = items.get(position);
        holder.textTitle.setText(holder.item.getLabel1());
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onTopMenuItemSelected(holder.item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public Action item;
        public TextView textTitle;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            textTitle = view.findViewById(R.id.textTitle);
        }
    }

}

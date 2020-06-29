package com.amazon.android.tv.tenfoot.ui.sliders;

import android.os.Handler;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HeroCardAdapter extends ObjectAdapter {

  private ArrayList<Slider> mItems = new ArrayList<>();
  private final Handler handler = new Handler();

  /**
   * Constructs an adapter with the given {@link PresenterSelector}.
   */
  public HeroCardAdapter(PresenterSelector presenterSelector) {
    super(presenterSelector);
  }

  /**
   * Constructs an adapter that uses the given {@link Presenter} for all items.
   */
  public HeroCardAdapter(Presenter presenter) {
    super(presenter);
  }

  /**
   * Constructs an adapter.
   */
  public HeroCardAdapter() {
    super();
  }

  @Override
  public int size() {
    return mItems.size() > 1 ?  Integer.MAX_VALUE : 1;
  }

  public int realSize() {
    return mItems.size();
  }

  @Override
  public Object get(int index) {
    if(mItems.size() == 1) {
      return mItems.get(index);
    }

    Slider slider = mItems.get(index % mItems.size());
    slider.setPosition(index);
    return slider;
  }

  /**
   * Returns the index for the first occurrence of item in the adapter, or -1 if
   * not found.
   *
   * @param item  The item to find in the list.
   * @return Index of the first occurrence of the item in the adapter, or -1
   *         if not found.
   */
  public int indexOf(Object item) {
    return mItems.indexOf(item);
  }

  /**
   * Notify that the content of a range of items changed. Note that this is
   * not same as items being added or removed.
   *
   * @param positionStart The position of first item that has changed.
   * @param itemCount The count of how many items have changed.
   */
  public void notifyArrayItemRangeChanged(int positionStart, int itemCount) {
    notifyItemRangeChanged(positionStart, itemCount);
  }

  /**
   * Adds an item to the end of the adapter.
   *
   * @param item The item to add to the end of the adapter.
   */
  public void add(Slider item) {
    add(mItems.size(), item);
  }

  /**
   * Inserts an item into this adapter at the specified index.
   * If the index is >= {@link #size} an exception will be thrown.
   *
   * @param index The index at which the item should be inserted.
   * @param item The item to insert into the adapter.
   */
  public void add(int index, Slider item) {
    mItems.add(index, item);
    notifyItemRangeInserted(index, 1);
  }

  /**
   * Adds the objects in the given collection to the adapter, starting at the
   * given index.  If the index is >= {@link #size} an exception will be thrown.
   *
   * @param index The index at which the items should be inserted.
   * @param items A {@link Collection} of items to insert.
   */
  public void addAll(int index, Collection<Slider> items) {
    int itemsCount = items.size();
    if (itemsCount == 0) {
      return;
    }
    mItems.addAll(index, items);
    notifyItemRangeInserted(index, itemsCount);
  }

  /**
   * Removes the first occurrence of the given item from the adapter.
   *
   * @param item The item to remove from the adapter.
   * @return True if the item was found and thus removed from the adapter.
   */
  public boolean remove(Object item) {
    int index = mItems.indexOf(item);
    if (index >= 0) {
      mItems.remove(index);
      notifyItemRangeRemoved(index, 1);
    }
    return index >= 0;
  }

  /**
   * Replaces item at position with a new item and calls notifyItemRangeChanged()
   * at the given position.  Note that this method does not compare new item to
   * existing item.
   * @param position  The index of item to replace.
   * @param item      The new item to be placed at given position.
   */
  public void replace(int position, Slider item) {
    mItems.set(position, item);
    notifyItemRangeChanged(position, 1);
  }

  /**
   * Removes a range of items from the adapter. The range is specified by giving
   * the starting position and the number of elements to remove.
   *
   * @param position The index of the first item to remove.
   * @param count The number of items to remove.
   * @return The number of items removed.
   */
  public int removeItems(int position, int count) {
    int itemsToRemove = Math.min(count, mItems.size() - position);
    if (itemsToRemove <= 0) {
      return 0;
    }

    for (int i = 0; i < itemsToRemove; i++) {
      mItems.remove(position);
    }
    notifyItemRangeRemoved(position, itemsToRemove);
    return itemsToRemove;
  }

  /**
   * Removes all items from this adapter, leaving it empty.
   */
  public void clear() {
    int itemCount = mItems.size();
    if (itemCount == 0) {
      return;
    }
    mItems.clear();
    notifyItemRangeRemoved(0, itemCount);
  }

  /**
   * Gets a read-only view of the list of object of this ArrayObjectAdapter.
   */
  public <E> List<E> unmodifiableList() {
    return Collections.unmodifiableList((List<E>) mItems);
  }

  public void notifyChanges(int selectedIndex) {

      int startIndex = selectedIndex - 2;

      if(startIndex < 0) {
          startIndex = 0;
      }

      int endIndex = selectedIndex + 2;

      if(endIndex >= size()) {
          endIndex = size();
      }


      final int start = startIndex;
      final int end = endIndex;

      if (mItems.size() > 0){
          handler.post(() -> {
              notifyItemRangeChanged(start, end - start);
          });
      }
  }

  public void reset() {
    for(Slider slider : mItems) {
      slider.setSelected(false);
    }
  }
}

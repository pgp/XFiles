package it.pgp.xfiles.dragdroplist;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Web source:
 * https://medium.com/@gopalawasthi383/android-recyclerview-drag-and-drop-a3f227cdb641
 */

public class DragDropItemTouchHelperCallback extends ItemTouchHelper.Callback {
    int dragFrom = -1;
    int dragTo = -1;

    final DragNDropAdapter adapter;
    final ArrayList arrayList;

    public DragDropItemTouchHelperCallback(DragNDropAdapter adapter, ArrayList arrayList) {
        this.adapter = adapter;
        this.arrayList = arrayList;
    }

    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        // get the viewHolder's and target's positions in your adapter data, swap them
        if(viewHolder.getItemViewType() != target.getItemViewType()){
            return false;
        }
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();


        if(dragFrom == -1) {
            dragFrom =  fromPosition;
        }
        dragTo = toPosition;

        if(dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
            reallyMoved(dragFrom, dragTo);
            dragFrom = dragTo = -1;
        }

        // and notify the adapter that its dataset has changed
        adapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        //nestedScrollView.requestDisallowInterceptTouchEvent(false);
        //recyclerView.setNestedScrollingEnabled(false);

        return true;
    }

    private void reallyMoved(int dragFrom, int dragTo) {
        if(dragFrom == 0 || dragTo == arrayList.size()+1) return;
        Collections.swap(arrayList, dragFrom-1, dragTo-1);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}

    //defines the enabled move directions in each state (idle, swiping, dragging).
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if(viewHolder.getItemViewType() == DragNDropAdapter.HEADERVIEW)
            return makeMovementFlags(0,0);
        int dragflags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragflags,0);
    }
}

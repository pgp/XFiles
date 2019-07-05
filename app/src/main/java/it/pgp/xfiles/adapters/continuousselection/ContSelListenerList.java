package it.pgp.xfiles.adapters.continuousselection;

import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;

import java.util.List;

public class ContSelListenerList extends ContSelListener {

    // for continuous selection mode
    private int idx_2 = -1; // before last selected index
    private int idx_1 = -1; // last selected index

    private boolean initialDestCheckStatus;

    private int startPos;


    public ContSelListenerList(AbsListView lv, ArrayAdapter adapter, List<? extends Checkable> objects, CSCheckboxes csCheckboxes) {
        super(lv, adapter, objects, csCheckboxes);
    }

    @Override
    public void startSelectMode(int startPos) {
        super.startSelectMode(startPos);
        this.startPos = startPos;
        destCheckStatus = initialDestCheckStatus = !getInvSel();
        fillSelectionBeforeStart();

        objects.get(startPos).setChecked(destCheckStatus);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void ongoingSelectMode(int position) {
        Log.d("POSITION", "Current: "+position+" Previous: "+idx_1+" Old: "+idx_2);
        if (position < 0 || position >= objects.size() || position == idx_1) return;
        Log.d("ContSelMode", "ContSelMode");

        atLeastOneMoveAfterDown = true;

        if (position == startPos && idx_1 >= 0) { // returned to startPos after having left it
            destCheckStatus = initialDestCheckStatus;
            if (!selectionBeforeStart.contains(idx_1)) {
                objects.get(idx_1).setChecked(!destCheckStatus);
            }
            if (idx_2 >= 0 && !selectionBeforeStart.contains(idx_2)) {
                objects.get(idx_2).setChecked(!destCheckStatus);
            }
            idx_1 = -1;
            idx_2 = -1;
            return;
        }

        if (position != idx_1) {
            if (position == idx_2) { // direction inverted, deselect previous
                destCheckStatus = !destCheckStatus;
                if (!selectionBeforeStart.contains(idx_1)) {
                    objects.get(idx_1).setChecked(destCheckStatus);
                }
                if (!selectionBeforeStart.contains(position)) {
                    objects.get(position).setChecked(destCheckStatus);
                }
            }
            else {
                if (!selectionBeforeStart.contains(position)) {
                    objects.get(position).setChecked(destCheckStatus);
                }
            }
            adapter.notifyDataSetChanged();

            if (idx_1 < 0) {
                idx_1 = idx_2 = position;
            }
            else {
                idx_2 = idx_1;
                idx_1 = position;
            }
        }

        if (position >= lv.getLastVisiblePosition()) {
            lv.smoothScrollToPosition(position+1);
        }
        else if (position <= lv.getFirstVisiblePosition()) {
            lv.smoothScrollToPosition(position-1);
        }
    }

    @Override
    public void endSelectMode(int endPos) {
        // Log.d("SelectModeEnd", "SelectModeEnd");
        if (!active) {
            // Log.d("MOTION", "Repeated ACTION_UP events (multitouch?), ignoring...");
            return;
        }
        if (startPos == endPos && !atLeastOneMoveAfterDown) {
            if (!selectionBeforeStart.contains(startPos)) {
                objects.get(startPos).setChecked(!initialDestCheckStatus);
            }
        }

        selectionBeforeStart.clear();
        idx_1 = -1;
        idx_2 = -1;
        active = false;
        destCheckStatus = initialDestCheckStatus;
        atLeastOneMoveAfterDown = false;
        adapter.notifyDataSetChanged();
    }
}

package it.pgp.xfiles.adapters.continuousselection;

import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.GridView;

import java.util.List;

import it.pgp.xfiles.utils.Pair;

public class ContSelListenerGrid extends ContSelListener {

    private static Pair<Integer,Integer> intToPoint(int h, int colsInRow) {
        if (h < 0) return new Pair<>(-1,-1); // convention for error-code return style
        return new Pair<>(h / colsInRow, h % colsInRow);
    }

    private static int pointToInt(Pair<Integer, Integer> p, int colsInRow) {
        if (p.i < 0 || p.j < 0) return -1; // convention for error-code return style
        return p.i*colsInRow + p.j;
    }

    private int colsInRow;

    // for select-on-move mode
    private final Pair<Integer,Integer> p_1 = new Pair<>(-1,-1); // last selected index

    private final Pair<Integer,Integer> startPos = new Pair<>(-1,-1);

    public ContSelListenerGrid(AbsListView lv, ArrayAdapter adapter, List<? extends Checkable> objects, CSCheckboxes csCheckboxes) {
        super(lv, adapter, objects, csCheckboxes);
    }

    @Override
    public void startSelectMode(int startPos) {
        super.startSelectMode(startPos);
        colsInRow = ((GridView)getLv()).getNumColumns();
        Log.e("AAAAAAAAAAAAAAA", "colNum: "+colsInRow);
        this.startPos.set(intToPoint(startPos,colsInRow));
        destCheckStatus = !getInvSel();
        fillSelectionBeforeStart();

        objects.get(startPos).setChecked(destCheckStatus);
        adapter.notifyDataSetChanged();
    }

    // restart from initially selected items on each action move
    private void startFromInitialSelection() {
        for (int i=0;i<objects.size();i++)
            if (!selectionBeforeStart.contains(i))
                objects.get(i).setChecked(getInvSel());
    }

    @Override
    public void ongoingSelectMode(int position_) {
        if (position_ < 0 || position_ >= objects.size() || position_ == pointToInt(p_1,colsInRow)) return;

        Log.e("EEE", "Abs: "+position_+" p_1: "+p_1+"p_1_flat: "+pointToInt(p_1,colsInRow));

        atLeastOneMoveAfterDown = true;
        startFromInitialSelection();
        final Pair<Integer,Integer> position = intToPoint(position_,colsInRow);
        p_1.set(position);

        int minx,miny,maxx,maxy;
        minx = startPos.i<position.i?startPos.i:position.i;
        miny = startPos.j<position.j?startPos.j:position.j;
        maxx = startPos.i>position.i?startPos.i:position.i;
        maxy = startPos.j>position.j?startPos.j:position.j;

        boolean checked = !getInvSel();
        for (int i=minx;i<=maxx;i++)
            for (int j=miny;j<=maxy;j++)
                objects.get(i*colsInRow+j).setChecked(checked);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void endSelectMode(int endPos) {
        // Log.e("SelectModeEnd", "SelectModeEnd");
        if (!active) {
            // Log.e("MOTION", "Repeated ACTION_UP events (multitouch?), ignoring...");
            return;
        }
        if (startPos.equals(intToPoint(endPos,colsInRow)) && !atLeastOneMoveAfterDown) {
            if (!selectionBeforeStart.contains(endPos)) {
                objects.get(endPos).setChecked(!destCheckStatus);
            }
        }

        selectionBeforeStart.clear();
        p_1.set(-1,-1);
        active = false;
        atLeastOneMoveAfterDown = false;
        adapter.notifyDataSetChanged();
    }
}

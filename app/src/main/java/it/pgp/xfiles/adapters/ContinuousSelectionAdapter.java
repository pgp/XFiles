package it.pgp.xfiles.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import java.util.List;

public abstract class ContinuousSelectionAdapter<T> extends ArrayAdapter<T> {

    protected final AbsListView lv; // indicate if select mode is active and needed to auto scroll, to be used with selection mode toggle performed separately (like in XFiles)
    protected final List<T> objects;
    protected final LayoutInflater inflater;

    public boolean active = false;

    public ContinuousSelectionAdapter(@NonNull Context context, int resource, AbsListView lv, List<T> objects) {
        super(context, resource, objects);
        this.objects = objects;
        this.lv = lv;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public AbsListView getLv() {
        return lv;
    }

    // this is used by the listener
    public void startSelectMode(int startPos) {
        if (active) {
            Log.d("MOTION", "Repeated ACTION_DOWN events (multitouch?), ignoring...");
            return;
        }
        if (startPos < 0 || startPos >= objects.size()) return;
        active = true;
    }

    abstract void ongoingSelectMode(int position);
    abstract void endSelectMode(int endPos);

    public abstract boolean getInvSel();

    public final View.OnTouchListener listener = (v, ev) -> {
        Log.d("Motion", "Default: " + ev.getActionMasked() + " " + ev.getX() + " " + ev.getY());
        try {
            int ptoPos = getLv().pointToPosition((int) ev.getX(), (int) ev.getY());
            switch(ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d("MotionSTART", "Start select mode");
                    startSelectMode(ptoPos);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d("MotionEND", "End select mode");
                    endSelectMode(ptoPos);
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("MotionCONTINUE", "Ongoing select mode");
                    ongoingSelectMode(ptoPos);
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    };

}

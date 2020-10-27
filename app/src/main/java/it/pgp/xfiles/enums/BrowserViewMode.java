package it.pgp.xfiles.enums;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.adapters.BrowserGridAdapter;
import it.pgp.xfiles.adapters.BrowserListAdapter;
import it.pgp.xfiles.adapters.continuousselection.CSCheckboxes;
import it.pgp.xfiles.adapters.continuousselection.ContSelListener;
import it.pgp.xfiles.adapters.continuousselection.ContSelListenerGrid;
import it.pgp.xfiles.adapters.continuousselection.ContSelListenerList;

/**
 * Created by pgp on 31/10/16
 */

public enum BrowserViewMode {
    LIST(R.layout.listview_layout,R.id.mainBrowserListView),
    GRID(R.layout.gridview_layout,R.id.mainBrowserGridView);

    int layout;
    int id;

    BrowserViewMode(int layout, int id) {
        this.layout = layout;
        this.id = id;
    }

    static final RuntimeException invalidMode = new RuntimeException("Invalid browser view mode");

    public BrowserAdapter newAdapter(MainActivity mainActivity, List<BrowserItem> objects) {
        return (this==LIST)?new BrowserListAdapter(mainActivity,objects):new BrowserGridAdapter(mainActivity,objects);
    }

    public BrowserViewMode next() {
        switch (this) {
            case LIST: return GRID;
            case GRID: return LIST;
            default: throw invalidMode;
        }
    }

    public int getLayout() {
        return layout;
    }

    public ContSelListener buildCSListener(AbsListView lv, ArrayAdapter adapter, List<? extends Checkable> objects, CSCheckboxes csCheckboxes) {
        switch (this) {
            case LIST: return new ContSelListenerList(lv,adapter,objects,csCheckboxes);
            case GRID: return new ContSelListenerGrid(lv,adapter,objects,csCheckboxes);
            default: throw invalidMode;
        }
    }

    public boolean isFullPadLayout() {
        return this==GRID;
    }

    public int getId() {
        return id;
    }
}

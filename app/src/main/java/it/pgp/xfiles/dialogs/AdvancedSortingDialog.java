package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.widget.Button;

import java.util.ArrayList;

import it.pgp.xfiles.R;
import it.pgp.xfiles.SortingItem;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.comparators.AdvancedComparator;
import it.pgp.xfiles.dragdroplist.DragDropItemTouchHelperCallback;
import it.pgp.xfiles.dragdroplist.DragNDropAdapter;
import it.pgp.xfiles.enums.ComparatorField;

/**
 * Created by pgp on 28/10/16
 * Last modified on 23/11/2016
 */

public class AdvancedSortingDialog extends BaseDialog {
    private DragNDropAdapter dragNDropAdapter;
    private RecyclerView listView;

    private AdvancedComparator advancedComparator;

    public AdvancedSortingDialog(final Activity activity, final BrowserAdapter browserAdapter) {
        super(activity);
        setTitle("Advanced sort");
        setContentView(R.layout.advanced_sorting_dialog);
        setDialogIcon(R.drawable.xfiles_sort_special);

        final ArrayList<SortingItem> content = new ArrayList<>();
        for (ComparatorField c : ComparatorField.values()) {
            content.add(new SortingItem(c,true,false)); // default: all attributes selected, no one reversed
        }

        listView = findViewById(R.id.sortingAttributesDragNDropListView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        dragNDropAdapter = new DragNDropAdapter(activity,content);
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(dragNDropAdapter);
        listView.setHasFixedSize(true);

        new ItemTouchHelper(new DragDropItemTouchHelperCallback(dragNDropAdapter,content)).attachToRecyclerView(listView);

        Button okButton = findViewById(R.id.advancedSortOKButton);
        okButton.setOnClickListener(v -> {
            advancedComparator = new AdvancedComparator(dragNDropAdapter.getSelectedItems());
            browserAdapter.sort(advancedComparator);
            dismiss();
        });

        Button cancelButton = findViewById(R.id.advancedSortCancelButton);
        cancelButton.setOnClickListener(v -> cancel());
    }
}


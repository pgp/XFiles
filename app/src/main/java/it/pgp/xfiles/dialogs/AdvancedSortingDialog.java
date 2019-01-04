package it.pgp.xfiles.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.R;
import it.pgp.xfiles.comparators.AdvancedComparator;
import it.pgp.xfiles.dragdroplist.DragNDropAdapter;
import it.pgp.xfiles.dragdroplist.DynamicListView;
import it.pgp.xfiles.enums.ComparatorField;
import it.pgp.xfiles.SortingItem;

/**
 * Created by pgp on 28/10/16
 * Last modified on 23/11/2016
 */

public class AdvancedSortingDialog extends BaseDialog {
    private DragNDropAdapter dragNDropAdapter;
    private DynamicListView listView;

    private AdvancedComparator advancedComparator;

    public AdvancedSortingDialog(final Context context, final BrowserAdapter browserAdapter) {
        super(context);
        setTitle("Advanced sort");
        setContentView(R.layout.advanced_sorting_dialog);
        setDialogIcon(R.drawable.xfiles_sort_special);

        ArrayList<SortingItem> content = new ArrayList<>();
        for (ComparatorField c : ComparatorField.values()) {
            content.add(new SortingItem(c,true,false)); // default: all attributes selected, no one reversed
        }

        View.OnClickListener selListener = v -> {
            View view = (View) v.getParent();
            int index = listView.indexOfChild(view);
            CheckBox checkBox = view.findViewById(R.id.sortingAttributeSelectedCheckbox);
            dragNDropAdapter.updateSelItem(checkBox.isChecked(), index);
        };

        View.OnClickListener revListener = v -> {
            View view = (View) v.getParent();
            int index = listView.indexOfChild(view);
            CheckBox checkBox = view.findViewById(R.id.sortingAttributeReversedCheckbox);
            dragNDropAdapter.updateRevItem(checkBox.isChecked(), index);
        };

        dragNDropAdapter = new DragNDropAdapter(context,R.layout.dragitem,content,selListener,revListener);
        listView = findViewById(R.id.sortingAttributesDragNDropListView);
        listView.setItemList(content);
        listView.setAdapter(dragNDropAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

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


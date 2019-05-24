package it.pgp.xfiles.dragdroplist;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.SortingItem;

public final class DragNDropAdapter extends ArrayAdapter<SortingItem> {

    private static class ViewHolder {
        private TextView textView;
        private CheckBox selected;
        private CheckBox reversed;
    }

    final int INVALID_ID = -1;
    private final View.OnClickListener onClickListenerSel, onClickListenerRev;

    Map<SortingItem, Integer> mIdMap = new HashMap<>();
    private ViewHolder viewHolder;

    private List<SortingItem> objects;
    private Locale currentLocale;


    public DragNDropAdapter(Context context, int textViewResourceId, List<SortingItem> objects, View.OnClickListener onClickListenerSel, View.OnClickListener onClickListenerRev) {
        super(context, textViewResourceId, objects);
        this.onClickListenerSel = onClickListenerSel;
        this.onClickListenerRev = onClickListenerRev;
        this.objects = objects;
        for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i), i);
        }
        currentLocale = context.getResources().getConfiguration().locale;
    }

    public Collection<SortingItem> getCurrentOrderingOfItems() {
        return objects;
    }

    public int getCount() {
        return objects.size();
    }

    public SortingItem getItem(int position) {
        return objects.get(position);
    }

    public List<SortingItem> getItems() {
        return objects;
    }

    public SortingItem[] getSelectedItems() {
        ArrayList<SortingItem> a = new ArrayList<>();
        for (SortingItem si : objects)
            if (si.isSelected()) a.add(si);
        SortingItem[] aa = new SortingItem[a.size()];
        a.toArray(aa);
        return aa;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        SortingItem item = getItem(position);
        return mIdMap.get(item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.dragitem, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = convertView.findViewById(R.id.sortingAttributeLabel);
            viewHolder.selected = convertView.findViewById(R.id.sortingAttributeSelectedCheckbox);
            viewHolder.reversed = convertView.findViewById(R.id.sortingAttributeReversedCheckbox);

            viewHolder.selected.setOnClickListener(onClickListenerSel);
            viewHolder.reversed.setOnClickListener(onClickListenerRev);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final SortingItem item = getItem(position);

        if(item!=null) {
            viewHolder.textView.setText(item.comparatorField.getLocalized(currentLocale));
            viewHolder.selected.setChecked(item.isSelected());
            viewHolder.reversed.setChecked(item.isReversed());
        }

        return convertView;
    }

    public void updateSelItem(boolean isChecked, int index) {
        getItem(index).setSelected(isChecked);
    }

    public void updateRevItem(boolean isChecked, int index) {
        getItem(index).setReversed(isChecked);
    }

    @Override
    public boolean hasStableIds()
    {
        return android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

}
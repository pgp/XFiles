package it.pgp.xfiles.dragdroplist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.pgp.xfiles.R;
import it.pgp.xfiles.SortingItem;

public final class DragNDropAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static class MyHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final CheckBox selected;
        private final CheckBox reversed;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.sortingAttributeLabel);
            selected = itemView.findViewById(R.id.sortingAttributeSelectedCheckbox);
            reversed = itemView.findViewById(R.id.sortingAttributeReversedCheckbox);
        }
    }

    final Context context;
    private final ArrayList<SortingItem> objects;
    private final Locale currentLocale;

    public DragNDropAdapter(Context context, ArrayList<SortingItem> objects) {
        this.context = context;
        this.objects = objects;
        currentLocale = context.getResources().getConfiguration().locale;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(context).inflate(R.layout.dragitem,viewGroup,false);
        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        MyHolder h = (MyHolder) viewHolder;
        SortingItem item = objects.get(i);
        h.textView.setText(item.comparatorField.getLocalized(currentLocale));
        h.selected.setChecked(item.isSelected());
        h.selected.setOnCheckedChangeListener((b,c) -> item.setSelected(c));
        h.reversed.setChecked(item.isReversed());
        h.reversed.setOnCheckedChangeListener((b,c) -> item.setReversed(c));
    }

    public SortingItem[] getSelectedItems() {
        ArrayList<SortingItem> a = new ArrayList<>();
        for (SortingItem si : objects)
            if (si.isSelected()) a.add(si);
        return a.toArray(new SortingItem[0]);
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }
}
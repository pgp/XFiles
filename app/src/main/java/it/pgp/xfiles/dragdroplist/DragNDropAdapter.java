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
import java.util.Locale;

import it.pgp.xfiles.R;
import it.pgp.xfiles.SortingItem;

public class DragNDropAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<SortingItem> arrayList;
    private final Locale currentLocale;
    public static final int HEADERVIEW =0;
    public static final int LISTVIEW =1;

    public DragNDropAdapter(Context context, ArrayList<SortingItem> arrayList) {
        this.arrayList =arrayList;
        this.context = context;
        currentLocale = context.getResources().getConfiguration().locale;
    }

    // DEBUG
//    public void printContent() {
//        for(SortingItem i : arrayList)
//            Log.d("@@@@@", ""+i);
//        Log.d("@@@@@", "-----");
//    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v;
        if(getItemViewType(i) == HEADERVIEW) {
            v = LayoutInflater.from(context).inflate(R.layout.sorting_dialog_header_view,viewGroup,false);
            return new HeaderViewHoler(v);
        }
        v = LayoutInflater.from(context).inflate(R.layout.dragitem,viewGroup,false);
        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if(viewHolder.getItemViewType() == HEADERVIEW ){
            HeaderViewHoler headerViewHoler = (HeaderViewHoler)viewHolder;
        }
        else {
            MyHolder myHolder = (MyHolder) viewHolder;
            SortingItem item = arrayList.get(i - 1);
            myHolder.name.setText(item.comparatorField.getLocalized(currentLocale));
            myHolder.sel.setChecked(item.isSelected());
            myHolder.sel.setOnCheckedChangeListener((b,c) -> item.setSelected(c));
            myHolder.rev.setChecked(item.isReversed());
            myHolder.rev.setOnCheckedChangeListener((b,c) -> item.setReversed(c));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return HEADERVIEW;
        }
        return LISTVIEW;
    }

    @Override
    public int getItemCount() {
        return arrayList.size()+1;
    }

    public SortingItem[] getSelectedItems() {
        ArrayList<SortingItem> a = new ArrayList<>();
        for (int i=0; i<arrayList.size(); i++)
            if (arrayList.get(i).isSelected()) a.add(arrayList.get(i));
        return a.toArray(new SortingItem[0]);
    }

    static class MyHolder extends RecyclerView.ViewHolder{
        TextView name;
        CheckBox sel, rev;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.sortingAttributeLabel);
            sel = itemView.findViewById(R.id.sortingAttributeSelectedCheckbox);
            rev = itemView.findViewById(R.id.sortingAttributeReversedCheckbox);
        }
    }

    static class HeaderViewHoler extends RecyclerView.ViewHolder{
        TextView headertextview;
        public HeaderViewHoler(@NonNull View itemView) {
            super(itemView);
//            headertextview = itemView.findViewById(R.id.header);
        }
    }
}

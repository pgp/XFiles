package it.pgp.xfiles.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import it.pgp.xfiles.R;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;

public class HashAlgorithmsAdapter extends ArrayAdapter<HashRequestCodes> {

    public static class ViewHolder {
        public CheckBox cb;
        public TextView tv;

        ViewHolder(CheckBox cb, TextView tv) {
            this.cb = cb;
            this.tv = tv;
        }
    }

    private final LayoutInflater inflater;

    public HashAlgorithmsAdapter(Context context) {
        super(context,
                R.layout.checksum_label_item,
                R.id.checksum_textview,
                Arrays.asList(HashRequestCodes.values()));
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HashRequestCodes item = getItem(position);

        TextView tv;
        CheckBox cb;

        if(convertView == null){
            convertView = inflater.inflate(R.layout.checksum_label_item, null);

            tv = convertView.findViewById(R.id.checksum_textview);
            cb = convertView.findViewById(R.id.checksum_checkbox);

            cb.setOnClickListener(v->{
                CheckBox checkBox = (CheckBox) v;
                HashRequestCodes h = (HashRequestCodes) checkBox.getTag();
                h.setChecked(checkBox.isChecked());
            });

            convertView.setTag(new ViewHolder(cb,tv));
        }
        else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            tv = viewHolder.tv;
            cb = viewHolder.cb;
        }

        cb.setTag(item);

        tv.setText(item.getLabel());
        cb.setChecked(item.isChecked());

        return convertView;
    }

    @Override
    public int getCount() {
        return HashRequestCodes.values().length;
    }

    public Set<HashRequestCodes> getSelectedItems() {
        return new LinkedHashSet<HashRequestCodes>(){{
            for (HashRequestCodes h: HashRequestCodes.values())
                if (h.isChecked()) add(h);
        }};
    }
}
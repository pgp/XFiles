package it.pgp.xfiles.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import it.pgp.xfiles.R;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;

public class HashAlgorithmsAdapter extends ArrayAdapter<HashRequestCodes> {

    public static class ViewHolder {
        public CheckedTextView ctv;

        ViewHolder(CheckedTextView ctv) {
            this.ctv = ctv;
        }
    }

    private final LayoutInflater inflater;
    private final Context context;

    public HashAlgorithmsAdapter(Context context) {
        super(context,
                R.layout.checksum_label_item,
                R.id.checksum_ctv,
                Arrays.asList(HashRequestCodes.values()));
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HashRequestCodes item = getItem(position);
        CheckedTextView ctv;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.checksum_label_item, null);
            ctv = convertView.findViewById(R.id.checksum_ctv);
            convertView.setTag(new ViewHolder(ctv));
        }
        else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            ctv = viewHolder.ctv;
        }

        ctv.setTag(item);

        ctv.setText(item.getLabel());
        ctv.setChecked(item.isChecked());
        ctv.setTextColor(context.getResources().getColor(item.getLabelColor()));

        return convertView;
    }

    @Override
    public int getCount() {
        return HashRequestCodes.values().length;
    }

    public Set<HashRequestCodes> getSelectedItems() {
        Set<HashRequestCodes> items = new LinkedHashSet<>();
        for(HashRequestCodes h: HashRequestCodes.values())
            if(h.isChecked()) items.add(h);
        return items;
    }
}
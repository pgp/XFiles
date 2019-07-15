package it.pgp.xfiles.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XreAnnouncesAdapter extends ArrayAdapter<Map.Entry<String,String>> {

    public final Set<Map.Entry<String,String>> support = new HashSet<>();
    public final List<Map.Entry<String,String>> items;

    public XreAnnouncesAdapter(Context context, List<Map.Entry<String,String>> items) {
        super(context, android.R.layout.simple_spinner_dropdown_item, items);
        this.items = items; // ugly workaround, cannot assign local variable before super() call
        this.support.addAll(items);
    }

    // accumulates indefinitely till dialog dismiss, unrespective of the fact that a XRE server may have stopped announcing itself
    @Override
    public void add(Map.Entry<String, String> object) {
        if(support.contains(object)) return;
        support.add(object);
        items.clear();
        items.addAll(support);
        notifyDataSetChanged();
    }
}

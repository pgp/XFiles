package it.pgp.xfiles.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import it.pgp.xfiles.utils.Pair;

public class XreAnnouncesAdapter extends ArrayAdapter<Pair<String,String>> {

    public final Set<Pair<String,String>> support = new TreeSet<>();
    public final List<Pair<String,String>> items;

    public XreAnnouncesAdapter(Context context, List<Pair<String,String>> items) {
        super(context, android.R.layout.simple_spinner_dropdown_item, items);
        this.items = items; // ugly workaround, cannot assign local variable before super() call
        this.support.addAll(items);
    }

    // accumulates indefinitely till dialog dismiss, irrespective of the fact that a XRE server may have stopped announcing itself
    @Override
    public void add(Pair<String, String> object) {
        if(support.contains(object)) return;
        support.add(object);
        items.clear();
        items.addAll(support);
        notifyDataSetChanged();
    }
}

package it.pgp.xfiles.adapters;

import android.content.Context;
import android.os.Environment;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Set;

import it.pgp.xfiles.utils.DiskHelper;

// currently used for showing external mounts
// TODO to be generalized (e.g. show favorites and currently opened xre connections as well)
public class QuickPathsAdapter extends ArrayAdapter<String> {

    public static QuickPathsAdapter create(Context context) {
        Set<String> s = DiskHelper.getExternalMounts();
        s.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        return new QuickPathsAdapter(context, new ArrayList<>(s));
    }

    public ArrayList<String> objects;

    private QuickPathsAdapter(Context context, ArrayList<String> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
        this.objects = objects;
    }
}

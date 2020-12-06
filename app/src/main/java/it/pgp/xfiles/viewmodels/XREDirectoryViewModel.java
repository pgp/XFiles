package it.pgp.xfiles.viewmodels;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.XreAnnouncesAdapter;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.Pair;

public class XREDirectoryViewModel {
    // xfiles remote dir
    public Spinner xreStoredData;
    public EditText xreServerHost;
    //    public EditText xreServerPort;
    public EditText xreRemotePath;
    public Map.Entry<String,String>[] xreItems;

    public final XreAnnouncesAdapter xreAnnouncesAdapter;
    public ListView xreAnnouncesListView;

    public final XreAnnouncesAdapter alreadyConnectedAdapter;
    public ListView alreadyConnectedListView;

    final Context a;
    fvbiInterface v;
    final GenericDBHelper dbh;

    final AtomicBoolean currentDirAutofillOverride;

    public interface fvbiInterface {
        <T extends View> T findViewById(int id);
    }

    public fvbiInterface getFvbiInterface(Object o) {
        if (o instanceof Activity)
            return ((Activity)o)::findViewById;
        else if (o instanceof Dialog)
            return ((Dialog)o)::findViewById;
        else if (o instanceof View)
            return ((View)o)::findViewById;
        throw new RuntimeException("Unexpected type in getFvbiInterface");
    }

    public static AdapterView.OnItemClickListener getDefaultAnnounceItemSelectListener(EditText xreServerHost, EditText xreRemotePath) {
        return (parent,view,position,id) -> {
            Pair<String,String> item = (Pair<String, String>) parent.getItemAtPosition(position);
            xreServerHost.setText(item.i);
            xreRemotePath.setText(item.j);
        };
    }

    final AdapterView.OnItemSelectedListener defaultSpinnerItemSelectListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (currentDirAutofillOverride.get()) {
                currentDirAutofillOverride.set(false);
                return;
            }

            Map.Entry<String,String> item = (Map.Entry<String, String>) parent.getItemAtPosition(position);
            xreServerHost.setText(item.getKey());
            xreRemotePath.setText(item.getValue());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    protected XREDirectoryViewModel(Activity activity, GenericDBHelper dbh, AtomicBoolean currentDirAutofillOverride) {
        this.a = activity;
        this.dbh = dbh;
        this.currentDirAutofillOverride = currentDirAutofillOverride;
        xreAnnouncesAdapter = XreAnnouncesAdapter.from(activity);
        alreadyConnectedAdapter = XreAnnouncesAdapter.fromAlreadyOpenedConnections(activity);
    }

    public XREDirectoryViewModel(Activity activity, View v, GenericDBHelper dbh, AtomicBoolean currentDirAutofillOverride) {
        this(activity, dbh, currentDirAutofillOverride);
        this.v = getFvbiInterface(v);
    }

    public XREDirectoryViewModel(Activity activity, Dialog dialog, GenericDBHelper dbh, AtomicBoolean currentDirAutofillOverride) {
        this(activity, dbh, currentDirAutofillOverride);
        this.v = getFvbiInterface(dialog);
    }

    public void initViews() {
        xreStoredData = v.findViewById(R.id.xreConnectionStoredDataSpinner);
        xreServerHost = v.findViewById(R.id.xreConnectionDomainEditText);
//        xreServerPort = v.findViewById(R.id.xreConnectionPortEditText);
        xreRemotePath = v.findViewById(R.id.xreRemoteDirEditText);

        xreAnnouncesListView = v.findViewById(R.id.xreAnnouncesListView);
        xreAnnouncesListView.setAdapter(xreAnnouncesAdapter);

        alreadyConnectedListView = v.findViewById(R.id.xreAlreadyConnectedListView);
        alreadyConnectedListView.setAdapter(alreadyConnectedAdapter);

        AdapterView.OnItemClickListener selectListener = getDefaultAnnounceItemSelectListener(xreServerHost, xreRemotePath);
        alreadyConnectedListView.setOnItemClickListener(selectListener);
        xreAnnouncesListView.setOnItemClickListener(selectListener);


        ArrayList<Map.Entry<String,String>> xreitems_ = new ArrayList<>();
        xreitems_.add(new AbstractMap.SimpleEntry<>("","")); // empty item for no selection
        xreitems_.add(new AbstractMap.SimpleEntry<>("192.168.43.1","")); // default remote server and default offered home path when server provides network access with its WiFi hotspot
        xreitems_.addAll(dbh.getAllRowsOfXreFavoritesTable().values());
        xreItems = xreitems_.toArray(new Map.Entry[0]);


        ArrayAdapter<Map.Entry<String,String>> xreAdapter = new ArrayAdapter<>(a,
                android.R.layout.simple_spinner_dropdown_item,
                xreItems);
        xreStoredData.setAdapter(xreAdapter);
        xreStoredData.setOnItemSelectedListener(defaultSpinnerItemSelectListener);
    }
}

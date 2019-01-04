package it.pgp.xfiles;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.NonInteractiveXFilesRemoteTransferService;
import it.pgp.xfiles.service.params.CopyMoveParams;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.IntentUtil;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;

public class XREDirectShareActivity extends EffectActivity {

    WifiButtonsLayout wbl;
    LinearLayout xre_embedded_layout;
    Button ok;

    GenericDBHelper dbh;

    Spinner xreStoredData;
    EditText xreServerHost;
    //    EditText xreServerPort;
    EditText xreRemotePath;
    Map.Entry<String,String>[] xreItems;

    List<String> filesToUpload_; // to be converged into CopyMoveListPathContent below
    BasePathContent srcPath; // to be converged into CopyMoveListPathContent below
    CopyMoveListPathContent filesToUpload;

    private boolean currentDirAutofillOverride = true;

    private void ok(View unused) {
        BasePathContent path = null;

        // empty base path means root path (/), so don't validate it
        if (!basicNonEmptyValidation(xreServerHost)) return;
        path = new XFilesRemotePathContent(
                xreServerHost.getText().toString(),
//                        Integer.valueOf(xreServerPort.getText().toString()),
                xreRemotePath.getText().toString()
        );

        // run xre copy service/task
        Intent startIntent = new Intent(this,NonInteractiveXFilesRemoteTransferService.class);
        startIntent.setAction(BaseBackgroundService.START_ACTION);
        startIntent.putExtra("params",new CopyMoveParams(filesToUpload,path));
        startService(startIntent);
        finish();
    }

    private boolean basicNonEmptyValidation(EditText... fields) {
        boolean valid = true;
        for (EditText field : fields) {
            valid &= (field != null) && !(field.getText().toString().equals(""));
        }
        if (!valid) Toast.makeText(this, "Invalid parameters", Toast.LENGTH_SHORT).show();
        return valid;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityIcon(R.drawable.xf_xre_server_up);
        MainActivity.mainActivityContext = getApplicationContext();

        // CHECK SHARE INTENT
        List<Uri> uris = IntentUtil.getShareSelectionFromIntent(getIntent());
        if (uris == null) {
            Toast.makeText(this, "Unable to get URI selection", Toast.LENGTH_SHORT).show();
            finish();
            return;
            /* onDestroy() called immediately after this
            web source: https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)
            */
        }
        Map.Entry<BasePathContent,List<String>> me = IntentUtil.getCommonAncestorAndItems(this,uris);
        filesToUpload_ = me.getValue();
        srcPath = me.getKey();

        RootHelperClientUsingPathContent rh = MainActivity.getRootHelperClient();

        // NOT SERIALIZABLE???
        /*List<BrowserItem> lb = new ArrayList<BrowserItem>(){{
            try {
                for (String path : filesToUpload_) {
                    // FIXME not optimized, one stat request for item
                    SingleStatsItem ssi = rh.statFile(srcPath.concat(path));
                    add(new BrowserItem(path,ssi.size,ssi.modificationTime,ssi.isDir,false));
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }};*/

        List<BrowserItem> lb = new ArrayList<>();
        try {
            for (String path : filesToUpload_) {
                // FIXME not optimized, one stat request for item
                SingleStatsItem ssi = rh.statFile(srcPath.concat(path));
                lb.add(new BrowserItem(path,ssi.size,ssi.modificationTime,ssi.isDir,false));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        filesToUpload = new CopyMoveListPathContent(lb,CopyMoveMode.COPY,srcPath);

        setContentView(R.layout.activity_xre_direct_share);
        dbh = new GenericDBHelper(this);
        xre_embedded_layout = findViewById(R.id.xre_embedded_layout);

        wbl = new WifiButtonsLayout(this);
        LinearLayout target = findViewById(R.id.targetWifiButtonsLayout);
        target.addView(wbl);

        // duplicated logic from GenericChangeDirectoryDialog (XRE switch branches)

        //////////////////////////////////

        xreStoredData = xre_embedded_layout.findViewById(R.id.xreConnectionStoredDataSpinner);
        xreServerHost = xre_embedded_layout.findViewById(R.id.xreConnectionDomainEditText);
//                xreServerPort = xre_embedded_layout.findViewById(R.id.xreConnectionPortEditText);
        xreRemotePath = xre_embedded_layout.findViewById(R.id.xreRemoteDirEditText);

        //////////////////////////////////

        ArrayList<Map.Entry<String,String>> xreitems_ = new ArrayList<>();
        xreitems_.add(new AbstractMap.SimpleEntry<>("","")); // empty item for no selection
        xreitems_.add(new AbstractMap.SimpleEntry<>("192.168.43.1","/sdcard")); // default remote server and path when server provides network access with its WiFi hotspot
        xreitems_.addAll(dbh.getAllRowsOfXreFavoritesTable().values());
        xreItems = new Map.Entry[xreitems_.size()];
        xreitems_.toArray(xreItems);

        ArrayAdapter<Map.Entry<String,String>> xreAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                xreItems);
        xreStoredData.setAdapter(xreAdapter);
        xreStoredData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentDirAutofillOverride) {
                    currentDirAutofillOverride = false;
                    return;
                }

                Map.Entry<String,String> item = (Map.Entry<String, String>) parent.getItemAtPosition(position);
                xreServerHost.setText(item.getKey());
                xreRemotePath.setText(item.getValue());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ///////////////////////////////////////

        ok = findViewById(R.id.xreDirectShareOkButton);
        ok.setOnClickListener(this::ok);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wbl.registerListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wbl != null) wbl.unregisterListeners();
        if (MainActivity.mainActivity == null) MainActivity.mainActivityContext = null;
    }
}

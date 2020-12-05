package it.pgp.xfiles;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import it.pgp.xfiles.adapters.XreAnnouncesAdapter;
import it.pgp.xfiles.dialogs.GenericChangeDirectoryDialog;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.NonInteractiveXFilesRemoteTransferService;
import it.pgp.xfiles.service.params.CopyMoveParams;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.IntentUtil;
import it.pgp.xfiles.utils.MulticastUtils;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;
import it.pgp.xfiles.viewmodels.XREDirectoryViewModel;

public class XREDirectShareActivity extends EffectActivity {

    WifiButtonsLayout wbl;
    LinearLayout xre_embedded_layout;

    GenericDBHelper dbh;

    Spinner xreStoredData;
    EditText xreServerHost;
    //    EditText xreServerPort;
    EditText xreRemotePath;
    Map.Entry<String,String>[] xreItems;

    List<String> filesToUpload_; // to be converged into CopyMoveListPathContent below
    BasePathContent srcPath; // to be converged into CopyMoveListPathContent below
    CopyMoveListPathContent filesToUpload;

    private final AtomicBoolean currentDirAutofillOverride = new AtomicBoolean(true);

    XREDirectoryViewModel xreDirectoryViewModel;

    public static XREDirectShareActivity instance;

    private void ok(View unused) {
        BasePathContent path;

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
//        finish(); // Security Manager prevents using content provider's file objects after the activity has ended
        if(filesToUpload instanceof CopyListUris) MainActivity.simulateHomePress(this); // pause activity instead of finishing it
        else finish();
    }

    private boolean basicNonEmptyValidation(EditText... fields) {
        boolean valid = true;
        for (EditText field : fields) {
            valid &= (field != null) && !(field.getText().toString().equals(""));
        }
        if (!valid) Toast.makeText(this, "Invalid parameters", Toast.LENGTH_SHORT).show();
        return valid;
    }

    static class ThreadWrapper extends Thread {
        private Runnable r;

        public void setRunnable(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            r.run();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setActivityIcon(R.drawable.xf_xre_server_up);
        MainActivity.mainActivityContext = getApplicationContext();
        MainActivity.refreshToastHandler(MainActivity.mainActivityContext);
        final RootHelperClientUsingPathContent rh = MainActivity.getRootHelperClient();

        // CHECK SHARE INTENT
        List<Uri> uris = IntentUtil.getShareSelectionFromIntent(getIntent());
        if (uris == null || uris.isEmpty()) {
            Toast.makeText(this, "Unable to get URI selection", Toast.LENGTH_SHORT).show();
            finish();
            return;
            /* onDestroy() called immediately after this
            web source: https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)
            */
        }

        if ("content".equals(uris.get(0).getScheme())) { // TODO check if condition is well written
            Log.d("DIRECTSHARE", "Populating from content uris");
            filesToUpload = CopyListUris.getFromUriList(uris);
        }
        else {
            Log.d("DIRECTSHARE", "Populating from path uris");
            Map.Entry<BasePathContent,List<String>> me = IntentUtil.getCommonAncestorAndItems(this,uris);
            filesToUpload_ = me.getValue();
            srcPath = me.getKey();

            // anonymous local classes are not serializable, so should populate it the standard way
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
        }

        setContentView(R.layout.activity_xre_direct_share);
        dbh = new GenericDBHelper(this);
        xre_embedded_layout = findViewById(R.id.xre_embedded_layout);

        wbl = new WifiButtonsLayout(this);
        LinearLayout target = findViewById(R.id.targetWifiButtonsLayout);
        target.addView(wbl);

        xreDirectoryViewModel = new XREDirectoryViewModel(this, xre_embedded_layout, dbh, currentDirAutofillOverride);
        xreDirectoryViewModel.initViews();

        findViewById(R.id.xreDirectShareOkButton).setOnClickListener(this::ok);

        /**
         * check extras for unattended direct share:
         * creates a cancelable progressdialog, waits for some xre server to send an announce,
         * then connects to it and sends data
         */
        if(getIntent().getExtras().getBoolean("unattended", false)) {

            ThreadWrapper t = new ThreadWrapper();

            ProgressDialog pd = new ProgressDialog(this);
            pd.setIndeterminate(true);
            pd.setCancelable(true);
            pd.setOnCancelListener(d -> {
                t.interrupt();
                finish();
            });
            pd.setMessage("Waiting for an XRE server to come online...");
            pd.show();

            // periodically check size of listener adapter, then click item and ok
            t.setRunnable(()->{
                while(!Thread.currentThread().isInterrupted()) {
                    if(xreDirectoryViewModel.xreAnnouncesAdapter.items.size() > 0) {
                        int activePosition = 0;
                        runOnUiThread(()->{
                            pd.dismiss();
                            xreDirectoryViewModel.xreAnnouncesListView.performItemClick(
                                    xreDirectoryViewModel.xreAnnouncesListView.getAdapter().getView(activePosition,null,null),
                                    activePosition,
                                    xreDirectoryViewModel.xreAnnouncesListView.getAdapter().getItemId(activePosition));
                            ok(null);
                        });
                        break;
                    }
                    LockSupport.parkNanos(500000000);
                }
                Log.d("UNATTENDED","Thread ended");
            });
            t.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        wbl.registerListeners();
        MulticastUtils.startXreAnnounceListenerThread(this,xreDirectoryViewModel.xreAnnouncesAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wbl != null) wbl.unregisterListeners();
        MulticastUtils.shutdownMulticastListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        if (MainActivity.mainActivity == null) MainActivity.mainActivityContext = null;
    }
}

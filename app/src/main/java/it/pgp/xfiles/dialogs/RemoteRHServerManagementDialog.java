package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Observable;
import java.util.Observer;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.fileservers.FileServer;
import it.pgp.xfiles.fileservers.SimpleFileServer;
import it.pgp.xfiles.roothelperclient.RHSSServerStatus;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.NetworkUtils;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.XREPathContent;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;

/**
 * Created by pgp on 13/10/17
 */

public class RemoteRHServerManagementDialog extends Dialog {
    private ImageButton rhss_show_xre_connections;

    private EditText xreHomePath;
    private EditText xreAnnouncedPath;
    private EditText xreExposedPath;
    private ImageButton xreSetHomePath;
    private ImageButton xreSetAnnouncedPath;
    private ImageButton xreSetExposedPath;

    private EditText ftpHttpRootPath;

    private CheckedTextView rhssSendXreAnnounceCtv;
    public final IfAddrsObserver ifAddrsObserver;

    private BasePathContent currentDir;

    public class IfAddrsObserver implements Observer {

        private final TextView rhssIPAddresses;

        private final Button ftpServerButton;
        private final Button httpServerButton;
        private final ImageButton rhss_status_button;

        private final boolean[] state = new boolean[3]; // FTP, HTTP, XRE (first two are FileServer enum values)

        private boolean anyOn() {
            boolean res = false;
            for(boolean x : state)
                res |= x;
            return res;
        }

        public IfAddrsObserver() {
            rhssIPAddresses = findViewById(R.id.rhssIPAddresses);

            ftpServerButton = findViewById(R.id.ftpServerButton);
            httpServerButton = findViewById(R.id.httpServerButton);
            rhss_status_button = findViewById(R.id.rhss_toggle_rhss_button);
            rhss_status_button.setOnClickListener(RemoteRHServerManagementDialog.this::switch_rhss_status);

            state[0] = FileServer.FTP.server.isAlive();
            state[1] = FileServer.HTTP.server.isAlive();
            state[2] = RemoteServerManager.rhssManagerRef.get()!=null;
            updateViews(null);
        }

        private void updateViews(Pair<Integer, Boolean> on) {
            activity.runOnUiThread(()->{
                rhssIPAddresses.setText(anyOn()?NetworkUtils.getInterfaceAddressesAsString():"");

                if(on==null) { // on dialog constructor, set all views
                    ftpServerButton.setTextColor(activity.getResources().getColor(state[0]? R.color.green:R.color.red));
                    httpServerButton.setTextColor(activity.getResources().getColor(state[1]? R.color.green:R.color.red));
                    rhss_status_button.setImageResource(state[2]?R.drawable.xf_xre_server_up:R.drawable.xf_xre_server_down);
                }
                else switch(on.i) {
                    case 0:
                        ftpServerButton.setTextColor(activity.getResources().getColor(on.j? R.color.green:R.color.red));
                        break;
                    case 1:
                        httpServerButton.setTextColor(activity.getResources().getColor(on.j? R.color.green:R.color.red));
                        break;
                    case 2:
                        rhss_status_button.setImageResource(on.j?R.drawable.xf_xre_server_up:R.drawable.xf_xre_server_down);
                        break;
                }
            });
        }

        @Override
        public void update(Observable o, Object arg) {
            Pair<Integer, Boolean> on = (Pair) arg;
            state[on.i] = on.j;
            updateViews(on);
        }
    }

    private final View.OnClickListener setCurrentDirectoryListener = v -> {
        EditText targetEditText;
        switch(v.getId()) {
            case R.id.setXreHomePathToCurrent:
                targetEditText = xreHomePath;
                break;
            case R.id.setXreAnnouncedPathToCurrent:
                targetEditText = xreAnnouncedPath;
                break;
            case R.id.setXreExposedPathToCurrent:
                targetEditText = xreExposedPath;
                break;
            default:
                MainActivity.showToast("Invalid resource id in setCurrentDirectoryListener");
                return;
        }

        targetEditText.setText(currentDir.dir);
    };

    private void togglePathsWidgets(boolean status) {
        rhssSendXreAnnounceCtv.setEnabled(status);
        xreHomePath.setEnabled(status);
        xreAnnouncedPath.setEnabled(status);
        xreExposedPath.setEnabled(status);
    }

    private void saveOrClearPaths(boolean save) {
        if(save) {
            RHSSServerStatus.xreHomePathStr = xreHomePath.getText().toString();
            RHSSServerStatus.xreAnnouncedPathStr = xreAnnouncedPath.getText().toString();
            RHSSServerStatus.xreExposedPathStr = xreExposedPath.getText().toString();
            RHSSServerStatus.announceEnabled = rhssSendXreAnnounceCtv.isChecked();
        }
        else {
            RHSSServerStatus.xreHomePathStr = "";
            RHSSServerStatus.xreAnnouncedPathStr = "";
            RHSSServerStatus.xreExposedPathStr = "";
        }
    }

    private void retrievePathsIntoEditTexts() {
        xreHomePath.setText(RHSSServerStatus.xreHomePathStr);
        xreAnnouncedPath.setText(RHSSServerStatus.xreAnnouncedPathStr);
        xreExposedPath.setText(RHSSServerStatus.xreExposedPathStr);
        rhssSendXreAnnounceCtv.setChecked(RHSSServerStatus.announceEnabled);
    }

    private void switch_rhss_status(View unused) {
        if (RemoteServerManager.rhssManagerRef.get()==null) { // OFF -> ON
            RemoteServerManager.RHSS_ACTION targetAction =
                    (rhssSendXreAnnounceCtv !=null && rhssSendXreAnnounceCtv.isChecked())?
                            RemoteServerManager.RHSS_ACTION.START_ANNOUNCE:
                            RemoteServerManager.RHSS_ACTION.START;

            MainActivity.getRootHelperClient(); // ensure RH local server is started

            int result = RemoteServerManager.rhss_action(targetAction,
                    xreHomePath.getText().toString(),
                    xreAnnouncedPath.getText().toString(),
                    xreExposedPath.getText().toString());

            switch (result) {
                case 1:
                    Toast.makeText(activity, "Remote RH server started on port "+ XREPathContent.defaultRHRemoteServerPort, Toast.LENGTH_SHORT).show();
//                    rhss_status_button.setImageResource(R.drawable.xf_xre_server_up);
                    togglePathsWidgets(false);
                    saveOrClearPaths(true);
//                    rhssIPAddresses.setText(NetworkUtils.getInterfaceAddressesAsString());
                    break;
                case 0:
                    Toast.makeText(activity, "Unable to start remote RH server", Toast.LENGTH_SHORT).show();
                    break;
                case -1:
                    Toast.makeText(activity, "Unable to start remote RH server (I/O error)", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    throw new RuntimeException("Unexpected return value from rhss_action");
            }
        }
        else { // ON -> OFF
            switch (RemoteServerManager.rhss_action(RemoteServerManager.RHSS_ACTION.STOP)) {
                case 1:
                    Toast.makeText(activity, "Remote RH server stopped", Toast.LENGTH_SHORT).show();
//                    rhss_status_button.setImageResource(R.drawable.xf_xre_server_down);
                    togglePathsWidgets(true);
                    saveOrClearPaths(false);

//                    rhssIPAddresses.setText("");
                    break;
                case 0:
                    Toast.makeText(activity, "Unable to stop remote RH server", Toast.LENGTH_SHORT).show();
                    break;
                case -1:
                    Toast.makeText(activity, "Unable to stop remote RH server (I/O error)", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    throw new RuntimeException("Unexpected return value from rhss_action");
            }
        }
    }

    private final Activity activity;

    public static RemoteRHServerManagementDialog instance;
    public RemoteRHServerManagementDialog(@NonNull Activity activity) {
        super(activity,R.style.fs_dialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        instance = this;
        setOnShowListener(EffectActivity.defaultDialogShowListener);
        this.activity = activity;
        setContentView(R.layout.remote_rh_server_management_dialog);

        currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();

        ifAddrsObserver = new IfAddrsObserver();
        FileServer.FTP.server.addObserver(ifAddrsObserver);
        FileServer.HTTP.server.addObserver(ifAddrsObserver);
        RemoteServerManager.rsmObservable.addObserver(ifAddrsObserver);

//        rhss_status_button = findViewById(R.id.rhss_toggle_rhss_button);
        rhss_show_xre_connections = findViewById(R.id.rhss_show_xre_connections);

        for (FileServer fileServer : FileServer.values()) {
            SimpleFileServer s = fileServer.server;
            s.serverButton = findViewById(s.serverButtonRes);
            s.serverButton.setOnClickListener(v->{
                s.setRootPath(((EditText)findViewById(R.id.ftpHttpRootPath)).getText().toString()); // not needed in case of server On->OFF
                s.toggle();
            });
        }

        WifiButtonsLayout wbl = new WifiButtonsLayout(activity);
        LinearLayout target = findViewById(R.id.targetWifiButtonsLayout);
        target.addView(wbl);

        xreHomePath = findViewById(R.id.xreHomePath);
        xreAnnouncedPath = findViewById(R.id.xreAnnouncedPath);
        xreExposedPath = findViewById(R.id.xreExposedPath);

        xreSetHomePath = findViewById(R.id.setXreHomePathToCurrent);
        xreSetAnnouncedPath = findViewById(R.id.setXreAnnouncedPathToCurrent);
        xreSetExposedPath = findViewById(R.id.setXreExposedPathToCurrent);
        if(currentDir instanceof LocalPathContent) {
            xreSetHomePath.setOnClickListener(setCurrentDirectoryListener);
            xreSetAnnouncedPath.setOnClickListener(setCurrentDirectoryListener);
            xreSetExposedPath.setOnClickListener(setCurrentDirectoryListener);
        }
        else {
            xreSetHomePath.setEnabled(false);
            xreSetAnnouncedPath.setEnabled(false);
            xreSetExposedPath.setEnabled(false);
        }

        ftpHttpRootPath = findViewById(R.id.ftpHttpRootPath);
        ftpHttpRootPath.setText(Misc.internalStorageDir.getAbsolutePath());

        rhssSendXreAnnounceCtv = findViewById(R.id.rhssAnnounceOptionCtv);
        rhssSendXreAnnounceCtv.setOnClickListener(Misc.ctvListener);

        // check rhss manager thread status
        if (RemoteServerManager.rhssManagerRef.get() == null) {
//            rhss_status_button.setImageResource(R.drawable.xf_xre_server_down);
            togglePathsWidgets(true);
        }
        else {
//            rhss_status_button.setImageResource(R.drawable.xf_xre_server_up);
            // rhssIPAddresses.setText(NetworkUtils.getInterfaceAddressesAsString());
            retrievePathsIntoEditTexts();
            togglePathsWidgets(false);
        }

        rhss_show_xre_connections.setOnClickListener(((MainActivity) activity)::showXREConnections);

        wbl.registerListeners();
        setOnDismissListener(dialog->{
            wbl.unregisterListeners();

            FileServer.FTP.server.deleteObserver(ifAddrsObserver);
            FileServer.HTTP.server.deleteObserver(ifAddrsObserver);
            RemoteServerManager.rsmObservable.deleteObserver(ifAddrsObserver);

            instance = null;
            EffectActivity.currentlyOnFocus = MainActivity.mainActivity;
        });
    }
}

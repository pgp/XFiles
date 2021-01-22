package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.fileservers.FileServer;
import it.pgp.xfiles.roothelperclient.RHSSServerStatus;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.NetworkUtils;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;

/**
 * Created by pgp on 13/10/17
 */

public class RemoteRHServerManagementDialog extends Dialog {

    private ImageButton rhss_status_button;
    private ImageButton rhss_show_xre_connections;

    private EditText xreHomePath;
    private EditText xreAnnouncedPath;
    private EditText xreExposedPath;
    private ImageButton xreSetHomePath;
    private ImageButton xreSetAnnouncedPath;
    private ImageButton xreSetExposedPath;

    private EditText ftpHttpRootPath;

    private CheckBox rhssSendXreAnnounceCheckbox;

    private TextView rhssIPAddresses;

    private BasePathContent currentDir;

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
                MainActivity.showToastOnUI("Invalid resource id in setCurrentDirectoryListener");
                return;
        }

        targetEditText.setText(currentDir.dir);
    };

    private void togglePathsWidgets(boolean status) {
        rhssSendXreAnnounceCheckbox.setEnabled(status);
        xreHomePath.setEnabled(status);
        xreAnnouncedPath.setEnabled(status);
        xreExposedPath.setEnabled(status);
    }

    private void saveOrClearPaths(boolean save) {
        if(save) {
            RHSSServerStatus.xreHomePathStr = xreHomePath.getText().toString();
            RHSSServerStatus.xreAnnouncedPathStr = xreAnnouncedPath.getText().toString();
            RHSSServerStatus.xreExposedPathStr = xreExposedPath.getText().toString();
            RHSSServerStatus.announceEnabled = rhssSendXreAnnounceCheckbox.isChecked();
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
        rhssSendXreAnnounceCheckbox.setChecked(RHSSServerStatus.announceEnabled);
    }

    private void switch_rhss_status(View unused) {
        if (RemoteServerManager.rhssManagerRef.get()==null) { // OFF -> ON
            RemoteServerManager.RHSS_ACTION targetAction =
                    (rhssSendXreAnnounceCheckbox!=null && rhssSendXreAnnounceCheckbox.isChecked())?
                            RemoteServerManager.RHSS_ACTION.START_ANNOUNCE:
                            RemoteServerManager.RHSS_ACTION.START;

            MainActivity.getRootHelperClient(); // ensure RH local server is started

            int result = RemoteServerManager.rhss_action(targetAction,
                    xreHomePath.getText().toString(),
                    xreAnnouncedPath.getText().toString(),
                    xreExposedPath.getText().toString());

            switch (result) {
                case 1:
                    Toast.makeText(activity, "Remote RH server started", Toast.LENGTH_SHORT).show();
                    rhss_status_button.setImageResource(R.drawable.xf_xre_server_up);
                    togglePathsWidgets(false);
                    saveOrClearPaths(true);
                    rhssIPAddresses.setText(NetworkUtils.getInterfaceAddressesAsString());
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
                    rhss_status_button.setImageResource(R.drawable.xf_xre_server_down);
                    togglePathsWidgets(true);
                    saveOrClearPaths(false);

                    rhssIPAddresses.setText("");
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

        rhssIPAddresses = findViewById(R.id.rhssIPAddresses);

        rhss_status_button = findViewById(R.id.rhss_toggle_rhss_button);
        rhss_show_xre_connections = findViewById(R.id.rhss_show_xre_connections);

        for (FileServer fileServer : FileServer.values()) {
            fileServer.server.serverButton = findViewById(fileServer.server.serverButtonRes);
            fileServer.server.serverButton.setOnClickListener(v->{
                fileServer.setRootPath(((EditText)findViewById(R.id.ftpHttpRootPath)).getText().toString()); // not needed in case of server On->OFF
                fileServer.toggle();
            });
            fileServer.refresh_button_color(activity, null);
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

        rhssSendXreAnnounceCheckbox = findViewById(R.id.rhssAnnounceOptionCheckBox);

        // check rhss manager thread status
        if (RemoteServerManager.rhssManagerRef.get() == null) {
            rhss_status_button.setImageResource(R.drawable.xf_xre_server_down);
            togglePathsWidgets(true);
        }
        else {
            rhss_status_button.setImageResource(R.drawable.xf_xre_server_up);
            rhssIPAddresses.setText(NetworkUtils.getInterfaceAddressesAsString());
            retrievePathsIntoEditTexts();
            togglePathsWidgets(false);
        }

        rhss_status_button.setOnClickListener(this::switch_rhss_status);
        rhss_show_xre_connections.setOnClickListener(((MainActivity) activity)::showXREConnections);

        wbl.registerListeners();
        setOnDismissListener(dialog->{
            wbl.unregisterListeners();
            instance = null;
            EffectActivity.currentlyOnFocus = MainActivity.mainActivity;
        });
    }
}

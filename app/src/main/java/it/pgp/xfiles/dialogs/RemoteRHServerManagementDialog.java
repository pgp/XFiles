package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.fileservers.FileServer;
import it.pgp.xfiles.roothelperclient.RHSSServerStatus;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 13/10/17
 */

public class RemoteRHServerManagementDialog extends Dialog {

    private ImageButton rhss_status_button;
    private ImageButton rhss_show_xre_connections;
    private TextView rhssServeOnlyCurrentDirectoryTextView;
    private CheckBox rhssServeOnlyCurrentDirectoryCheckBox;

    private String candidateLocalPath;
    private TextView rhssIPAddresses;

    private void switch_rhss_status(View unused) {
        if (RemoteServerManager.rhssManagerThreadRef.get()==null) { // OFF -> ON
            int result = RemoteServerManager.rhss_action(
                            RemoteServerManager.RHSS_ACTION.START,
                            rhssServeOnlyCurrentDirectoryCheckBox.isChecked()?candidateLocalPath:"");

            switch (result) {
                case 1:
                    Toast.makeText(activity, "Remote RH server started", Toast.LENGTH_SHORT).show();
                    rhss_status_button.setImageResource(R.drawable.xf_xre_server_up);
                    rhssServeOnlyCurrentDirectoryCheckBox.setEnabled(false);
                    rhssIPAddresses.setText(getInterfaceAddressesAsString());
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
                    rhssServeOnlyCurrentDirectoryCheckBox.setEnabled(true);
                    // reload current directory path, which may have changed between the two dialog openings
                    rhssServeOnlyCurrentDirectoryTextView.setText(R.string.serve_only_current_directory);
                    setCandidateLocalPath(); // doesn't set anything if we are on a non-local path
                    rhssServeOnlyCurrentDirectoryTextView.setText(
                            rhssServeOnlyCurrentDirectoryTextView.getText().toString()+"\n"+
                                    candidateLocalPath
                    );
                    rhssIPAddresses.setText("");
                    // clientSessionsAdapter.syncFromActivity(null);
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

    public static Map<String,List<String>> getInterfacesAddresses() {
        Map<String,List<String>> addresses = new HashMap<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                List<String> addressesOfInterface = new ArrayList<>();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        //return inetAddress.getHostAddress().toString();
                        addressesOfInterface.add(inetAddress.getHostAddress());
                    }
                }
                addresses.put(intf.getName(),addressesOfInterface);
            }
        }
        catch (Exception ignored) {}
        return addresses;
    }

    public static String getInterfaceAddressesAsString() {
        StringBuilder s = new StringBuilder();
        Map<String,List<String>> addresses = getInterfacesAddresses();
        for (Map.Entry<String,List<String>> t : addresses.entrySet()) {
            StringBuilder inner = new StringBuilder();
            for (String j : t.getValue())
                if (!j.isEmpty()) inner.append(j).append(" ");
            if (!inner.toString().isEmpty()) {
                s.append(t.getKey()).append(": ").append(inner);
                s.append("\n");
            }
        }
        return s.toString();
    }

    public static RemoteRHServerManagementDialog instance;
    public RemoteRHServerManagementDialog(@NonNull Activity activity) {
        super(activity);
        instance = this;
        this.activity = activity;
        setTitle("RHSS Management");
        setContentView(R.layout.remote_rh_server_management_dialog);

        rhssIPAddresses = findViewById(R.id.rhssIPAddresses);

        rhss_status_button = findViewById(R.id.rhss_toggle_rhss_button);
        rhss_show_xre_connections = findViewById(R.id.rhss_show_xre_connections);

        for (FileServer fileServer : FileServer.values()) {
            fileServer.server.serverButton = findViewById(fileServer.server.serverButtonRes);
            fileServer.server.serverButton.setOnClickListener(v->{
                fileServer.setRootPath(((EditText)findViewById(R.id.ftpHttpRootPath)).getText().toString()); // not needed in case of server On->OFF
                fileServer.toggle();
            });
            fileServer.refresh_button_color(activity);
        }

        WifiButtonsLayout wbl = new WifiButtonsLayout(activity);
        LinearLayout target = findViewById(R.id.targetWifiButtonsLayout);
        target.addView(wbl);

        rhssServeOnlyCurrentDirectoryTextView = findViewById(R.id.rhssServeOnlyCurrentDirectoryTextView);
        rhssServeOnlyCurrentDirectoryCheckBox = findViewById(R.id.rhssServeOnlyCurrentDirectoryCheckBox);

        // check rhss manager thread status
        if (RemoteServerManager.rhssManagerThreadRef.get() == null) {
            rhss_status_button.setImageResource(R.drawable.xf_xre_server_down);
            // for robustness, better to ask rh directly if rhss remote server is active

            setCandidateLocalPath(); // doesn't set anything if we are on a non-local path
            rhssServeOnlyCurrentDirectoryCheckBox.setEnabled(true);
            rhssServeOnlyCurrentDirectoryCheckBox.setChecked(false);
            rhssServeOnlyCurrentDirectoryTextView.setText(
                    rhssServeOnlyCurrentDirectoryTextView.getText().toString()+"\n"+
                            candidateLocalPath
            );
        }
        else {
            rhss_status_button.setImageResource(R.drawable.xf_xre_server_up);
            rhssIPAddresses.setText(getInterfaceAddressesAsString());
            rhssServeOnlyCurrentDirectoryCheckBox.setChecked(
                    !RHSSServerStatus.currentlyServedLocalPath.isEmpty());
            rhssServeOnlyCurrentDirectoryCheckBox.setEnabled(false);
            rhssServeOnlyCurrentDirectoryTextView.setText(
                    rhssServeOnlyCurrentDirectoryTextView.getText().toString()+"\n"+
                            RHSSServerStatus.currentlyServedLocalPath
            );
        }

        rhss_status_button.setOnClickListener(this::switch_rhss_status);
        rhss_show_xre_connections.setOnClickListener(((MainActivity) activity)::showXREConnections);

        wbl.registerListeners();
        setOnDismissListener(dialog->{
            wbl.unregisterListeners();
            instance = null;
        });
    }

    private void setCandidateLocalPath() {
            BasePathContent bpc = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (bpc.providerType != ProviderType.LOCAL) candidateLocalPath = "";
            else candidateLocalPath = bpc.dir;
    }
}

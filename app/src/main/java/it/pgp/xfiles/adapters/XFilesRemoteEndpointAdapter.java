package it.pgp.xfiles.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.roothelperclient.RHSSServerStatus;
import it.pgp.xfiles.roothelperclient.RemoteManager;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

/**
 * Created by pgp on 27/10/17
 */

public class XFilesRemoteEndpointAdapter extends BaseAdapter {

    final Activity context;

    private final ArrayList<String> addresses = new ArrayList<>();

    /*
     if true, loads and updates adapter status from RHSSServerStatus,
     else from static RemoteClientManager in MainActivity
     */
    private final boolean serverMode;

    private void fillAdapter() {
        addresses.clear();
        if (serverMode) {
            if (RemoteServerManager.rhssManagerRef.get()!=null) {
                addresses.addAll(RHSSServerStatus.StoCSessions.keySet());
            }
        }
        else {
            for (Map.Entry<String,RemoteManager> entry :
                    MainActivity.rootHelperRemoteClientManager.fastClients.entrySet()) {
                addresses.add("F_"+entry.getKey());
            }
            // long term sessions
            for (Map.Entry<String,RemoteManager> entry :
                    MainActivity.rootHelperRemoteClientManager.longTermClients.entrySet()) {
                addresses.add("LT_"+entry.getKey());
            }
        }
    }

    public XFilesRemoteEndpointAdapter(final Activity context, boolean serverMode) {
        this.context = context;
        this.serverMode = serverMode;
        fillAdapter();
    }

    // (legacy comment for StoC sessions)
    // rhss update thread modifies serverSessionData, then calls syncFromActivity
    // map reference to serverSessionData is always the same, just reload array from map

    public void syncFromActivity() {
        fillAdapter();
        context.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public int getCount() {
        return addresses.size();
    }

    @Override
    public String getItem(int i) {
        return addresses.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.rhss_server_sessions_item, null);

            TextView clientIPandPort = view.findViewById(R.id.rhss_session_endpoint_IP_and_port);

            ImageButton endpointInfo = view.findViewById(R.id.rhss_session_endpoint_info);

            endpointInfo.setOnClickListener(v -> {
                byte[] sessionHash;
                String endpoint = getItem(i);
                if (serverMode) sessionHash = RHSSServerStatus.StoCSessions.get(endpoint);
                else {
                     if (endpoint.startsWith("F_")) {
                         endpoint = endpoint.replace("F_","");
                         sessionHash = MainActivity.rootHelperRemoteClientManager.fastClients.get(endpoint).tlsSessionHash;
                     }
                     else if (endpoint.startsWith("LT_")) {
                         endpoint = endpoint.replace("LT_","");
                         sessionHash = MainActivity.rootHelperRemoteClientManager.longTermClients.get(endpoint).tlsSessionHash;
                     }
                     else throw new RuntimeException("Unexpected prefix for remote client session");
                }

                // also show visual hash dialog
                // new HashViewDialog(context,sessionHash,false).show();
                PopupWindowUtils.createAndShowHashViewCommon(context,sessionHash,false,endpointInfo);
            });

//            ImageButton clientDisconnect = view.findViewById(R.id.rhss_server_session_client_disconnect);

            clientIPandPort.setText(getItem(i));
        }
        return view;
    }
}

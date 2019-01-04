package it.pgp.xfiles.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ListView;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.XFilesRemoteEndpointAdapter;

/**
 * Created by pgp on 21/12/17
 */

public class XFilesRemoteSessionsManagementActivity extends EffectActivity {

    public static XFilesRemoteEndpointAdapter StoCAdapter = null;
    public static XFilesRemoteEndpointAdapter CtoSAdapter = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityIcon(R.drawable.xf_xre_server_up);
        setContentView(R.layout.xfiles_remote_sessions_management);
        StoCAdapter = new XFilesRemoteEndpointAdapter(this, true);
        ListView StoCListView = findViewById(R.id.currentlyOpenServerSessions); // observed
        StoCListView.setAdapter(StoCAdapter);

        CtoSAdapter = new XFilesRemoteEndpointAdapter(this, false);
        ListView CtoSListView = findViewById(R.id.currentlyOpenClientSessions); // not observed
        CtoSListView.setAdapter(CtoSAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StoCAdapter = null;
        CtoSAdapter = null;
    }
}

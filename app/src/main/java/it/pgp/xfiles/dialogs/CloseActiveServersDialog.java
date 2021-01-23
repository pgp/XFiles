package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.fileservers.FileServer;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;

public class CloseActiveServersDialog extends Dialog {

    Button Bxre,Bftp,Bhttp;
    TextView Txre,Tftp,Thttp;

    final Activity context;

    int red,green;

    public CloseActiveServersDialog(final Activity context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.context = context;
        setOnShowListener(EffectActivity.defaultDialogShowListener);
        setOnDismissListener(d->EffectActivity.currentlyOnFocus = MainActivity.mainActivity);

        red = context.getResources().getColor(R.color.red);
        green = context.getResources().getColor(R.color.green);

        setContentView(R.layout.active_remote_servers_layout);

        Txre = findViewById(R.id.active_servers_xre_textview);
        Tftp = findViewById(R.id.active_servers_ftp_textview);
        Thttp = findViewById(R.id.active_servers_http_textview);

        Bxre = findViewById(R.id.active_servers_xre_button);
        Bftp = findViewById(R.id.active_servers_ftp_button);
        Bhttp = findViewById(R.id.active_servers_http_button);

        if(RemoteServerManager.rhssManagerRef.get() != null) {
            Txre.setTextColor(green);
            Bxre.setOnClickListener(v->{
                RemoteServerManager.rhss_action(RemoteServerManager.RHSS_ACTION.STOP);
                Txre.setTextColor(red);
                Bxre.setEnabled(false);
            });
        }
        else {
            Txre.setTextColor(red);
            Bxre.setEnabled(false);
        }
        
        if(FileServer.FTP.server.isAlive()) {
            Tftp.setTextColor(green);
            Bftp.setOnClickListener(v-> {
                FileServer.FTP.server.stopServer();
                Tftp.setTextColor(red);
                Bftp.setEnabled(false);
            });
        }
        else {
            Tftp.setTextColor(red);
            Bftp.setEnabled(false);
        }

        if(FileServer.HTTP.server.isAlive()) {
            Thttp.setTextColor(green);
            Bhttp.setOnClickListener(v-> {
                FileServer.HTTP.server.stopServer();
                Thttp.setTextColor(red);
                Bhttp.setEnabled(false);
            });
        }
        else {
            Thttp.setTextColor(red);
            Bhttp.setEnabled(false);
        }
    }

    @Override
    public void onBackPressed() {
        context.finishAffinity();
    }
}

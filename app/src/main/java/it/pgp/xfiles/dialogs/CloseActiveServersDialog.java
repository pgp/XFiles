package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

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

        red = context.getResources().getColor(R.color.red);
        green = context.getResources().getColor(R.color.green);

        setContentView(R.layout.active_remote_servers_layout);

        Txre = findViewById(R.id.active_servers_xre_textview);
        Tftp = findViewById(R.id.active_servers_ftp_textview);
        Thttp = findViewById(R.id.active_servers_http_textview);

        Bxre = findViewById(R.id.active_servers_xre_button);
        Bftp = findViewById(R.id.active_servers_ftp_button);
        Bhttp = findViewById(R.id.active_servers_http_button);

        Txre.setTextColor((RemoteServerManager.rhssManagerThreadRef.get() != null)?green:red);
        Tftp.setTextColor(FileServer.FTP.isAlive()?green:red);
        Thttp.setTextColor((FileServer.HTTP.isAlive()?green:red));

        Bxre.setOnClickListener(v->{
            RemoteServerManager.rhss_action(RemoteServerManager.RHSS_ACTION.STOP);
            Txre.setTextColor(red);
            Bxre.setEnabled(false);
        });

        Bftp.setOnClickListener(v-> {
            FileServer.FTP.stop();
            Tftp.setTextColor(red);
            Bftp.setEnabled(false);
        });

        Bhttp.setOnClickListener(v-> {
            FileServer.HTTP.stop();
            Thttp.setTextColor(red);
            Bhttp.setEnabled(false);
        });

    }

    @Override
    public void onBackPressed() {
        context.finishAffinity();
    }
}

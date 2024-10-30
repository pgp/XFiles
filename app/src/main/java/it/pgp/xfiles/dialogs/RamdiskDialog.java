package it.pgp.xfiles.dialogs;

import android.os.Build;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.roothelperclient.RootHandler;

public class RamdiskDialog extends ImmersiveModeDialog {
    public RamdiskDialog(MainActivity mainActivity) {
        super(mainActivity);
        setContentView(R.layout.single_filename_dialog);
        setTitle("Ramdisk");
        EditText size_ = findViewById(R.id.singleFilenameEditText);
        Button ok = findViewById(R.id.singleFilenameOkButton);
        size_.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        size_.setText("500");
        String mountpath = "/data/local/tmp/devshm";

        ok.setOnClickListener(v -> {
            int sz;
            String commands;
            try {
                sz = Integer.parseInt(size_.getText().toString());
                if(sz == 0) { // Unmount existing
                    commands = "umount "+mountpath+" && rm -rf "+mountpath;
                }
                else {
                    String size = size_.getText().toString() + "m";
                    commands = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? "" : "setenforce 0 && ")+
                            "mkdir -p "+mountpath+" && chmod 777 "+mountpath+
                            " && mount -t tmpfs xfilesramdisk "+mountpath+" -o mode=0777,size="+size;
                }
            }
            catch(Exception e) {
                MainActivity.showToast("Error getting ramdisk size: "+e.getMessage());
                return;
            }

            try {
                Process p = RootHandler.executeCommandSimple(commands, new File("/"), true, false);
                int ret = p.waitFor();
                if(ret != 0) throw new Exception("Non-zero return value "+ret);
            }
            catch(Exception e) {
                MainActivity.showToast("Error creating or unmounting ramdisk: "+e.getMessage());
                return;
            }
            MainActivity.showToast(sz == 0 ? "Ramdisk unmounted" : "Ramdisk of size "+sz+" Mb created at "+mountpath);
            dismiss();
        });
    }
}

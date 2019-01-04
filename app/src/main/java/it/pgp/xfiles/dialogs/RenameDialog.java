package it.pgp.xfiles.dialogs;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */
public class RenameDialog extends ImmersiveModeDialog {

    public RenameDialog(final MainActivity mainActivity, final BasePathContent f) {
        super(mainActivity);
        setContentView(R.layout.single_filename_dialog);
        setTitle("Rename");
        EditText filename = findViewById(R.id.singleFilenameEditText);
        Button ok = findViewById(R.id.singleFilenameOkButton);
        filename.setText(f.getName());

        ok.setOnClickListener(v -> {
            String filename_ = filename.getText().toString();
            BasePathContent ff = f.getParent().concat(filename_);
            try {
                if (MainActivity.currentHelper.renameFile(f,ff)) {
                    Toast.makeText(mainActivity, "Renamed", Toast.LENGTH_SHORT).show();
                    mainActivity.browserPagerAdapter.showDirContent(mainActivity.getCurrentDirCommander().refresh(),mainActivity.browserPager.getCurrentItem(),filename_);
                }
                else {
                    Toast.makeText(mainActivity, "Error renaming item", Toast.LENGTH_SHORT).show();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(mainActivity, "Roothelper communication error", Toast.LENGTH_SHORT).show();
            }

            dismiss();
        });
    }
}

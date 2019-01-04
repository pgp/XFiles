package it.pgp.xfiles.utils.legacy;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */

@Deprecated
public class ChangeDirectoryDialog extends Dialog {
    private MainActivity mainActivity;
    private EditText newDirPath;

    private void ok(View unused) {
        mainActivity.goDir(newDirPath.getText().toString());
        dismiss();
    }

    public ChangeDirectoryDialog(MainActivity mainActivity, Context context, String curDirPath) {
        super(context);
        this.mainActivity = mainActivity;
        setTitle("Change directory");
        setContentView(R.layout.single_filename_dialog);

        newDirPath = findViewById(R.id.singleFilenameEditText);
        newDirPath.setText(curDirPath);
        // TODO find how to confirm on Enter key press

        Button okButton = findViewById(R.id.singleFilenameOkButton);
        okButton.setOnClickListener(this::ok);

    }
}

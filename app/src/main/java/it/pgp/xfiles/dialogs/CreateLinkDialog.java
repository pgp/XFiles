package it.pgp.xfiles.dialogs;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.IOException;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class CreateLinkDialog extends BaseDialog {

    BasePathContent originPath;
    EditText linkPathEditText;
    Button ok;

    RadioButton isSoftLink,isHardLink;
    final MainActivity mainActivity;

    public CreateLinkDialog(final MainActivity mainActivity, final BasePathContent originPath, final FileMode type) {
        super(mainActivity);
        this.mainActivity = mainActivity;
        this.originPath = originPath;
        setContentView(R.layout.create_link_dialog);
        setDialogIcon(R.drawable.xfiles_link_icon);

        linkPathEditText = findViewById(R.id.linkCreate_targetPath);
        isSoftLink = findViewById(R.id.linkCreate_type_soft);
        isHardLink = findViewById(R.id.linkCreate_type_hard);
        ok = findViewById(R.id.linkCreate_OkButton);

        if(type==FileMode.DIRECTORY) { // allow only symlinks for directories
            isHardLink.setEnabled(false);
        }
        isSoftLink.setChecked(true);

        linkPathEditText.setText(originPath.dir+".link");

        ok.setOnClickListener(this::ok);
    }

    public void ok(View unused) {
        // clone source path, then assign dest path
        final BasePathContent linkPath = originPath.getCopy();
        linkPath.dir = linkPathEditText.getText().toString();
        if (linkPath.dir.isEmpty()) {
            Toast.makeText(mainActivity, "Empty target path provided", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mainActivity.getFileOpsHelper(originPath.providerType).createLink(originPath,linkPath,isHardLink.isChecked());
            Toast.makeText(mainActivity, "Link created", Toast.LENGTH_SHORT).show();
            mainActivity.browserPagerAdapter.showDirContent(
                    mainActivity.getCurrentDirCommander().refresh(),
                    mainActivity.browserPager.getCurrentItem(), linkPath.getName());
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mainActivity, "Link creation error, reason: "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }
}

package it.pgp.xfiles.dialogs;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class CreateLinkDialog extends BaseDialog {

    private final FileMode type;
    BasePathContent originPath;
    EditText linkPathEditText;
    Button ok;

    RadioButton isSoftLink,isHardLink;
    final MainActivity mainActivity;

    public CreateLinkDialog(final MainActivity mainActivity, final BasePathContent originPath, final FileMode type) {
        super(mainActivity);
        this.mainActivity = mainActivity;
        this.type = type;
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

        // dirty hack to workaround final variable requirements in lambdas and catch-finally data flow dependency
        // Collection.singletonList or Arrays.asList cannot be used here, in that they create immutables
        List<String> nameToLocate = new ArrayList<String>(){{add(linkPath.getName());}};

        try {
            /*if (originPath.providerType == ProviderType.XFILES_REMOTE) {
                if (!MainActivity.rootHelperRemoteClientManager.createLink(
                        (XFilesRemotePathContent)originPath,
                        (XFilesRemotePathContent)linkPath,
                        isHardLink.isChecked()))
                    throw new IOException("");
            }
            else*/
            MainActivity.currentHelper.createLink(originPath,linkPath,isHardLink.isChecked());
            Toast.makeText(mainActivity, "Link created", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            e.printStackTrace();
            nameToLocate.clear();
            MainActivity.showToastOnUI("Link creation error, reason: "+e.getMessage());
        }
        finally {
            mainActivity.browserPagerAdapter.showDirContent(mainActivity.getCurrentDirCommander().refresh(),mainActivity.browserPager.getCurrentItem(),
                    nameToLocate.isEmpty()?
                            new String[0]:new String[]{nameToLocate.get(0)});

            dismiss();
        }
    }
}

package it.pgp.xfiles.service;

import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.compress.AskPasswordDialogOnExtract;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.ExtractParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/06/17
 */

public class ExtractTask extends RootHelperClientTask {

//    public static final SocketNames extractSocketName = SocketNames.theextractor;
    public static final SocketNames extractSocketName = SocketNames.theroothelper;

    // direct input to extractArchive
    private BasePathContent srcArchive; // subDir taken from here
    private BasePathContent destDirectory;
    private String password;
    private List<String> filenames;

    private static final FileOpsErrorCodes defaultErrorResult = FileOpsErrorCodes.TRANSFER_ERROR;
    private BasePathContent currentDir;

    ExtractTask(Serializable params_) {
        super(params_);
        ExtractParams params = (ExtractParams) params_;
        srcArchive = params.srcArchive;
        destDirectory = params.destDirectory;
        password = params.password;
        filenames = params.filenames;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = new MovingRibbon(service,windowManager);
        return true;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
    }

    @Override
    protected Object doInBackground(Object[] params) {
        if (rh == null) {
            result = FileOpsErrorCodes.ROOTHELPER_INIT_ERROR;
            return null;
        }
        try {
            rh.initProgressSupport(this);
            result = rh.extractFromArchive(
                    srcArchive,
                    destDirectory,
                    password,
                    filenames);
        }
        catch (IOException e) {
            e.printStackTrace();
            result = defaultErrorResult; // maybe better extract error
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        MainActivity activity = MainActivity.mainActivity;
        if (result == null) {
            if (activity == null) return; // activity closed while service active, nothing to refresh
            BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (cd.equals(currentDir))
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem());
            Toast.makeText(service.getApplicationContext(), "Extract completed", Toast.LENGTH_LONG).show();
        }
        else if (result == FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD) {
            Toast.makeText(service.getApplicationContext(),"Empty or wrong password",Toast.LENGTH_LONG).show();
            if (activity == null) return; // activity closed while service active, nothing to refresh
            AskPasswordDialogOnExtract askPasswordDialogOnExtract = new AskPasswordDialogOnExtract(
                    MainActivity.mainActivity,(ExtractParams)params);
            askPasswordDialogOnExtract.show();
        }
        else if (result == FileOpsErrorCodes.CRC_FAILED) {
            Toast.makeText(service.getApplicationContext(),"CRC failed in data, wrong password provided for extraction?",Toast.LENGTH_LONG).show();
        }
        else {
            if (status != ServiceStatus.CANCELLED)
                Toast.makeText(service.getApplicationContext(),"Extract error: "+result.getValue(),Toast.LENGTH_LONG).show();
            else
                Toast.makeText(service,"Extract cancelled",Toast.LENGTH_LONG).show();
        }
    }
}

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
import it.pgp.xfiles.service.params.TestParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.service.visualization.MovingRibbonTwoBars;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/06/17
 */

public class ExtractTask extends RootHelperClientTask {

//    public static final SocketNames extractSocketName = SocketNames.theextractor;
//    public static final SocketNames extractSocketName = SocketNames.theroothelper;

    // direct input to extractArchive
    private List<BasePathContent> srcArchives; // subDir taken from here
    private BasePathContent destDirectory;
    private String password;
    private List<String> filenames;
    private boolean smartDirectoryCreation;

    private static final FileOpsErrorCodes defaultErrorResult = FileOpsErrorCodes.TRANSFER_ERROR;
    private BasePathContent currentDir;

    public String prefix;

    ExtractTask(Serializable params_) {
        super(params_);
        ExtractParams params = (ExtractParams) params_;
        srcArchives = params.srcArchives;
        destDirectory = params.destDirectory;
        password = params.password;
        filenames = params.filenames;
        smartDirectoryCreation = params.smartDirectoryCreation;
        prefix = (params_ instanceof TestParams)?"Test":"Extract";
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = srcArchives.size()==1 ? new MovingRibbon(service, wm) : new MovingRibbonTwoBars(service, wm);
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
                    srcArchives,
                    destDirectory,
                    password,
                    filenames,
                    smartDirectoryCreation);
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
            if (cd.equals(currentDir) && !(params instanceof TestParams))
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),null);
            Toast.makeText(service.getApplicationContext(), prefix+" completed", Toast.LENGTH_LONG).show();
        }
        else if (result == FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD) {
            Toast.makeText(service.getApplicationContext(),"Empty or wrong password",Toast.LENGTH_LONG).show();
            if (activity == null) return; // activity closed while service active, nothing to refresh
            new AskPasswordDialogOnExtract(MainActivity.mainActivity,(ExtractParams)params).show();
        }
        else if (result == FileOpsErrorCodes.CRC_FAILED) {
            Toast.makeText(service.getApplicationContext(),"CRC failed in data, wrong password provided for extraction?",Toast.LENGTH_LONG).show();
        }
        else {
            if (status != ServiceStatus.CANCELLED)
                Toast.makeText(service.getApplicationContext(),prefix+" error: "+result.getValue(),Toast.LENGTH_LONG).show();
            else
                Toast.makeText(service,prefix+" cancelled",Toast.LENGTH_LONG).show();
        }
    }
}

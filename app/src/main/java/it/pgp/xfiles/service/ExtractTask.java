package it.pgp.xfiles.service;

import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.compress.AskPasswordDialogOnExtract;
import it.pgp.xfiles.dialogs.compress.ExtractResultsDialog;
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

    // direct input to extractArchive
    private List<BasePathContent> srcArchives; // subDir taken from here
    private BasePathContent destDirectory;
    private String password;
    private List<String> filenames;
    private boolean smartDirectoryCreation;

    private static final FileOpsErrorCodes defaultErrorResult = FileOpsErrorCodes.TRANSFER_ERROR;
    private BasePathContent currentDir;

    public String prefix;

    public List<FileOpsErrorCodes> results;
    public boolean allOk;

    public boolean allResultsOk() {
        boolean ok = true;
        for(FileOpsErrorCodes code : results)
            ok &= (code == null || code == FileOpsErrorCodes.OK);
        return ok;
    }

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
            results = rh.extractFromArchive(
                    srcArchives,
                    destDirectory,
                    password,
                    filenames,
                    smartDirectoryCreation);
            allOk = allResultsOk();
            if(!allOk) result = results.get(0); // propagate first error for toast message
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
        if (allOk) {
            Toast.makeText(service.getApplicationContext(), prefix+" completed", Toast.LENGTH_LONG).show();
        }
        else if (results.size()==1) {
            if (result == FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD) {
                Toast.makeText(service.getApplicationContext(),"Empty or wrong password",Toast.LENGTH_LONG).show();
                if (activity != null)
                    new AskPasswordDialogOnExtract(activity,(ExtractParams)params).show();
                return;
            }
            else if (result == FileOpsErrorCodes.CRC_FAILED) {
                Toast.makeText(service.getApplicationContext(),"CRC failed in data, damaged archive?",Toast.LENGTH_LONG).show();
            }
            else {
                if (status != ServiceStatus.CANCELLED)
                    Toast.makeText(service.getApplicationContext(),prefix+" error: "+result.getValue(),Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(service,prefix+" cancelled",Toast.LENGTH_LONG).show();
            }
        }
        else { // there were errors when extracting from multiple archives, show results dialog
            if(activity != null) new ExtractResultsDialog(activity, srcArchives, results, params instanceof TestParams).show();
            else Toast.makeText(activity, "There were extraction/test errors, unable to display them without an active activity", Toast.LENGTH_SHORT).show();
        }

        // anyway, if we are not testing archives, refresh adapter if we are in the same folder
        if(activity != null && !(params instanceof TestParams)) {
            BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (cd.equals(currentDir))
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),null);
        }
    }
}

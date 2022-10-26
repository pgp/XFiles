package it.pgp.xfiles.service;

import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.CreateFileParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/06/17
 */

public class CreateFileTask extends RootHelperClientTask {

    // direct input to compressArchive
    public CreateFileParams params;

    private static final FileOpsErrorCodes defaultErrorResult = FileOpsErrorCodes.TRANSFER_ERROR;

    private BasePathContent currentDir;

    CreateFileTask(Serializable params_) {
        super(params_);
        this.params = (CreateFileParams) params_;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if(!super.init(service)) return false;

        mr = new MovingRibbon(service);
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
    }

    @Override
    protected Object doInBackground(Object[] unused) {
        if(rh == null) {
            result = FileOpsErrorCodes.ROOTHELPER_INIT_ERROR;
            return null;
        }
        try {
            rh.initProgressSupport(this);
            rh.createFileOrDirectory(params.path, FileMode.FILE, params.opts);
        }
        catch(IOException e) {
            e.printStackTrace();
            result = defaultErrorResult;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        try {
            if(result == null) {
                // refresh dir only if it's the same of when the task started
                Toast.makeText(service,"File created",Toast.LENGTH_LONG).show();
                MainActivity activity = MainActivity.mainActivity;
                BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
                if(cd.equals(currentDir))
                    activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),params.path.getName());
            }
            else {
                // show error message only if task was not interrupted by user
                if(status != ServiceStatus.CANCELLED)
                    Toast.makeText(service,"File creation error: "+result.getValue(),Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(service,"File creation cancelled",Toast.LENGTH_LONG).show();
            }
        }
        finally {
            super.onPostExecute(o);
        }
    }
}

package it.pgp.xfiles.service;

import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.CopyMoveParams;
import it.pgp.xfiles.service.visualization.MovingRibbonTwoBars;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 23/06/17
 * Using roothelper only
 */

public class CopyMoveTask extends RootHelperClientTask {

    CopyMoveParams params;
    private BasePathContent currentDir; // for refreshing dir listview (if not changed meanwhile) on operation end


    CopyMoveTask(Serializable params) {
        super(params);
        this.params = (CopyMoveParams) params;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = new MovingRibbonTwoBars(service,windowManager); // TODO try catch RuntimeException
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
    }

    @Override
    protected Object doInBackground(Object[] unusedParams) {
        if (rh == null) {
            result = FileOpsErrorCodes.ROOTHELPER_INIT_ERROR;
            return null;
        }
        try {
            rh.initProgressSupport(this);
            rh.copyMoveFilesToDirectory(this.params.list,this.params.destPath);

            // reset progress, in case this object has to be used again after file transfer end
//            rh.destroyProgressSupport();
        } catch (IOException e) {
            e.printStackTrace();
            result = FileOpsErrorCodes.TRANSFER_ERROR;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        // not needed anymore, rh server is multithreaded
//        if (rh != null) {
//            try {rh.killServer();}
//            catch (IOException ignored) {}
//        }

        if (result == null) {
            // refresh dir only if it's the same of when the task started
            Toast.makeText(service,params.list.copyOrMove.name().toLowerCase()+" completed",Toast.LENGTH_LONG).show();
            MainActivity activity = MainActivity.mainActivity;
            if (activity == null) return; // activity closed while service active, nothing to refresh
            BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (cd.equals(currentDir))
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),params.list.files.get(0).getFilename());
        }
        else {
            String errType = params.list.copyOrMove.name().toLowerCase();
            // show error message only if task was not interrupted by user
            if (status != ServiceStatus.CANCELLED)
                Toast.makeText(service,errType+" error: "+result.getValue(),Toast.LENGTH_LONG).show();
            else
                Toast.makeText(service,errType+" cancelled",Toast.LENGTH_LONG).show();
        }
    }
}

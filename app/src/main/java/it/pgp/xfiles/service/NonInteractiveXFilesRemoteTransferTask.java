package it.pgp.xfiles.service;

import android.util.Log;
import android.widget.Toast;

import java.io.Serializable;

import it.pgp.xfiles.CopyListUris;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.XREDirectShareActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.roothelperclient.RemoteManager;
import it.pgp.xfiles.service.params.CopyMoveParams;
import it.pgp.xfiles.service.visualization.MovingRibbonTwoBars;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;

/**
 * Created by pgp on 07/10/17
 */

public class NonInteractiveXFilesRemoteTransferTask extends RootHelperClientTask {
    /*
     * determine operation type (DOWNLOAD or UPLOAD) by type inspection of CopyMoveParams:
     * - DOWNLOAD:
     *      CopyMoveParams.CopyMoveListPathContent.BasePathContent (parentDir) -> XFilesRemotePathContent
     *      CopyMoveParams.BasePathContent -> LocalPathContent
     * - UPLOAD:
     *      CopyMoveParams.CopyMoveListPathContent.BasePathContent (parentDir) -> LocalPathContent
     *      CopyMoveParams.BasePathContent -> XFilesRemotePathContent
     */
    public CopyMoveParams params;
    public ControlCodes action;

    private BasePathContent currentDir; // for refreshing dir listview (if not changed meanwhile) on operation end

    NonInteractiveXFilesRemoteTransferTask(Serializable params_) {
        super(params_);
        params = (CopyMoveParams) params_;

        try {
            if (params.list.parentDir.providerType == ProviderType.XFILES_REMOTE &&
                    params.destPath.providerType == ProviderType.LOCAL)
                action = ControlCodes.ACTION_DOWNLOAD;
            else if (params.list.parentDir.providerType == ProviderType.LOCAL &&
                    params.destPath.providerType == ProviderType.XFILES_REMOTE)
                action = ControlCodes.ACTION_UPLOAD;
            else throw new RuntimeException("Unexpected CopyMoveParams content");
        }
        catch (NullPointerException n) {
            if (!(params.list instanceof CopyListUris)) throw n;
            Log.d("XRETASK", "params contain content uris, defaulting action to upload...");
            action = ControlCodes.ACTION_UPLOAD;
        }
    }

    /**
     * Partially working
     * TODO close also local RH streams
     */
    @Override
    public void cancelTask() {
//        super.cancelTask();
        status = ServiceStatus.CANCELLED;
        Log.d("XRETASK","Interrupted, closing XRE client connection...");
        try {
            XFilesRemotePathContent xrePath = (params.destPath instanceof XFilesRemotePathContent)?
                    (XFilesRemotePathContent) params.destPath :
                    (XFilesRemotePathContent) params.list.parentDir;
            RemoteManager client = MainActivity.rootHelperRemoteClientManager.getClient(
                    xrePath.serverHost, false);
            client.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = new MovingRibbonTwoBars(service,windowManager);
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        resolver = service.getApplicationContext().getContentResolver();
        try {
            currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
        }
        catch (Exception e) {
            // standalone activity, no path view to update after end of task
        }
    }

    @Override
    protected Object doInBackground(Object[] unusedParams) {
        result = MainActivity.rootHelperRemoteClientManager.transferItems(this.params.list,this.params.destPath,action,this, resolver);
        return result;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        MainActivity activity = MainActivity.mainActivity;
        if(activity == null) {
            Toast.makeText(service,
                    (result == null || result == FileOpsErrorCodes.OK)?
                            "Remote transfer completed in standalone mode":
                            params.list.copyOrMove.name().toLowerCase()+" error in standalone mode: "+result.getValue()
                    , Toast.LENGTH_SHORT).show();
            MainActivity.rootHelperRemoteClientManager.closeAllSessions();
            // workaround in order to avoid showing again XREDirectShareActivity after opening and closing the app again after transfer end
//            if(params.list instanceof CopyListUris)
            if(XREDirectShareActivity.instance != null)
                XREDirectShareActivity.instance.finishAffinity();
            return;
        }


        if (result == null || result == FileOpsErrorCodes.OK) {
            // refresh dir only if it's the same of when the task started
            Toast.makeText(service,"Remote transfer completed",Toast.LENGTH_LONG).show();
            BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (cd.equals(currentDir))
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),params.getFirstFilename(resolver));
        }
        else {
            Toast.makeText(service,params.list.copyOrMove.name().toLowerCase()+" error: "+result.getValue(),Toast.LENGTH_LONG).show();
        }
    }
}

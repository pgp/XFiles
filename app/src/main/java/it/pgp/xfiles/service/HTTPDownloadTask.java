package it.pgp.xfiles.service;

import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/11/17
 */

public class HTTPDownloadTask extends RootHelperClientTask {

    DownloadParams params;
    private BasePathContent currentDir;

    final String[] targetFileNameOnly = new String[]{null};

    HTTPDownloadTask(Serializable params) {
        super(params);
        this.params = (DownloadParams) params;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = new MovingRibbon(service);
        ((MovingRibbon)mr).pb.setIndeterminate(true); // keep into account the case where server doesn't publish content size
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
    }

    @Override
    protected void onProgressUpdate(Pair<Long,Long>... values) {
        ((MovingRibbon)mr).pb.setIndeterminate(false);
        super.onProgressUpdate(values);
    }

    @Override
    protected Object doInBackground(Object... unusedParams) {
        rh.initProgressSupport(this);
        // use rh http client for both http and https urls
        if(params.destPath == null || params.destPath.isEmpty()) { // user did not specify a destination path
            if (MainActivity.mainActivity != null) {
                BasePathContent bpc = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
                if (bpc.providerType == ProviderType.LOCAL)
                    params.destPath = bpc.dir;
                else params.destPath = Misc.internalStorageDir.getAbsolutePath();
            }
        }
        targetFileNameOnly[0] = params.filename==null?"":params.filename;
        try {
            rh.downloadHttpsUrl(params.url,443,params.destPath,targetFileNameOnly, params.httpsOnly); // here pass String array, content will be replaced with guessed or same input filename
        }
        catch (IOException e) {
            e.printStackTrace();
            result = FileOpsErrorCodes.TRANSFER_ERROR;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        if (result == null) {
            Toast.makeText(service, "Download completed", Toast.LENGTH_SHORT).show();
            MainActivity activity = MainActivity.mainActivity;
            if (activity == null) return; // activity closed while service active, nothing to refresh
            BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (cd.equals(currentDir))
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),targetFileNameOnly[0]);
        }
        else {
            Toast.makeText(service, "Download "+
                    (status == ServiceStatus.CANCELLED ? "cancelled" : "error"), Toast.LENGTH_SHORT).show();
        }
    }
}

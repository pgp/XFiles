package it.pgp.xfiles.service;

import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/11/17
 */

public class HTTPDownloadTask extends RootHelperClientTask {

    DownloadParams params;
    private BasePathContent currentDir;

    String targetFileName;
    final String[] targetFileNameOnly = new String[]{null};

    HTTPDownloadTask(Serializable params) {
        super(params);
        this.params = (DownloadParams) params;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = new MovingRibbon(service,windowManager);
        ((MovingRibbon)mr).pb.setIndeterminate(true); // keep into account the case where server doesn't publish content size
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        ((MovingRibbon)mr).pb.setIndeterminate(false);
        mr.setProgress(values);
        mBuilder.setProgress(100, values[0], false);
        notificationManager.notify(service.getForegroundServiceNotificationId(),
                mBuilder.build());
        super.onProgressUpdate(values);
    }

    @Override
    protected Object doInBackground(Object... unusedParams) {
        HttpURLConnection connection = null;

        rh.initProgressSupport(this);

        if(params.url.startsWith("http://")) {
            try {
                URL url = new URL(params.url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                // TODO also handle 301 and 302 (moved temporarily or permanently)
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    // return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                    result = FileOpsErrorCodes.TRANSFER_ERROR;
                    return result;
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();


                if (params.filename != null && !params.filename.equals("")) {
                    targetFileName = params.filename;
                }
                else {
                    targetFileName = URLUtil.guessFileName(url.toString(),connection.getHeaderField("Content-Disposition"),null);
                    if(targetFileName==null) targetFileName = "file.bin";
//                  targetFileName = getRemoteFilename(connection);
                }

                targetFileNameOnly[0] = targetFileName;

                if (params.destPath != null && !params.destPath.equals("")) {
                    targetFileName = params.destPath.concat(targetFileName);
                }
                else { // user did not specify a destination path
                    if (MainActivity.mainActivity != null) {
                        BasePathContent bpc = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
                        if (bpc.providerType == ProviderType.LOCAL)
                            targetFileName = bpc.concat(targetFileName).toString();
                        else targetFileName = Misc.internalStorageDir.getAbsolutePath()+"/"+targetFileName;
                    }
                    else { // app not started, service/task started by intent
                        targetFileName = Misc.internalStorageDir.getAbsolutePath()+"/"+targetFileName;
                    }
                }
                // download the file
                try (InputStream input = connection.getInputStream()) {
                    rh.downloadUrl(input,targetFileName,fileLength);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                result = FileOpsErrorCodes.TRANSFER_ERROR;
            }
            finally {
                if (connection != null) connection.disconnect();
            }
        }
        else { // https, rh's Botan back-end
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
                rh.downloadHttpsUrl(params.url,443,params.destPath,targetFileNameOnly); // here pass String array, content will be replaced with guessed or same input filename
            }
            catch (IOException e) {
                e.printStackTrace();
                result = FileOpsErrorCodes.TRANSFER_ERROR;
            }
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

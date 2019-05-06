package it.pgp.xfiles.service;

import android.os.Environment;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 05/11/17
 */

public class HTTPDownloadTask extends RootHelperClientTask {

    DownloadParams params;
    private BasePathContent currentDir;

    String targetFileName,targetFileNameOnly;

    HTTPDownloadTask(Serializable params) {
        super(params);
        this.params = (DownloadParams) params;
    }

    private String getRemoteFilename(HttpURLConnection connection) {
        String filename = null;
        String raw = connection.getHeaderField("Content-Disposition");
        if (raw != null && raw.contains("="))
            filename = raw.split("=")[1];
        if (filename == null) {
            String[] chunks = params.url.split("/");
            if (chunks.length != 0)
                filename = chunks[chunks.length-1]!=null?chunks[chunks.length-1]:"file.bin";
        }
        return filename;
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
                    targetFileName = getRemoteFilename(connection);
                }

                targetFileNameOnly = targetFileName;

                if (params.destPath != null && !params.destPath.equals("")) {
                    targetFileName = params.destPath.concat(targetFileName);
                }
                else { // user did not specify a destination path
                    if (MainActivity.mainActivity != null) {
                        BasePathContent bpc = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
                        if (bpc.providerType == ProviderType.LOCAL)
                            targetFileName = bpc.concat(targetFileName).toString();
                        else targetFileName = "/sdcard/"+targetFileName;
                    }
                    else { // app not started, service/task started by intent
                        targetFileName = "/sdcard/"+targetFileName;
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
                    else params.destPath = "/sdcard";
                }
            }
            targetFileNameOnly = params.filename==null?"":params.filename;
            try {
                rh.downloadHttpsUrl(params.url,443,params.destPath,targetFileNameOnly);
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
                activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),targetFileNameOnly);
        }
        else {
            Toast.makeText(service, "Download error", Toast.LENGTH_SHORT).show();
        }
    }
}

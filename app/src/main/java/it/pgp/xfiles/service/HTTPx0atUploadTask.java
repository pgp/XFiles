package it.pgp.xfiles.service;

import android.app.AlertDialog;
import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.service.visualization.ViewType;
import it.pgp.xfiles.utils.Pair;

public class HTTPx0atUploadTask extends RootHelperClientTask {

    DownloadParams params; // actually upload params, only destPath used as full path (destPath is actually a source path)
    String generatedLink;
    Exception lastException;
    public static final String prefix = "Upload ";

    HTTPx0atUploadTask(Serializable params) {
        super(params);
        this.params = (DownloadParams) params;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;
        mr = new MovingRibbon(service);
        return true;
    }

    @Override
    protected void onProgressUpdate(Pair<Long,Long>... values) {
        ((MovingRibbon)mr).pb.setIndeterminate(false);
        super.onProgressUpdate(values);
    }

    @Override
    protected Object doInBackground(Object... unusedParams) {
        rh.initProgressSupport(this);

        try {
            generatedLink = rh.uploadx0atHttpsUrl(params.destPath).trim();
        }
        catch (IOException e) {
            e.printStackTrace();
            lastException = e;
            result = FileOpsErrorCodes.TRANSFER_ERROR;
        }

        return result;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        if (result == null) {
            Toast.makeText(service, prefix+"completed", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.mainActivity);
            bld.setTitle("Download link:\n"+generatedLink);
            bld.setNeutralButton(android.R.string.ok, null);
            AlertDialog alertDialog = bld.create();
            alertDialog.getWindow().setType(ViewType.OVERLAY_WINDOW_TYPE);
            alertDialog.show();
        }
        else {
            Toast.makeText(service, prefix+
                    (status == ServiceStatus.CANCELLED ? "cancelled" :
                            "error, reason: "+(lastException == null ? "" : lastException.getMessage())),
                    Toast.LENGTH_SHORT).show();
        }
    }
}

package it.pgp.xfiles.service;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.Pair;

public class HTTPUploadTask extends RootHelperClientTask {

    DownloadParams params; // actually upload params, only destPath used as full path (destPath is actually a source path)
    String generatedLink;
    Exception lastException;
    public static final String prefix = "Upload ";

    HTTPUploadTask(Serializable params) {
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
            generatedLink = rh.uploadHttpsUrl(params.url,params.destPath);
            // extract only link (needed for x0.at, which sends additional information as well)
            Matcher matcher = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", Pattern.MULTILINE).matcher(generatedLink);
            if(matcher.find()) {
                generatedLink = matcher.group(); // group 0, a.k.a. entire occurrence
                // take first url found
            }
            // on no matches, return the text as-is
        }
        catch(Exception e) {
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
            String label = "Download link";
            Toast.makeText(service, prefix+"completed", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.mainActivity);
            TextView title = new TextView(MainActivity.mainActivity);
            title.setText(label+":\n"+generatedLink);
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            title.setTypeface(Typeface.MONOSPACE);
            bld.setCustomTitle(title);
            bld.setNegativeButton("Copy to clipboard", (d,w) -> {
                ClipboardManager clipboard = (ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(label,generatedLink));
                Toast.makeText(service, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
            });
            bld.setPositiveButton("Share", (d,w) -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, label);
                intent.putExtra(Intent.EXTRA_TEXT, generatedLink);
                Intent wi = Intent.createChooser(intent,"Share link using");
                wi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                service.startActivity(wi);
            });
            bld.setNeutralButton(android.R.string.ok, null);
            AlertDialog alertDialog = bld.create();
            // prevent dismiss on unintentional touches once the download link has been generated
            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
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

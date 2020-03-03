package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.HTTPDownloadService;
import it.pgp.xfiles.service.params.DownloadParams;

public class UpdateCheckDialog extends Dialog {

    private static class JsonParseDuringCompareException extends RuntimeException {
        public JsonParseDuringCompareException(Exception e) {
            super(e);
        }
        public JsonParseDuringCompareException(String msg) {
            super(msg);
        }
    }

    final Activity activity;

    final ISO8601DateFormat df = new ISO8601DateFormat();

    String currentVersionTagname,latestVersionTagName,latestVersionDownloadUrl;

    Date currentVersionCreatedAt,latestVersionCreatedAt;

    TextView updateMessage;
    TextView currentVersion, latestVersion;
    Button downloadButton,cancelButton;

    List<Map> releases;

    private void compareReleases(final Activity activity) throws ParseException {
        if(releases.isEmpty())
            throw new JsonParseDuringCompareException("Empty releases list");

        Collections.sort(releases,(o1,o2) -> {
            try {
                Date d1 = df.parse((String) o1.get("created_at"));
                Date d2 = df.parse((String) o2.get("created_at"));
                return -(d1.compareTo(d2)); // sort in reversed order
            }
            catch(Exception e) {
                throw new JsonParseDuringCompareException(e);
            }
        });

        Map<String,Integer> tagnames = new HashMap<>();
        int cnt=0;
        for(Map<String,String> rel : releases) // actually not Map<String,String>, variable types, but we are interested only in tag_name, so no ClassCastException if the format is the expected one
            tagnames.put(rel.get("tag_name"),cnt++);
        Integer currentVersionIdx = tagnames.get(currentVersionTagname);
        if(currentVersionIdx == null) // currently installed version not found in GH releases, assume very old
            currentVersionCreatedAt = new Date(0);
        else currentVersionCreatedAt = df.parse((String) releases.get(currentVersionIdx).get("created_at"));

        latestVersionTagName = (String) releases.get(0).get("tag_name");
        latestVersionCreatedAt = df.parse((String) releases.get(0).get("created_at"));
        latestVersionDownloadUrl = (String)((Map)((List)releases.get(0).get("assets")).get(0)).get("browser_download_url");
        activity.runOnUiThread(()->{
            updateMessage.setText((currentVersionCreatedAt.compareTo(latestVersionCreatedAt)>=0)?"Already at the latest version":"Update avilable");
            currentVersion.setText(currentVersionTagname);
            latestVersion.setText(latestVersionTagName);
        });
    }

    public UpdateCheckDialog(@NonNull final Activity activity) {
        super(activity);
        this.activity = activity;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.update_check_dialog);
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            currentVersionTagname = pInfo.versionName+"_"+pInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        currentVersion = findViewById(R.id.updateCheckCurrentVersion);
        latestVersion = findViewById(R.id.updateCheckLatestVersion);
        downloadButton = findViewById(R.id.updateCheckOkButton);
        downloadButton.setOnClickListener(this::ok);
        cancelButton = findViewById(R.id.updateCheckCancelButton);
        cancelButton.setOnClickListener(v->dismiss());
        updateMessage = findViewById(R.id.updateCheckMessage);
        updateMessage.setText("Checking for updates...");
        new Thread(()->{
            try {
                byte[] x = new RootHelperClientUsingPathContent().downloadHttpsUrlInMemory("api.github.com/repos/pgp/XFiles/releases",443);
                Log.d(UpdateCheckDialog.class.getName(),new String(x));
                releases = new ObjectMapper().readValue(x, List.class);
                compareReleases(activity);
                activity.runOnUiThread(()->{
                    Toast.makeText(activity, "Prefetch completed, check logcat", Toast.LENGTH_SHORT).show();
                    downloadButton.setEnabled(true);
                });
            }
            catch(JsonParseDuringCompareException e) {
                e.printStackTrace();
                Toast.makeText(activity, "Json parse error during release sorting, check logcat", Toast.LENGTH_SHORT).show();
            }
            catch(JsonParseException | JsonMappingException e) {
                e.printStackTrace();
                Toast.makeText(activity, "Json parse error after downloading releases file, check logcat", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(activity, "Prefetch error, check logcat", Toast.LENGTH_SHORT).show();
            }
            catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(activity, "Date parse error, check logcat", Toast.LENGTH_SHORT).show();
            }

        }).start();
    }

    private void startDownloadOfLatestRelease() {
        Intent relDownloadIntent = new Intent(activity, HTTPDownloadService.class);
        relDownloadIntent.setAction(BaseBackgroundService.START_ACTION);
        relDownloadIntent.putExtra("params",new DownloadParams(
                latestVersionDownloadUrl,
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                ""));
        activity.startService(relDownloadIntent);
        dismiss();
    }

    public void ok(View unused) {
        /**
         * 1) download latest release zip from GH assets, into /sdcard
         * 2) on complete, extract zip into same folder
         * 3) on extract complete, show popup "Install now?"
         */
        Toast.makeText(activity, "Download url: "+latestVersionDownloadUrl, Toast.LENGTH_LONG).show();
        startDownloadOfLatestRelease();
        // TODO 2) and 3)
    }
}

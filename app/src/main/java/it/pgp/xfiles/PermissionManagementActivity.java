package it.pgp.xfiles;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

public class PermissionManagementActivity extends Activity {

    public enum PermReqCodes { STORAGE, SYSTEM_SETTINGS, OVERLAYS, STORAGE_READ /*, EXTERNAL_SD*/, INSTALL_UNKNOWN_APPS, NOTIFS13 }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermReqCodes.STORAGE.ordinal()) {
            if (grantResults.length == 0) { // request cancelled
                Toast.makeText(this, R.string.storage_perm_denied, Toast.LENGTH_SHORT).show();
                return;
            }

            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.storage_perm_denied, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Toast.makeText(this, R.string.storage_perm_granted, Toast.LENGTH_SHORT).show();

            requestStorageReadPermissions(); // Oreo and above needs this!
        }
        else if (requestCode == PermReqCodes.STORAGE_READ.ordinal()) {
            if (grantResults.length == 0) { // request cancelled
                Toast.makeText(this, R.string.storage_read_perm_denied, Toast.LENGTH_SHORT).show();
                return;
            }

            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.storage_read_perm_denied, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Toast.makeText(this, R.string.storage_read_perm_granted, Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PermReqCodes prc = PermReqCodes.values()[requestCode];
        switch (prc) {
            case STORAGE:
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                    Toast.makeText(this, "Nothing to do here, already handled in onRequestPermissionsResult", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "(Android >= 11) All-files permission "+
                            (Environment.isExternalStorageManager()?"granted":"denied"), Toast.LENGTH_SHORT).show();
                break;
            case SYSTEM_SETTINGS:
                Toast.makeText(this, "System settings permission "+
                        (Settings.System.canWrite(this)?"granted":"denied"), Toast.LENGTH_SHORT).show();
                break;
            case OVERLAYS:
                Toast.makeText(this, "Overlay permission "+
                        (Settings.canDrawOverlays(this)?"granted":"denied"), Toast.LENGTH_SHORT).show();
                break;
            case INSTALL_UNKNOWN_APPS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Toast.makeText(this, "Install unknown apps permission "+
                            (getPackageManager().canRequestPackageInstalls()?"granted":"denied"), Toast.LENGTH_SHORT).show();
                }
                break;
            case NOTIFS13:
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Toast.makeText(this, "Notification permission "+(nm.areNotificationsEnabled()?"granted":"denied"), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void requestStoragePermissions(View unused) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PermReqCodes.STORAGE.ordinal());
        else {
            // with Android >= 11, by having this signature permission granted by user, we can access all files (both read and write, even external sd and usb drives)
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PermReqCodes.STORAGE.ordinal());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestNotifs13Permissions(View view) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
        startActivityForResult(intent, PermReqCodes.NOTIFS13.ordinal());
    }

    // for Oreo, that absurdly needs READ external storage permission request AFTER WRITE one has already been granted (and the latter in this case is automatically granted!)
    protected void requestStorageReadPermissions() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PermReqCodes.STORAGE_READ.ordinal());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openSystemSettingsPermissionsManagement(View unused) {
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())), PermReqCodes.SYSTEM_SETTINGS.ordinal());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openOverlayPermissionsManagement(View unused) {
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), PermReqCodes.OVERLAYS.ordinal());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void openInstallUnknownAppsPermissionsManagement(View unused) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())), PermReqCodes.INSTALL_UNKNOWN_APPS.ordinal());
        else
            Toast.makeText(this, "Request not needed on Android < Oreo", Toast.LENGTH_LONG).show();
    }

    public void completePermissions(View unused) {
        Intent i = new Intent(this,MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Permission management");
        setContentView(R.layout.activity_permission_management);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            findViewById(R.id.notifs13Permissions).setVisibility(View.VISIBLE);
            findViewById(R.id.notifs13PermissionsExplain).setVisibility(View.VISIBLE);
        }
    }
}

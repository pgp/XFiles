package it.pgp.xfiles;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

public class PermissionManagementActivity extends Activity {

    public enum PermReqCodes { STORAGE, SYSTEM_SETTINGS, OVERLAYS, STORAGE_READ /*, EXTERNAL_SD*/, INSTALL_UNKNOWN_APPS }

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
                Toast.makeText(this, "Nothing to do here, already handled in onRequestPermissionsResult", Toast.LENGTH_SHORT).show();
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

//            case EXTERNAL_SD:
//                if (resultCode != RESULT_OK) {
//                    Toast.makeText(this, "External SD write permission denied", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                else {
//                    try {
//                        Uri treeUri = data.getData();
//                        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
//                        grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                        getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                        Toast.makeText(this, "Custom path RW permission granted for path "+pickedDir.getUri(), Toast.LENGTH_SHORT).show();
////                        pickedDir.createDirectory("prova123"); // this works
//
//
////                        DocumentFile targetDir = DocumentFile.fromSingleUri(this,Uri.fromFile(new File("/storage/1702-3A0E/")));
////                        targetDir.createDirectory("prova123"); // this doesn't work!
//
//                        SharedPreferences.Editor editor = getSharedPreferences(getPackageName(),MODE_PRIVATE).edit();
//                        editor.putString("EXTSDFILE",treeUri.toString());
//                        editor.apply();
//                    }
//                    catch (Exception e) {
//                        e.printStackTrace();
//                        Toast.makeText(this, "Error during external sd permission request", Toast.LENGTH_SHORT).show();
//                    }
//                }
//                break;
        }
    }

    public void requestStoragePermissions(View unused) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PermReqCodes.STORAGE.ordinal());
    }

    // for Oreo, that absurdly needs READ external storage permission request AFTER WRITE one has already been granted (and the latter in this case is automatically granted!)
    protected void requestStorageReadPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PermReqCodes.STORAGE_READ.ordinal());
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    public void requestExternalSDWritePermissions(View unused) {
//        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), PermReqCodes.EXTERNAL_SD.ordinal());
//    }

//    public void testExternalSDWritePermissions(View unused) {
//        SharedPreferences sp = getSharedPreferences(getPackageName(),MODE_PRIVATE);
//        String savedTreeUri = sp.getString("EXTSDFILE",null);
//        if(savedTreeUri != null) {
//            Uri treeUri = Uri.parse(savedTreeUri);
//            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
//            pickedDir.createDirectory("1abcdef");
//            Toast.makeText(this, "@@@dir created@@@", Toast.LENGTH_SHORT).show();
//        }
//        else {
//            Toast.makeText(this, "No prior ext sdcard perm grant", Toast.LENGTH_SHORT).show();
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openSystemSettingsPermissionsManagement(View unused) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),PermReqCodes.SYSTEM_SETTINGS.ordinal());
        else
            Toast.makeText(this, "Hotspot toggle feature is not available on Android >= Oreo, so this permission is not needed", Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openOverlayPermissionsManagement(View unused) {
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),PermReqCodes.OVERLAYS.ordinal());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void openInstallUnknownAppsPermissionsManagement(View unused) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),PermReqCodes.INSTALL_UNKNOWN_APPS.ordinal());
        else
            Toast.makeText(this, "Request not needed on Android < Oreo", Toast.LENGTH_LONG).show();
    }

    public void completePermissions(View unused) {
        SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
        editor.putBoolean("1stRun", false);
        editor.apply();

        Intent i = new Intent(this,MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("First run configuration");
        setContentView(R.layout.activity_permission_management);
    }
}

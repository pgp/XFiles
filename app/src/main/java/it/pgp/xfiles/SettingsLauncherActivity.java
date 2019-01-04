package it.pgp.xfiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

public class SettingsLauncherActivity extends Activity {

    static final int overlay_rq_code = 123;
    static final int write_settings_rq_code = 456;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.empty);

        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),overlay_rq_code);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == overlay_rq_code) {
            if (Settings.canDrawOverlays(this)) {
                // ok, ask for next signature permission
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),write_settings_rq_code);
            }
            else {
                Toast.makeText(this, "Alert permissions must be granted, exiting...", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }
        else if (requestCode == write_settings_rq_code){
            if (Settings.canDrawOverlays(this) && Settings.System.canWrite(this)) {
                Intent i = new Intent(SettingsLauncherActivity.this,MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
            else {
                Toast.makeText(this, "Both signature permissions must be granted, exiting...", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }
        else throw new RuntimeException("Invalid request code returned in SettingsLauncherActivity");
    }
}

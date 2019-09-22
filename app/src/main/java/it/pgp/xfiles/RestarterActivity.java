package it.pgp.xfiles;

/**
 * Created by pgp on 13/07/17
 * Activity in standalone Dalvik process that restarts MainActivity
 * when permissions are granted on Android >= 6
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RestarterActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("RESTART","RESTART");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);

        int targetPid = getIntent().getExtras().getInt("");
        android.os.Process.sendSignal(targetPid,2); // SIGINT

        // start target activity again
        Intent i = new Intent(RestarterActivity.this,MainActivity.class);

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(i);

        // self kill process
//        android.os.Process.sendSignal(android.os.Process.myPid(),2); // SIGINT
    }
}

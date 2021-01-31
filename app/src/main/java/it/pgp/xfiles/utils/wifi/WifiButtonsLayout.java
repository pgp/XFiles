package it.pgp.xfiles.utils.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;

public class WifiButtonsLayout extends LinearLayout {

    private final Activity activity;

    private final ImageButton wifiBtn;
    public final ImageButton apBtn;

    private final BroadcastReceiver wifiReceiver;
    private final WifiApManager ap;
    private final WifiManager wifiManager;
    private final IntentFilter filter;

    private volatile boolean latestApState;

    final LinearLayout.LayoutParams horizontal_equal_weight = new LinearLayout.LayoutParams(
            0,
            LayoutParams.MATCH_PARENT,
            1
    );

    void toggleButtons(boolean on) {
        wifiBtn.setEnabled(on);
        apBtn.setEnabled(on);
    }

    public WifiButtonsLayout(Activity context) {
        super(context);
        activity = context;

        filter = new IntentFilter();
        filter.addAction(ConnectionChangeReceiver.CONN_ACTION);
        filter.addAction(ConnectionChangeReceiver.WIFI_ACTION);
        wifiReceiver = new ConnectionChangeReceiver();
//        ap = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)?
//                new OreoWifiAPManager(context,this):
//                new WifiApManager(context);
        ap = new WifiApManager(context);
        wifiManager = (WifiManager)activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        setOrientation(HORIZONTAL);
        wifiBtn = new ImageButton(context);
        apBtn = new ImageButton(context);
        wifiBtn.setImageResource(R.drawable.xfiles_wifi_off);
        apBtn.setImageResource(R.drawable.xfiles_hotspot_off);
        wifiBtn.setLayoutParams(horizontal_equal_weight);
        apBtn.setLayoutParams(horizontal_equal_weight);
        wifiBtn.setOnLongClickListener(this::showWifiNetworkPicker);
        wifiBtn.setOnClickListener(this::switchWifi);
        apBtn.setOnClickListener(this::switchAp);
        addView(apBtn);
        addView(wifiBtn);
        receiveAp();
    }

    public void registerListeners() {
        activity.registerReceiver(wifiReceiver,filter);
    }

    public void unregisterListeners() {
        activity.unregisterReceiver(wifiReceiver);
    }

    private boolean showWifiNetworkPicker(View unused) {
        activity.startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
        return true;
    }

    private void switchWifi(View unused) {
        toggleButtons(false);
        if(!wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled())) {
            toggleButtons(true);
            MainActivity.showToastOnUIWithHandler("Unable to switch WiFi state, is airplane mode active?");
        }
    }

    private static final int RECHECK_AP_CHANGED_TIMEOUT_SEC = 10;

    private void startWifiAPSystemActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private void switchAp(View unused) {
        boolean expectedStateAfterSwitch = !ap.isApOn();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startWifiAPSystemActivity();
        }
        else {
            toggleButtons(false);
            if (!ap.configApState(expectedStateAfterSwitch)) {
                MainActivity.showToastOnUIWithHandler(
                        "Could not change WiFi AP status directly from app," +
                                "ensure you have granted system settings permissions");
                startWifiAPSystemActivity();
                return;
            }
            // for some reason, ap state change is not detected by broadcast receiver, force query state after a while

            new CountDownAPCheck(activity,RECHECK_AP_CHANGED_TIMEOUT_SEC,expectedStateAfterSwitch).run();
        }
    }

    private void receiveWifi() {
        activity.runOnUiThread(()->wifiBtn.setImageResource(ap.checkWifiOnAndConnected().resId));
    }

    private void receiveAp() {
        latestApState = ap.isWifiApEnabled();
        activity.runOnUiThread(()->apBtn.setImageResource(
                latestApState?R.drawable.xfiles_hotspot_on:R.drawable.xfiles_hotspot_off));
    }

    public class ConnectionChangeReceiver extends BroadcastReceiver {
        public static final String CONN_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
        public static final String WIFI_ACTION = "android.net.wifi.WIFI_STATE_CHANGED";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BROADCAST","onReceive "+intent.getAction());
            receiveWifi();
            receiveAp();
            activity.runOnUiThread(()->toggleButtons(true));
        }
    }

    /**
     * Runnable that forces a wifi ap state update every second without thread sleep
     */
    public class CountDownAPCheck implements Runnable {

        final Context context;
        final int count;
        final boolean expectedState;

        private CountDownAPCheck(Context context, int count, boolean expectedState) {
            this.context = context;
            this.count = count;
            this.expectedState = expectedState;
        }

        @Override
        public void run() {
            receiveAp();

            String errMsg = null;
            if (count <= 0) { // this should crash if initialCount is 0 (first runnable is not submitted on UI thread)
                errMsg = "Timeout AP check";
            }
            else if (latestApState == expectedState) {
                errMsg = "expected AP change detected, periodic AP check ended";
            }

            if(errMsg!=null) {
                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
                toggleButtons(true);
                return;
            }

            MainActivity.handler.postDelayed(new CountDownAPCheck(context,count-1,expectedState),1000);
        }
    }
}

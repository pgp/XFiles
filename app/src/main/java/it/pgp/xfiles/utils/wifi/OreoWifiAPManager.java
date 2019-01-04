package it.pgp.xfiles.utils.wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import it.pgp.xfiles.R;

/**
 * Web source:
 * https://stackoverflow.com/questions/45984345/how-to-turn-on-off-wifi-hotspot-programmatically-in-android-8-0-oreo
 * Unused because unpractical, requires APP_COARSE_LOCATION runtime permission (not requested by XFiles), and, moreover, requires that GPS is turned on in order to work
 */

@RequiresApi(api = Build.VERSION_CODES.O)
public class OreoWifiAPManager extends WifiApManager {

    private static final String LOGTAG = "OREOWIFIAP";

    private static WifiManager.LocalOnlyHotspotReservation mReservation;
    private final WifiButtonsLayout wifiButtonsLayout;

    OreoWifiAPManager(Context context, WifiButtonsLayout wifiButtonsLayout) {
        super(context);
        this.wifiButtonsLayout = wifiButtonsLayout;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOnHotspot() {
        try {
            mWifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    Log.d(LOGTAG, "Wifi Local Hotspot is ON");
                    mReservation = reservation;
                    wifiButtonsLayout.apBtn.setImageResource(R.drawable.xfiles_hotspot_on);
                }

                @Override
                public void onStopped() { // stopped externally from settings UI
                    super.onStopped();
                    Log.d(LOGTAG, "Wifi Local Hotspot is OFF");
                    wifiButtonsLayout.apBtn.setImageResource(R.drawable.xfiles_hotspot_off);
                    turnOffHotspot();
                    mReservation = null;
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    Log.d(LOGTAG, "onFailed: ");
                    wifiButtonsLayout.apBtn.setEnabled(false);
                }
            }, new Handler());

        }
        catch (SecurityException e) { // TODO launch intent for asking runtime permission for coarse location
            Toast.makeText(context, "Location permission not enabled, check app settings", Toast.LENGTH_SHORT).show();
        }
    }

    public void turnOffHotspot() {
        if (mReservation != null) {
            mReservation.close();
            mReservation = null;
        }
    }

    @Override
    public boolean isApOn() {
        return mReservation != null && super.isApOn();
    }

    @Override
    boolean configApState(boolean expectedState) {
        if (expectedState) {
            if (mReservation == null)
                turnOnHotspot();
        }
        else turnOffHotspot();
        return true;
    }
}

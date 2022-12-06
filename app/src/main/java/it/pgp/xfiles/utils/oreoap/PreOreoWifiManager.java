package it.pgp.xfiles.utils.oreoap;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class PreOreoWifiManager {
    final Context context;

    public PreOreoWifiManager(Context context) {
        this.context = context;
    }

    public boolean isApOn() {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        }
        catch (Throwable ignored) {}
        return false;
    }

    // toggle wifi hotspot on or off
    public boolean configApState(boolean expectedState) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {
            // if WiFi is on, turn it off // FIXME isApOn checks ap, not wifi
            if(isApOn()) wifimanager.setWifiEnabled(false);
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
//            method.invoke(wifimanager, wificonfiguration, !isApOn(context));
            method.invoke(wifimanager, wificonfiguration, expectedState);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

package it.pgp.xfiles.utils.oreoap;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.android.dx.stock.ProxyBuilder;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

// Source: https://github.com/aegis1980/WifiHotSpot

// @RequiresApi(api = Build.VERSION_CODES.O)
public class MyOreoWifiManager {
    private static final String TAG = MyOreoWifiManager.class.getSimpleName();

    private Context mContext;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    public MyOreoWifiManager(Context c) {
        mContext = c;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(ConnectivityManager.class);
    }

    /**
     * This sets the Wifi SSID and password
     * Call this before {@code startTethering} if app is a system/privileged app
     * Requires: android.permission.TETHER_PRIVILEGED which is only granted to system apps
     */
    public void configureHotspot(String name, String password) {
        WifiConfiguration apConfig = new WifiConfiguration();
        apConfig.SSID = name;
        apConfig.preSharedKey = password;
        apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        try {
            Method setConfigMethod = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            boolean status = (boolean) setConfigMethod.invoke(mWifiManager, apConfig);
            Log.d(TAG, "setWifiApConfiguration - success? " + status);
        } catch (Exception e) {
            Log.e(TAG, "Error in configureHotspot");
            e.printStackTrace();
        }
    }

    /**
     * Checks where tethering is on.
     * This is determined by the getTetheredIfaces() method,
     * that will return an empty array if not devices are tethered
     *
     * @return true if a tethered device is found, false if not found
     */
    public boolean isTetherActive() {
        try {
            Method method = mConnectivityManager.getClass().getDeclaredMethod("getTetheredIfaces");
            if (method == null) {
                Log.e(TAG, "getTetheredIfaces is null");
            } else {
                String[] res = (String[]) method.invoke(mConnectivityManager, null);
                Log.d(TAG, "getTetheredIfaces invoked");
                Log.d(TAG, Arrays.toString(res));
                if (res.length > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getTetheredIfaces");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This enables tethering using the ssid/password defined in Settings App>Hotspot & tethering
     * Does not require app to have system/privileged access
     * Credit: Vishal Sharma - https://stackoverflow.com/a/52219887
     */
    public boolean startTethering(final MyOnStartTetheringCallback callback) {

        // On Pie if we try to start tethering while it is already on, it will
        // be disabled. This is needed when startTethering() is called programmatically.
        if (isTetherActive()) {
            Log.d(TAG, "Tether already active, returning");
            return false;
        }

        File outputDir = mContext.getCodeCacheDir();
        Object proxy;
        try {
            proxy = ProxyBuilder.forClass(OnStartTetheringCallbackClass())
                    .dexCache(outputDir).handler((proxy1, method, args) -> {
                        switch (method.getName()) {
                            case "onTetheringStarted":
                                callback.onTetheringStarted();
                                break;
                            case "onTetheringFailed":
                                callback.onTetheringFailed();
                                break;
                            default:
                                ProxyBuilder.callSuper(proxy1, method, args);
                        }
                        return null;
                    }).build();
        } catch (Exception e) {
            Log.e(TAG, "Error in enableTethering ProxyBuilder");
            e.printStackTrace();
            return false;
        }

        Method method;
        try {
            method = mConnectivityManager.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, OnStartTetheringCallbackClass(), Handler.class);
            if (method == null) {
                Log.e(TAG, "startTetheringMethod is null");
            } else {
                method.invoke(mConnectivityManager, ConnectivityManager.TYPE_MOBILE, false, proxy, null);
                Log.d(TAG, "startTethering invoked");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in enableTethering");
            e.printStackTrace();
        }
        return false;
    }

    public void stopTethering() {
        try {
            Method method = mConnectivityManager.getClass().getDeclaredMethod("stopTethering", int.class);
            if (method == null) {
                Log.e(TAG, "stopTetheringMethod is null");
            } else {
                method.invoke(mConnectivityManager, ConnectivityManager.TYPE_MOBILE);
                Log.d(TAG, "stopTethering invoked");
            }
        } catch (Exception e) {
            Log.e(TAG, "stopTethering error: " + e.toString());
            e.printStackTrace();
        }
    }

    private Class OnStartTetheringCallbackClass() {
        try {
            return Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "OnStartTetheringCallbackClass error: " + e.toString());
            e.printStackTrace();
        }
        return null;
    }
}


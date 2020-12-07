package it.pgp.xfiles.utils;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
// import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.adapters.XreAnnouncesAdapter;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;

public class MulticastUtils {

    /**
     * Web source:
     * https://stackoverflow.com/questions/23644997/android-send-udp-broadcast-silently-fails
     */
    public static synchronized void startXreAnnounceListenerThread(Activity activity, XreAnnouncesAdapter xreAnnouncesAdapter) {
        if(xreAnnounceReceiveSocket != null || xreAnnounceMulticastLock != null) {
            MainActivity.showToastOnUI("Announce receiver thread already running, updates could be not visible if the adapter has been recreated meanwhile", activity);
            return;
        }

        WifiManager wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        xreAnnounceMulticastLock = wifi.createMulticastLock("multicastLock");
        xreAnnounceMulticastLock.setReferenceCounted(true);
        xreAnnounceMulticastLock.acquire();

        new Thread(() -> {
            Log.d(xreAnnounceLogTag,"XRE announce receiver thread started");
            try {
                xreAnnounceReceiveSocket = new DatagramSocket(11111);
//                xreAnnounceReceiveSocket = new DatagramSocket(null);
//                xreAnnounceReceiveSocket.setReuseAddress(true);
//                xreAnnounceReceiveSocket.bind(new InetSocketAddress("0.0.0.0",11111));
//                xreAnnounceReceiveSocket.setBroadcast(true);
                for(;;) {
                    DatagramPacket data = new DatagramPacket(new byte[256], 256);
                    xreAnnounceReceiveSocket.receive(data);
                    String received = new String(data.getData(), data.getOffset(), data.getLength(), StandardCharsets.UTF_8);
                    Log.e(xreAnnounceLogTag,received);

                    XFilesRemotePathContent xrpc = fromXREAnnounce(data);
                    if(xrpc != null) activity.runOnUiThread(()-> xreAnnouncesAdapter.add(new Pair<>(xrpc.serverHost,xrpc.dir)));
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            xreAnnounceReceiveSocket = null; // the dismiss listener or the onPause method in XREDirectShareActivity have closed the socket
            Log.d(xreAnnounceLogTag,"XRE announce receiver thread ended");
        }).start();
    }

    public static void shutdownMulticastListening() {
        if(xreAnnounceReceiveSocket != null) {
            xreAnnounceReceiveSocket.close();
            xreAnnounceReceiveSocket = null;
        }
        if(xreAnnounceMulticastLock != null) {
            xreAnnounceMulticastLock.release();
            xreAnnounceMulticastLock = null;
        }
    }

    public static XFilesRemotePathContent fromXREAnnounce(DatagramPacket packet) {
        try {
            byte[] origin = packet.getData();
            int o = packet.getOffset();
            int l = packet.getLength();
            byte[] receivedChecksum = new byte[4];
            byte[] payload = new byte[l-4];
            System.arraycopy(origin,o,receivedChecksum,0,4);
            System.arraycopy(origin,o+4,payload,0,l-4);

            // verify checksum
            CRC32 crc = new CRC32();
            crc.update(payload);
            long computedChecksum = crc.getValue();
            if (computedChecksum != Misc.castBytesToUnsignedNumber(receivedChecksum,4)) {
                Log.e(xreAnnounceLogTag,"Verification failed for XRE announce");
                return null;
            }

            // format: 2 bytes for port, 2 bytes string length + host, 2 bytes string length + path
            byte[] tmp = new byte[2];
            System.arraycopy(payload,0,tmp,0,2);
            int port = (int)Misc.castBytesToUnsignedNumber(tmp,2);
            System.arraycopy(payload,2,tmp,0,2);
            int hostLength = (int)Misc.castBytesToUnsignedNumber(tmp,2);
            String host = new String(payload,4,hostLength, StandardCharsets.UTF_8);
            System.arraycopy(payload,4+hostLength,tmp,0,2);
            int pathLength = (int)Misc.castBytesToUnsignedNumber(tmp,2);
            String path = new String(payload,6+hostLength,pathLength, StandardCharsets.UTF_8);

            // while received in the UDP packet, port is still default (11111) hence ignored
            return new XFilesRemotePathContent(host,path);
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final String xreAnnounceLogTag = "XREANNOUNCE";
    public static DatagramSocket xreAnnounceReceiveSocket;
    public static WifiManager.MulticastLock xreAnnounceMulticastLock;
}

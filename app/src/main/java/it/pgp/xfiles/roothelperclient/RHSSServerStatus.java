package it.pgp.xfiles.roothelperclient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pgp on 23/11/17
 * Bean of static members containing server status (should be in sync with information sent to roothelper client)
 * Singleton 0-1 object, with non-final inner fields
 */

public class RHSSServerStatus {

    public static volatile String xreHomePathStr;
    public static volatile String xreAnnouncedPathStr;
    public static volatile String xreExposedPathStr; // if null or empty, remote clients have access to the entire filesystem
    public static volatile boolean announceEnabled = true;

    // key is String (IPv4:port or [IPv6]:port)
    // byte[] value contains shared TLS key hash for that session
    // cleared on thread exit (rhss exit)
    public static final Map<String,byte[]> StoCSessions = new ConcurrentHashMap<>();

    public static void destroyServer() {
        xreHomePathStr = "";
        xreAnnouncedPathStr = "";
        xreExposedPathStr = "";
        StoCSessions.clear();
    }

    // CtoS connections are contained in the static final variable RemoteClientManager in MainActivity

}

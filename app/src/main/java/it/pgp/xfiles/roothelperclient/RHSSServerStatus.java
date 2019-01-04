package it.pgp.xfiles.roothelperclient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pgp on 23/11/17
 * Bean of static members containing server status (should be in sync with information sent to roothelper client)
 * Singleton 0-1 object, with non-final inner fields
 */

public class RHSSServerStatus {

    // no need to keep active flag here, already have it.pgp.xfiles.roothelperclient.RemoteServerManager.RHSSUpdateThread reference (null if off)
//    public static final AtomicBoolean active = new AtomicBoolean(false);

    public static volatile String currentlyServedLocalPath; // if null, remote clients have access to the entire filesystem

    // key is String (IPv4:port or [IPv6]:port)
    // byte[] value contains shared TLS key for that session
    // cleared on thread exit (rhss exit)
    public static final Map<String,byte[]> StoCSessions = new ConcurrentHashMap<>();

    public static synchronized void createServer(String currentlyServedLocalPath) {
        RHSSServerStatus.currentlyServedLocalPath = currentlyServedLocalPath;
        StoCSessions.clear();
//        active.set(true);
    }

    public static synchronized void destroyServer() {
        currentlyServedLocalPath = "";
        StoCSessions.clear();
//        active.set(false);
    }

    // CtoS connections are contained in the static final variable RemoteClientManager in MainActivity

}

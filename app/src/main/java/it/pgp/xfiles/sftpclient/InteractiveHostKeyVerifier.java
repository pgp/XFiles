package it.pgp.xfiles.sftpclient;

import android.util.Log;

import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.common.KeyType;

import java.io.*;
import java.security.PublicKey;

/**
 * Created by pgp on 03/03/17
 */

public class InteractiveHostKeyVerifier extends OpenSSHKnownHosts {

    public static PublicKey currentHostKey;
    public static Boolean lastHostKeyHasChanged;

    public InteractiveHostKeyVerifier(File khFile) throws IOException {
        super(khFile);
    }

    @Override
    public boolean verify(final String hostname, final int port, final PublicKey key) {
        // reset current host key, if any, and changed state
        currentHostKey = null;
        lastHostKeyHasChanged = null;

        // DEBUG INFO
        Log.d(this.getClass().getName(),key.toString());
        Log.d(this.getClass().getName(),KeyType.fromKey(key).toString());
        Log.d(this.getClass().getName(),key.getAlgorithm());
        Log.d(this.getClass().getName(),key.getFormat());

        //////////////////////////////////////////

        final KeyType type = KeyType.fromKey(key);

        if (type == KeyType.UNKNOWN)
            return false;

        final String adjustedHostname = (port != 22) ? "[" + hostname + "]:" + port : hostname;

        // for (KnownHostEntry e : entries) { // sshj 0.23
        for (KnownHostEntry e : entries) {
            try {
                if (e.appliesTo(type, adjustedHostname)) {
                    if (e.verify(key)) return true;
                    // here, host key has changed
                    lastHostKeyHasChanged = true;
                    currentHostKey = key;
//                    return hostKeyChangedAction(e, adjustedHostname, key); // sshj 0.21.2
                    return hostKeyChangedAction(adjustedHostname, key);
                }
            }
            catch (IOException ioe) {
                log.error("Error with {}: {}", e, ioe);
                return false;
            }
        }

        boolean result = hostKeyUnverifiableAction(adjustedHostname, key);
        if (result) return true;

        //////////////////////////////////////////

        // save current host key in static field and return false
        lastHostKeyHasChanged = false;
        currentHostKey = key;
        return false;
    }
}

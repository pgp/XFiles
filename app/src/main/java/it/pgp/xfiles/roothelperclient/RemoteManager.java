package it.pgp.xfiles.roothelperclient;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.service.SocketNames;
import it.pgp.xfiles.utils.StreamsPair;

/**
 * Created by pgp on 20/09/17
 */

public class RemoteManager extends StreamsPair {
    private static final SocketNames defaultaddress = SocketNames.theroothelper;

    // streams connected to local socket
    public final LocalSocket ls;

    public final byte[] tlsSessionHash = new byte[32]; // hex string of SHA256

    RemoteManager() throws IOException {
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(
                defaultaddress.name(),
                LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.d(getClass().getName(),"Connected");

        ls = clientSocket;

        o = clientSocket.getOutputStream();
        i = new DataInputStream(clientSocket.getInputStream());
        Log.d(getClass().getName(),"Streams acquired");
    }

    @Override
    public void close() {
        // Close method on streams won't work, use shutdown methods
        // Web source:
        // https://stackoverflow.com/questions/10984175/android-localsocket-wont-close-when-in-blocked-read

        if(ls != null) {
            try {ls.shutdownInput();} catch (Exception ignored) {}
            try {ls.shutdownOutput();} catch (Exception ignored) {}
        }

        try {i.close();} catch (Exception ignored) {}
        try {o.close();} catch (Exception ignored) {}
        Log.d(getClass().getName(),"Streams closed");
    }
}

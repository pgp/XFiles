package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 09/12/17
 */

public class ssh_keygen_resp {
    public String privateKey; // private
    public String publicKey; // public

    public ssh_keygen_resp(DataInputStream inputStream) throws IOException {
        // private key
        byte[] b = new byte[4];
        inputStream.readFully(b);
        int len = (int) Misc.castBytesToUnsignedNumber(b,4);
        b = new byte[len];
        inputStream.readFully(b);
        privateKey = new String(b);

        // public key
        b = new byte[4];
        inputStream.readFully(b);
        len = (int) Misc.castBytesToUnsignedNumber(b,4);
        b = new byte[len];
        inputStream.readFully(b);
        publicKey = new String(b);
    }
}

package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 09/12/17
 * Contains a PKCS8 PEM encoded private key and a X509 PEM encoded public key
 */

public class pem_keygen_resp {
    public String pkcs8; // private
    public String x509; // public

    public pem_keygen_resp(DataInputStream inputStream) throws IOException {
        // private key
        byte[] b = new byte[4];
        inputStream.readFully(b);
        int len = (int) Misc.castBytesToUnsignedNumber(b,4);
        b = new byte[len];
        inputStream.readFully(b);
        pkcs8 = new String(b);

        // public key
        b = new byte[4];
        inputStream.readFully(b);
        len = (int) Misc.castBytesToUnsignedNumber(b,4);
        b = new byte[len];
        inputStream.readFully(b);
        x509 = new String(b);
    }
}

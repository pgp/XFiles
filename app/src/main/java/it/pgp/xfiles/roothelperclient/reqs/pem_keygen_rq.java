package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 09/12/17
 * Only RSA key pair generation, for adding other formats (once SSHJ will support them)
 * add flag bits to requestType
 */

public class pem_keygen_rq {
    public ControlCodes requestType = ControlCodes.ACTION_SSH_KEYGEN;
    public final int keySize;

    public pem_keygen_rq(int keySize) {
        this.keySize = keySize;
    }

    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            nbf.write(requestType.getValue());
            nbf.write(Misc.castUnsignedNumberToBytes(keySize,4));
        }
    }
}


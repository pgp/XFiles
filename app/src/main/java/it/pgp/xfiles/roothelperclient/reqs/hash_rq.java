package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 03/02/17
 */

public class hash_rq extends SinglePath_rq {
    HashRequestCodes hashAlgorithm;
    public hash_rq(Object pathname, HashRequestCodes hashAlgorithm) {
        super(pathname);
        this.requestType = ControlCodes.ACTION_HASH;
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // write request byte
            nbf.write(requestType.getValue());
            // write algorithm byte
            nbf.write(hashAlgorithm.getValue());

            // write len and field (digest output length is implicit)
            nbf.write(Misc.castUnsignedNumberToBytes(this.pathname_len,2));
            nbf.write(this.pathname);
        }
    }


}

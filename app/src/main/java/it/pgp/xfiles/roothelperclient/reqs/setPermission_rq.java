package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 22/11/17
 */

public class setPermission_rq extends setAttributes_rq {
    int permissions;

    public setPermission_rq(Object pathname, int permissions) {
        super(pathname);
        this.permissions = permissions;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            additionalByte = SubRequest.SET_PERMISSIONS.ordinal() << setAttributes_rq.bitOffsetForSubrequest;

            // write request byte
            nbf.write(requestType.getValue());

            // write additional byte
            nbf.write(additionalByte); // writes only LSB 8 bits of integer, as expected

            // write len and filename
            nbf.write(Misc.castUnsignedNumberToBytes(pathname_len,2));
            nbf.write(pathname);

            // write permission
            nbf.write(Misc.castUnsignedNumberToBytes(permissions,4));
        }
    }
}

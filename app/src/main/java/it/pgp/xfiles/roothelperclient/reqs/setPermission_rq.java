package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

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
        additionalByte = SubRequest.SET_PERMISSIONS.ordinal() << setAttributes_rq.bitOffsetForSubrequest;

        byte[] tmp;
        tmp = Misc.castUnsignedNumberToBytes(this.pathname_len,2);

        // write request byte
        outputStream.write(requestType.getValue());

        // write additional byte
        outputStream.write(additionalByte); // writes only LSB 8 bits of integer, as expected

        // write len and filename
        outputStream.write(tmp);
        outputStream.write(this.pathname);

        // write permission
        tmp = Misc.castUnsignedNumberToBytes(permissions,4);
        outputStream.write(tmp);
    }
}

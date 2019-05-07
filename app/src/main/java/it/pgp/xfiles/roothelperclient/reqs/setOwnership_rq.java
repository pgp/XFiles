package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 22/11/17
 */

public class setOwnership_rq extends setAttributes_rq {
    @Nullable Integer ownerId;
    @Nullable Integer groupId;

    public setOwnership_rq(Object pathname, @Nullable Integer ownerId, @Nullable Integer groupId) {
        super(pathname);
        this.ownerId = ownerId;
        this.groupId = groupId;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            additionalByte = ((ownerId==null)?0:1) + ((groupId==null)?0:2);
            additionalByte += (SubRequest.SET_OWNERSHIP.ordinal() << setAttributes_rq.bitOffsetForSubrequest);

            // write request byte
            nbf.write(requestType.getValue());

            // write additional byte
            nbf.write(additionalByte); // writes only LSB 8 bits of integer, as expected

            // write len and filename
            nbf.write(Misc.castUnsignedNumberToBytes(pathname_len,2));
            nbf.write(pathname);

            // write ownerships
            if (ownerId != null)
                nbf.write(Misc.castUnsignedNumberToBytes(ownerId,4));
            if (groupId != null)
                nbf.write(Misc.castUnsignedNumberToBytes(groupId,4));
        }
    }
}

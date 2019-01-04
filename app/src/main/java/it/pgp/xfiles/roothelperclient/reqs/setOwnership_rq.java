package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.roothelperclient.reqs.setAttributes_rq;
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
        this.additionalByte = ((ownerId==null)?0:1) + ((groupId==null)?0:2);

        additionalByte += (SubRequest.SET_OWNERSHIP.ordinal() << setAttributes_rq.bitOffsetForSubrequest);

        byte[] tmp;
        tmp = Misc.castUnsignedNumberToBytes(this.pathname_len,2);

        // write request byte
        outputStream.write(requestType.getValue());

        // write additional byte
        outputStream.write(additionalByte); // writes only LSB 8 bits of integer, as expected

        // write len and filename
        outputStream.write(tmp);
        outputStream.write(this.pathname);

        // write ownerships
        if (ownerId != null) {
            tmp = Misc.castUnsignedNumberToBytes(ownerId,4);
            outputStream.write(tmp);
        }
        if (groupId != null) {
            tmp = Misc.castUnsignedNumberToBytes(groupId,4);
            outputStream.write(tmp);
        }
    }
}

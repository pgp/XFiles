package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 22/11/17
 */

public class setDates_rq extends setAttributes_rq {

    @Nullable Integer accessDateSeconds;
    @Nullable Integer modificationDateSeconds;

    public setDates_rq(Object pathname, @Nullable Date accessDate, @Nullable Date modificationDate) {
        super(pathname);
        if (accessDate != null) accessDateSeconds = (int)(accessDate.getTime()/1000);
        if (modificationDate != null) modificationDateSeconds = (int)(modificationDate.getTime()/1000);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        additionalByte =
                ((accessDateSeconds==null)?0:1) +
                ((modificationDateSeconds==null)?0:2);

        additionalByte += (SubRequest.SET_DATES.ordinal() << setAttributes_rq.bitOffsetForSubrequest);

        byte[] tmp;
        tmp = Misc.castUnsignedNumberToBytes(this.pathname_len,2);

        // write request byte
        outputStream.write(requestType.getValue());

        // write additional byte
        outputStream.write(additionalByte); // writes only LSB 8 bits of integer, as expected

        // write len and filename
        outputStream.write(tmp);
        outputStream.write(this.pathname);

        // write timestamps
        if (accessDateSeconds != null) {
            tmp = Misc.castUnsignedNumberToBytes(accessDateSeconds,4);
            outputStream.write(tmp);
        }
        if (modificationDateSeconds != null) {
            tmp = Misc.castUnsignedNumberToBytes(modificationDateSeconds,4);
            outputStream.write(tmp);
        }
    }
}

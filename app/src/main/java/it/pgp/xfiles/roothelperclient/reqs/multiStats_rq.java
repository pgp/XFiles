package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.List;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 06/02/17
 */

public class multiStats_rq extends BaseRHRequest {

    public List<String> pathnames;
    protected final BitSet flags = BitSet.valueOf(new long[]{4});

    // Request type to be set by inheritors
    public multiStats_rq(List<String> pathnames) {
        super(ControlCodes.ACTION_STATS);
        this.pathnames = pathnames;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        // customize with flag bits
        for (int i=0;i<ControlCodes.flags_bit_length;i++) {
            rq ^= ((flags.get(i)?1:0) << (i+ControlCodes.rq_bit_length));
        }
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            byte[] entry,entryLen;
            nbf.write(getRequestByteWithFlags());

            for (String pathname : pathnames) {
                // write len and field
                entry = pathname.getBytes(UTF8);
                entryLen = Misc.castUnsignedNumberToBytes(entry.length,2);
                nbf.write(entryLen);
                nbf.write(entry);
            }
            // list termination (length 0)
            nbf.write(new byte[2]);
        }
    }
}

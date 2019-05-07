package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 31/01/17
 */

public class exists_rq extends SinglePath_rq {
    public BitSet flags;
    public exists_rq(Object pathname, boolean exists, boolean isFile, boolean isDir) {
        super(pathname);
        flags = new BitSet(3);
        flags.set(0,exists);
        flags.set(1,isFile);
        flags.set(2,isDir);
        this.requestType = ControlCodes.ACTION_EXISTS; // exists/is file/is dir
    }

    @Override
    public byte getRequestByteWithFlags() {
        // write request byte
        byte rq = requestType.getValue();
        // customize with flag bits
        for (int i=0;i<flags_bit_length;i++) {
            rq ^= ((flags.get(i)?1:0) << (i+rq_bit_length));
        }
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            nbf.write(getRequestByteWithFlags());
            // write len and field
            nbf.write(Misc.castUnsignedNumberToBytes(this.pathname_len,2));
            nbf.write(this.pathname);
        }
    }
}

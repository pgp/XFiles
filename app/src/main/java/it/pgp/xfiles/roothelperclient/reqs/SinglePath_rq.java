package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 25/01/17
 */

public abstract class SinglePath_rq {
    static final Charset UTF8 = Charset.forName("UTF-8");
    protected static final int rq_bit_length = 5;
    protected static final int flags_bit_length = 3;

    public ControlCodes requestType;
    public int pathname_len;
    public byte[] pathname;

    // overriden for customizing with flag bits
    public byte getRequestByteWithFlags() {
        return requestType.getValue();
    }

    // Request type to be set by inheritors
    public SinglePath_rq(Object pathname) {
        if (pathname instanceof byte[]) {
            this.pathname = (byte[]) pathname;
            this.pathname_len = this.pathname.length;
        }
        else if (pathname instanceof String) {
            // UTF-8 String
            this.pathname = ((String) pathname).getBytes(UTF8);
            this.pathname_len = this.pathname.length;
        }
        else throw new RuntimeException("Unexpected object type in request constructor, allowed bytes and string");
    }

    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // write request byte
            nbf.write(requestType.getValue());
            // write len and field
            nbf.write(Misc.castUnsignedNumberToBytes(this.pathname_len,2));
            nbf.write(this.pathname);
        }
    }
}

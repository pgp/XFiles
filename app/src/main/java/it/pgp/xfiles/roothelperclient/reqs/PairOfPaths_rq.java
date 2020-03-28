package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 25/01/17
 * Deprecated, no longer aligned with rh protocol, see {@link ListOfPathPairs_rq} instead
 */

public abstract class PairOfPaths_rq {
    static final Charset UTF8 = Charset.forName("UTF-8");

    public ControlCodes requestType;
    public int lx,ly; // lengths
    public byte[] fx,fy; // pathnames

    public byte getRequestByteWithFlags() {
        return requestType.getValue();
    }

    // Request type to be set by inheritors
    public PairOfPaths_rq(Object fx, Object fy) {
        if (fx instanceof byte[] && fy instanceof byte[]) {
            this.fx = (byte[]) fx;
            this.lx = this.fx.length;
            this.fy = (byte[]) fy;
            this.ly = this.fy.length;
        }
        else if (fx instanceof String && fy instanceof String) {
            // UTF-8 String
            this.fx = ((String) fx).getBytes(UTF8);
            this.lx = this.fx.length;
            this.fy = ((String) fy).getBytes(UTF8);
            this.ly = this.fy.length;
        }
        else {
            throw new RuntimeException("Unexpected object type(s) in request constructor, allowed bytes and string");
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // write control byte
            nbf.write(getRequestByteWithFlags());
            // write lengths and fields
            nbf.write(Misc.castUnsignedNumberToBytes(lx,2));
            nbf.write(Misc.castUnsignedNumberToBytes(ly,2));
            nbf.write(fx);
            nbf.write(fy);
        }
    }
}

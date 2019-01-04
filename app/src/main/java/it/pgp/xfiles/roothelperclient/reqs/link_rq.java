package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

public class link_rq extends PairOfPaths_rq {
    protected static final int rq_bit_length = 5;

    boolean isHardLink;

    public link_rq(Object fx, Object fy, boolean isHardLink) {
        super(fx, fy);
        requestType = ControlCodes.ACTION_LINK;
        this.isHardLink = isHardLink;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        // customize with flag bits
        rq ^= ((isHardLink?2:0) << (rq_bit_length));
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        byte[] tmpx,tmpy;
        tmpx = Misc.castUnsignedNumberToBytes(this.lx,2);
        tmpy = Misc.castUnsignedNumberToBytes(this.ly,2);

        outputStream.write(getRequestByteWithFlags());

        // write lengths and fields
        outputStream.write(tmpx);
        outputStream.write(tmpy);
        outputStream.write(fx);
        outputStream.write(fy);
    }
}

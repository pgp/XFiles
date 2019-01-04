package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 06/02/17
 */

public class singleStats_rq extends SinglePath_rq {
    private BitSet flags; // file/folder/multi
    public singleStats_rq(Object pathname, FileMode fileMode) {
        super(pathname);
        this.flags = (fileMode==FileMode.FILE ?BitSet.valueOf(new long[]{1}):BitSet.valueOf(new long[]{2})); // 0,0,1 --- 0,1,0
        this.requestType = ControlCodes.ACTION_STATS;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        // customize with flag bits
        for (int i=0;i<flags_bit_length;i++) {
            rq ^= ((flags.get(i)?1:0) << (i+rq_bit_length));
        }
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        byte[] tmp;
        tmp = Misc.castUnsignedNumberToBytes(this.pathname_len,2);

        outputStream.write(getRequestByteWithFlags());

        // write len and field
        outputStream.write(tmp);
        outputStream.write(this.pathname);
    }
}

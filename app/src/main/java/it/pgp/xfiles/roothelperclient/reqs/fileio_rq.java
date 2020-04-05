package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.enums.FileIOMode;
import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 06/11/17
 */

public class fileio_rq extends SinglePath_rq {
    FileIOMode mode;

    public fileio_rq(Object pathname, FileIOMode mode) {
        super(ControlCodes.ACTION_FILEIO, pathname);
        this.mode = mode;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        // customize with flag bits (only one bit)
        rq ^= ((mode == FileIOMode.READFROMFILE?1:0) << (ControlCodes.rq_bit_length));
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

package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 22/05/17
 */
public class ls_archive_rq extends SinglePath_rq {
    public int password_len;
    public byte[] password;
    public byte flags = 0x07; // 0x07 = {1,1,1} (bitmask)

    // Object: String or byte[]
    public ls_archive_rq(Object archivePath, Object password) {
        super(archivePath);
        if (password instanceof String) {
            this.password = ((String) password).getBytes();
            this.password_len = this.password.length;
        }
        else if (password instanceof byte[]) {
            this.password = (byte[]) password;
            this.password_len = (byte) this.password.length;
        }
        else {
            throw new RuntimeException("Unexpected password object type");
        }

        this.requestType = ControlCodes.ACTION_LS;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        rq ^= (flags << rq_bit_length);
        return rq;
    }

    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            nbf.write(getRequestByteWithFlags());
            nbf.write(Misc.castUnsignedNumberToBytes(this.pathname_len,2));
            nbf.write(this.pathname);
            nbf.write(Misc.castUnsignedNumberToBytes(this.password_len,1));
            nbf.write(this.password);
        }
    }
}
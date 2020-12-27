package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class multi_extract_rq extends BaseRHRequest {

    public final boolean smartDirectoryCreation;
    private final List<BasePathContent> fx;
    private final String fy;
    public byte[] password;

    public multi_extract_rq(List<BasePathContent> fx, String fy,
                            @Nullable Object password,
                            boolean smartDirectoryCreation) {
        super(ControlCodes.ACTION_EXTRACT);
        this.fx = fx;
        this.fy = fy;
        this.smartDirectoryCreation = smartDirectoryCreation;
        if (password != null)
            this.password = (password instanceof String)?((String) password).getBytes():(byte[])password;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        rq ^= ((smartDirectoryCreation?2:3) << ControlCodes.rq_bit_length); // 010 vs 011 flags
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        byte[] b;
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // send control byte
            nbf.write(getRequestByteWithFlags());

            // send source archives list and destination directory
            for(BasePathContent s : fx) {
                b = s.dir.getBytes(UTF8);
                nbf.write(Misc.castUnsignedNumberToBytes(b.length,2));
                nbf.write(b);
            }
            nbf.write(Misc.castUnsignedNumberToBytes(0,2)); // eol

            b = fy.getBytes(UTF8);
            nbf.write(Misc.castUnsignedNumberToBytes(b.length,2));
            nbf.write(b);

            if (password == null) {
                nbf.write(new byte[]{0}); // password length 0, 1 byte
            }
            else {
                nbf.write(Misc.castUnsignedNumberToBytes(password.length,1));
                nbf.write(password);
            }
            nbf.write(new byte[]{0,0,0,0}); // 0-length as integer, 4 byte
        }
    }
}

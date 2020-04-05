package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.roothelperclient.RelativeExtractEntries;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 01/06/17
 */

public class extract_rq extends PairOfPaths_rq {

    public byte[] password;
    public byte[] subDir;
    public RelativeExtractEntries entries;
    public boolean smartDirectoryCreation;

    public extract_rq(Object fx, Object fy, // source archive and destination directory (both LocalPathContent)
                      @Nullable Object password, // password will be used if present to try to open archive
                      @Nullable Object subDir, // subDir prefix will be removed by entries path when extracting
                      @Nullable RelativeExtractEntries entries, // for selective extraction
                      boolean smartDirectoryCreation // valid only if entries is null
                      ) {
        super(ControlCodes.ACTION_EXTRACT, fx, fy);
        this.smartDirectoryCreation = smartDirectoryCreation;
        if (password != null)
            this.password = (password instanceof String)?((String) password).getBytes():(byte[])password;
        if (subDir != null)
            this.subDir = (subDir instanceof String)?((String) subDir).getBytes():(byte[])subDir;
        this.entries = entries;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        rq ^= ((smartDirectoryCreation?6:7) << ControlCodes.rq_bit_length); // 110 vs 111 flags
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        super.write(outputStream);
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            if (password == null) {
                nbf.write(new byte[]{0}); // password length 0, 1 byte
            }
            else {
                nbf.write(Misc.castUnsignedNumberToBytes(password.length,1));
                nbf.write(password);
            }
            if (entries == null) {
                nbf.write(new byte[]{0,0,0,0}); // 0-length as integer, 4 byte
            }
            else {
                nbf.write(Misc.castUnsignedNumberToBytes(entries.entries.size(),4));
                for (Integer entry : entries.entries)
                    nbf.write(Misc.castUnsignedNumberToBytes(entry,4));
                nbf.write(Misc.castUnsignedNumberToBytes(entries.stripPathLen,4));
            }
        }
    }
}

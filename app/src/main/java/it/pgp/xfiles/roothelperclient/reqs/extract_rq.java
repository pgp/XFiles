package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

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

    public extract_rq(Object fx, Object fy, // source archive and destination directory (both LocalPathContent)
                      @Nullable Object password, // password will be used if present to try to open archive
                      @Nullable Object subDir, // subDir prefix will be removed by entries path when extracting
                      @Nullable RelativeExtractEntries entries // for selective extraction
                      ) {
        super(fx, fy);
        requestType = ControlCodes.ACTION_EXTRACT;
        if (password != null)
            this.password = (password instanceof String)?((String) password).getBytes():(byte[])password;
        if (subDir != null)
            this.subDir = (subDir instanceof String)?((String) subDir).getBytes():(byte[])subDir;
        this.entries = entries;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        super.write(outputStream);

        if (password == null) {
            outputStream.write(new byte[]{0}); // password length 0, 1 byte
        }
        else {
            byte[] password_len_bytes = Misc.castUnsignedNumberToBytes(password.length,1);
            outputStream.write(password_len_bytes);
            outputStream.write(password);
        }

        if (entries == null) {
            outputStream.write(new byte[]{0,0,0,0}); // 0-length as integer, 4 byte
        }
        else {
            byte[] intVal = Misc.castUnsignedNumberToBytes(entries.entries.size(),4);
            outputStream.write(intVal);
            for (Integer entry : entries.entries) {
                intVal = Misc.castUnsignedNumberToBytes(entry,4);
                outputStream.write(intVal);
            }
            outputStream.write(Misc.castUnsignedNumberToBytes(entries.stripPathLen,4));
        }
    }
}

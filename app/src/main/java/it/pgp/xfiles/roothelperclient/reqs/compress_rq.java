package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 30/05/17
 */

public class compress_rq extends PairOfPaths_rq {

    List<byte[]> filenames;
    byte[] password;
    compress_rq_options compress_options;

    // at least source folder and destination archive
    public compress_rq(Object fx, Object fy,
                       @Nullable Integer compressionLevel,
                       @Nullable Boolean encryptHeaders,
                       @Nullable Boolean solidMode,
                       @Nullable String password,
                       @Nullable List<String> filenames) {
        super(fx, fy);
        requestType = ControlCodes.ACTION_COMPRESS;
        compress_options = new compress_rq_options(compressionLevel,encryptHeaders,solidMode);

        if (password == null) this.password = new byte[0];
        else this.password = password.getBytes();
        if (filenames == null) return;
        this.filenames = new ArrayList<>();
        for (String x : filenames) {
            this.filenames.add(x.getBytes(PairOfPaths_rq.UTF8));
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        super.write(outputStream);

        compress_options.writecompress_rq_options(outputStream);

        outputStream.write(password.length); // single byte (8 least significant bits of the 32-bit integer)
        if (password.length != 0)
            outputStream.write(password);

        if (filenames == null) { // filenames not initialized, compress entire source directory content
            outputStream.write(new byte[]{0,0,0,0}); // n. of filenames is 0
        }
        else {
            // send n. of filenames
            outputStream.write(Misc.castUnsignedNumberToBytes(filenames.size(),4)); // send as 4-byte integer
            for (byte[] x : filenames) {
                outputStream.write(Misc.castUnsignedNumberToBytes(x.length,2));
                outputStream.write(x);
            }
        }
    }
}

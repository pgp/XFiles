package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 03/02/17
 */

public class hash_rq extends SinglePath_rq {
    HashRequestCodes hashAlgorithm;
    BitSet dirHashOpts;

    /**
     * Dir hash opts:
     * 0: dirHashWithNames, // keep into account filenames in directory hashing
     * 1: dirHashIgnoreThumbsFiles, // ignore .DS_Store and Thumbs.db
     * 2: dirHashIgnoreUnixHiddenFiles, // ignore files and folders whose names start with '.'
     * 3: dirHashIgnoreEmptyDirs
     */

    public hash_rq(Object pathname,
                   HashRequestCodes hashAlgorithm,
                   BitSet dirHashOpts) {
        super(ControlCodes.ACTION_HASH, pathname);
        this.hashAlgorithm = hashAlgorithm;
        this.dirHashOpts = dirHashOpts;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // write request byte
            nbf.write(requestType.getValue());
            // write algorithm byte
            nbf.write(hashAlgorithm.getValue());

            // write dirHashOpts
            byte dirHashOpts_ = 0;
            // customize with flag bits
            for (int i=0;i<ControlCodes.flags_bit_length;i++)
                dirHashOpts_ ^= ((dirHashOpts.get(i)?1:0) << i);
            nbf.write(dirHashOpts_);

            // write len and field (digest output length is implicit)
            nbf.write(Misc.castUnsignedNumberToBytes(this.pathname_len,2));
            nbf.write(this.pathname);
        }
    }

}

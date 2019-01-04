package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by pgp on 28/06/17
 * to be sent embedded from within compress_rq
 */

public class compress_rq_options {
    private byte compressionLevel;
    private byte encryptHeader;
    private byte solid;

    public compress_rq_options(byte compressionLevel, byte encryptHeader, byte solid)  {
        this.compressionLevel = compressionLevel;
        this.encryptHeader = encryptHeader;
        this.solid = solid;
    }

    public void writecompress_rq_options(OutputStream outputStream) throws IOException {
        byte[] tmp;
        tmp = new byte[]{compressionLevel};
        outputStream.write(tmp);
        tmp = new byte[]{encryptHeader};
        outputStream.write(tmp);
        tmp = new byte[]{solid};
        outputStream.write(tmp);
    }
}
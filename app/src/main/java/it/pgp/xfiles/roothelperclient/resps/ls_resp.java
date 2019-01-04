package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

public class ls_resp {
    public byte[] filename;
    public long date;
    public byte[] permissions;
    public long size;

    public ls_resp(DataInputStream inputStream) throws IOException {
        byte[] tmp;
        tmp = new byte[2];
        inputStream.readFully(tmp);
        int filename_len = (int) Misc.castBytesToUnsignedNumber(tmp,2);
        if (filename_len == 0) return; // FIXME this enforces double zero checking (here and by caller) in order to detect end of stream
        this.filename = new byte[filename_len];
        inputStream.readFully(this.filename);
        tmp = new byte[4];
        inputStream.readFully(tmp);
        this.date = Misc.castBytesToUnsignedNumber(tmp,4);
        tmp = new byte[10];
        inputStream.readFully(tmp);
        this.permissions = tmp;
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.size = Misc.castBytesToUnsignedNumber(tmp,8);
    }

}

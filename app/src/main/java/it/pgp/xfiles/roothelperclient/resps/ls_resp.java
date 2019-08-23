package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

public class ls_resp {
    public byte[] filename;
    public long date;
    public byte[] permissions;
    public long size;

    private ls_resp() {}

    public static ls_resp readNext(DataInputStream inputStream) throws IOException {
        ls_resp resp = new ls_resp();
        byte[] tmp;
        tmp = new byte[2];
        inputStream.readFully(tmp);
        int filename_len = (int) Misc.castBytesToUnsignedNumber(tmp,2);
        if (filename_len == 0) return null; // end of list indication
        resp.filename = new byte[filename_len];
        inputStream.readFully(resp.filename);
        tmp = new byte[4];
        inputStream.readFully(tmp);
        resp.date = Misc.castBytesToUnsignedNumber(tmp,4);
        tmp = new byte[10];
        inputStream.readFully(tmp);
        resp.permissions = tmp;
        tmp = new byte[8];
        inputStream.readFully(tmp);
        resp.size = Misc.castBytesToUnsignedNumber(tmp,8);
        return resp;
    }

}

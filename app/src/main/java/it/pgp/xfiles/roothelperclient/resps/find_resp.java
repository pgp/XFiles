package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

public class find_resp {

    public ls_resp fileItem;
    public byte[] contentAround; // not null only in non-trivial find in content
    public long offset;

    private find_resp() {}

    public static find_resp readNext(DataInputStream inputStream) throws IOException {
        find_resp resp = new find_resp();
        resp.fileItem = ls_resp.readNext(inputStream);
        if(resp.fileItem == null) return null; // end of list indication

        byte[] tmp = new byte[1];
        inputStream.readFully(tmp);
        int contentAround_len = (int) Misc.castBytesToUnsignedNumber(tmp,1);

        if (contentAround_len != 0) {
            resp.contentAround = new byte[contentAround_len];
            inputStream.readFully(resp.contentAround);
            tmp = new byte[8];
            inputStream.readFully(tmp);
            resp.offset = Misc.castBytesToUnsignedNumber(tmp,8);
        }

        return resp;
    }

    @Override
    public String toString() {
        return new String(fileItem.filename);
    }
}

package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

public class find_resp {

    public ls_resp fileItem;
    public byte[] contentAround; // not null only in non-trivial find in content
    public long offset;

    public boolean eol = false; // end of list indication

    public find_resp(DataInputStream inputStream) throws IOException {
        byte[] tmp;

        this.fileItem = new ls_resp(inputStream);
        if (fileItem.filename == null) {
            eol = true;
            return;
        }

        tmp = new byte[1];
        inputStream.readFully(tmp);
        int contentAround_len = (int) Misc.castBytesToUnsignedNumber(tmp,1);

        if (contentAround_len != 0) {
            contentAround = new byte[contentAround_len];
            inputStream.readFully(contentAround);
            tmp = new byte[8];
            inputStream.readFully(tmp);
            offset = Misc.castBytesToUnsignedNumber(tmp,8);
        }
    }

    @Override
    public String toString() {
        return new String(fileItem.filename);
    }
}

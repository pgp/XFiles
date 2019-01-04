package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.BitSet;

/**
 * Created by pgp on 31/01/17
 */

public class exists_resp {
    public byte response;
    public BitSet respFlags;
    public exists_resp(DataInputStream inputStream) throws IOException {
        response = inputStream.readByte();
        if (response == 0) { // unconditionally 0x00 (OK) for this kind of requests
            respFlags = BitSet.valueOf(new byte[]{inputStream.readByte()});
        }
        else throw new RuntimeException("Wrong response byte on exist query, should never happen");
    }
    public boolean getExists() {
        return respFlags.get(0);
    }

    public boolean getIsFile() {
        return respFlags.get(1);
    }

    public boolean getIsDir() {
        return respFlags.get(2);
    }
}

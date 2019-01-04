package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import it.pgp.Native;
import it.pgp.xfiles.utils.Misc;

public class find_resp {

    public ls_resp fileItem;
    public byte[] contentAround; // not null only in non-trivial find in content
//    public byte[] filename; // full path, LEGACY
    public long offset;

    public boolean eol = false; // end of list indication

    public find_resp(DataInputStream inputStream) throws IOException {
        byte[] tmp;
//        tmp = new byte[2];
//        inputStream.readFully(tmp);
//        int filename_len = (int) Misc.castBytesToUnsignedNumber(tmp,2);
//        if (filename_len == 0) {
//            eol = true;
//            return;
//        }
//        this.filename = new byte[filename_len];
//        inputStream.readFully(this.filename);

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

    // LEGACY, and not needed, we are using FindResultsAdapter is an array adapter
//    @Override
//    public int hashCode() {
//        if(eol) return 0; // assume two EOL packets are equal, even if they could contain different data (forbidden by protocol design)
//        ByteBuffer bb = ByteBuffer.allocate(contentAround.length+filename.length+(Long.SIZE/8));
//        bb.put(contentAround);
//        bb.put(filename);
//        bb.putLong(offset);
//        return Native.nHashCode(bb.array());
//    }
//
//    @Override
//    public boolean equals(Object obj_) {
//        if (!(obj_ instanceof find_resp)) return false;
//        find_resp obj = (find_resp) obj_;
//        if (this.eol && obj.eol) return true;
//        return Arrays.equals(this.contentAround,obj.contentAround)
//                && Arrays.equals(this.filename,obj.filename)
//                && (this.offset == obj.offset);
//    }
}

package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.List;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 19/07/17
 * One single request for copying/moving/uploading/downloading a selection of files
 */

public class ListOfPathPairs_rq extends BaseRHRequest {

    public List<String> v_fx,v_fy; // pathnames

    // Request type to be set by inheritors
    public ListOfPathPairs_rq(ControlCodes requestType, List<String> v_fx, List<String> v_fy) {
        super(requestType);
        this.v_fx = v_fx;
        this.v_fy = v_fy;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        final byte[] listEnd = new byte[]{0,0,0,0};
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // write control byte
            nbf.write(requestType.getValue());

            Iterator<String> fxi = v_fx.iterator();
            Iterator<String> fyi = v_fy.iterator();

            while(fxi.hasNext()) {
                byte[] x = fxi.next().getBytes(UTF8);
                byte[] y = fyi.next().getBytes(UTF8);

                // write pair of lengths
                byte[] tmpx,tmpy;
                tmpx = Misc.castUnsignedNumberToBytes(x.length,2);
                tmpy = Misc.castUnsignedNumberToBytes(y.length,2);
                ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()); // change here if length decode error or swapped lengths
                b.put(tmpx);
                b.put(tmpy);
                nbf.write(b.array());

                // write pair of paths
                nbf.write(x);
                nbf.write(y);
            }

            nbf.write(listEnd);
        }
    }
}

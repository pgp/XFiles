package it.pgp.xfiles.roothelperclient.reqs;

public class retrieveHomePath_rq extends ls_rq {

    public byte flags = 0x02; // 0x02 = {0,1,0} (bitmask)

    public retrieveHomePath_rq(Object dirPath) {
        super(dirPath);
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        rq ^= (flags << rq_bit_length);
        return rq;
    }
}

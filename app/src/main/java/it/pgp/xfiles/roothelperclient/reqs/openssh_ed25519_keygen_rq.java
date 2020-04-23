package it.pgp.xfiles.roothelperclient.reqs;

public class openssh_ed25519_keygen_rq extends openssl_rsa_pem_keygen_rq {
    public openssh_ed25519_keygen_rq(int keySize) {
        super(-1); // keysize argument unused
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        rq ^= (1 << 5); // flags: 001
        return rq;
    }
}

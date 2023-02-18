package it.pgp.xfiles.roothelperclient;

public enum StreamCiphers {
    CHACHA,
    AES128CTR,
    AES256CTR,
    SHACAL2CTR;

    public String toString() {
        switch(this) {
            case CHACHA:
                return "ChaCha";
            case AES128CTR:
                return "AES-128/CTR";
            case AES256CTR:
                return "AES-256/CTR";
            case SHACAL2CTR:
                return "SHACAL2/CTR";
            default:
                return null;
        }
    }
}
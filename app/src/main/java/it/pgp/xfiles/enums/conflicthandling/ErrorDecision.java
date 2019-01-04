package it.pgp.xfiles.enums.conflicthandling;

public enum ErrorDecision {
    ED_CONTINUE((byte)0x00),
    ED_CANCEL((byte)0xFF);

    byte i;

    ErrorDecision(byte i) {
        this.i = i;
    }

    public byte getValue() {
        return i;
    }
}

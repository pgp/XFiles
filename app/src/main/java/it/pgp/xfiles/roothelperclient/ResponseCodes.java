package it.pgp.xfiles.roothelperclient;

import java.util.HashMap;
import java.util.Map;

public enum ResponseCodes {

    // responses (full byte)
    RESPONSE_OK((byte)0x00),
    RESPONSE_ERROR((byte)0xFF),
    RESPONSE_REDIRECT((byte)0x11);

    final byte value;
    static final Map<Byte, ResponseCodes> codeMap = new HashMap<Byte, ResponseCodes>(){{
        for (ResponseCodes x: ResponseCodes.values())
            put(x.getValue(),x);
    }};

    ResponseCodes(byte value) {
        this.value = value;
    }

    // enum value to byte value
    public byte getValue() {
        return value;
    }

    // byte value to enum value
    public static ResponseCodes getCode(byte value) {
        return codeMap.get(value);
    }

}

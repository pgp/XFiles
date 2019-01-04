package it.pgp.xfiles.roothelperclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pgp on 03/02/17
 *
 * corresponding to hashMethods,hashLabels,hashLengths enum and arrays in roothelper c code
 */

public enum HashRequestCodes {

    md5((byte)0x01,"MD5",16),
    sha1((byte)0x02,"SHA1",20),
    sha256((byte)0x03,"SHA256",32),
    sha384((byte)0x04,"SHA384",48),
    sha512((byte)0x05,"SHA512",64),
    sha3_224((byte)0x06,"SHA3-224",28),
    sha3_256((byte)0x07,"SHA3-256",32),
    sha3_384((byte)0x08,"SHA3-384",48),
    sha3_512((byte)0x09,"SHA3-512",64),
    blake2b_256((byte)0x0A,"BLAKE2B-256",32);

    final byte value;
    final String label;
    final int length;
    static final Map<Byte,HashRequestCodes> codeMap = new HashMap<Byte,HashRequestCodes>(){{
        for (HashRequestCodes x: HashRequestCodes.values())
            put(x.getValue(),x);
    }};

    HashRequestCodes(byte value, String label, int length) {
        this.value = value;
        this.label = label;
        this.length = length;
    }

    // enum value to byte value
    public byte getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public int getLength() {
        return length;
    }

    // byte value to enum value
    public static HashRequestCodes getCode(byte value) {
        return codeMap.get(value);
    }


    /**
     * Just for handling selection status in {@link it.pgp.xfiles.adapters.HashAlgorithmsAdapter}
     */
    boolean checked = false;

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void toggleChecked() {
        checked = !checked;
    }

    public static void clear() {
        for (HashRequestCodes c : HashRequestCodes.values())
            c.checked = false;
    }
}

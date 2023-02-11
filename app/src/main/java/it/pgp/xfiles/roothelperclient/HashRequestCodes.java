package it.pgp.xfiles.roothelperclient;

import android.widget.Checkable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pgp on 03/02/17
 *
 * corresponding to hashMethods,hashLabels,hashLengths enum and arrays in roothelper code
 * (rh_hasher_botan.h, cli_hashLabels vector)
 */

public enum HashRequestCodes implements Checkable {

    crc32((byte)0x00,"CRC32",4,android.R.color.holo_purple),
    md5((byte)0x01,"MD5",16, android.R.color.holo_red_dark),
    sha1((byte)0x02,"SHA1",20, android.R.color.holo_red_dark),
    sha224((byte)0x0B,"SHA224",28, android.R.color.holo_green_light),
    sha256((byte)0x03,"SHA256",32, android.R.color.holo_orange_light),
    sha384((byte)0x04,"SHA384",48, android.R.color.holo_green_light),
    sha512((byte)0x05,"SHA512",64, android.R.color.holo_orange_light),
    sha3_224((byte)0x06,"SHA3-224",28, android.R.color.holo_green_light),
    sha3_256((byte)0x07,"SHA3-256",32, android.R.color.holo_green_light),
    sha3_384((byte)0x08,"SHA3-384",48, android.R.color.holo_green_light),
    sha3_512((byte)0x09,"SHA3-512",64, android.R.color.holo_green_light),
    blake2b_256((byte)0x0A,"BLAKE2B-256",32, android.R.color.holo_green_light);

    final byte value;
    final String label;
    final int length;
    final int labelColor;
    static final Map<Byte,HashRequestCodes> codeMap = new HashMap<Byte,HashRequestCodes>(){{
        for (HashRequestCodes x: HashRequestCodes.values())
            put(x.getValue(),x);
    }};

    HashRequestCodes(byte value, String label, int length, int labelColor) {
        this.value = value;
        this.label = label;
        this.length = length;
        this.labelColor = labelColor;
    }

    // enum value to byte value
    public byte getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label;
    }

    public int getLength() {
        return length;
    }

    public int getLabelColor() {
        return labelColor;
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

    public void toggle() {
        checked = !checked;
    }

    public static void clear() {
        for (HashRequestCodes c : HashRequestCodes.values())
            c.checked = false;
    }
}

package it.pgp.xfiles.enums.conflicthandling;

import it.pgp.xfiles.R;

public enum ConflictType {
    FILE((byte)0x00, R.drawable.xfiles_file_icon),
    DIR((byte)0x01, R.drawable.xf_dir_blu);

    byte b;
    int imageRes;

    ConflictType(byte b, int imageRes) {
        this.b = b;
        this.imageRes = imageRes;
    }

    public byte getValue() {
        return b;
    }

    public int getImageRes() {
        return imageRes;
    }

    public static ConflictType fromNumeric(byte b) {
        switch (b) {
            case ((byte)0x00):
                return FILE;
            case ((byte)0x01):
                return DIR;
            default:
                return null;
        }
    }
}

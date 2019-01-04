package it.pgp.xfiles.enums;

/**
 * Created by pgp on 31/10/16
 */

public enum FileMode {
    FILE(0644),
    DIRECTORY(0755);

    int mask;

    FileMode(int mask) {
        this.mask = mask;
    }

    public int getDefaultMask() {
        return mask;
    }
}

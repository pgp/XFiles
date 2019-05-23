package it.pgp.xfiles.enums;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pgp on 23/05/17
 * Archive types supported by rootHelper's p7zip embedded library
 */

public enum ArchiveType {
    _7Z("7z"),
    XZ("xz"),
    RAR("rar"),
    RAR5("rar5"),
    ZIP("zip"),
    CAB("cab"),
    GZ("gz"),
    BZ2("bz2"),
    TAR("tar"), // with offset
    APK("apk"),
    UNKNOWN("unknown");

    String s;

    ArchiveType(String s) {
        this.s = s;
    }

    public String getValue() {
        return s;
    }

    public static final Set<String> formats = new HashSet<String>(){{
        for (ArchiveType a : ArchiveType.values()) {
            if (a != RAR5 && a != UNKNOWN) {
                add(a.getValue());
            }
        }
    }};
}

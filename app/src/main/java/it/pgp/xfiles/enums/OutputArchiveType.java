package it.pgp.xfiles.enums;

/**
 * Created by pgp on 30/05/17
 * Supported 7z output formats
 */

public enum OutputArchiveType {
    // no need to use explicit numberings, ordinal() is enough
    _7Z("7z"),
//    XZ("xz"), // need to add single-file check
    ZIP("zip"),
//    GZ("gz"), // need to add single-file check
//    BZ2("bz2"), // need to add single-file check
    TAR("tar") // with offset
    ;

    String s;

    OutputArchiveType(String s) {
        this.s = s;
    }

    public String getValue() {
        return s;
    }
}

package it.pgp.xfiles.enums;

import it.pgp.xfiles.R;

/**
 * Created by pgp on 30/05/17
 * Supported 7z output formats
 */

public enum OutputArchiveType {
    // no need to use explicit numberings, ordinal() is enough
    _7Z("7z", R.id._7zRadioButton),
//    XZ("xz"), // need to add single-file check
    ZIP("zip", R.id.zipRadioButton),
//    GZ("gz"), // need to add single-file check
//    BZ2("bz2"), // need to add single-file check
    TAR("tar", R.id.tarRadioButton) // with offset
    ;

    final String s;
    final int id;

    OutputArchiveType(String s, int id) {
        this.s = s;
        this.id = id;
    }

    public String getValue() {
        return s;
    }

    public int getId() {
        return id;
    }
}

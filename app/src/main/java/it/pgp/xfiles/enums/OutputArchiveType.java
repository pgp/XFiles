package it.pgp.xfiles.enums;

import it.pgp.xfiles.R;

/**
 * Created by pgp on 30/05/17
 * Supported 7z output formats
 */

public enum OutputArchiveType {
    // no need to use explicit numberings, ordinal() is enough
    _7Z("7z", R.id._7zRadioButton),
    ZIP("zip", R.id.zipRadioButton),
    TAR("tar", R.id.tarRadioButton), // with offset
    GZ("gz", R.id.gzRadioButton),
    BZ2("bz2", R.id.bz2RadioButton),
    XZ("xz", R.id.xzRadioButton)
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

package it.pgp.xfiles.enums;

import java.util.HashSet;
import java.util.Set;

import it.pgp.xfiles.R;

/**
 * Created by pgp on 23/05/17
 * Archive types supported by rootHelper's p7zip embedded library
 */

public enum ArchiveType {
    _7Z("7z", R.drawable.xfiles_archive_7z),
    XZ("xz", R.drawable.xfiles_archive_xz),
    RAR("rar", R.drawable.xfiles_archive_rar),
    RAR5("rar5", R.drawable.xfiles_archive_rar),
    ZIP("zip", R.drawable.xfiles_archive_zip),
    CAB("cab", R.drawable.xfiles_archive_cab),
    GZ("gz", R.drawable.xfiles_archive_gz),
    BZ2("bz2", R.drawable.xfiles_archive_bz2),
    TAR("tar", R.drawable.xfiles_archive_tar), // with offset
    APK("apk", R.drawable.xfiles_archive_apk),
    UNKNOWN("unknown", -1);

    public final String s;
    public final int resId;

    ArchiveType(String s, int resId) {
        this.s = s;
        this.resId = resId;
    }

    public static final Set<String> formats = new HashSet<String>(){{
        for (ArchiveType a : ArchiveType.values()) {
            if (a != RAR5 && a != UNKNOWN) {
                add(a.s);
            }
        }
    }};
}

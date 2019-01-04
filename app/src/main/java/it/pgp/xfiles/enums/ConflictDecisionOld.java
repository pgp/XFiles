package it.pgp.xfiles.enums;

/**
 * Created by pgp on 08/11/16
 */
@Deprecated
public enum ConflictDecisionOld {
    ASK,
    RENAME_SRC, // assign new name to the file to be copied
    RENAME_DEST, // assign new name to the file already in the destination folder
    AUTO_RENAME_SRC,
    AUTO_RENAME_DEST,
    OVERWRITE,
    SKIP,
    CANCEL
}

package it.pgp.xfiles.enums;

/**
 * Created by pgp on 13/11/17
 * To be used with ProgressIndicator
 */

public enum ForegroundServiceType {
    FILE_TRANSFER,
    FILE_ARCHIVING,
    XRE_TRANSFER,
    XRE_HASH, // currently without overlay usage, only resource locking for long term tls connection, enable overlay when progress indication will be implemented in rh's hashFile
    SFTP_TRANSFER,
    SMB_TRANSFER,
    URL_DOWNLOAD,
    FIND,
    CREATE_FILE
}

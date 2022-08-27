package it.pgp.xfiles.enums;

/**
 * Created by pgp on 14/02/17 (adapted from roothelperclient ControlCodes)
 */

public enum FileOpsErrorCodes {
    OK("OK"),
    COMMANDER_CANNOT_SHIFT("Cannot go back or ahead"),
    COMMANDER_CANNOT_REFRESH("Cannot refresh"),
    COMMANDER_CANNOT_ACCESS("Cannot access"), // generic
    NOT_IMPLEMENTED("Not implemented"),
    ILLEGAL_ARGUMENT("Illegal argument"),
    CURRENT_DIR_NO_LONGER_AVAILABLE("Current directory no longer available"),

    TRANSFER_ERROR(""), // generic copy error (read/write error)
    TRANSFER_CANCELLED(""), // transfer explicitly cancelled by user
    DEST_FILE_ALREADY_EXISTS("Destination file already exists"), // to trigger file conflict dialog

    ROOTHELPER_INIT_ERROR("Cannot start or connect to roothelper process"),
    COMPRESS_ERROR("Error during compression"), // TODO duplicate error messages from rh server's 7z code

    NULL_OR_WRONG_PASSWORD("Null or wrong password"),
    CRC_FAILED("CRC failed"), // wrong password in extract as well

    // SFTP errors
    MALFORMED_PATH_ERROR("Malformed path"),
    CONNECTION_ERROR("Connection error"),
    SFTP_PATH_CANONICALIZE_ERROR("SFTP path canonicalization error"), // FIXME to be removed, just for debugging
    AUTHENTICATION_ERROR("Authentication error"), // to trigger password insert
    HOST_KEY_CHANGED_ERROR("Host key changed"), // to trigger dialog on host key changed
    HOST_KEY_INEXISTENT_ERROR("Host key does not exists") // to trigger dialog on host key not found
    ;

    final String value;

    FileOpsErrorCodes(String value) {
        this.value = value;
    }

    // enum value to string value
    public String getValue() {
        return value;
    }

}

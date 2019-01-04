package it.pgp.xfiles.exceptions;

/**
 * Created by pgp on 02/11/16
 */

public class InvalidBrowserViewModeException extends RuntimeException {
    public InvalidBrowserViewModeException() {
        super("Invalid browser view mode");
    }
}

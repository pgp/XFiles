package it.pgp.xfiles.exceptions;

/**
 * Created by pgp on 19/01/18
 * For now, indicating only that current dir cannot be accessed by dir commander
 * Could be generalized (with an exception type enum) including other access errors of dir commander
 */

public class DirCommanderException extends Exception {
    public DirCommanderException() {
        super("Dir not accessible by commander");
    }

    public DirCommanderException(String message) {
        super(message);
    }
}

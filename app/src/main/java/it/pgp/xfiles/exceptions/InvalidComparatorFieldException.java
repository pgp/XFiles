package it.pgp.xfiles.exceptions;

/**
 * Created by pgp on 02/11/16
 */

public class InvalidComparatorFieldException extends RuntimeException {
    public InvalidComparatorFieldException() {
        super("Unexpected attribute type, please check attribute enum");
    }
}

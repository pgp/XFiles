package it.pgp.xfiles.exceptions;

import java.io.IOException;

/**
 * Created by pgp on 16/11/17
 * Just to check whether to show copy error in case of IOException
 * (SSHJ IOException by stream copier listener trick, will cancel transfer
 * web source: https://github.com/hierynomus/sshj/issues/288 )
 */

public class InterruptedTransferAsIOException extends IOException {
    public InterruptedTransferAsIOException() {
        super("Sftp transfer cancelled by user");
    }
}

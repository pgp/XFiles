package it.pgp.xfiles.sftpclient;

import net.schmizz.sshj.sftp.SFTPEngine;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.xfer.TransferListener;

import java.io.IOException;

public class XSFTPFileTransfer extends SFTPFileTransfer {

    TransferListener transferListener;

    public XSFTPFileTransfer(SFTPEngine engine) {
        super(engine);
        this.transferListener = new XTransferListener();
    }

    @Override
    public TransferListener getTransferListener() {
        return transferListener;
    }

    @Override
    public void setTransferListener(TransferListener transferListener) {
        this.transferListener = transferListener;
    }

    // comment upload and download methods to revert to standard Java code for file IO (no roothelper)
    @Override
    public void upload(String source, String dest) throws IOException {
        upload(new XFileSystemFile(source), dest);
    }

    @Override
    public void download(String source, String dest) throws IOException {
        download(source, new XFileSystemFile(dest));
    }
}

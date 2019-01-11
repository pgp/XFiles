package it.pgp.xfiles.sftpclient;

import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.xfer.TransferListener;

public class XTransferListener implements TransferListener {

    public XProgress progressIndicator;

    // should be already initialized with total files
    public void setProgressIndicator(XProgress progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    @Override
    public TransferListener directory(String name) {
//        progressIndicator.incrementOuterProgressThenPublish(1); // increment dir progress at the beginning - avoid division by zero with dummy size of 1
        return this;
    }

    @Override
    public StreamCopier.Listener file(String name, long size) {
        progressIndicator.incrementOuterProgressThenPublish(size);
        return progressIndicator::publishInnerProgress;
    }
}

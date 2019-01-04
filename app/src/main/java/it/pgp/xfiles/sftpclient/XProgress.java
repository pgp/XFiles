package it.pgp.xfiles.sftpclient;

import android.app.Service;
import android.view.WindowManager;

import java.io.IOException;

import it.pgp.xfiles.exceptions.InterruptedTransferAsIOException;
import it.pgp.xfiles.service.visualization.MovingRibbonTwoBars;

/**
 * Adapter class preserving external progress state to be used with SSHJ progress listeners
 */

public class XProgress extends MovingRibbonTwoBars {

    public long totalFiles,currentFiles;
    public long totalSize,currentSize,lastShownSize;

    public boolean cancelled = false;

    private static final long SIZE_THRESHOLD = 1000000;

    public XProgress(Service service, WindowManager wm) {
        super(service, wm);
    }

//    public XProgress() {
//        clear();
//    }

    public void clear() {
        this.totalFiles = 0;
        this.currentFiles = 0;
        this.totalSize = 0;
        this.currentSize = 0;
        this.lastShownSize = 0;
    }

//    public XProgress(long totalFiles) {
//        super();
//        this.totalFiles = totalFiles;
//        this.currentFiles = 0;
//        this.totalSize = 0;
//        this.currentSize = 0;
//        this.lastShownSize = 0;
//    }

    private void publish() {
        setProgress(
                (int)Math.floor(currentFiles*100/totalFiles),
                (int)Math.floor(currentSize*100/totalSize)
        );
    }

    public void publishProgress(long outerProgress, long innerProgress) {
        currentFiles = outerProgress;
        currentSize = innerProgress;
        publish();
    }

    public void publishInnerProgress(long innerProgress) throws IOException {
        if (cancelled) throw new InterruptedTransferAsIOException();
        currentSize = innerProgress;
        if (currentSize - lastShownSize > SIZE_THRESHOLD) {
            lastShownSize = currentSize;
            publish();
        }
    }

    public void publishOuterProgress(long outerProgress, long newFileSize) {
        currentFiles = outerProgress;
        totalSize = newFileSize;
        currentSize = 0;
        lastShownSize = 0;
        publish();
    }

    public void incrementOuterProgressThenPublish(long newFileSize) {
        currentFiles++;
        totalSize = newFileSize;
        currentSize = 0;
        lastShownSize = 0;
        publish();
    }

    public void cancelByProgressCrash() {
        cancelled = true;
    }
}

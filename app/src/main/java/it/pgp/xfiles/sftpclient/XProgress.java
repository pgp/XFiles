package it.pgp.xfiles.sftpclient;

import android.app.Service;
import android.view.WindowManager;

import java.io.IOException;

import it.pgp.xfiles.exceptions.InterruptedTransferAsIOException;
import it.pgp.xfiles.service.visualization.MovingRibbonTwoBars;
import it.pgp.xfiles.utils.Pair;

/**
 * Adapter class preserving external progress state to be used with SSHJ progress listeners
 * 23/02/2018: used also for SMB task, create separate class if needed later
 */

public class XProgress extends MovingRibbonTwoBars {

    public long totalFiles,currentFiles;
    public long totalSize,currentSize,lastShownSize;

    public boolean cancelled = false;

    protected static final long SIZE_THRESHOLD = 1000000;

    public XProgress(Service service, WindowManager wm) {
        super(service, wm);
    }

    public void clear() {
        this.totalFiles = 0;
        this.currentFiles = 0;
        this.totalSize = 0;
        this.currentSize = 0;
        this.lastShownSize = 0;
    }

    // detailed progress variables and methods
    protected long completedFilesSizeSoFar;
    public long totalFilesSize; // set only in case of upload

    public boolean isDetailedProgress = false; // set to true in case of upload

    protected void publish() {
        if(isDetailedProgress)
            setProgress(
                    new Pair<>(completedFilesSizeSoFar + currentSize, totalFilesSize),
                    new Pair<>(currentSize, totalSize)
            );
        else
            setProgress(
                    new Pair<>(currentFiles, totalFiles),
                    new Pair<>(currentSize, totalSize)
            );
    }

    public void publishInnerProgress(long innerProgress) throws IOException {
        if (cancelled) throw new InterruptedTransferAsIOException();
        currentSize = innerProgress;
        if (currentSize - lastShownSize > SIZE_THRESHOLD) {
            lastShownSize = currentSize;
            publish();
        }
    }

    public void incrementOuterProgressThenPublish(long newFileSize) {
        if(isDetailedProgress) completedFilesSizeSoFar += totalSize;  // increment of last completed file size
        else currentFiles++;

        totalSize = newFileSize;
        currentSize = 0;
        lastShownSize = 0;
        publish();
    }

    public void cancelByProgressCrash() {
        cancelled = true;
    }
}

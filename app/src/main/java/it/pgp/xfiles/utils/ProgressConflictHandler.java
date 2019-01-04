package it.pgp.xfiles.utils;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.ConflictDialog;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.conflicthandling.ConflictDecision;
import it.pgp.xfiles.enums.conflicthandling.ConflictType;
import it.pgp.xfiles.enums.conflicthandling.ErrorDecision;
import it.pgp.xfiles.service.BaseBackgroundTask;

/**
 * Class that handles progress and conflict resolution interactions for file copy, based on status map
 * code ported from python fileCopy_socketStatusMap
 */

public class ProgressConflictHandler {

    @FunctionalInterface
    private interface fromStatusInterface {
        void from(ProgressConflictHandler handler) throws IOException;
    }

    /////////////////////////////////////////
    private static void commonTransition(ProgressConflictHandler handler) throws IOException {
        long n = Misc.receiveTotalOrProgress(handler.rs.i);
        // should be equivalent to unsigned comparison: n < 2**64 -5
        Status s = Status.fromNumeric(n);
        if (n >= 0) {
            Log.d("Progress", "Size is: "+n);
            publishAfterSizeReceived(handler,n);
            handler.currentStatus = Status.SIZE;
        }
        else {
            if (handler.mode == CopyMoveMode.COPY) {
                if (s != Status.EOFs && s != Status.ERR && s != Status.CFL && s != Status.SKIP)
                    throw new RuntimeException("Expected EOFs, ERR, CFL or SKIP here, protocol error, n is "+n);
            }
            handler.currentStatus = s;
        }
    }

    private static void publishAfterSizeReceived(ProgressConflictHandler handler, long n) throws IOException {
        handler.currentFileSize = n;
        handler.task.publishProgressWrapper(
                (int)Math.round(handler.currentFileCount*100.0/handler.totalFileCount),
                0
        );
    }

    // strategy for transition functions
    static fromStatusInterface fromEOF = handler -> {
        // TODO to be tested, increment total file count after receiving EOF
        handler.currentFileCount++;
        handler.totalSizeSoFar += handler.currentFileSize;

        // LEGACY
//        handler.task.publishProgressWrapper(
//                (int)Math.round(handler.currentFileCount*100.0/handler.totalFileCount),
//                0
//        );

        // NEW, uses total size info
        handler.task.publishProgressWrapper(
                (int)Math.round(handler.totalSizeSoFar*100.0/handler.totalSize),
                0
        );

        commonTransition(handler);
    };

    static fromStatusInterface fromEOFs = handler -> {
        Log.d("Progress", "Received EOFs, all done, exiting...");
        handler.copyRunning = false;
    };

    static fromStatusInterface fromCFL = handler -> {
//        byte cflType = handler.rs.i.readByte();
        String x = Misc.receiveStringWithLen(handler.rs.i);
        String y = Misc.receiveStringWithLen(handler.rs.i);

        // abuse of notation, it is just a file/dir type for src and dest
        ConflictType xtype = ConflictType.fromNumeric(handler.rs.i.readByte()); // == cflType
        ConflictType ytype = ConflictType.fromNumeric(handler.rs.i.readByte());

        Log.d("Progress", "Conflict type is: "+((int)xtype.getValue())+" "+ xtype.name());
        Log.d("Progress", "Conflicting paths are: "+x+" of type "+xtype.name()+", "+y+" of type"+ytype.name());

        // launch conflict decision dialog and wait for it to be dismissed
        MainActivity.mainActivity.runOnUiThread(()->new ConflictDialog(
                MainActivity.mainActivity,
                xtype,
                x,
                ytype,
                y,
                handler // to set taken decision and optionally new filename
        ).show());
        synchronized (ConflictDecision.m) {
            try {
                ConflictDecision.m.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        handler.rs.o.write(handler.lastDecision.getValue());
        if (handler.lastDecision == ConflictDecision.CD_REN_SRC ||
                handler.lastDecision == ConflictDecision.CD_REN_DEST) {
            Misc.sendStringWithLen(handler.rs.o,handler.lastNewName);
        }

        commonTransition(handler);
    };

    static fromStatusInterface fromSKIP = handler -> {
        long outerProgressIncrement = Misc.receiveTotalOrProgress(handler.rs.i);
        long totalSizeProgressIncrement = Misc.receiveTotalOrProgress(handler.rs.i);
        Log.d("Progress", "Outer progress increment for skip item is: "+outerProgressIncrement);
        Log.d("Progress", "Total size progress increment for skip item is: "+totalSizeProgressIncrement);
        handler.currentFileCount += outerProgressIncrement;
        handler.totalSizeSoFar += totalSizeProgressIncrement;

        // update outer progress in MovingRibbonTwoBars
        // LEGACY
//        handler.task.publishProgressWrapper(
//                (int)Math.round(handler.currentFileCount*100.0/handler.totalFileCount),
//                0
//        );

        // END
        handler.task.publishProgressWrapper(
                (int)Math.round(handler.totalSizeSoFar*100.0/handler.totalSize),
                0
        );

        commonTransition(handler);
    };

    static fromStatusInterface fromERR = handler -> {
        String x = Misc.receiveStringWithLen(handler.rs.i);
        String y = Misc.receiveStringWithLen(handler.rs.i);
        // @@@@@@@@@@@@@@@@@@@@@@@
        // abuse of notation, it is just a file/dir type for src and dest
        ConflictType xtype = ConflictType.fromNumeric(handler.rs.i.readByte());
        ConflictType ytype = ConflictType.fromNumeric(handler.rs.i.readByte());
        // @@@@@@@@@@@@@@@@@@@@@@@
        Log.d("Progress", "Error paths are: "+x+" of type "+xtype.name()+", "+y+" of type"+ytype.name());
        // TODO launch error decision dialog
        ErrorDecision dec = ErrorDecision.ED_CANCEL; // stub

        if (dec == ErrorDecision.ED_CANCEL) {
            Log.d("Progress", "Exiting copy on user cancel after error");
            handler.copyRunning = false;
            return;
        }
        handler.rs.o.write(dec.getValue());
        long n = Misc.receiveTotalOrProgress(handler.rs.i);
        Status s = Status.fromNumeric(n);
        if (n >= 0) {
            Log.d("Progress", "Size is: "+n);
            publishAfterSizeReceived(handler,n);
            handler.currentStatus = Status.SIZE;
        }
        else {
            if (s != Status.EOFs && s != Status.SKIP)
                throw new RuntimeException("Expected EOFs or SKIP here, protocol error");
            handler.currentStatus = Status.EOFs;
        }
    };

    static fromStatusInterface fromPROGRESS = handler -> {
        long n = Misc.receiveTotalOrProgress(handler.rs.i);
        if (n >= 0) {
            Log.d("Progress", "Progress is "+n);
            // update inner progress in MovingRibbonTwoBars
            // LEGACY
//            handler.task.publishProgressWrapper(
//                    (int)Math.round(handler.currentFileCount*100.0/handler.totalFileCount),
//                    (int)Math.round(n*100.0/handler.currentFileSize)
//            );
            // NEW, uses total size info
            handler.task.publishProgressWrapper(
                    (int)Math.round((handler.totalSizeSoFar+n)*100.0/handler.totalSize),
                    (int)Math.round(n*100.0/handler.currentFileSize)
            );

            handler.currentStatus = Status.PROGRESS;
        }
        else handler.currentStatus = Status.fromNumeric(n);
    };

    static fromStatusInterface fromSIZE = handler -> fromPROGRESS.from(handler);
    /////////////////////////////////////////

    public enum Status {
        EOF(-1L,fromEOF),
        EOFs(-2L, fromEOFs),
        CFL(-3L, fromCFL),
        ERR(-4L, fromERR),
        SKIP(-5L, fromSKIP),
        SIZE(null, fromSIZE),
        PROGRESS(null, fromPROGRESS);

        final Long status;
        final fromStatusInterface transition;

        Status(Long status, fromStatusInterface transition) {
            this.status = status;
            this.transition = transition;
        }

        public Long getStatus() {
            return status;
        }

        public fromStatusInterface getTransition() {
            return transition;
        }

        private static final Map<Long,Status> m = new HashMap<Long,Status>(){{
            for (Status s : Status.values()) put(s.getStatus(),s); // just ignore null key overwrite
        }};

        public static Status fromNumeric(long n) {
            return m.get(n);
        }
    }

    //////////////////////////////////////

    private final CopyMoveMode mode; // for dealing with EOF-after-EOF behaviour only in move mode

    private final StreamsPair rs;
    private final BaseBackgroundTask task;
    private boolean copyRunning = true;
    private Status currentStatus = Status.SIZE;

    private long currentFileCount = 0;
    private long currentFileSize;
    private final long totalFileCount;

    private final long totalSize;
    private long totalSizeSoFar = 0; // rounded to last completed file

    public String lastNewName;
    public ConflictDecision lastDecision;

    public ProgressConflictHandler(StreamsPair rs,
                                   BaseBackgroundTask task,
                                   long totalFileCount,
                                   long totalSize,
                                   CopyMoveMode mode) {
        this.rs = rs;
        this.task = task;
        this.totalFileCount = totalFileCount;
        this.totalSize = totalSize;
        this.mode = mode;
    }

    public void start() throws IOException {
        commonTransition(this); // needed to receive first size info as size and not as progress
        while(copyRunning)
            currentStatus.getTransition().from(this);

        // check global return value at the end, show toast if there were errors
        int globalRet = Misc.receiveBaseResponse(rs.i);
        if (globalRet != 0) {
            String errOrWarn = "There were errors during "+mode.name().toLowerCase()+", please check output files/dirs";
            if (mode == CopyMoveMode.MOVE) errOrWarn += "\nPlease be aware that conflict resolution and cross-device transfers are not implemented yet for "+mode.name()+" mode";
            MainActivity.showToastOnUIWithHandler(errOrWarn);
        }

    }
}

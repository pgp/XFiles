package it.pgp.xfiles.enums.conflicthandling;

import java.util.HashMap;
import java.util.Map;

import it.pgp.xfiles.R;

public enum ConflictDecision {
    CD_SKIP((byte)0x00, R.id.conflictSkip),
    CD_SKIP_ALL((byte)0x10,R.id.conflictSkipAll),
    CD_OVERWRITE((byte)0x01,R.id.conflictOverwrite),
    CD_OVERWRITE_ALL((byte)0x11,R.id.conflictOverwriteAll),
    CD_REN_SRC((byte)0x02,R.id.conflictRenameSrc),
    CD_REN_DEST((byte)0x03,R.id.conflictRenameDest),
    CD_REN_SRC_ALL((byte)0x12,R.id.conflictAutoRenameSrc),
    CD_REN_DEST_ALL((byte)0x13,R.id.conflictAutoRenameDest),

    CD_MERGE((byte)0x04,R.id.conflictMerge),
//    CD_MERGE_RECURSIVE((byte)0x14,R.id.conflictMergeRecursive), // not implemented yet
    CD_MERGE_ALL((byte)0x24,R.id.conflictMergeAll),

    CD_CANCEL((byte)0x05,R.id.conflictCancel),

    NO_PREV_DEC((byte)0xFF,-1);

    public static final Map<Integer,ConflictDecision> m = new HashMap<Integer,ConflictDecision>(){{
        for (ConflictDecision c: ConflictDecision.values()) put(c.res,c);
    }};

    public static ConflictDecision fromResource(int res) {
        return m.get(res);
    }

    byte i;
    int res;
    
    ConflictDecision(byte i, int res) {
        this.i = i;
        this.res = res;
    }

    public byte getValue() {
        return i;
    }

    public int getResource() {
        return 0;
    }
}

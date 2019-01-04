package it.pgp.xfiles.roothelperclient;

import java.util.List;

/**
 * Created by pgp on 17/02/18
 */

public class RelativeExtractEntries {
    public int stripPathLen;
    public List<Integer> entries;

    public RelativeExtractEntries(int stripPathLen, List<Integer> entries) {
        this.stripPathLen = stripPathLen;
        this.entries = entries;
    }
}

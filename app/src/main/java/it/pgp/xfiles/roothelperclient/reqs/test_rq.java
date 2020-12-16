package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import it.pgp.xfiles.roothelperclient.RelativeExtractEntries;

public class test_rq extends extract_rq {
    public test_rq(Object fx, @Nullable Object password, @Nullable RelativeExtractEntries entries) {
        super(fx, "", password, null, entries, false);
    }
}

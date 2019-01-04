package it.pgp.xfiles.roothelperclient.reqs;

import it.pgp.xfiles.roothelperclient.ControlCodes;

/**
 * Created by pgp on 25/01/17
 * Deprecated, no longer aligned with rh protocol, see {@link copylist_rq} instead
 */

@Deprecated
public class copy_rq extends PairOfPaths_rq {
    public copy_rq(Object fx, Object fy) {
        super(fx, fy);
        this.requestType = ControlCodes.ACTION_COPY;
    }
}

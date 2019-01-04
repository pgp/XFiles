package it.pgp.xfiles.roothelperclient.reqs;

import it.pgp.xfiles.roothelperclient.ControlCodes;

/**
 * Created by pgp on 25/01/17
 * Deprecated, no longer aligned with rh protocol, see {@link movelist_rq} instead
 */

@Deprecated
public class move_rq extends PairOfPaths_rq {
    public move_rq(Object fx, Object fy) {
        super(fx, fy);
        this.requestType = ControlCodes.ACTION_MOVE;
    }
}

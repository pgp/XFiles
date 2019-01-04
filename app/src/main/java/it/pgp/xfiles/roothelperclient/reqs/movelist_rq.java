package it.pgp.xfiles.roothelperclient.reqs;

import java.util.List;

import it.pgp.xfiles.roothelperclient.ControlCodes;

/**
 * Created by pgp on 19/07/17
 */

public class movelist_rq extends ListOfPathPairs_rq {

    public movelist_rq(List<String> v_fx, List<String> v_fy) {
        super(v_fx, v_fy);
        requestType = ControlCodes.ACTION_MOVE;
    }
}

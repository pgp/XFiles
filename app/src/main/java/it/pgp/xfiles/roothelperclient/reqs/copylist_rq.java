package it.pgp.xfiles.roothelperclient.reqs;

import java.util.List;

import it.pgp.xfiles.roothelperclient.ControlCodes;

/**
 * Created by pgp on 19/07/17
 */

public class copylist_rq extends ListOfPathPairs_rq {
    public copylist_rq(List<String> v_fx, List<String> v_fy) {
        super(ControlCodes.ACTION_COPY, v_fx, v_fy);
    }
}

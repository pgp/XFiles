package it.pgp.xfiles.roothelperclient.reqs;

import it.pgp.xfiles.roothelperclient.ControlCodes;

/**
 * Created by pgp on 25/01/17
 */

public class del_rq extends SinglePath_rq {
    public del_rq(Object pathname) {
        super(ControlCodes.ACTION_DELETE, pathname);
    }
}

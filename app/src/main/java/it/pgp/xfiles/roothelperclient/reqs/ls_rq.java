package it.pgp.xfiles.roothelperclient.reqs;

import it.pgp.xfiles.roothelperclient.ControlCodes;

public class ls_rq extends SinglePath_rq {

    public ls_rq(Object dirPath)  {
        super(ControlCodes.ACTION_LS, dirPath);
    }
}
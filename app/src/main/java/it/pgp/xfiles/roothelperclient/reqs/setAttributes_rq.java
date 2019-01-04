package it.pgp.xfiles.roothelperclient.reqs;

import it.pgp.xfiles.roothelperclient.ControlCodes;

/**
 * Created by pgp on 22/11/17
 */

public abstract class setAttributes_rq extends SinglePath_rq {

    public static final int bitOffsetForSubrequest = 6;

    public enum SubRequest {
        SET_DATES,
        SET_OWNERSHIP,
        SET_PERMISSIONS
    }

    public int additionalByte = 0;

    public setAttributes_rq(Object pathname) {
        super(pathname);
        requestType = ControlCodes.ACTION_SETATTRIBUTES;
    }
}

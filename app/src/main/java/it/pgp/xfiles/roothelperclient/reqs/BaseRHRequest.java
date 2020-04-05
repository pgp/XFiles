package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import it.pgp.xfiles.roothelperclient.ControlCodes;

public abstract class BaseRHRequest {

    static final Charset UTF8 = StandardCharsets.UTF_8;

    public final ControlCodes requestType;

    public BaseRHRequest(ControlCodes requestType) {
        this.requestType = requestType;
    }

    public byte getRequestByteWithFlags() {
        return requestType.getValue();
    }

    public abstract void write(OutputStream outputStream) throws IOException;

}

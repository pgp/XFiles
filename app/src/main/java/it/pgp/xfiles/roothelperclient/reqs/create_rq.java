package it.pgp.xfiles.roothelperclient.reqs;

import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 31/01/17
 */

public class create_rq extends SinglePath_rq {
    final FileMode fileMode;
    final FileCreationAdvancedOptions fileOptions;

    public create_rq(Object pathname, FileMode fileMode) {
        super(pathname);
        this.fileMode = fileMode;
        this.requestType = ControlCodes.ACTION_CREATE;
        this.fileOptions = null;
    }

    // only for file creation requests
    public create_rq(Object pathname, FileCreationAdvancedOptions fileOptions) {
        super(pathname);
        this.fileMode = FileMode.FILE;
        this.requestType = ControlCodes.ACTION_CREATE;
        this.fileOptions = fileOptions;
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        // customize with flag bits
        rq ^= ((fileMode == FileMode.FILE ?1:0) << (rq_bit_length));
        if (fileOptions != null && fileMode == FileMode.FILE)
            rq ^= (2 << rq_bit_length);
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            // write request byte
            nbf.write(getRequestByteWithFlags());
            // write len and field
            nbf.write(Misc.castUnsignedNumberToBytes(this.pathname_len,2));
            nbf.write(this.pathname);
            // write mode
            nbf.write(Misc.castUnsignedNumberToBytes(fileMode.getDefaultMask(),4));
            if(fileOptions != null) nbf.write(fileOptions.toRootHelperRequestOptions());
        }
    }
}

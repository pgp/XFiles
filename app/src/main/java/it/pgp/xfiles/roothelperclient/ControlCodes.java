package it.pgp.xfiles.roothelperclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pgp on 21/01/17
 * (corresponding to common_uds.h in C roothelper project)
 */

public enum ControlCodes {
    // these are to be considered 5-bit opcodes, most significant 3 bits (when set) are option flags
    // requests (5 bit)
    ACTION_LS((byte)0x01), // list or list archive, depending on flags (000 or 111)
    ACTION_MOVE((byte)0x02),
    ACTION_COPY((byte)0x03),
    ACTION_DELETE((byte)0x04),
    ACTION_STATS((byte)0x05),
    ACTION_COMPRESS((byte)0x06),
    ACTION_EXTRACT((byte)0x07),
    ACTION_EXISTS((byte)0x08), // exists/is file/is directory
    ACTION_CREATE((byte)0x09), // create file/directory
    ACTION_HASH((byte)0x0A),
    ACTION_FIND((byte)0x0B),
    ACTION_KILL((byte)0x0C), // for killing long-term roothelper processes from main roothelper process
    ACTION_GETPID((byte)0x0D), // for probing connection and getting RH server pid for later sending kill signals from the main RH server instance (via ACTION_KILL request)
//    ACTION_FORK((byte)0x0E),
    ACTION_FILEIO((byte)0x0F),

    ACTION_DOWNLOAD((byte)0x10),
    ACTION_UPLOAD((byte)0x11),

    REMOTE_SERVER_MANAGEMENT((byte)0x12), // flags: 000: stop, 111: start, 101: start with announce, 010: get status

    REMOTE_CONNECT((byte)0x14),

    ACTION_SETATTRIBUTES((byte)0x15), // embeds set ownership, permissions and dates actions

    ACTION_SSH_KEYGEN((byte)0x16),

    ACTION_LINK((byte)0x17),

    ACTION_HTTPS_URL_DOWNLOAD((byte)0x18),

//    ACTION_CANCEL((byte)0x1E),
    ACTION_EXIT((byte)0x1F);

    final byte value;
    static final Map<Byte,ControlCodes> codeMap = new HashMap<Byte,ControlCodes>(){{
        for (ControlCodes x: ControlCodes.values())
            put(x.getValue(),x);
    }};

    ControlCodes(byte value) {
        this.value = value;
    }

    // enum value to byte value
    public byte getValue() {
        return value;
    }

    // byte value to enum value
    public static ControlCodes getCode(byte value) {
        return codeMap.get(value);
    }

}

package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;

import it.pgp.xfiles.utils.Misc;

public class singleStats_resp {

    private int group_len;
    public byte [] group;
    private int owner_len;
    public byte [] owner;
    public long creationTime;
    public long lastAccessTime;
    public long modificationTime;
    public byte [] permissions;
    public long size;

    public singleStats_resp(DataInputStream inputStream) throws IOException {
        byte[] tmp;
        tmp = new byte[1];
        inputStream.readFully(tmp);
        this.group_len = (int) Misc.castBytesToUnsignedNumber(tmp,1);
        this.group = new byte[group_len];
        inputStream.readFully(this.group);
        tmp = new byte[1];
        inputStream.readFully(tmp);
        this.owner_len = (int) Misc.castBytesToUnsignedNumber(tmp,1);
        this.owner = new byte[owner_len];
        inputStream.readFully(this.owner);
        tmp = new byte[4];
        inputStream.readFully(tmp);
        this.creationTime = Misc.castBytesToUnsignedNumber(tmp,4);
        tmp = new byte[4];
        inputStream.readFully(tmp);
        this.lastAccessTime = Misc.castBytesToUnsignedNumber(tmp,4);
        tmp = new byte[4];
        inputStream.readFully(tmp);
        this.modificationTime = Misc.castBytesToUnsignedNumber(tmp,4);
        tmp = new byte[10];
        inputStream.readFully(tmp);
        this.permissions = tmp;
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.size = Misc.castBytesToUnsignedNumber(tmp,8);
    }

    public singleStats_resp(byte[] group, byte[] owner, long creationTime, long lastAccessTime, long modificationTime, byte[] permissions, long size)  {
        this.group_len = group.length;
        this.group = group;
        this.owner_len = owner.length;
        this.owner = owner;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
        this.modificationTime = modificationTime;
        this.permissions = permissions;
        this.size = size;
    }
}
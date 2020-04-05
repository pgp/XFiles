package it.pgp.xfiles.items;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;

/**
 * Created by pgp on 14/07/17
 */

public class SingleStatsItem {

    public String group;
    public String owner;
    public Date creationTime;
    public Date lastAccessTime;
    public Date modificationTime;
    public String permissions;
    public long size;
    public boolean isDir;

    public SingleStatsItem(String group, String owner, Date creationTime, Date lastAccessTime, Date modificationTime, String permissions, long size) {
        this.group = group;
        this.owner = owner;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
        this.modificationTime = modificationTime;
        this.permissions = permissions;
        this.size = size;
    }

    public SingleStatsItem(singleStats_resp r) {
        group = new String(r.group, StandardCharsets.UTF_8);
        owner = new String(r.owner, StandardCharsets.UTF_8);
        creationTime = new Date(r.creationTime*1000L);
        lastAccessTime = new Date(r.lastAccessTime*1000L);
        modificationTime = new Date(r.modificationTime*1000L);
        permissions = new String(r.permissions,StandardCharsets.UTF_8);
        isDir = permissions.charAt(0) == 'd';
        size = r.size;
    }
}

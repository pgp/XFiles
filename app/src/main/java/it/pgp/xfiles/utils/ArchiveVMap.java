package it.pgp.xfiles.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pgp on 23/05/17
 */

public class ArchiveVMap extends VMap {
    public String password; // for avoiding re-asking password on extract if archive has encrypted filenames
    public static final String sentinelKeyForNodeProperties = ""; // in practically every filesystem, filenames cannot be empty

    public ArchiveVMap() {
        super();
    }

    public ArchiveVMap(String password) {
        super();
        this.password = password;
    }

    public Object getByPath(String inArchivePath) throws ValueAsKeyException {
        return get(inArchivePath.split("/"));
    }

    public Map getNodeProps(String inArchivePath) throws ValueAsKeyException {
        Map nodeProps = (Map) get(Misc.concatAll(inArchivePath.split("/"),
                new String[]{ArchiveVMap.sentinelKeyForNodeProperties}));
        // the directory node need not necessarily be present in an archive
        if(nodeProps==null) {
            nodeProps = new HashMap();
            nodeProps.put("date",new Date(0));
            nodeProps.put("size",0L);
            nodeProps.put("isDir",true);
        }
        return nodeProps;
    }
}

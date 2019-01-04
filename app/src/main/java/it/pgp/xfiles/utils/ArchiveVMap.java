package it.pgp.xfiles.utils;

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
        return (Map) get(Misc.concatAll(inArchivePath.split("/"),
                new String[]{ArchiveVMap.sentinelKeyForNodeProperties}));
    }

    // TODO modify method to add node properties for implicitly stored folder paths (clone Date from leaf node, size = 0, isdir = true)
//    @Override
//    public void set(Object v, Object... keys) throws ValueAsKeyException {
//        if (h==null) h = new HashMap<>();
//        HashMap<Object,Object> currentLevelMap = h;
//
//        for (int i=0;i<keys.length-1;i++) {
//            if (currentLevelMap.get(keys[i]) == null) {
//                currentLevelMap.put(keys[i],new HashMap<>());
//            }
//            try {
//                currentLevelMap = (HashMap<Object, Object>) currentLevelMap.get(keys[i]);
//            }
//            catch (Exception e) {
//                throw new ValueAsKeyException();
//            }
//        }
//
//        currentLevelMap.put(keys[keys.length-1],v);
//    }
}

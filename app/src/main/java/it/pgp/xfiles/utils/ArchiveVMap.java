package it.pgp.xfiles.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.FindActivity;
import it.pgp.xfiles.adapters.FindResultsAdapter;

/**
 * Created by pgp on 23/05/17
 */

public class ArchiveVMap extends VMap {
//    public String password; // for avoiding re-asking password on extract if archive has encrypted filenames
    public static final String sentinelKeyForNodeProperties = ""; // in practically every filesystem, filenames cannot be empty

    public ArchiveVMap() {
        super();
    }

//    public ArchiveVMap(String password) {
//        super();
//        this.password = password;
//    }

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

//    public interface VisitNodeInterface<T> {
//        T visit(String path);
//    }

    private static void dfsPaths(Map<String,Object> m, String recursivePrefix, Predicate<String> matcher) {
        for(Map.Entry<String,Object> x : m.entrySet()) {
            String k = x.getKey();
            if(sentinelKeyForNodeProperties.equals(k)) continue; // exclude node props
            Object v = x.getValue();
//            System.out.println(recursivePrefix+"/"+k); // here we have the full path within archive
//            T item = vni.visit(recursivePrefix+"/"+k);
//            if(item != null) return item; // found

            // FIXME it would be much better than the condition on recursivePrefix, to add a sanitize/trim slashes method in BasePathContent hierarchy
            String joinedPath = (recursivePrefix==null || recursivePrefix.isEmpty())?k:recursivePrefix+"/"+k;
            if(matcher.test(k)) {
                Map<String,?> nodeProps = (Map)((Map)v).get(sentinelKeyForNodeProperties);
                BrowserItem b = new BrowserItem(joinedPath,
                        (Long)nodeProps.get("size"),
                        (Date)nodeProps.get("date"),
                        (Boolean) nodeProps.get("isDir"),
                        false);

                try {FindActivity.instance.runOnUiThread(() -> FindResultsAdapter.instance.add(b));}
                catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            if(recursivePrefix != null && v instanceof Map)
                dfsPaths((Map<String, Object>) v,joinedPath, matcher);
        }
    }

    public void findInArchive(Predicate<String> matcher, String recursivePrefix) {
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//        BrowserItem b = (BrowserItem) visitPaths((Map)h,"",p->{
//            if(p.contains(namePattern)) return new BrowserItem(p,0,new Date(),false,false);
//            else return null;
//        });
//        System.out.println(b);
        dfsPaths((Map)h,recursivePrefix, matcher);
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }
}

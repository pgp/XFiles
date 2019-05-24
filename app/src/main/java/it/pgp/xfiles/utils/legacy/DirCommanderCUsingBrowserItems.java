package it.pgp.xfiles.utils.legacy;

import java.util.HashMap;
import java.util.Map;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 01/10/2016
 */
@Deprecated
public class DirCommanderCUsingBrowserItems {
    private Map<Integer,String> recentDirs;
    private Map<Integer,Integer> previousListViewPositions; // position of list view when previous directory was listed
    private int currentIndex;

    private void truncateListMaps(int maxIndex) {
        Map<Integer,String> tmp1 = new HashMap<>();
        for (Integer i : recentDirs.keySet()) {
            if (i <= maxIndex) tmp1.put(i,recentDirs.get(i));
        }
        Map<Integer,Integer> tmp2 = new HashMap<>();
        for (Integer i : previousListViewPositions.keySet()) {
            if (i <= maxIndex) tmp2.put(i,previousListViewPositions.get(i));
        }
        recentDirs = tmp1;
        previousListViewPositions = tmp2;
    }

    // responsibilities:
    /*
    on refresh, returns current dir with content, without setting list view position
     */
    private DirWithContentUsingBrowserItems validateDirAccess(String dir) {
//        File[] content = new File(dir).listFiles();
//        if (content == null) return null;
//        List<BrowserItem> l = new ArrayList<>();
//        for (File f : content) {
//            l.add(new BrowserItem(f.getName(),f.length(),new Date(f.lastModified()),f.isDirectory()));
//        }
//        return new DirWithContentUsingBrowserItems(dir, l);

        // commented to remove compile-time errors while avoiding deleting the class
//        if (dir.startsWith("/")) {
//            // ensure to use the correct helper (local, standard or root)
//            // current helper setting is done on successful list return from each helper
//            if (MainActivity.usingRootHelperForLocal) {
//                return MainActivity.rootHelperClient.listDirectory(dir);
//            }
//            else {
//                return MainActivity.xFilesUtils.listDirectory(dir);
//            }
//        }
//        else if (dir.startsWith("sftp://")) {
//            return MainActivity.sftpProvider.listDirectory(dir);
//        }
//        else return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.MALFORMED_PATH_ERROR);
        return null;
    }

    // same as before, returns data provider instead of calling list dir
    public static ProviderType getProviderFromPath(String dir) {
        if (dir.startsWith("/")) {
            return ProviderType.LOCAL;
        }
        else if (dir.startsWith("sftp://")) {
            return ProviderType.SFTP;
        }
        else throw new RuntimeException("Unknown data provider");
    }

    public DirCommanderCUsingBrowserItems(String startingDir) {
        currentIndex = 0;
        recentDirs = new HashMap<>();
        previousListViewPositions = new HashMap<>();
        recentDirs.put(currentIndex,startingDir); // expecting valid dir at least on start
        // previousListViewPositions.add(0); // placeholder, no previous dir on start
    }

    public String getCurrentDirectoryPathname() {
        return recentDirs.get(currentIndex);
    }

    public DirWithContentUsingBrowserItems goBack(int previousPosition) {
        DirWithContentUsingBrowserItems cwd;
        // asks for previous dir in command; commander object updates its state and returns previous dir
        if (recentDirs == null || recentDirs.size()==0) // guard block
            throw new RuntimeException("Commander not initialized correctly");

        if (currentIndex==0) // no previous dir (assume you cannot delete the folder you're in), also do not set previous positions
           return validateDirAccess(recentDirs.get(0));

        cwd = validateDirAccess(recentDirs.get(currentIndex-1));
        if (cwd == null || cwd.errorCode != null)
            return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.COMMANDER_CANNOT_GO_BACK);
        cwd.listViewPosition = previousListViewPositions.get(currentIndex-1);

        // set current position
        previousListViewPositions.put(currentIndex,previousPosition);

        currentIndex--;
        return cwd;
    }

    public DirWithContentUsingBrowserItems refresh() {
        // refresh can only be done at the beginning of the list view (scroll down gesture)
        DirWithContentUsingBrowserItems cwd = validateDirAccess(recentDirs.get(currentIndex));
        if (cwd == null || cwd.errorCode != null)
            return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.COMMANDER_CANNOT_REFRESH);
        return cwd;
    }

    public DirWithContentUsingBrowserItems goAhead(int previousPosition) {
        if (recentDirs.size()==currentIndex+1) // cannot go ahead, already last item of commander
            return validateDirAccess(recentDirs.get(currentIndex));

        DirWithContentUsingBrowserItems cwd = validateDirAccess(recentDirs.get(currentIndex+1));

        if (cwd == null || cwd.errorCode != null) // cannot go ahead (dir not found, IO error)
            return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.COMMANDER_CANNOT_GO_AHEAD);
        cwd.listViewPosition = previousListViewPositions.get(currentIndex+1); // may be null

        // set current positions
        previousListViewPositions.put(currentIndex,previousPosition);

        currentIndex++;
        return cwd;
    }

    public DirWithContentUsingBrowserItems setDir(String dir, int previousPosition) {
        DirWithContentUsingBrowserItems cwd;
        if (recentDirs.size()<currentIndex+1) // guard block
            throw new RuntimeException("Commander error");

        cwd = validateDirAccess(dir);
        if (cwd.errorCode != null)
            return cwd;

        if (recentDirs.size()>currentIndex+1) {
            // resize array list, set new element (currentIndex remains unchanged)
            truncateListMaps(currentIndex);
        }
        previousListViewPositions.put(currentIndex,previousPosition);
        currentIndex++;
        recentDirs.put(currentIndex,dir);

        return cwd;
    }

}

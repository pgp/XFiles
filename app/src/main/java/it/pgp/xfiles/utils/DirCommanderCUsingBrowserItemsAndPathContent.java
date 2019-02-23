package it.pgp.xfiles.utils;

import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.exceptions.DirCommanderException;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.LocalDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;

/**
 * Created by pgp on 13/05/2017 - adapted from DirCommanderCUsingBrowserItems
 */
public class DirCommanderCUsingBrowserItemsAndPathContent {
    private HashMap<Integer,BasePathContent> recentDirs;
    private HashMap<Integer,Integer> previousListViewPositions; // position of list view when previous directory was listed
    private int currentIndex;

    // for cleanup of old commander entries when a series of goBack commands is followed by a goDir
    private void truncateListMaps(int maxIndex) {
        HashMap<Integer,BasePathContent> tmp1 = new HashMap<>();
        for (Integer i : recentDirs.keySet()) {
            if (i <= maxIndex) tmp1.put(i,recentDirs.get(i));
        }
        HashMap<Integer,Integer> tmp2 = new HashMap<>();
        for (Integer i : previousListViewPositions.keySet()) {
            if (i <= maxIndex) tmp2.put(i,previousListViewPositions.get(i));
        }
        recentDirs = tmp1;
        previousListViewPositions = tmp2;
    }

    private GenericDirWithContent validateDirAccess(BasePathContent dir) {
        switch (dir.providerType) {
            case LOCAL:
            case XFILES_REMOTE:
                // ensure to use the correct helper (local, standard or root)
                // current helper setting is done on successful list return from each helper
                return MainActivity.usingRootHelperForLocal?
                        MainActivity.getRootHelperClient().listDirectory(dir):
                        MainActivity.xFilesUtils.listDirectory(dir);
            case LOCAL_WITHIN_ARCHIVE:
//                return MainActivity.currentHelper.listArchive(dir);
                return MainActivity.getRootHelperClient().listArchive(dir);
            case SFTP:
                return MainActivity.sftpProvider.listDirectory(dir);
            case SMB:
                return MainActivity.smbProvider.listDirectory(dir);
            default: // URL_DOWNLOAD is not a goDir label
                throw new RuntimeException("Invalid BasePathContent subclass type");
        }
    }

    // same as before, returns data provider instead of calling list dir
    public static ProviderType getProviderFromPath(String dir) {
        if (dir.startsWith("/")) {
            return ProviderType.LOCAL;
        }
        else if (dir.startsWith("sftp://")) {
            return ProviderType.SFTP;
        }
        else if (dir.startsWith("xre://")) {
            return ProviderType.XFILES_REMOTE;
        }
        else if (dir.startsWith("smb://")) {
            return ProviderType.SMB;
        }
        else throw new RuntimeException("Unknown data provider");
    }

    public DirCommanderCUsingBrowserItemsAndPathContent(BasePathContent startingDir) throws DirCommanderException {
        currentIndex = 0;
        recentDirs = new HashMap<>();
        previousListViewPositions = new HashMap<>();
        recentDirs.put(currentIndex,startingDir); // expecting valid dir at least on start
        // previousListViewPositions.add(0); // placeholder, no previous dir on start

        GenericDirWithContent dwc = refreshFailFast();
        if (dwc == null || dwc.errorCode != null) throw new DirCommanderException();
    }

    public DirCommanderCUsingBrowserItemsAndPathContent(BasePathContent startingDir, BasePathContent fallbackDir) throws DirCommanderException {
        currentIndex = 0;
        recentDirs = new HashMap<>();
        previousListViewPositions = new HashMap<>();
        recentDirs.put(currentIndex,startingDir);
        GenericDirWithContent dwc = refreshFailFast();
        if (dwc != null && dwc.errorCode == null) return;

        recentDirs.put(currentIndex,fallbackDir);
        dwc = refreshFailFast();
        if (dwc == null || dwc.errorCode != null) throw new DirCommanderException("Unable to set neither standard nor fallback path in dir commander");
    }

    public BasePathContent getCurrentDirectoryPathname() {
        return recentDirs.get(currentIndex);
    }


    public GenericDirWithContent goBack(int previousPosition) {
        GenericDirWithContent cwd;
        // asks for previous dir in command; commander object updates its state and returns previous dir
        if (recentDirs == null || recentDirs.size()==0) // guard block
            throw new RuntimeException("Commander not initialized correctly");

        if (currentIndex==0) // no previous dir (assume you cannot delete the folder you're in), also do not set previous positions
           return validateDirAccess(recentDirs.get(0));

        cwd = validateDirAccess(recentDirs.get(currentIndex-1));
        if (cwd == null || cwd.errorCode != null)
            return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_GO_BACK);
        cwd.listViewPosition = previousListViewPositions.get(currentIndex-1);

        // set current position
        previousListViewPositions.put(currentIndex,previousPosition);

        currentIndex--;
        return cwd;
    }

    public GenericDirWithContent refreshFailFast() {
        // refresh can only be done at the beginning of the list view (scroll down gesture)
        GenericDirWithContent cwd = validateDirAccess(recentDirs.get(currentIndex));
        if (cwd == null || cwd.errorCode != null)
            return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_REFRESH);
        return cwd;
    }

    public GenericDirWithContent refresh() {
        GenericDirWithContent cwd;
        int startIndex = currentIndex;

        do {
            cwd = validateDirAccess(recentDirs.get(currentIndex));
            if (cwd != null && cwd.errorCode == null) break;
            currentIndex--;
        } while (currentIndex >= 0);

        if (currentIndex < 0) {
            Toast.makeText(MainActivity.mainActivity, "Current dir was no longer available, unable to go back even to start folder, exiting...", Toast.LENGTH_SHORT).show();

            currentIndex = 0;
            recentDirs.put(currentIndex,new LocalPathContent("/"));
            MainActivity.mainActivity.finishAffinity();
            return new LocalDirWithContent("/",new ArrayList<>()); // dummy return object, just to avoid NPE in Collections.sort before finishAffinity is actually called
        }

        if (currentIndex != startIndex) {
            truncateListMaps(currentIndex);
            Toast.makeText(MainActivity.mainActivity, "Current dir is no longer available, went back of "+(startIndex-currentIndex)+" positions", Toast.LENGTH_SHORT).show();
        }
        return cwd;
    }

    public GenericDirWithContent goAhead(int previousPosition) {
        if (recentDirs.size()==currentIndex+1) // cannot go ahead, already last item of commander
            return validateDirAccess(recentDirs.get(currentIndex));

        GenericDirWithContent cwd = validateDirAccess(recentDirs.get(currentIndex+1));

        if (cwd == null || cwd.errorCode != null) // cannot go ahead (dir not found, IO error)
            return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_GO_AHEAD);
        cwd.listViewPosition = previousListViewPositions.get(currentIndex+1); // may be null

        // set current positions
        previousListViewPositions.put(currentIndex,previousPosition);

        currentIndex++;
        return cwd;
    }

    public GenericDirWithContent setDir(BasePathContent dir, int previousPosition) {
        GenericDirWithContent cwd;
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

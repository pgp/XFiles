package it.pgp.xfiles.utils.legacy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.utils.DirWithContent;

/**
 * Created by pgp on 01/10/2016
 */

@Deprecated
public class DirCommanderC {
    protected List<File> recentDirs;
    protected int currentIndex;

    protected DirWithContent validateDirAccess(File dir) {
        File[] content = dir.listFiles();
        if (content == null) return null;
        return new DirWithContent(dir,content);
    }

    public DirCommanderC(File startingDir) {
        recentDirs = new ArrayList<>();
        recentDirs.add(startingDir); // expecting valid dir at least on start
        currentIndex = 0;
    }

    public File getCurrentDirectoryFile() {
        return recentDirs.get(currentIndex);
    }

    public DirWithContent goBack() throws Exception {
        DirWithContent cwd;
        // asks for previous dir in command; commander object updates its state and returns previous dir
        if (recentDirs == null || recentDirs.size()==0) // guard block
            throw new Exception("Commander not initialized correctly");

        if (currentIndex==0) // no previous dir (assume you cannot delete the folder you're in)
           return validateDirAccess(recentDirs.get(0));

        cwd = validateDirAccess(recentDirs.get(currentIndex-1));
        if (cwd == null) throw new IOException("Cannot go back");

        currentIndex--;
        return cwd;
    }

    public DirWithContent refresh() throws IOException {
        DirWithContent cwd = validateDirAccess(recentDirs.get(currentIndex));
        if (cwd == null)
            throw new IOException("Cannot refresh");

        return cwd;
    }

    public DirWithContent goAhead() throws IOException {
        if (recentDirs.size()==currentIndex+1) // cannot go ahead, already last item of commander
            return validateDirAccess(recentDirs.get(currentIndex));

        DirWithContent cwd = validateDirAccess(recentDirs.get(currentIndex+1));

        if (cwd == null) // cannot go ahead (dir not found, IO error)
            throw new IOException("Cannot go ahead");

        currentIndex++;
        return cwd;
    }

    public DirWithContent setDir(File dir) throws Exception {
        DirWithContent cwd;
        if (recentDirs.size()<currentIndex+1) // guard block
            throw new Exception("Commander error");

        cwd = validateDirAccess(dir);
        if (cwd == null)
            throw new IOException("Cannot list directory content");

        if (recentDirs.size()>currentIndex+1) {
            // resize array list, set new element (currentIndex remains unchanged)
            recentDirs = recentDirs.subList(0,currentIndex+1);
        }
        recentDirs.add(dir);
        currentIndex++;

        return cwd;
    }

}

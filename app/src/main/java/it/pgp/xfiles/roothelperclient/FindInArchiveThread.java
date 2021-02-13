package it.pgp.xfiles.roothelperclient;

import it.pgp.xfiles.utils.ArchiveVMap;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;

public class FindInArchiveThread extends FindUpdatesThread {
    private final ArchiveVMap m;
    private final String namePattern;
    private final boolean recursiveSearch;
    private final boolean caseInsensitive;

    @Override
    protected String getErrMsg() {
        return "Generic error in "+getClass().getName();
    }

    public FindInArchiveThread(ArchivePathContent basePath, String namePattern, boolean recursiveSearch, boolean caseInsensitive) {
        super(null, basePath);
        this.m = RootHelperClient.archiveMRU.getByPath(basePath.archivePath,null);
        if(this.m==null) throw new RuntimeException("archive mru item should be present at this point");
        this.namePattern = namePattern;
        this.recursiveSearch = recursiveSearch; // if false, just retrieve the subtree map at the given path key, and loop over map keys
        this.caseInsensitive = caseInsensitive;
    }

    private boolean matchFilename(String filename) {
        // TODO here it would be simple to match using regex and globbing, adapt logic from bulk rename
        return (!caseInsensitive && filename.contains(namePattern)) ||
                (caseInsensitive && filename.toUpperCase().contains(namePattern.toUpperCase()));
    }

    @Override
    protected void doFind() {
        m.findInArchive(this::matchFilename,recursiveSearch?"":null);
    }
}


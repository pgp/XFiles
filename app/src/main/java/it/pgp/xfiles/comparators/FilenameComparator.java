package it.pgp.xfiles.comparators;

import java.util.Comparator;

import it.pgp.xfiles.BrowserItem;

/**
 * Created by pgp on 26/10/16
 */

public class FilenameComparator implements Comparator<BrowserItem> {
    @Override
    public int compare(BrowserItem o1, BrowserItem o2) {
        // directory priority (directories first, then files)
        if ((o1.isDirectory && o2.isDirectory)||(!o1.isDirectory && !o2.isDirectory))
            return o1.getFilename().compareTo(o2.getFilename()); // both files or both dirs
        else if (!o1.isDirectory /* && o2.isDirectory */) return 1;
        else return -1;
    }
}

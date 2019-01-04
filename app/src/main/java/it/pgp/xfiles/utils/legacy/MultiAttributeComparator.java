package it.pgp.xfiles.utils.legacy;

import java.util.Comparator;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.comparators.AdvancedComparator;
import it.pgp.xfiles.enums.ComparatorField;

/**
 * Created by pgp on 26/10/16
 * to be deprecated, replace with {@link AdvancedComparator}
 */

@Deprecated
public class MultiAttributeComparator implements Comparator<BrowserItem> {
    int reverse;
    int priorityDirs;
    ComparatorField whichAttribute;
    // TODO reversed is not used yet
    public MultiAttributeComparator(boolean priorityForDirectories,ComparatorField whichAttribute, boolean reversed) {
        reverse = reversed?-1:1;
        priorityDirs = priorityForDirectories?1:0; // FIXME
        this.whichAttribute = whichAttribute;
    }

    @Override
    public int compare(BrowserItem o1, BrowserItem o2) {
        if(priorityDirs==0)
            switch (whichAttribute) {
                case FILENAME:
                    return o1.getFilename().compareTo(o2.getFilename());
                case DATE:
                    return o1.date.compareTo(o2.date);
                case SIZE:
                    return o1.size.compareTo(o2.size);
                case TYPE:
                    return o1.getFileExt().compareTo(o2.getFileExt());
                default:
                    return 0; // unreachable statement
            }
        else
            switch (whichAttribute) {
                case FILENAME:
                    if ((o1.isDirectory && o2.isDirectory)||(!o1.isDirectory && !o2.isDirectory)) {
                        return o1.getFilename().compareTo(o2.getFilename()); // both files or both dirs
                    }
                    else if (!o1.isDirectory && o2.isDirectory) return 1;
                    else return -1;
                case DATE:
                    if ((o1.isDirectory && o2.isDirectory)||(!o1.isDirectory && !o2.isDirectory)) {
                        return o1.date.compareTo(o2.date); // both files or both dirs
                    }
                    else if (!o1.isDirectory && o2.isDirectory) return 1;
                    else return -1;
                case SIZE:
                    if ((o1.isDirectory && o2.isDirectory)||(!o1.isDirectory && !o2.isDirectory)) {
                        return o1.size.compareTo(o2.size); // both files or both dirs
                    }
                    else if (!o1.isDirectory && o2.isDirectory) return 1;
                    else return -1;
                case TYPE:
                    if ((o1.isDirectory && o2.isDirectory)||(!o1.isDirectory && !o2.isDirectory)) {
                        return o1.getFileExt().compareTo(o2.getFileExt()); // both files or both dirs
                    }
                    else if (!o1.isDirectory && o2.isDirectory) return 1;
                    else return -1;
                default:
                    return 0; // unreachable statement
            }
    }
}

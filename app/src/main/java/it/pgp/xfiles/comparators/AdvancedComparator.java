package it.pgp.xfiles.comparators;

import java.util.Comparator;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.SortingItem;

/**
 * Created by pgp on 27/10/16
 * Last modified on 30/10/16
 */

public class AdvancedComparator implements Comparator<BrowserItem> {

    private SortingItem[] attributes; // sorting is sequential w.r.t. these attributes

    public AdvancedComparator(SortingItem... attributes) {
        this.attributes = attributes;
    }

    @Override
    public int compare(BrowserItem o1, BrowserItem o2) {
        int currentComparisonResult = 0;

        for (SortingItem a : attributes) {
            switch (a.comparatorField) {
                case FILENAME:
                    currentComparisonResult = o1.getFilename().compareTo(o2.getFilename());
                    if (a.isReversed()) currentComparisonResult*=-1;
                    if (currentComparisonResult == 0) continue;
                    else return currentComparisonResult;
                case DATE:
                    currentComparisonResult = o1.date.compareTo(o2.date);
                    if (a.isReversed()) currentComparisonResult*=-1;
                    if (currentComparisonResult == 0) continue;
                    else return currentComparisonResult;
                case SIZE:
                    currentComparisonResult = o1.size.compareTo(o2.size);
                    if (a.isReversed()) currentComparisonResult*=-1;
                    if (currentComparisonResult == 0) continue;
                    else return currentComparisonResult;
                case TYPE:
                    boolean d1 = o1.isDirectory;
                    boolean d2 = o2.isDirectory;

                    // use directory/file comparison logic if both items have no extension
                    // (this in order to prevent mixing folders and files without extension)
                    if(d1 && !d2) currentComparisonResult = -1;
                    else if(!d1 && d2) currentComparisonResult = 1;
                    else /*if((!d1 && !d2) || (d1 && d2))*/ // both files or both directories
                        currentComparisonResult = o1.getFileExt().compareTo(o2.getFileExt());
                    if (a.isReversed()) currentComparisonResult*=-1;
                    if (currentComparisonResult == 0) continue;
                    else return currentComparisonResult;
                case DIR:
                    currentComparisonResult = o1.isDirectory.compareTo(o2.isDirectory);
                    if (!a.isReversed()) currentComparisonResult*=-1; // more natural order than considering boolean precedence (reverse means files first, then direectories)
                    if (currentComparisonResult == 0) continue;
                    else return currentComparisonResult;
                default:
                    throw new RuntimeException("Guard block");
            }
        }
        return currentComparisonResult;
    }
}

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

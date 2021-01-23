package it.pgp.xfiles.comparators;

import java.util.*;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.SortingItem;
import it.pgp.xfiles.enums.ComparatorField;

/**
 * Created by pgp on 30/10/16
 */

public class AdvancedComparatorStrategy implements Comparator<BrowserItem> {
    // code more readable, but slightly less efficient than switch-based one (AdvancedComparator)
    private static class FilenameComp implements Comparator<BrowserItem> {
        @Override
        public int compare(BrowserItem o1, BrowserItem o2) {
            return o1.getFilename().compareTo(o2.getFilename());
        }
    }

    private static class DateComp implements Comparator<BrowserItem> {
        @Override
        public int compare(BrowserItem o1, BrowserItem o2) {
            return o1.date.compareTo(o2.date);
        }
    }

    private static class SizeComp implements Comparator<BrowserItem> {
        @Override
        public int compare(BrowserItem o1, BrowserItem o2) {
            return o1.size.compareTo(o2.size);
        }
    }

    private static class TypeComp implements Comparator<BrowserItem> {
        @Override
        public int compare(BrowserItem o1, BrowserItem o2) {
            return o1.getFileExt().compareTo(o2.getFileExt());
        }
    }

    private static class DirComp implements Comparator<BrowserItem> {
        // -1 factor: more natural order than considering boolean precedence (reverse means files first, then directories)
        @Override
        public int compare(BrowserItem o1, BrowserItem o2) {
            return -1*o1.isDirectory.compareTo(o2.isDirectory);
        }
    }

    private List<SortingItem> attributes; // sorting is sequential w.r.t. these attributes
    private static final Map<ComparatorField,Comparator> comparators;

    static {
        Map<ComparatorField,Comparator> m = new HashMap<>();
        m.put(ComparatorField.FILENAME,new FilenameComp());
        m.put(ComparatorField.DATE,new DateComp());
        m.put(ComparatorField.SIZE,new SizeComp());
        m.put(ComparatorField.TYPE,new TypeComp());
        m.put(ComparatorField.DIR,new DirComp());
        comparators = Collections.unmodifiableMap(m);
    }

    public AdvancedComparatorStrategy(List<SortingItem> attributes) {
        this.attributes = attributes;
    }

    @Override
    public int compare(BrowserItem o1, BrowserItem o2) {
        int currentComparisonResult = 0;
        for (SortingItem a : attributes) {
            try {currentComparisonResult = comparators.get(a.comparatorField).compare(o1,o2);}
            catch (NullPointerException n) {
                throw new RuntimeException("Guard block");
            }
            if (a.isReversed()) currentComparisonResult*=-1;
            if (currentComparisonResult != 0) return currentComparisonResult;
        }
        return currentComparisonResult;
    }
}

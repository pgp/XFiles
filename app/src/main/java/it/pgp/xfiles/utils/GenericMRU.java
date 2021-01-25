package it.pgp.xfiles.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by pgp on 17/05/17
 *
 * MRU cache for archives' VMap representations - generic version
 */

public class GenericMRU<T,U> {

    private List<T> archivePaths;
    private List<U> vMaps;
    private List<Date> modified;

    Integer currentIndex;
    public final Integer maxIndex;

    public void decrementIndex() {
        currentIndex = (currentIndex + maxIndex -1) % maxIndex;
    }

    public void incrementIndex() {
        currentIndex = (currentIndex +1) % maxIndex;
    }

    public GenericMRU(Integer maxIndex) {
        this.maxIndex = maxIndex;
        clear();
    }

    public void clear() {
        vMaps = new ArrayList<>();
        modified = new ArrayList<>();
        archivePaths = new ArrayList<>();
        currentIndex = 0; // least recent index is (currentIndex + maxIndex -1) % maxIndex

        // add maxIndex elements to each list
        for (int i=0;i<maxIndex;i++) {
            vMaps.add(null);
            modified.add(null);
            archivePaths.add(null);
        }
    }

    // find methods
    private int findIndex(T archivePath) {
        for (int i=0;i<maxIndex;i++) {
            if (archivePath.equals(archivePaths.get(i)))
                return i;
        }
        return -1;
    }

    private void swapMruObjects(int i1, int i2) {
        if (i1 == i2) return;
        Collections.swap(archivePaths,i1,i2);
        Collections.swap(vMaps,i1,i2);
        Collections.swap(modified,i1,i2);
    }

    /*
    Return values:
        - true/false if file has/has not been modified since last entries reading
        - null if not present in MRU cache
     */
    public Boolean hasBeenModified(T archivePath, Date modifiedDate) {
        int foundIdx = findIndex(archivePath);
        if (foundIdx < 0) return null;
        return !(modified.get(foundIdx).equals(modifiedDate));
    }

    // tuple {archive, vmap, date}
    public Object[] getLatest() {
        return new Object[]{
                archivePaths.get(currentIndex),
                vMaps.get(currentIndex),
                modified.get(currentIndex)
        };
    }

    /*
    Checks whether a MRU entry exists for archivePath and, if so, whether the underlying file
    has been modified since last cache entry setting
    If not modified, returns the corresponding VMap, and brings the entry position to front,
    else returns null and invalidates that entry
    To be used by client in conjunction with setLatest
     */
    public U getByPath(T archivePath, Date modifiedDate) {
        int foundIdx = findIndex(archivePath);

        if (foundIdx >= 0) { // some entry exists
            if (modified.get(foundIdx).equals(modifiedDate)) {
                // OK, no file modification, bring on top and return the vMap
                swapMruObjects(foundIdx,currentIndex);
                // then return the current vMap
                return vMaps.get(currentIndex);
            }
            else { // don't bring on top anything, delete that cache entry
                archivePaths.set(foundIdx,null);
                modified.set(foundIdx,null);
                vMaps.set(foundIdx,null);
                return null;
            }
        }
        else {
            // not found
            return null;
        }
    }

    // unconditionally get by path (for extraction from within archive)
    public U getByPath(T archivePath) {
        int foundIdx = findIndex(archivePath);
        if (foundIdx >= 0) { // some entry exists
            // OK, no file modification, bring on top and return the vMap
            swapMruObjects(foundIdx,currentIndex);
            // then return the current vMap
            return vMaps.get(currentIndex);
        }
        else {
            // not found
            return null;
        }
    }

    // unconditionally set latest
    // assumed to be called after a full archive entries retrieval
    public void setLatest(T archivePath, U vMap, Date modifiedDate) {
        // find current
        int foundIdx = findIndex(archivePath);
        if (foundIdx >= 0) {
            // unconditionally overwrite old cache entry for this archivePath, and bring to top
            vMaps.set(foundIdx,vMap);
            modified.set(foundIdx,modifiedDate);

            // simply bring this cache entry to top, swapping it with the current 0
            if (currentIndex != foundIdx)
                swapMruObjects(foundIdx,currentIndex);
        }
        else {
            // add a new entry, overwriting the least recent one
            incrementIndex();
            archivePaths.set(currentIndex,archivePath);
            vMaps.set(currentIndex,vMap);
            modified.set(currentIndex,modifiedDate);
        }
    }
}

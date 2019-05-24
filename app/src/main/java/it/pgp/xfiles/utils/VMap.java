package it.pgp.xfiles.utils;

import it.pgp.xfiles.utils.iterators.VMapAbstractIterable;
import it.pgp.xfiles.utils.iterators.VMapChildParentIterable;
import it.pgp.xfiles.utils.iterators.VMapFullTreeIterable;
import it.pgp.xfiles.utils.iterators.VMapSubTreeIterable;

import java.util.*;

/**
 * Created by pgp on 30/03/17
 * Java equivalent of auto-vivified dictionary in Python.
 * Provides getter and setter with varargs input for arbitrary-depth map nesting
 *
 * Used in XFiles to provide an internal tree representation of an archive's entries
 * (retrieved altogether on archive open)
 */
public class VMap {

    public Map<Object,Object> h; // nested map

    public VMap() {
        h = new HashMap<>();
    }

    public class ValueAsKeyException extends RuntimeException {
        public ValueAsKeyException() {
            super("Tried to access value as key!");
        }
    }

    // containsKey: true if key exists, false if the key or any of its ancestors do not exist, or if any ancestor is already not a map

    public boolean containsKey(Collection<Object> keys) {
        return containsKey(keys.toArray());
    }

    public boolean containsKey(Object[] keys) {
        if (h==null) h = new HashMap<>();
        Map<Object,Object> currentLevelMap = h;
        for (int i=0;i<keys.length-1;i++) {
            if (currentLevelMap.get(keys[i]) == null) {
                return false;
            }
            try {
                currentLevelMap = (Map<Object, Object>) currentLevelMap.get(keys[i]);
            }
            catch (Exception e) {
                return false;
            }
        }

        return (currentLevelMap.get(keys[keys.length-1]) != null);
    }

    public void setEmpty(Collection<Object> keys) {
        set(new HashMap<>(),keys.toArray());
    }

    public void set(Object v, Object[] keys) throws ValueAsKeyException {
        if (h==null) h = new HashMap<>();
        Map<Object,Object> currentLevelMap = h;

        for (int i=0;i<keys.length-1;i++) {
            if (currentLevelMap.get(keys[i]) == null) {
                currentLevelMap.put(keys[i],new HashMap<>());
            }
            try {
                currentLevelMap = (Map<Object, Object>) currentLevelMap.get(keys[i]);
            }
            catch (Exception e) {
                throw new ValueAsKeyException();
            }
        }

        currentLevelMap.put(keys[keys.length-1],v);
    }

    public Object get(Object[] keys) throws ValueAsKeyException {
        if (h==null) h = new HashMap<>();
        Map<Object,Object> currentLevelMap = h;
        for (int i=0;i<keys.length-1;i++) {
            if (currentLevelMap.get(keys[i]) == null) {
                return null;
            }
            try {
                currentLevelMap = (Map<Object, Object>) currentLevelMap.get(keys[i]);
            }
            catch (Exception e) {
                throw new ValueAsKeyException();
            }
        }

        return currentLevelMap.get(keys[keys.length-1]);
    }

    // removes if present, returning the value, otherwise returns null
    public Object remove(Object[] keys) {
        if (h == null) return null;
        List<Map> mapRefs = new ArrayList<>();
        Map<Object,Object> currentLevelMap = h;
//        mapRefs.add(currentLevelMap);
        for (int i=0;i<keys.length-1;i++) {
            if (currentLevelMap.get(keys[i]) == null) {
                return null;
            }
            currentLevelMap = (Map<Object, Object>) currentLevelMap.get(keys[i]);
            mapRefs.add(currentLevelMap);
        }

        Object x = currentLevelMap.remove(keys[keys.length-1]);

        // backward deletion of empty ancestors
        boolean deleteFromParent = currentLevelMap.isEmpty();
        for (int i=keys.length-2;i>=0;i--) {
            if (deleteFromParent) mapRefs.get(i).remove(keys[i+1]);

            deleteFromParent = mapRefs.get(i).isEmpty(); // set current map to be deleted in next iteration from its parent (if empty)
        }

        if(deleteFromParent) h.remove(keys[0]);

        return x;
    }

    public void clear() {
        h = new HashMap<>();
    }

    public VMapAbstractIterable getChildParentIterable() {
        return new VMapChildParentIterable(this);
    }

    public VMapAbstractIterable getFullTreeIterable() {
        return new VMapFullTreeIterable(this);
    }

    public VMapAbstractIterable getSubTreeIterable(Object[] keys) {
        return new VMapSubTreeIterable(this,keys);
    }
}

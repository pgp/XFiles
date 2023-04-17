package it.pgp.xfiles.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class MRUArray<T> implements Iterable<T> {
    final Object[] items;
    final int size;
    int currentPos = 0; // currentPos: most recently added, older positions: currentPos-1 % size, currentPos-2 % size, ...

    private MRUArray(T[] items) {
        this.size = items.length;
        this.items = items;
        currentPos = size-1;
    }

    public MRUArray(int size) {
        this.size = size;
        items = new Object[size];
    }

    // last collection item is treated as newest, first one as oldest
    public static <T> MRUArray<T> fromCollection(Collection<T> c) {
        MRUArray<T> m = new MRUArray<>(c.size());
        int i=0;
        for(T item: c) m.items[i++] = item;
        m.currentPos = m.size-1;
        return m;
    }

    // last array item is treated as newest, first one as oldest
    public static <T> MRUArray<T> fromArray(T[] a) {
        MRUArray<T> m = new MRUArray<>(a.length);
        int i=0;
        for(T item: a) m.items[i++] = item;
        m.currentPos = m.size-1;
        return m;
    }

    // use an existing array as support
    public static <T> MRUArray<T> fromMutableArray(T[] a) {
        return new MRUArray<>(a);
    }

    // sets the newest, and returns the oldest, which gets removed (reference overwritten) from the array
    public T setCurrent(T item) {
        int pos = Misc.mod(currentPos+1, size);
        T oldest = (T) items[pos]; // TODO this will return null as oldest item, until the array is full
        currentPos = pos; // increment current position
        items[currentPos] = item;
        return oldest;
    }

    public T get(int relativePos) { // relativePos: 0 to -size+1 (anyway any value is allowed, since we are using unsigned remainder)
        return (T)items[Misc.mod(currentPos+relativePos, size)];
    }

    public T getCurrent() {
        return (T)items[currentPos];
    }

    private Iterator<T> getIterator(boolean reverse) {
        return new Iterator<T>() {
            int pos = reverse ? currentPos : Misc.mod(currentPos-size+1, size); // currently oldest (or newest in reverse mode) item position
            int iterations = 0;
            final int incr = reverse ? -1 : +1;

            @Override
            public boolean hasNext() {
                return iterations < size;
            }

            @Override
            public T next() {
                T ret = (T)items[pos];
                pos = Misc.mod(pos+incr,size);
                iterations++;
                return ret;
            }
        };
    }

    public Iterable<T> fromOldestToCurrent() {
        return () -> getIterator(false);
    }

    public Iterable<T> fromCurrentToOldest() {
        return () -> getIterator(true);
    }

    // default iteration order: from oldest to newest
    @Override
    public Iterator<T> iterator() {
        return getIterator(false);
    }

    @Override
    public String toString() {
        return Arrays.toString(items);
    }
}

package it.pgp.xfiles.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MRUArray<T> implements Iterable<T> {
    final Object[] items;
    final int capacity;
    int size = 0;
    int currentPos = 0; // currentPos: most recently added, older positions: currentPos-1 % size, currentPos-2 % size, ...

    boolean iterationDirection = false; // false: from oldest to current, true: from current to oldest

    private MRUArray(T[] items) {
        this.capacity = this.size = items.length;
        this.items = items;
        currentPos = capacity -1;
    }

    public MRUArray(int capacity) {
        this.capacity = capacity;
        items = new Object[capacity];
    }

    public MRUArray<T> withReverseIteration() {
        this.iterationDirection = true;
        return this;
    }

    // last collection item is treated as newest, first one as oldest
    public static <T> MRUArray<T> fromCollection(Collection<T> c) {
        MRUArray<T> m = new MRUArray<>(c.size());
        int i=0;
        for(T item: c) m.items[i++] = item;
        m.currentPos = m.capacity -1;
        m.size = m.capacity;
        return m;
    }

    // last array item is treated as newest, first one as oldest
    public static <T> MRUArray<T> fromArray(T[] a) {
        MRUArray<T> m = new MRUArray<>(a.length);
        int i=0;
        for(T item: a) m.items[i++] = item;
        m.currentPos = m.capacity -1;
        m.size = m.capacity;
        return m;
    }

    // use an existing array as support
    public static <T> MRUArray<T> fromMutableArray(T[] a) {
        return new MRUArray<>(a);
    }

    public List<T> toList() {
        List<T> l = new ArrayList<>();
        for(T i: this) l.add(i);
        return l;
    }

    // sets the newest, and returns the oldest, which gets removed (reference overwritten) from the array
    public T setCurrent(T item) {
        int pos = Misc.mod(currentPos+1, capacity);
        T oldest = (T) items[pos]; // TODO this will return null as oldest item, until the array is full
        currentPos = pos; // increment current position
        items[currentPos] = item;
        size = Math.min(capacity, size+1);
        return oldest;
    }

    public T get(int relativePos) { // relativePos: 0 to -size+1 (anyway any value is allowed, since we are using unsigned remainder)
        return (T)items[Misc.mod(currentPos+relativePos, capacity)];
    }

    public T getCurrent() {
        return (T)items[currentPos];
    }

    private Iterator<T> getIterator(boolean reverse) {
        return new Iterator<T>() {
            int pos = reverse ? currentPos : Misc.mod(currentPos-size+1, capacity); // currently oldest (or newest in reverse mode) item position
            int iterations = 0;
            final int incr = reverse ? -1 : +1;

            @Override
            public boolean hasNext() {
                return iterations < size;
            }

            @Override
            public T next() {
                T ret = (T)items[pos];
                pos = Misc.mod(pos+incr, capacity);
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

    @Override
    public Iterator<T> iterator() {
        return getIterator(iterationDirection);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int idx=0;
        for(T i: this) sb.append(idx++==0 ? i.toString() : ","+i.toString());
        return "[" + sb.toString() + "]";
    }
}

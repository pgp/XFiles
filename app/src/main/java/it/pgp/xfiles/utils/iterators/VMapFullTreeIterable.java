package it.pgp.xfiles.utils.iterators;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import it.pgp.xfiles.utils.VMap;

/**
 * Created by pgp on 12/04/17
 */

public class VMapFullTreeIterable extends VMapAbstractIterable {
    public VMapFullTreeIterable(VMap vMap) {
        super(vMap);
    }

    @Override
    public Iterator<Map.Entry<?, ?>> iterator() {
        return new VMapFullTreeIterator();
    }

    class VMapFullTreeIterator implements Iterator<Map.Entry<?,?>> {

        Stack<Map.Entry> S;

        VMapFullTreeIterator() {
            S = new Stack<>();
            S.addAll(vMap.h.entrySet());
        }

        @Override
        public boolean hasNext() {
            return !S.isEmpty();
        }

        @Override
        public Map.Entry<?,?> next() {
            Map.Entry t = S.pop();
            if (t.getValue() instanceof Map) {
                S.addAll(((Map) t.getValue()).entrySet());
            }
            return t;
        }
    }
}

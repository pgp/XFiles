package it.pgp.xfiles.utils.iterators;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import it.pgp.xfiles.utils.VMap;

/**
 * Created by pgp on 12/04/17
 */

public class VMapSubTreeIterable extends VMapAbstractIterable {
    private Object[] keys;
    public VMapSubTreeIterable(VMap vMap, Object... keys) {
        super(vMap);
        this.keys = keys;
    }

    @Override
    public Iterator<Map.Entry<?, ?>> iterator() {
        return new VMapSubTreeIterator();
    }

    class VMapSubTreeIterator implements Iterator<Map.Entry<?,?>> {

        Stack<Map.Entry> S;

        VMapSubTreeIterator() {
            S = new Stack<>();
            try {
                S.addAll(((Map)vMap.get(keys)).entrySet());
            }
            catch (NullPointerException|ClassCastException ignored) {}
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

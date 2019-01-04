package it.pgp.xfiles.utils.iterators;

import it.pgp.xfiles.utils.VMap;

import java.util.*;

/**
 * Created by pgp on 12/04/17
 * // Java 8 language support out-of-the-box from Studio 2.4
 */

public class VMapChildParentIterable extends VMapAbstractIterable {
    public VMapChildParentIterable(VMap vMap) {
        super(vMap);
    }

    @Override
    public Iterator<Map.Entry<?, ?>> iterator() {
        return new VMapChildParentIterator();
    }

    // only working correctly if every node name is unique in the tree
    class VMapChildParentIterator implements Iterator<Map.Entry<?,?>> {

        Stack<Map.Entry> S;
        HashMap parentMap; // for ancestor list retrieval of nodes traversed so far in DFS iteration

        VMapChildParentIterator() {
            parentMap = new HashMap();
            S = new Stack<>();
            for (Object x: vMap.h.keySet()) {
                S.push(new AbstractMap.SimpleEntry(x,null)); // null means root parent
                parentMap.put(x,null); // this should be useless
            }
        }

        @Override
        public boolean hasNext() {
            return !S.isEmpty();
        }

        @Override
        public Map.Entry<?,?> next() {
            Map.Entry t = S.pop();

            // look up in current parent map, and build ancestor list
            ArrayDeque vMapKeysList = new ArrayDeque();

            Object currentParent = parentMap.get(t.getValue());
            while (currentParent != null) {
                vMapKeysList.addFirst(currentParent);
                currentParent = parentMap.get(currentParent);
            }
            if (t.getValue() != null) vMapKeysList.addLast(t.getValue());
            vMapKeysList.addLast(t.getKey());

            // get (ancestor list) from this vmap, if type of value is map, add elements to stack
            Object tmp;
            try {
                tmp = vMap.get(vMapKeysList.toArray());
            } catch (VMap.ValueAsKeyException e) { // not a map, nothing to expand, simply return child-parent pair
                return t;
            }

            if (tmp != null) {
                if (tmp instanceof Map) {
                    for (Object x : ((Map) tmp).keySet()) {
                        S.push(new AbstractMap.SimpleEntry(x,t.getKey()));
                        parentMap.put(x,t.getKey());
                    }
                }
                else {
                    S.push(new AbstractMap.SimpleEntry(tmp,t.getKey()));
                    parentMap.put(tmp,t.getKey());
                }
            }

            return t;
        }
    }
}

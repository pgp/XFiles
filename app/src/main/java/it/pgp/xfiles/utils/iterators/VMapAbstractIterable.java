package it.pgp.xfiles.utils.iterators;

import it.pgp.xfiles.utils.VMap;

import java.util.Map;

/**
 * Created by pgp on 12/04/17
 *
 * base class in order to allow for and foreach syntax on different it.pgp.utils.iterators for it.pgp.utils.VMap,
 * without explicitly instantiating iterator objects
 */
public abstract class VMapAbstractIterable implements Iterable<Map.Entry<?,?>> {

    VMap vMap;
    public VMapAbstractIterable(VMap vMap) {
        this.vMap = vMap;
    }

}

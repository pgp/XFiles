package it.pgp.xfiles.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class FavoritesList<T> {
    public T a;
    public Set<String> paths; // favorites as remote paths

    public FavoritesList(T a, String... paths) {
        this.a = a;
        this.paths = new TreeSet<>(Arrays.asList(paths));
    }

    public FavoritesList(T a, Collection<String> paths) {
        this.a = a;
        if(paths instanceof TreeSet) this.paths = (Set<String>)paths;
        else this.paths = new TreeSet<>(paths);
    }
}

package it.pgp.xfiles.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * TODO will replace {@link it.pgp.xfiles.sftpclient.AuthDataWithFavorites} and {@link it.pgp.xfiles.smbclient.SmbAuthDataWithFavorites}
 */

public class FavoritesList<T> {
    public T a;
    public Set<String> paths; // favorites as remote paths

    public FavoritesList(T a, String... paths) {
        this.a = a;
        this.paths = new TreeSet<>(Arrays.asList(paths));
    }

    public FavoritesList(T a, Iterable<String> paths) {
        this.a = a;

        if(paths instanceof TreeSet)
            this.paths = (Set<String>)paths;
        else if(paths instanceof Set)
            this.paths = new TreeSet<>((Set<String>) paths);
        else this.paths = new TreeSet<String>(){{for(String a : paths) add(a);}};
    }
}

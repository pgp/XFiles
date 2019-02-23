package it.pgp.xfiles.smbclient;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * TODO common structure with {@link it.pgp.xfiles.sftpclient.AuthDataWithFavorites}
 * better to use generics
 */
public class SmbAuthDataWithFavorites {
    public SmbAuthData a;
    public Set<String> paths; // favorites as remote paths

    public SmbAuthDataWithFavorites(SmbAuthData a, String... paths) {
        this.a = a;
        this.paths = new TreeSet<>(Arrays.asList(paths));
    }

    public SmbAuthDataWithFavorites(SmbAuthData a, Iterable<String> paths) {
        this.a = a;

        if(paths instanceof TreeSet)
            this.paths = (Set<String>)paths;
        else if(paths instanceof Set)
            this.paths = new TreeSet<>((Set<String>) paths);
        else this.paths = new TreeSet<String>(){{for(String a : paths) add(a);}};
    }
}

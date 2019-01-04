package it.pgp.xfiles.sftpclient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by pgp on 04/07/17
 */

public class AuthDataWithFavorites {
    public AuthData a;
    public Set<String> paths; // favorites as remote paths

    public AuthDataWithFavorites(AuthData a, String... paths) {
        this.a = a;
        this.paths = new TreeSet<>(Arrays.asList(paths));
    }

    public AuthDataWithFavorites(AuthData a, Set<String> paths) {
        this.a = a;
        this.paths = paths;
    }
}

package it.pgp.xfiles.sftpclient;

import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by pgp on 11/02/17
 */
public class AuthData implements Serializable {

    // TODO when refactoring SmbAuthData as subclass of AuthData, remember to convert the "instanceof AuthData" checks against the subclass instead of the superclass

    public static final AuthData ref = new AuthData(null,null,0,null); // just for type checking in generic method

    public String username;
    public String domain;
    public int port;
    @Nullable public String password;

    // connection_id: user@domain:port
    public AuthData(String connection_id) {
        RuntimeException r = new RuntimeException("Invalid connection id");
        String[] s1 = connection_id.split("@");
        if (s1.length != 2) throw r;
        String[] s2 = s1[1].split(":");
        if (s2.length != 2) throw r;
        username = s1[0];
        domain = s2[0];
        try {
            port = Integer.parseInt(s2[1]);
        }
        catch (NumberFormatException n) {
            throw r;
        }
    }

    public AuthData(String username, String domain, int port, @Nullable String password) {
        this.username = username;
        this.domain = domain;
        this.port = port;
        this.password = password;
    }

    @Override
    public String toString() {
        return username + "@" + domain + ":" + port;
    }

    @Override
    public boolean equals(Object obj_) {
        if (!(obj_ instanceof AuthData)) return false;
        AuthData obj = (AuthData) obj_;
        boolean usernameE = false, domainE = false;
        if (username == null && obj.username == null) usernameE = true;
        else if (username != null && obj.username != null) {
            usernameE = username.equals(obj.username);
        }
        if (!usernameE) return false;

        if (domain == null && obj.domain == null) domainE = true;
        else if (domain != null && obj.domain != null) {
            domainE = domain.equals(obj.domain);
        }
        return domainE && this.port == obj.port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int prime2 = 271;
        int hash1 = (this.username == null) ? 0 : this.username.hashCode();
        int hash2 = (this.domain == null) ? 0 : this.domain.hashCode();
        return prime2*((prime * (hash1 ^ hash2))^this.port);
    }
}

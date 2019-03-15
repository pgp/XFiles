package it.pgp.xfiles.smbclient;

import android.support.annotation.Nullable;

import java.io.Serializable;

public class SmbAuthData implements Serializable {

    public static final SmbAuthData ref = new SmbAuthData(null,null,null,0,null);  // just for type checking in generic method

    public static final String defaultDomain = "WORKGROUP";
    public static final int defaultPort = 445;

    public String username;
    public String domain;
    public String host;
    public int port;
    @Nullable public String password;

    public SmbAuthData(String username, String host, int port, @Nullable String password) {
        this(username,defaultDomain,host,port,password);
    }

    public SmbAuthData(String username, String host, @Nullable String password) {
        this(username,defaultDomain,host,defaultPort,password);
    }

    public SmbAuthData(String username, String host, int port) {
        this(username,defaultDomain,host,port,null);
    }

    public SmbAuthData(String username, String host) {
        this(username,defaultDomain,host,defaultPort,null);
    }

    public SmbAuthData(String username,
                       @Nullable String domain,
                       String host,
                       int port,
                       @Nullable String password) {
        this.username = username;
        this.domain = (domain==null||"".equals(domain))?defaultDomain:domain;
        this.host = host;
        this.port = port<=0?defaultPort:port;
        this.password = password;
    }


    // non-canonical, contains domain(e.g. WORKGROUP)
    @Override
    public String toString() {
        return username + "@" + domain + ":" + host + ":" + port;
    }

    @Override
    public int hashCode() {
        final int prime = 23;
        final int prime2 = 37;
        final int prime3 = 101;
        int hash1 = (this.username == null) ? 0 : this.username.hashCode();
        int hash2 = (this.domain == null) ? 0 : this.domain.hashCode();
        int hash3 = (this.host == null) ? 0 : this.host.hashCode();
        return prime3*((prime2*((prime * (hash1 ^ hash2))^this.port))^hash3);
    }
}

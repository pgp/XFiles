package com.tomclaw.imageloader.util.loader;

import com.tomclaw.imageloader.core.Loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import it.pgp.xfiles.utils.Misc;

public class UrlLoader implements Loader {
    private static final String METHOD_GET = "GET";
    private static final int TIMEOUT_SOCKET = 70 * 1000;
    private static final int TIMEOUT_CONNECTION = 60 * 1000;


    @Override
    public List<String> getSchemes() {
        return Arrays.asList("http", "https");
    }

    @Override
    public boolean load(String uriString, File file) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            connection = openConnection(uriString);
            input = connection.getInputStream();
            output = new FileOutputStream(file);
            if(input != null) {
                int code = connection.getResponseCode();
                if(code >= 200 && code <= 299) {
                    Misc.pipe(input, output);
                    return true;
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {input.close();} catch(Exception ignored) {}
            try {output.close();} catch(Exception ignored) {}
            if(connection != null) connection.disconnect();
        }
        return false;
    }

    private HttpURLConnection openConnection(String uri) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod(METHOD_GET);
        c.setDoInput(true);
        c.setDoOutput(false);
        c.setConnectTimeout(TIMEOUT_CONNECTION);
        c.setReadTimeout(TIMEOUT_SOCKET);
        return c;
    }
}

package it.pgp.xfiles.fileservers;

import android.widget.Button;

public abstract class SimpleFileServer {

    public Button serverButton;
    public int serverButtonRes;

    public String rootPath = "/sdcard";
    public int port;

    public abstract void startServer();
    public abstract void stopServer();

    public abstract boolean isAlive();

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

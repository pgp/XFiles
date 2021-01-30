package it.pgp.xfiles.fileservers;

import android.widget.Button;

import java.util.Observable;

import it.pgp.xfiles.utils.Misc;

public abstract class SimpleFileServer extends Observable {

    public Button serverButton;
    public int serverButtonRes;

    public String rootPath = Misc.internalStorageDir.getAbsolutePath();
    public int port;

    public abstract void startServer();
    public abstract void stopServer();

    public abstract boolean isAlive();

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void toggle() {
        if (isAlive()) stopServer(); else startServer();
    }

    @Override
    public synchronized boolean hasChanged() {
        return true;
    }
}

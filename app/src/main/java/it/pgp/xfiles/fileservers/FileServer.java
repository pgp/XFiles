package it.pgp.xfiles.fileservers;

import android.app.Activity;

import it.pgp.xfiles.R;

public enum FileServer {
    FTP(new SimpleFTPServer()),
    HTTP(new SimpleHTTPServer());

    public SimpleFileServer server;

    FileServer(SimpleFileServer server) {
        this.server = server;
    }

    public void start() {
        server.startServer();
    }

    public void stop() {
        server.stopServer();
    }

    public void setRootPath(String path) {
        server.setRootPath(path);
    }

    public boolean isAlive() {
        return server.isAlive();
    }

    public void toggle() {
        if (server.isAlive()) server.stopServer();
        else server.startServer();
    }

    public void refresh_button_color(Activity activity, boolean... on_) {
        boolean on = on_.length>0?on_[0]:isAlive();
        if (server.serverButton != null)
            server.serverButton.setTextColor(activity.getResources().getColor(
                    on?R.color.green:R.color.red
            ));
    }
}

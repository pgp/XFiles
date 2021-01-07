package it.pgp.xfiles.fileservers;

import android.app.Activity;

import it.pgp.xfiles.R;

public enum FileServer {
    FTP(new SimpleFTPServer(),R.id.itemShareOverFTP,R.id.ftpServerButton),
    HTTP(new SimpleHTTPServer(),R.id.itemShareOverHTTP,R.id.httpServerButton);

    public SimpleFileServer server;
    public int menuId;
    public int buttonId;

    FileServer(SimpleFileServer server, int menuId, int buttonId) {
        this.server = server;
        this.menuId = menuId;
        this.buttonId = buttonId;
    }

    public static FileServer fromMenuRes(int res) {
        switch(res) {
            case R.id.itemShareOverFTP:
                return FTP;
            case R.id.itemShareOverHTTP:
                return HTTP;
            default:
                throw new RuntimeException("Invalid resource id provided");
        }
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

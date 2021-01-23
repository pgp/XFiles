package it.pgp.xfiles.fileservers;

import it.pgp.xfiles.R;

public enum FileServer {
    FTP(new SimpleFTPServer(),R.id.itemShareOverFTP,R.id.ftpServerButton),
    HTTP(new SimpleHTTPServer(),R.id.itemShareOverHTTP,R.id.httpServerButton);

    public final SimpleFileServer server;
    public final int menuId;
    public final int buttonId;

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
}

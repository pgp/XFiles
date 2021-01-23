package it.pgp.xfiles.fileservers;

import android.app.Activity;
import android.widget.Button;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Misc;

public abstract class SimpleFileServer {

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

    public void refresh_button_color(Activity activity, Boolean on) {
        if(on == null) on = isAlive();
        if (serverButton != null)
            serverButton.setTextColor(activity.getResources().getColor(
                    on? R.color.green:R.color.red
            ));
    }
}

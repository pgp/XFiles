package it.pgp.xfiles.enums;

import android.Manifest;

/**
 * Created by pgp on 12/07/17
 * Enum of permissions used by XFiles (for switching logic in runtime permissions request)
 */

public enum Permissions {
//    SYSTEM_ALERT_WINDOW(Manifest.permission.SYSTEM_ALERT_WINDOW), // for showing alert dialogs and progress bar overlays in long-term operations
//    WAKE_LOCK(Manifest.permission.WAKE_LOCK), // for preventing app to be shutdown in long-term operations (file transfers, etc...)
//    INTERNET(Manifest.permission.INTERNET),
    // actually, this is the only dangerous permission needed
    WRITE_EXTERNAL_STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE) // obviously, for reading and writing files :D
    ; // for using sftp browser

    String value;

    Permissions(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}

package it.pgp.xfiles.service;

import android.content.ContentResolver;

import java.io.Serializable;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;

/**
 * Created by pgp on 06/06/17
 * Base class for tasks utilizing roothelper instances
 */

public abstract class RootHelperClientTask extends BaseBackgroundTask {

    public RootHelperClientUsingPathContent rh;
    public ContentResolver resolver; // for XRE direct share and compress from 3rd party providers

    public RootHelperClientTask(Serializable params) {
        super(params);

        MainActivity.getRootHelperClient(); // ensure started (not sure if instance can be re-used here)
        rh = new RootHelperClientUsingPathContent();
    }


    @Override
    public void cancelTask() {
        super.cancelTask();

        // TODO may be useful in all long-term tasks, change following comment if needed
        // force close RootHelperStreams so that both the AsyncTask and the forked p7zip C++ process terminate
        try {rh.rs.close();}
        catch (Exception ignored) {}
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        // if main activity has been closed meanwhile, stop the main RH server instance as well
        if (MainActivity.mainActivity == null) {
            MainActivity.killRHWrapper();
        }
    }

}

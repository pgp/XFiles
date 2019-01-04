package it.pgp.xfiles.service;

import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;

/**
 * Created by pgp on 06/06/17
 * Base class for tasks utilizing roothelper instances
 */

public abstract class RootHelperClientTask extends BaseBackgroundTask {

    public RootHelperClientUsingPathContent rh;

    public FileOpsErrorCodes result;

    // launch a new RootHelper process on the given unix socket name,
    // then create a new RootHelperClient instance connected to that process
    // at the end, terminate the process
    public RootHelperClientTask(Serializable params) {
        super(params);
//        rh = RootHandler.startAndGetRH(socketName);

        MainActivity.getRootHelperClient(); // ensure started (not sure if instance can be re-used here)
        rh = new RootHelperClientUsingPathContent();
    }

//    @Override
//    public void cancelTask() {
//        super.cancelTask();
//        // connect to main roothelper and send kill -INT request to this task's long-term rh process
//        // cannot use android.os.Process.killProcess because it wouldn't work if RH server is run as root
//        Long pid = Services.activePids.get(rh.address);
//        if (pid != null) {
//            int ret = MainActivity.rootHelperClient.killRHProcess(pid,rh.address);
//            if (ret != 0) {
//                Log.e(this.getClass().getName(),"Error interrupting long-term rh, pid: "+pid);
//            }
//            else {
//                Log.e(this.getClass().getName(),"Interrupted long-term rh, pid: "+pid);
//            }
//        }
//
//    }


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
        // not needed anymore, rh server is multithreaded
//        try { rh.killServer(); }
//        catch (IOException|NullPointerException ignored) {}

        // if main activity has been closed meanwhile, stop the main RH server instance as well
        if (MainActivity.mainActivity == null) {
            MainActivity.killRHWrapper();
        }
    }

}

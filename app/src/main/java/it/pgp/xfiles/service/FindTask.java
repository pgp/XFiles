package it.pgp.xfiles.service;

import java.io.Serializable;

import it.pgp.xfiles.service.params.FindParams;

/**
 * Created by pgp on 22/03/17
 */

public class FindTask extends RootHelperClientTask {

    public FindParams params;

    FindTask(Serializable params_) {
        super(params_);
        params = (FindParams)params_;
//        d = new DirTreeWalker(new File(targetFolder)); // not used anymore, dir tree walk performed by roothelper
    }

    // FIXME String progress collides with RootHelperClientTask definition
    // protected void onProgressUpdate(String... values) {}

    @Override
    protected Object doInBackground(Object[] params) {
        // with AsyncTask<?,String,?>, on each String progress read by local socket,
        // update FindResultsActivity with a new adapter entry
        // use flag for interruption, roothelper thread at the other end exits on socket write error
        return null;
    }
}

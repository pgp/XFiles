package it.pgp.xfiles.roothelperclient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.roothelperclient.reqs.find_rq;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 22/01/18
 * Updater class for find tasks, to be embedded into AsyncTask/Service if needed
 */

public class FindManager extends RemoteManager {

    public static final AtomicReference<Thread> findManagerThreadRef = new AtomicReference<>(null);

    private FindManager() throws IOException {
        super();
    }

    private boolean start_find(find_rq find_rq) throws IOException {
        // start RH find thread
        find_rq.write(o);

        int resp = Misc.receiveBaseResponse(i);
        if (resp != 0) {
            // Unable to start RH find thread
            return false;
        }
        // ok, RH find thread started
        // now, start rhss update thread
        new FindUpdatesThread(this).start();
        return true;
    }

    // cancel current search, if any
    private boolean stop_find() throws IOException {
        new find_rq().write(o);

        int resp = Misc.receiveBaseResponse(i);
        if (resp != 0) return false;
        // ok, RH find thread stopped, local updates thread will end automatically as well
        findManagerThreadRef.set(null);
        return true;
    }

    ////////////////////////////////////
    // methods with auto-close after request send

    public enum FIND_ACTION {START,STOP}

    public static int find_action(FIND_ACTION action, find_rq... request) {
        if (action == FIND_ACTION.START) {
            MainActivity.getRootHelperClient();
            // without auto-close
            try {return (new FindManager().start_find(request[0])) ? 1 : 0;}
            catch (IOException e) {return -1;}
        }
        else {
            // with auto-close
            try(FindManager f = new FindManager()) {
                return f.stop_find()?1:0;
            }
            catch (IOException ignored) {}
            return -1;
        }
    }

}

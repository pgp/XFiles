package it.pgp.xfiles.adapters;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import it.pgp.xfiles.FindActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.roothelperclient.reqs.find_rq;

/**
 * Construction rationale behind asynchronous adapter (keeping state and surviving across activities-listviews instances, while preserving real-time notify requirements):
 * - Adapter is nullable singleton
 * - It is created using the first available activity reference, in order to perform add method via runOnUIThread
 * - Current activity instance is kept updated via static instance, and it is used for runOnUIThread;
 *   moreover, by construction the static instance is never null at the time runOnUIThread is invoked
 */

public class FindResultsAdapter extends BrowserListAdapter { // uses FindBrowserItem instead of BrowserItem

    public static FindResultsAdapter instance;

    public find_rq request; // the request which started this search - non-null, the whole adapter instance would be null otherwise

    private FindResultsAdapter(@NonNull MainActivity mainActivity) {
//        super(context,android.R.layout.simple_list_item_1);
        super(mainActivity,new ArrayList<>());
    }

    public static synchronized void createIfNotExisting() {
        if (instance == null) {
            instance = new FindResultsAdapter(MainActivity.mainActivity);
        }
    }

    public static synchronized void reset() {
        createIfNotExisting();
        FindActivity.instance.runOnUiThread(()->FindResultsAdapter.instance.clear());
    }

    // USE app context, NOT activity context
    public static void createAdapter(@NonNull find_rq request) {
        reset();
        instance.request = request;
    }
}

package it.pgp.xfiles.roothelperclient;

import android.util.Log;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.FindActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.adapters.FindResultsAdapter;
import it.pgp.xfiles.roothelperclient.resps.find_resp;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

import static it.pgp.xfiles.roothelperclient.FindManager.findManagerThreadRef;

public class FindUpdatesThread extends Thread {
    protected final BasePathContent bpc;
    protected final AutoCloseable ac;

    protected String getErrMsg() {
        return "Local socket closed by rhss server or other exception, exiting...";
    }

    FindUpdatesThread(AutoCloseable ac, BasePathContent bpc) {
        this.bpc = bpc;
        this.ac = ac;
    }

    private static boolean onSearchItemFound(find_resp item) {
        try {
            // TODO when content search will be available, should replace BrowserItem with a subclass including content results
            FindActivity.instance.runOnUiThread(() ->
                    FindResultsAdapter.instance.add(new BrowserItem(item.fileItem)));
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void doFind() throws Exception {
        for(;;) { // exits on IOException when the other socket endpoint is closed (search interrupted), or when receives end of list (not strictly needed, roothelper find thread could also close the connection after sending last item found)
            // receive search results
            find_resp item = find_resp.readNext(((FindManager)ac).i);
            if(item == null) break;
            if(!onSearchItemFound(item)) break; // exit immediately if adapter has been destroyed (actually, that should not happen)
        }
    }

    @Override
    public void run() {
        // strong cas, a thread is guaranteed to win
        if (!findManagerThreadRef.compareAndSet(null,this)) {
            MainActivity.showToast("Another find thread is already receiving updates");
            return;
        }

        try {
            FindActivity.instance.runOnUiThread(()->FindActivity.instance.toggleSearchButtons(true));
            FindResultsAdapter.createAdapter(bpc);
            doFind();
            MainActivity.showToast("Search completed");
        }
        catch(Throwable t) {
            t.printStackTrace();
            Log.d(getClass().getName(),getErrMsg());
        }

        try {ac.close();} catch(Exception ignored) {}
        findManagerThreadRef.set(null); // unset reference only if compareAndSet was successful
        FindActivity.instance.runOnUiThread(()->FindActivity.instance.toggleSearchButtons(false));
        Log.d(getClass().getName(),"Really exiting find thread now!");
    }
}
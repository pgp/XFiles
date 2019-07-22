package it.pgp.xfiles.roothelperclient;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.XRE_RHSS_Widget;
import it.pgp.xfiles.dialogs.RemoteRHServerManagementDialog;
import it.pgp.xfiles.dialogs.XFilesRemoteSessionsManagementActivity;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

/**
 * Created by pgp on 20/09/17
 * Client Thread that starts a RH remote server, and keeps connected to the underlying local socket
 * in order to receive information about started and ended server sessions with remote clients
 * Disconnection of local socket should cause RH remote server process termination
 */

public class RemoteServerManager extends RemoteManager {

    public static final AtomicReference<Thread> rhssManagerThreadRef = new AtomicReference<>(null);
    private static final int rq_bit_length = 5;

    private final byte rq_byte = ControlCodes.REMOTE_SERVER_MANAGEMENT.getValue();

    // constructor without auto start
    private RemoteServerManager() throws IOException {
        super();
    }

    private boolean start_rhss(boolean announce, String... servedDirectory) throws IOException {
        // start RH remote server instance
        byte rq = announce?
                (byte)(rq_byte ^ (5 << rq_bit_length)) : // flags: 101, start with UDP broadcast announce
                (byte)(rq_byte ^ (7 << rq_bit_length)); // flags: 111, no announce
        o.write(rq);

        byte serveCustom;
        // send byte to indicate whether current dir is to be served or not
        // send served directory if present
        if (servedDirectory.length > 0) {
            serveCustom = (byte)0xFF;
            o.write(serveCustom);
            byte[] bb = servedDirectory[0].getBytes();
            byte[] tmp = Misc.castUnsignedNumberToBytes(bb.length,2);
            o.write(tmp);
            o.write(bb);
        }
        else {
            serveCustom = 0x00;
            o.write(serveCustom);
        }

        int resp = receiveBaseResponse();
        if (resp != 0) {
            // Unable to start RH remote server
            return false;
        }
        // ok, streams connected and RH remote server instance started
        // now, start rhss update thread
        RHSSServerStatus.createServer(servedDirectory.length==0?"":servedDirectory[0]);
        new RHSSUpdateThread().start();
        return true;
    }

    private boolean stop_rhss() throws IOException {
        // stop RH remote server instance
        o.write(rq_byte); // flags: 000

        int resp = receiveBaseResponse();
        if (resp != 0) {
            // Unable to stop RH remote server
            return false;
        }
        // ok, streams connected and RH remote server instance stopped, now terminate local updates thread
//        MainActivity.rhssManagerThreadRef.get().close();
        rhssManagerThreadRef.set(null);
        return true;
    }

    // true: running, false: not running
    private boolean status_rhss() throws IOException {
        byte rq = (byte)(rq_byte ^ (2 << rq_bit_length)); // flags: 010
        o.write(rq);

        int resp = receiveBaseResponse();
        if (resp != 0) {
            close();
            throw new RuntimeException("Should not happen, response unconditionally OK in rh");
        }

        // receive status byte
        byte status = i.readByte();
        return (status == 0);
    }

    ////////////////////////////////////
    // methods with auto-close after request send

    public enum RHSS_ACTION {START,START_ANNOUNCE,STOP,STATUS}

    public static int rhss_action(RHSS_ACTION action, String... servedDirectory) {
        switch (action) {
            case START:
            case START_ANNOUNCE:
                // without auto-close
                try {return (new RemoteServerManager().start_rhss(action==RHSS_ACTION.START_ANNOUNCE,servedDirectory)) ? 1 : 0;}
                catch (IOException e) {return -1;}
            case STOP:
            case STATUS:
                // with auto-close
                try(RemoteServerManager r = new RemoteServerManager()) {
                    switch (action) {
                        case STOP: return r.stop_rhss()?1:0;
                        case STATUS: return r.status_rhss()?1:0;
                    }
                }
                catch (IOException ignored) {}
            default:
                return -1;
        }
    }
    ////////////////////////////////////

    private void onClientConnect(String clientIPandPort, byte[] sessionKey) {
        RHSSServerStatus.StoCSessions.put(clientIPandPort,sessionKey);
        if (XFilesRemoteSessionsManagementActivity.StoCAdapter != null)
            XFilesRemoteSessionsManagementActivity.StoCAdapter.syncFromActivity();

        // show the visual hash of the shared TLS master secret
        if (MainActivity.mainActivity != null) {
            MainActivity.mainActivity.runOnUiThread(()->
//                    new HashViewDialog(MainActivity.mainActivity,sessionKey,true).show());
            {
                try {
                    PopupWindowUtils.createAndShowHashViewPopupWindow(
                            MainActivity.mainActivity,
                            sessionKey,
                            true,
                            RemoteRHServerManagementDialog.instance.findViewById(R.id.remote_rh_server_management_dialog));
                }
                catch (NullPointerException e) {
                    PopupWindowUtils.createAndShowHashViewPopupWindow(
                            MainActivity.mainActivity,
                            sessionKey,
                            true,
                            MainActivity.mainActivity.findViewById(R.id.activity_main));
                }
            });
        }
    }

    private void onClientDisconnect(String clientIPandPort) {
        RHSSServerStatus.StoCSessions.remove(clientIPandPort);
        if (XFilesRemoteSessionsManagementActivity.StoCAdapter != null)
            XFilesRemoteSessionsManagementActivity.StoCAdapter.syncFromActivity();
    }

    // inner thread to be started only after rhss server start, to receive client connect/disconnect updates
    public class RHSSUpdateThread extends Thread /*implements AutoCloseable*/ {
//        @Override
//        public void close() {
//            RemoteServerManager.this.close();
//        }
        private Context rhssLocalContext;

        @Override
        public void run() {
            try {
                // LEGACY
//                boolean ok = false;
//                Thread current = rhssManagerThreadRef.get();
//                if (current == null) {
//                    while(!ok) ok = rhssManagerThreadRef.compareAndSet(null,this);
//                }
//                else error Toast

                // strong cas, a thread is guaranteed to win
                if (!rhssManagerThreadRef.compareAndSet(null,this)) {
                    MainActivity.showToastOnUIWithHandler("Another rhss thread is already receiving updates");
//                    MainActivity.mainActivity.runOnUiThread(() -> Toast.makeText(MainActivity.mainActivity, "Another rhss thread is already receiving updates", Toast.LENGTH_LONG).show());
                    return;
                }

                // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                // update on-screen widgets
                if (MainActivity.mainActivityContext != null) {
                    Log.d("XRE_RHSS","refreshing XRE widget (-> ON)");
                    XRE_RHSS_Widget.updateAllDirect(MainActivity.mainActivityContext);
                }
                else {
                    Log.e("XRE_RHSS","unable to refresh XRE widget, mainActivityContext is null, exiting...");
                    throw new Exception();
                }
                rhssLocalContext = MainActivity.mainActivityContext; // onDestroy resets mainActivityContext reference before this thread can use it for updating widget to OFF
                // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

                for(;;) { // always exits on IOException, when the other socket endpoint is closed
                    // receive started/ended session flag
                    int flag = i.readUnsignedByte();
                    String message = "Remote client "+((flag == 0)?"connected":"disconnected");

                    // receive 1-byte lenght-prended string with client IP
                    int s_len = i.readUnsignedByte();
                    byte[] s_ = new byte[s_len];
                    i.readFully(s_);
                    String clientIP = new String(s_);

                    message += ": " + clientIP;

                    if (flag == 0) {
                        // receive hex SHA256 hash of TLS session shared secret
                        byte[] sessionKey = new byte[32];
                        i.readFully(sessionKey);
                        onClientConnect(clientIP,sessionKey);
                        message += "\nShared secret hash: "+Misc.toHexString(sessionKey);
                    }
                    else {
                        onClientDisconnect(clientIP);
                    }

                    Log.d(this.getClass().getName(),message);
                    final String msg = message;

                    // to be replaced with onClientUpdate
                    MainActivity.showToastOnUIWithHandler(msg);
//                    MainActivity.mainActivity.runOnUiThread(() -> Toast.makeText(MainActivity.mainActivity, msg, Toast.LENGTH_LONG).show());
                }
            }
            catch (Throwable t) {
                Log.d(this.getClass().getName(),"Local socket closed by rhss server or other exception, exiting...");
                if (!(t instanceof IOException)) t.printStackTrace();
                // no need for finally, thread always exits with exception
                close();
                rhssManagerThreadRef.set(null);

                // also update local views of dialog to off, if dialog is active
                RHSSServerStatus.destroyServer();
                if (MainActivity.mainActivity != null) {
                    MainActivity.mainActivity.runOnUiThread(() -> {
                        if (XFilesRemoteSessionsManagementActivity.StoCAdapter != null)
                            XFilesRemoteSessionsManagementActivity.StoCAdapter.syncFromActivity();
                    });
                }

                if (rhssLocalContext != null) {
                    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                    // update on-screen widgets, if any, as well
                    Log.d("XRE_RHSS","refreshing XRE widget (-> OFF)");
                    XRE_RHSS_Widget.updateAllDirect(rhssLocalContext);
                    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                }

                Log.d(this.getClass().getName(),"Really exiting update thread now!");
            }
        }
    }

}

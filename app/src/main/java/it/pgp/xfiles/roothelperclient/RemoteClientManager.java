package it.pgp.xfiles.roothelperclient;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.XFilesRemoteSessionsManagementActivity;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.roothelperclient.reqs.ListOfPathPairs_rq;
import it.pgp.xfiles.roothelperclient.reqs.SinglePath_rq;
import it.pgp.xfiles.roothelperclient.reqs.create_rq;
import it.pgp.xfiles.roothelperclient.reqs.hash_rq;
import it.pgp.xfiles.roothelperclient.reqs.link_rq;
import it.pgp.xfiles.roothelperclient.reqs.ls_rq;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.service.NonInteractiveXFilesRemoteTransferTask;
import it.pgp.xfiles.service.visualization.ProgressIndicator;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.ProgressConflictHandler;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.LocalDirWithContent;
import it.pgp.xfiles.utils.dircontent.XFilesRemoteDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

/**
 * Created by pgp on 20/09/17
 *
 * Remote client manager that performs remote action via the connected RH remote client session
 */

public class RemoteClientManager {

    private static final long EOF_ind = ProgressConflictHandler.Status.EOF.getStatus(); // end of file
    private static final long EOFs_ind = ProgressConflictHandler.Status.EOFs.getStatus(); // end of files

    // use only serverHost as key for now
    public final Map<String,RemoteManager> fastClients = new ConcurrentHashMap<>();
    public final Map<String,RemoteManager> longTermClients = new ConcurrentHashMap<>();

    public synchronized void closeAllSessions() {
        for (RemoteManager rm : fastClients.values()) rm.close();
        for (RemoteManager rm : longTermClients.values()) rm.close();
        fastClients.clear();
        longTermClients.clear();
        if (XFilesRemoteSessionsManagementActivity.CtoSAdapter != null) {
            XFilesRemoteSessionsManagementActivity.CtoSAdapter.syncFromActivity();
        }
    }

    // progress methods and task field put here in order to avoid putting them also in RemoteServerManager, which extends RemoteManager and doesn't publish progress

    public BaseBackgroundTask progressTask;

    private void initProgressSupport(BaseBackgroundTask task) {
        this.progressTask = task;
    }

    private void destroyProgressSupport() {
        this.progressTask = null;
    }

    public RemoteManager createAndConnectClient(String serverHost) {
        try {
            RemoteManager client = new RemoteManager();
            client.o.write(ControlCodes.REMOTE_CONNECT.getValue());

            // send host string with length
            byte[] serverHost_ = serverHost.getBytes();
            byte[] hostLen_ = Misc.castUnsignedNumberToBytes(serverHost_.length,1);
            client.o.write(hostLen_);
            client.o.write(serverHost_);
            // send port
            byte[] port_ = Misc.castUnsignedNumberToBytes(XFilesRemotePathContent.defaultRHRemoteServerPort,2);
            client.o.write(port_);

            int resp = client.receiveBaseResponse();
            if (resp != 0) {
                client.close();
                return null;
            }
            // ok, streams connected to RH in remote client mode

            // receive TLS session hash
            client.i.readFully(client.tlsSessionHash);
            Log.d(this.getClass().getName(),"Client TLS session shared secret hash: "+new String(client.tlsSessionHash));

            // show the visual hash of the shared TLS master secret
            if (MainActivity.mainActivity != null) {
                MainActivity.mainActivity.runOnUiThread(()->
//                        new HashViewDialog(MainActivity.mainActivity,client.tlsSessionHash,true).show());
                PopupWindowUtils.createAndShowHashViewPopupWindow(
                        MainActivity.mainActivity,
                        client.tlsSessionHash,
                        true,
                        MainActivity.mainActivity.findViewById(R.id.activity_main)));
            }

            return client;
        }
        catch (IOException e) {
            return null;
        }
    }

    public RemoteManager getClient(String serverHost, boolean isFastClient) {
        RemoteManager client;
        if (isFastClient) {
            if (fastClients.containsKey(serverHost)) return fastClients.get(serverHost);
            client = createAndConnectClient(serverHost);
            if (client == null) return null;
            fastClients.put(serverHost,client);
        }
        else {
            if (longTermClients.containsKey(serverHost)) return longTermClients.get(serverHost);
            client = createAndConnectClient(serverHost);
            if (client == null) return null;
            longTermClients.put(serverHost,client);
        }
        if (XFilesRemoteSessionsManagementActivity.CtoSAdapter != null) {
            XFilesRemoteSessionsManagementActivity.CtoSAdapter.syncFromActivity();
        }
        return client;
    }

    // TODO make RemoteClientManager implementor of FileOperationHelperUsingPathContent and remove duplicated code in RootHelperClientUsingPathContent
    // TODO use StreamsPair and getStreams(...) in RootHelperClientUsingPathContent, remove duplicated methods from here

    public FileOpsErrorCodes transferItems(CopyMoveListPathContent items, BasePathContent destDir, ControlCodes action, NonInteractiveXFilesRemoteTransferTask progressTask) {

        // get communication endpoint
        String clientKey;
        switch (action) {
            case ACTION_DOWNLOAD:
                clientKey = ((XFilesRemotePathContent)items.parentDir).serverHost;
                break;
            case ACTION_UPLOAD:
                clientKey = ((XFilesRemotePathContent)destDir).serverHost;
                break;
            default:
                throw new RuntimeException("Unexpected action in remote transfer task");
        }

        RemoteManager client = getClient(clientKey,false);
        if (client == null) {
            // unable to connect
            return FileOpsErrorCodes.CONNECTION_ERROR;
        }

        initProgressSupport(progressTask);

        ArrayList<String> v_fx = new ArrayList<>();
        ArrayList<String> v_fy = new ArrayList<>();

        for (BrowserItem fname : items.files) {
            v_fx.add(items.parentDir.dir+"/"+fname.getFilename());
            v_fy.add(destDir.dir+"/"+fname.getFilename());
        }

        ListOfPathPairs_rq r = new ListOfPathPairs_rq(v_fx,v_fy);
        r.requestType = action;

        try {
            r.write(client.o);

            // receive total number of files for outer progress
            final long totalFileCount = Misc.receiveTotalOrProgress(client.i);
            final long totalSize = Misc.receiveTotalOrProgress(client.i);
            Log.e("XREProgress","Total size is "+totalSize);
            long currentFileCount = 0;
            long totalSizeSoFar = 0; // rounded to last completed file
//            long currentFileSize = EOF_ind; // legacy, maybe breaks things
            long currentFileSize = 0; // placeholder, just to avoid uninitialized error

            boolean hasReceivedSizeForCurrentFile = false;

            // receive progress for single files, increment outer progress bar by 1 on EOF_ind progress

            for (;;) {
                long tmp = Misc.receiveTotalOrProgress(client.i);

                if (tmp == EOF_ind) {
                    Log.e("XREProgress","Received EOF, file count before: "+currentFileCount);
                    hasReceivedSizeForCurrentFile = false;
                    currentFileCount++;
                    totalSizeSoFar += currentFileSize;
                    // LEGACY
                    /*this.progressTask.publishProgressWrapper(
                            (int)Math.round(currentFileCount*100.0/totalFileCount),
                            0
                    );*/

                    // NEW
                    this.progressTask.publishProgressWrapper(
                            (int)Math.round(totalSizeSoFar*100.0/totalSize),
                            0
                    );

                }
                else if (tmp == EOFs_ind) {
                    Log.e("XREProgress","Received EOFs");
                    break;
                }
                else {
                    Log.e("XREProgress","Received progress or size");
                    if (hasReceivedSizeForCurrentFile) {
                        Log.e("XREProgress","It's progress: "+tmp);
                        if (this.progressTask != null) {
                            // LEGACY
                            /*this.progressTask.publishProgressWrapper(
                                    (int) Math.round(currentFileCount * 100.0 / totalFileCount),
                                    (int) Math.round(tmp * 100.0 / currentFileSize)
                            );*/

                            // NEW
                            this.progressTask.publishProgressWrapper(
                                    (int) Math.round((totalSizeSoFar+tmp) * 100.0 / totalSize),
                                    (int) Math.round(tmp * 100.0 / currentFileSize)
                            );
                        }
                    }
                    else {
                        Log.e("XREProgress","It's size: "+tmp);
                        // here, tmp is current file's size, before starting copying current file
                        currentFileSize = tmp;
                        hasReceivedSizeForCurrentFile = true;
                        if (this.progressTask != null) {
                            // LEGACY
                            /*this.progressTask.publishProgressWrapper(
                                    (int)Math.round(currentFileCount*100.0/totalFileCount),
                                    0
                            );*/

                            // NEW
                            this.progressTask.publishProgressWrapper(
                                    (int) Math.round(totalSizeSoFar * 100.0 / totalSize),
                                    0
                            );
                        }
                    }
                }
            }
            return FileOpsErrorCodes.TRANSFER_OK;
        }
        catch (IOException e) {
            client.close();
            longTermClients.remove(clientKey);
            return FileOpsErrorCodes.TRANSFER_ERROR;
        }
        finally {
            destroyProgressSupport();
        }
    }
}

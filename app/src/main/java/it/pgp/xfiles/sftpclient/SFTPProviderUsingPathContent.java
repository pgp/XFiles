package it.pgp.xfiles.sftpclient;

import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import net.schmizz.sshj.common.Base64;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPEngine;
import net.schmizz.sshj.sftp.StatefulSFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FilePermission;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.SSHAlreadyInKnownHostsDialog;
import it.pgp.xfiles.dialogs.SSHNotInKnownHostsDialog;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.utils.FileOperationHelperUsingPathContent;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.SftpDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;

/**
 * Created by pgp on 15/05/17
 * Adapted from {@link SFTPProvider}
 * Last updated on 03/11/17 (migration to custom XSFTP types for handling transfer progress)
 */

public class SFTPProviderUsingPathContent implements FileOperationHelperUsingPathContent {

    BaseBackgroundTask task;
    @Override
    public void initProgressSupport(BaseBackgroundTask task) {
        this.task = task;
    }

    @Override
    public void destroyProgressSupport() {
        task = null;
    }

    private final Map<String,XSFTPClient> channels = new ConcurrentHashMap<>(); // TODO to be renamed in clients
    private final Map<String,XSSHClient> xsshclients = new ConcurrentHashMap<>(); // needed for launching count files commands, 1 to 1 with channels

    private final File sshIdsDir;
    private File knownHostsFile;
    private GenericDBHelper dbh;

    static final String sshIdsDirName = ".ssh";
    static final String knownHostsFilename = "known_hosts"; // concat path with sshIdsDirName

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int CHANNEL_TIMEOUT_MS = 1000;

    private final List<File> identities = new ArrayList<>();
    private final MainActivity mainActivity;

    static {
        // TODO restructure code using AsyncTask and remove policy loosening
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

    public void closeAllSessions() {
        for (XSFTPClient x : channels.values())
            try {x.close();} catch (Exception ignored) {}
        for (XSSHClient x : xsshclients.values())
            try {x.close();} catch (Exception ignored) {}

        // actually not needed, on next onCreate a SFTPProviderUsingPathContent is created
//        channels.clear();
//        xsshclients.clear();
    }

    /********************* SSHJ methods ***************************************/

    // adds a host key to known_hosts
    public void addHostKey(String hostname, PublicKey key) throws IOException {
        String keyString = Base64.encodeBytes(new Buffer.PlainBuffer().putPublicKey(key).getCompactData());
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(knownHostsFile,true));
        outputStream.write((hostname+" "+ KeyType.fromKey(key)+" "+keyString+"\n").getBytes());
        outputStream.close();
    }

    // reads known_hosts line by line, copying lines to a new file, excluding
    // the one with the given host and host key (if present)
    // then it replaces the old with the new file
    public void removeHostKey(String adjustedHostname, PublicKey key) throws IOException {
        String keyString = Base64.encodeBytes(new Buffer.PlainBuffer().putPublicKey(key).getCompactData());
        String s = adjustedHostname + " " + KeyType.fromKey(key) + " " + keyString;
        BufferedReader r = new BufferedReader(new FileReader(knownHostsFile));
        File g = new File(knownHostsFile.getAbsolutePath()+"_new");
        BufferedWriter w = new BufferedWriter(new FileWriter(g));

        String line;
        while ((line = r.readLine()) != null) {
            if (!line.equals(s)) {
                w.write(line + "\n");
            }
        }
        r.close();
        w.close();

        if (knownHostsFile.delete() && g.renameTo(knownHostsFile))
            Log.d(getClass().getName(),"known_hosts updated");
        else
            Log.e(getClass().getName(),"error replacing old known_hosts file");
    }

    // replaces the key for the (possibly existing) pair (host, host key algorithm) with the current key
    // TODO string split by space and then split by ',' , hostnames can be concatenated for same algorithm and key (eg. IPs and relative hostnames)
    public void updateHostKey(String adjustedHostname, PublicKey key) throws IOException {
        String keyString = Base64.encodeBytes(new Buffer.PlainBuffer().putPublicKey(key).getCompactData());
        String s = adjustedHostname + " " + KeyType.fromKey(key);
        BufferedReader r = new BufferedReader(new FileReader(knownHostsFile));
        File g = new File(knownHostsFile.getAbsolutePath()+"_new");
        BufferedWriter w = new BufferedWriter(new FileWriter(g));

        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith(s)) {
                w.write(s+" "+keyString);
            }
            else {
                w.write(line + "\n");
            }
        }
        r.close();
        w.close();

        if (knownHostsFile.delete() && g.renameTo(knownHostsFile))
            Log.d(getClass().getName(),"known_hosts updated");
        else
            Log.e(getClass().getName(),"error replacing old known_hosts file");
    }

    /************************************************************/

    public SFTPProviderUsingPathContent(final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        sshIdsDir = new File(mainActivity.getApplicationContext().getFilesDir(),sshIdsDirName);
        if (!sshIdsDir.exists()) sshIdsDir.mkdirs();
        knownHostsFile = new File(sshIdsDir,knownHostsFilename);
        if (!knownHostsFile.exists()) try {
            knownHostsFile.createNewFile();
        } catch (IOException e) {
            Log.e(getClass().getName(),"Cannot create known_hosts file");
        }

        identities.clear();
        identities.addAll(Arrays.asList(sshIdsDir.listFiles(IdentitiesVaultAdapter.idFilter)));
        dbh = new GenericDBHelper(mainActivity.getApplicationContext());
        channels.clear();
    }

    // better version of getChannel
    // input: RemotePathContent without dir
    // output: RemotePathContent with default (home) dir (stat(.) after login) or with FileOpsErrorCodes error
    public RemotePathContent tryConnectAndGetPath(RemotePathContent path) {
        // try to get channel, if already connected
        XSFTPClient cSFTP = channels.get(path.authData.toString());

        if (cSFTP != null) { // connection already active, stat home dir in order to get its pathname
            try {
                String fullPath = cSFTP.canonicalize("."); // TODO check if canonicalize works
                return new RemotePathContent(path.authData,fullPath);
            } catch (IOException e) {
                e.printStackTrace();
                return new RemotePathContent(path.authData,FileOpsErrorCodes.CONNECTION_ERROR);
            }
        }

        XSSHClient c = null;
        try {
            // if not connected
            // try to open a session using all possible identities, and password if available
            c = new XSSHClient(new CustomizedAndroidCipherSuiteConfig());
            c.addHostKeyVerifier(new InteractiveHostKeyVerifier(knownHostsFile));
            AuthData dbData = dbh.find(path.authData);
            if (dbData != null) // found (not necessarily with password)
                path.authData = dbData;

            c.setConnectTimeout(100000); // 100 seconds timeout for debugging
//            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
//            c.setTimeout(CHANNEL_TIMEOUT_MS);
            c.connect(path.authData.domain,path.authData.port);

            // try auth with every available identity
            if (identities != null && identities.size() != 0) {
                for (File i : identities) {
                    KeyProvider keys = c.loadKeys(i.getAbsolutePath());
                    try {
                        c.authPublickey(path.authData.username,keys);
                        break;
                    }
                    catch (UserAuthException e) {
                        // auth error with the given identity
                        continue;
                    }
                }
            }

            // if every identity-based authentication has failed, try with password (if any)
            if (!c.isAuthenticated() && path.authData.password != null) {
                try {
                    c.authPassword(path.authData.username,path.authData.password);
                }
                catch (UserAuthException ignored) {}
            }

            // exhausted auth methods, no valid credentials found
            if (!c.isAuthenticated()) {
                return new RemotePathContent(path.authData,FileOpsErrorCodes.AUTHENTICATION_ERROR);
            }

            cSFTP = c.newXSFTPClient();
            if (cSFTP != null) { // connection already active, stat home dir in order to get its pathname
                channels.put(path.authData.toString(),cSFTP);
                xsshclients.put(path.authData.toString(),c);
                try {
                    String fullPath = cSFTP.canonicalize("."); // TODO check if canonicalize works
                    return new RemotePathContent(path.authData,fullPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return new RemotePathContent(path.authData,FileOpsErrorCodes.CONNECTION_ERROR);
                }
            }
            else return new RemotePathContent(path.authData,FileOpsErrorCodes.CONNECTION_ERROR);
        }
        catch(TransportException e) {
            if (e.getDisconnectReason() == DisconnectReason.HOST_KEY_NOT_VERIFIABLE) {
                if (InteractiveHostKeyVerifier.lastHostKeyHasChanged != null){
                    if (InteractiveHostKeyVerifier.lastHostKeyHasChanged) {
                        return new RemotePathContent(path.authData,FileOpsErrorCodes.HOST_KEY_CHANGED_ERROR);
                    } else {
                        return new RemotePathContent(path.authData,FileOpsErrorCodes.HOST_KEY_INEXISTENT_ERROR);
                    }
                }
            }
            else {
                Log.e(getClass().getName(),"transport exception in getChannel: "+e.getMessage());
            }
        }
        catch (IOException e) {
            Log.e(getClass().getName(),"getChannel error");
        }

        // in any failure case, close connection with SSH server and return null
        try {
            cSFTP.close();
            c.disconnect();
        }
        catch (IOException|NullPointerException ignored) {}
        return new RemotePathContent(path.authData,FileOpsErrorCodes.CONNECTION_ERROR);
    }

    public XSFTPClient getChannelIfAlreadyExists(AuthData authData) throws IOException {
        XSFTPClient x = channels.get(authData.toString());
        if (x == null) throw new IOException("No remote channel currently opened for the given remote path");
        return x;
    }

    /**
     * pendingLsPath: path for doing again ls request in dialog dismiss listener
     * on resolvable failure (host key added/updated, auth retry)
     * Only LS allowed (if starting request is create file or dir (which invokes exists/ is dir)
     * simply show toast error message and don't propagate request
     */
    public XSFTPClient getChannel(AuthData authData, BasePathContent pendingLsPath) {
        // try to get channel, if already connected
        XSFTPClient cSFTP = channels.get(authData.toString());
        if (cSFTP != null) return cSFTP;
        XSSHClient c = null;

        try {
            // if not connected
            // try to open a session using all possible identities, and password if available
            c = new XSSHClient(new CustomizedAndroidCipherSuiteConfig());
            c.addHostKeyVerifier(new InteractiveHostKeyVerifier(knownHostsFile));

            AuthData completeData = (authData.password == null)?dbh.find(authData):authData;
            if (completeData != null) // found (not necessarily with password)
                authData = completeData;

            c.setConnectTimeout(100000); // 100 seconds timeout for debugging
//            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
//            c.setTimeout(CHANNEL_TIMEOUT_MS);
            c.connect(authData.domain,authData.port);

            // try auth with every available identity
            if (identities != null && identities.size() != 0) {
                for (File i : identities) {
                    KeyProvider keys = c.loadKeys(i.getAbsolutePath());
                    try {
                        c.authPublickey(authData.username,keys);
                        break;
                    }
                    catch (UserAuthException e) {
                        // auth error with the given identity
                        continue;
                    }
                }
            }

            // if every identity-based authentication has failed, try with password (if any)
            if (!c.isAuthenticated() && authData.password != null) {
                try {
                    c.authPassword(authData.username,authData.password);
                }
                catch (UserAuthException ignored) {}
            }

            // No valid auth found
            if (!c.isAuthenticated()) {
                Toast.makeText(mainActivity, "Exhausted auth methods", Toast.LENGTH_SHORT).show();
                return null;
            }
            cSFTP = c.newXSFTPClient();
            channels.put(authData.toString(),cSFTP);
            xsshclients.put(authData.toString(),c);
            // no sessionConnectRunnable for now
            return cSFTP;
        }
        catch(TransportException e) {
            if (e.getDisconnectReason() == DisconnectReason.HOST_KEY_NOT_VERIFIABLE) {
                if (InteractiveHostKeyVerifier.lastHostKeyHasChanged != null && pendingLsPath != null){
                    if (InteractiveHostKeyVerifier.lastHostKeyHasChanged) {
                        // show "last host key changed" dialog, containing current getChannel input parameter
                        new SSHAlreadyInKnownHostsDialog(
                                        mainActivity,
                                        authData,
                                        null, // FIXME need to have old host key here
                                        InteractiveHostKeyVerifier.currentHostKey,
                                        this,
                                        pendingLsPath).show();
                    }
                    else {
                        // show "add host key" dialog, containing current getChannel input parameter
                        new SSHNotInKnownHostsDialog(
                                        mainActivity,
                                        authData,
                                        InteractiveHostKeyVerifier.currentHostKey,
                                        this,
                                        pendingLsPath).show();
                    }
                }
                 // any way, won't get a list dir response at this request, dismiss listeners in dialogs will do the job by calling main activity methods
            }
            else {
                Log.e(getClass().getName(),"transport exception in getChannel: "+e.getMessage());
            }
        }
        catch (IOException e) {
            Log.e(getClass().getName(),"getChannel error");
        }

        // in any failure case, close connection with SSH server and return null
        try {
            cSFTP.close();
            c.disconnect();
        }
        catch (IOException|NullPointerException ignored) {}
        return null;
    }

    @Override
    public void createFileOrDirectory(BasePathContent filePath, FileMode fileOrDirectory, FileCreationAdvancedOptions... unused) throws IOException {
        RemotePathContent g = (RemotePathContent) filePath;

        // try to get channel, using prefix from last GenericRemotePath object
        XSFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) throw new IOException("No channel found");

        if (exists(filePath))
            throw new IOException("File already exists"); // FIXME temporary, remove once changed Fileopshelper interface to boolean return values

        try {
            switch (fileOrDirectory) {
                case FILE:
                    channelSftp.open(g.dir, EnumSet.of(OpenMode.CREAT));
//                    channelSftp.put("/dev/null",g.remotePath); // not working with dev null
                    break;
                case DIRECTORY:
                    channelSftp.mkdir(g.dir);
                    break;
                default:
                    throw new RuntimeException("Unknown file mode");
            }
        }
        catch (IOException e) { // TODO simplify to boolean return value
            throw new IOException("Unable to create item "+filePath);
        }
    }

    @Override
    public void createLink(BasePathContent originPath, BasePathContent linkPath, boolean isHardLink) throws IOException {
        RemotePathContent originPath_,linkPath_;
        try {
            originPath_ = (RemotePathContent) originPath;
            linkPath_ = (RemotePathContent) linkPath;
        }
        catch (ClassCastException e) {
            throw new IOException("Only SFTP paths allowed from here");
        }
        if (!originPath_.authData.equals(linkPath_.authData)) throw new IOException("Origin and link must belong to the same SFTP remote filesystem");

        // try to get channel, using prefix from last GenericRemotePath object
        XSFTPClient channelSftp = getChannel(originPath_.authData,null);
        if (channelSftp == null) throw new IOException("No channel found");

        if (isHardLink) throw new IOException("SFTP Hard links currently not supported");
        channelSftp.symlink(linkPath_.dir,originPath_.dir);
    }

    private void recursiveFolderDelete(SFTPEngine channelSftp_, String path) throws IOException {
        StatefulSFTPClient channelSftp = new StatefulSFTPClient(channelSftp_);
        channelSftp.cd(path); // Change Directory on SFTP Server
        // List source directory structure.
        List<RemoteResourceInfo> fileAndFolderList;
        try {
            fileAndFolderList = channelSftp.ls(path);
        }
        catch (IOException e) {
            return; // folder to be deleted does not exist
        }
        // Iterate objects in the list to get file/folder names.
        for (RemoteResourceInfo item : fileAndFolderList) {
            // If it is a file (not a directory).
            if (item.getAttributes().getType() != net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY) {
                channelSftp.rm(path + "/" + item.getName()); // Remove file.
            }
            else if (!(".".equals(item.getName()) || "..".equals(item.getName()))) { // If it is a subdir.
                recursiveFolderDelete(channelSftp.getSFTPEngine(), path + "/" + item.getName()); // remove listable content first
            }
        }
        channelSftp.rmdir(path); // try to delete the parent directory if emptied successfully
    }

    @Override
    public void deleteFilesOrDirectories(List<BasePathContent> files) throws IOException {
        // TODO replace IOExceptions with return values
        RemotePathContent g = null;
        List<String> remotePaths = new ArrayList<>();

        for (BasePathContent x : files) {
            try {
                // parse generic path
                g = (RemotePathContent)x;
                remotePaths.add(g.dir);
            }
            catch (RuntimeException r) {
                throw new IOException("Malformed path");
            }
        }
        // try to get channel, using prefix from last GenericRemotePath object
        XSFTPClient channelSftp = getChannel(g.authData,null); // assuming all paths in browseradapter selection having authdata in common
        if (channelSftp == null) throw new IOException("No channel found");

        FileAttributes attrs;
        for (String x : remotePaths) {
            try {
                attrs = channelSftp.stat(x);
//                Log.d(getClass().getName(),"item name: "+x+"\tattr type: "+attrs.getType());
                if (attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY)
                     recursiveFolderDelete(channelSftp.getSFTPEngine(),x);
                else channelSftp.rm(x);
            }
            catch (IOException e) {
                Log.e(getClass().getName(),"Unable to delete element "+x);
                throw new IOException("Unable to delete element "+x);
            }
        }
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListPathContent files, BasePathContent dstFolder) throws IOException {
        // needs collaboration of fileopshelper handling operations on files in source folder,
        // discriminate based on prefix (like dircommander performs dispatching)
        // (e.g. local, or other channel in SFTPProvider for remote-to-local-to-remote copy)
        // most interesting case: remote-to-remote copy (protocol-level sftp)

        // supported operations: upload (files from local path, dstFolder remote) and download
        // (files remote, dstFolder local)
        // TODO on download, try listing dstFolder in order to check access on dstFolder

        if (files.parentDir.providerType == ProviderType.LOCAL && dstFolder.providerType == ProviderType.SFTP) {
            // upload (assumes remote directory already listed (connection already open))
            XSFTPClient sftpClient = getChannelIfAlreadyExists(((RemotePathContent)dstFolder).authData);

            // legacy, without progress & service support
//            for (String localItem : files)
//                sftpClient.put(localItem,dstFolder.dir+"/");

            // with progress & service support
            // for safety, reset progress before counting files
            XProgress xp = (XProgress) task.mr;
            xp.clear();
            sftpClient.setProgressIndicator(xp);

            // count local files via local roothelper or xfilesopshelper and set them in xprogress
//            long totalLocalFiles = 0;
            long totalLocalSize = 0;
            for (BrowserItem localItem : files.files) {
                BasePathContent bpc = files.parentDir.concat(localItem.getFilename());
                if (MainActivity.xFilesUtils.isDir(bpc)) {
                    folderStats_resp fsr = MainActivity.xFilesUtils.statFolder(bpc);
//                    totalLocalFiles+=fsr.totalFiles;
//                    totalLocalFiles+=fsr.totalDirs;
                    totalLocalSize+=fsr.totalSize;
                }
                else {
//                    totalLocalFiles++;
                    totalLocalSize+=MainActivity.xFilesUtils.statFile(bpc).size;
                }
            }

            xp.totalFilesSize = totalLocalSize;
            xp.isDetailedProgress = true;
            for (BrowserItem localItem : files.files)
                sftpClient.put(files.parentDir.concat(localItem.getFilename()).toString(),dstFolder.dir+"/"+localItem.getFilename());
        }
        else if (files.parentDir.providerType == ProviderType.SFTP && dstFolder.providerType == ProviderType.LOCAL) {
            // download
            AuthData authData = ((RemotePathContent)files.parentDir).authData;
            XSFTPClient sftpClient = getChannelIfAlreadyExists(authData);
            XSSHClient xsshClient = xsshclients.get(authData.toString()); // needed for remote file counting, must exist if the previous line doesn't throw exception
            if (xsshClient == null) throw new IOException("Unexpected null: xsshclient");

            XProgress xp = (XProgress) task.mr;
            xp.clear();
            sftpClient.setProgressIndicator(xp);


            // LEGACY
//            xp.totalFiles = xsshClient.countTotalRegularFilesInItems(files.getSFTPProgressHelperIterable());
//            if (xp.totalFiles < 0) {
//                MainActivity.showToastOnUIWithHandler("Unable to count remote files, external progress will not be available");
//                xp.totalFiles = Long.MAX_VALUE;
//            }

            // first attempt: launch external commands (du, python, dir) to count remote files
            long totalRemoteSize = xsshClient.countTotalSizeInItems(files.getSFTPProgressHelperIterableFilenamesOnly(),files.parentDir.dir);
            if (totalRemoteSize <= 0) {
                MainActivity.showToastOnUIWithHandler("All external commands for remote size count failed, external progress won't be available");
                xp.totalFiles = Long.MAX_VALUE;
            }
            else {
                xp.totalFilesSize = totalRemoteSize;
                xp.isDetailedProgress = true;
            }

            for (BrowserItem remoteItemName : files.files) { // iterator over filenames only
                // remote dir as local path string
                // ending "/" in order to paste a folder as a child of the destination folder
                sftpClient.get(files.parentDir.dir+"/"+remoteItemName.getFilename(),dstFolder.dir+"/");
            }
        }
        else if (files.parentDir.providerType == ProviderType.SFTP && dstFolder.providerType == ProviderType.SFTP) {
            if (files.copyOrMove==CopyMoveMode.MOVE) {
                if (((RemotePathContent)files.parentDir).authData.equals(((RemotePathContent)dstFolder).authData)) {
                    XSFTPClient sftpClient = getChannelIfAlreadyExists(((RemotePathContent)files.parentDir).authData);

                    for (BrowserItem remoteItemName : files.files) { // iterator over filenames only
                        // remote dir as local path string
                        sftpClient.rename(files.parentDir.dir+"/"+remoteItemName.getFilename(),dstFolder.dir+"/"+remoteItemName.getFilename());
                    }
                }
                else {
                    throw new IOException("Unsupported remote-to-remote copy on the same host (only move)");
                }
            }
            else {
                throw new IOException("Unsupported remote-to-remote copy");
            }
        }
        else throw new IOException("Unsupported remote transfer");
    }

    @Override
    public boolean renameFile(BasePathContent oldPathname, BasePathContent newPathname) throws IOException {
        try {
            RemotePathContent oldPathname_ = (RemotePathContent) oldPathname;
            RemotePathContent newPathname_ = (RemotePathContent) newPathname;
            if (!oldPathname_.authData.equals(newPathname_.authData))
                throw new IOException("Rename paths must belong to the same remote filesystem");

            XSFTPClient sftpClient = channels.get(oldPathname_.authData.toString());
            // copying assumes remote directory already listed (connection already open)
            if (sftpClient == null) throw new IOException("No remote channel currently opened for the given remote path");
            sftpClient.rename(oldPathname_.dir,newPathname_.dir);
            return true; // not really indicative here, SSHJ does not return value for rename
        }
        catch (ClassCastException c) {
            throw new IOException("Both paths have to be remote for renaming");
        }
    }

    public String getPermString(Set<FilePermission> s, boolean isDir) {
        String p = isDir?"d":"-";

        p += s.contains(FilePermission.USR_R)?"r":"-";
        p += s.contains(FilePermission.USR_W)?"w":"-";
        p += s.contains(FilePermission.USR_X)?"x":"-";
        p += s.contains(FilePermission.GRP_R)?"r":"-";
        p += s.contains(FilePermission.GRP_W)?"w":"-";
        p += s.contains(FilePermission.GRP_X)?"x":"-";
        p += s.contains(FilePermission.OTH_R)?"r":"-";
        p += s.contains(FilePermission.OTH_W)?"w":"-";
        p += s.contains(FilePermission.OTH_X)?"x":"-";

        return p;
    }

    @Override
    public SingleStatsItem statFile(BasePathContent pathname) throws IOException {
        RemotePathContent g = (RemotePathContent) pathname;

        // try to get channel
        XSFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return null; // abuse of notation

        FileAttributes attrs = channelSftp.stat(g.dir);

        return new SingleStatsItem(
                (attrs.getGID()+""), // TODO group string instead of id
                (attrs.getUID()+""), // TODO owner string instead of id
                new Date(0L), // creation time not available
                new Date(attrs.getAtime()*1000L),
                new Date(attrs.getMtime()*1000L),
                getPermString(attrs.getPermissions(),
                        attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY),
                attrs.getSize()
        );
    }

    @Override
    public folderStats_resp statFiles(List<BasePathContent> files) throws IOException {
        // TODO
        return null;
    }

    @Override
    public folderStats_resp statFolder(BasePathContent pathname) throws IOException {
        if (!(pathname instanceof RemotePathContent)) throw new IOException("Wrong path content type");
        RemotePathContent rpc = (RemotePathContent) pathname;
        XSSHClient xsshClient = xsshclients.get(rpc.authData.toString());
        if (xsshClient==null) return null;

        // all items
        folderStats_resp fs = xsshClient.statFoldersInPaths(new AbstractMap.SimpleEntry<>(pathname.dir,true));
        if(fs.totalDirs!=0) fs.totalDirs--; // exclude current directory from find results

        // children items
        RemotePathContent g = (RemotePathContent) pathname;
        XSFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return fs;
        try {
            List<RemoteResourceInfo> lsContent = channelSftp.ls(g.dir);

            for (RemoteResourceInfo entry : lsContent) {
                FileAttributes fa = entry.getAttributes();
                if (fa.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY)
                    fs.childrenDirs++;
                else fs.childrenFiles++;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // total size
        fs.totalSize = xsshClient.countTotalSizeInItems(
                Collections.singletonList(new AbstractMap.SimpleEntry<>(pathname.dir, null)),
                pathname.getParent().dir);

        return fs;
    }

    @Override
    public boolean exists(BasePathContent pathname) {
        RemotePathContent g = (RemotePathContent) pathname;

        // try to get channel
        XSFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return false; // abuse of notation
        try {
            FileAttributes fa = channelSftp.statExistence(g.dir);
            return fa != null;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isFile(BasePathContent pathname) {
        return !isDir(pathname); // not used for now
    }

    @Override
    public boolean isDir(BasePathContent pathname) {
        RemotePathContent g = (RemotePathContent) pathname;

        // try to get channel
        XSFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return false; // abuse of notation

        try {
            FileAttributes fa = channelSftp.stat(g.dir);
            return fa.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public byte[] hashFile(BasePathContent pathname,
                           HashRequestCodes hashAlgorithm,
                           BitSet dirHashOpts) throws IOException {
        return new byte[0];
    }

    @Override
    public GenericDirWithContent listDirectory(BasePathContent directory) {
        RemotePathContent g = (RemotePathContent) directory;

        // try to get channel
        XSFTPClient channelSftp = getChannel(g.authData,directory);
        if (channelSftp == null) return new SftpDirWithContent(g.authData,FileOpsErrorCodes.AUTHENTICATION_ERROR); // TODO connection error to be handled and returned as well

        // list dir
        try {
            List<RemoteResourceInfo> lsContent = channelSftp.ls(g.dir);
            List<BrowserItem> l = new ArrayList<>();

            for (RemoteResourceInfo entry : lsContent) {
                boolean isLink = false;
                FileAttributes fa = entry.getAttributes();
                if (fa.getType() == net.schmizz.sshj.sftp.FileMode.Type.SYMLINK) {
                    isLink = true;
                    fa = channelSftp.stat(g.dir+"/"+entry.getName());
                }

                l.add(new BrowserItem(entry.getName(),
                        fa.getSize(),
                        new Date(fa.getMtime()*1000L),
                        fa.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY,
                        isLink));
            }

            // successful return, change current helper
            MainActivity.currentHelper = MainActivity.sftpProvider; // or = this
            return new SftpDirWithContent(g.authData,directory.dir,l);
        }
        catch (IOException e) {
            return new SftpDirWithContent(g.authData,FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
        }
    }

    @Override
    public GenericDirWithContent listArchive(BasePathContent archivePath) {
        return new GenericDirWithContent(FileOpsErrorCodes.NOT_IMPLEMENTED);
    }

    @Override
    public int compressToArchive(BasePathContent srcDirectory,
                                  BasePathContent destArchive,
                                  Integer compressionLevel,
                                  Boolean encryptHeaders,
                                  Boolean solidMode,
                                  String password,
                                  List<String> filenames) throws IOException {
        return -1; // not implemented
    }

    @Override
    public FileOpsErrorCodes extractFromArchive(BasePathContent srcArchive,
                                                BasePathContent destDirectory,
                                                @Nullable String password,
                                                @Nullable List<String> filenames,
                                                boolean smartDirectoryCreation) throws IOException {
        return null;
    }

    @Override
    public int setDates(BasePathContent file, @Nullable Date accessDate, @Nullable Date modificationDate) {
        return -1;
    }

    @Override
    public int setPermissions(BasePathContent file, int permMask) {
        return -1;
    }

    @Override
    public int setOwnership(BasePathContent file, @Nullable Integer ownerId, @Nullable Integer groupId) {
        return -1;
    }

}

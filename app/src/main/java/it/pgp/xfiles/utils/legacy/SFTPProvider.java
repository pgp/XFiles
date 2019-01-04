package it.pgp.xfiles.utils.legacy;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Base64;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;
import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.sftpclient.CustomizedAndroidCipherSuiteConfig;
import it.pgp.xfiles.sftpclient.IdentitiesVaultAdapter;
import it.pgp.xfiles.sftpclient.InteractiveHostKeyVerifier;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 05/10/16
 * Last modified on 06/03/17
 */

@Deprecated
public class SFTPProvider implements FileOperationHelper {

    private Map<String,SFTPClient> channels; // TODO to be renamed in clients
    private final File sshIdsDir;
    private File knownHostsFile;
    private GenericDBHelper dbh;

    static final String sshIdsDirName = ".ssh";
    static final String knownHostsFilename = "known_hosts"; // concat path with sshIdsDirName

    /**************** TODO find how to set timeout **********************/
    private static final int SESSION_CONNECT_TIMEOUT_MS = 3000;
    private static final int CHANNEL_CONNECT_TIMEOUT_MS = 1000;

    private File[] identities;
    private final MainActivity mainActivity;

    static {
        // TODO restructure code using AsyncTask and remove policy loosening
        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);
    }

    /********************* SSHJ methods ***************************************/

    public void addHostKey(String hostname, PublicKey key) throws IOException {
        String keyString = Base64.encodeBytes(new Buffer.PlainBuffer().putPublicKey(key).getCompactData());
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(knownHostsFile,true));
        outputStream.write((hostname+" "+ KeyType.fromKey(key)+" "+keyString+"\n").getBytes());
        outputStream.close();
    }

    // reads known_hosts line by line, copying lines to a new file, excluding
    // the one with the given host and host key (if present)
    // then it replaces the old with the new file
    public void removeHostKey(String hostname, PublicKey key) throws IOException {
        String keyString = Base64.encodeBytes(new Buffer.PlainBuffer().putPublicKey(key).getCompactData());
        String s = hostname + " " + KeyType.fromKey(key) + " " + keyString;
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
            Log.d(this.getClass().getName(),"known_hosts updated");
        else
            Log.e(this.getClass().getName(),"error replacing old known_hosts file");
    }

    // replaces the key for the (possibly existing) pair (host, host key algorithm) with the current key
    // TODO string split by space and then split by ',' , hostnames can be concatenated for same algorithm and key (eg. IPs and relative hostnames)
    public void updateHostKey(String hostname, PublicKey key) throws IOException {
        String keyString = Base64.encodeBytes(new Buffer.PlainBuffer().putPublicKey(key).getCompactData());
        String s = hostname + " " + KeyType.fromKey(key);
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
            Log.d(this.getClass().getName(),"known_hosts updated");
        else
            Log.e(this.getClass().getName(),"error replacing old known_hosts file");
    }

    /************************************************************/

    private class GenericRemotePath {
        AuthData authData; // parsed from sftp://user@domain:port
        String remotePath; // format: /remote/path

        GenericRemotePath(String path) throws RuntimeException {
            if (path.startsWith("sftp://")) {
                String s = path.substring(7); // user@domain:port/remote/path
                String authString = s.split("/")[0]; // user@domain:port
                authData = new AuthData(authString);
                int idx = s.indexOf('/');
                // remote path beginning immediately after port number, and beginning with '/'
                remotePath = s.substring(idx);
            }
            else throw new RuntimeException("");
        }
    }

    public SFTPProvider(final Context context, final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        sshIdsDir = new File(context.getFilesDir(),sshIdsDirName);
        if (!sshIdsDir.exists()) sshIdsDir.mkdirs();
        knownHostsFile = new File(sshIdsDir,knownHostsFilename);
        if (!knownHostsFile.exists()) try {
            knownHostsFile.createNewFile();
        } catch (IOException e) {
            Log.e(this.getClass().getName(),"Cannot create known_hosts file");
        }

        identities = sshIdsDir.listFiles(IdentitiesVaultAdapter.idFilter);
        dbh = new GenericDBHelper(context);
        channels = new HashMap<>();
    }

    public SFTPClient getChannel(String connId, String pendingLsPath) {
        return getChannel(new AuthData(connId), pendingLsPath);
    }

    /**
     * pendingLsPath: path for doing again ls request in dialog dismiss listener
     * on resolvable failure (host key added/updated, auth retry)
     * Only LS allowed (if starting request is create file or dir (which invokes exists/ is dir)
     * simply show toast error message and don't propagate request
     */
    public SFTPClient getChannel(AuthData authData, String pendingLsPath) {
        // try to get channel, if already connected
        SFTPClient cSFTP = channels.get(authData.toString());
        if (cSFTP != null) return cSFTP;
        SSHClient c = null;

        try {
            // if not connected
            // try to open a session using all possible identities, and password if available
            c = new SSHClient(new CustomizedAndroidCipherSuiteConfig());
            c.addHostKeyVerifier(new InteractiveHostKeyVerifier(knownHostsFile));
            AuthData dbData = dbh.find(authData);
            if (dbData != null) // found (not necessarily with password)
                authData = dbData;

            c.setConnectTimeout(100000); // 100 seconds timeout for debugging
            c.connect(authData.domain,authData.port);

            // try auth with every available identity
            if (identities != null && identities.length != 0) {
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

            // No valid auth found TODO remove runtime exception
            if (!c.isAuthenticated()) throw new RuntimeException("Exhausted auth methods");
            cSFTP = c.newSFTPClient();
            channels.put(authData.toString(),cSFTP);
            // no sessionConnectRunnable for now
            return cSFTP;
        }
        catch(TransportException e) {
            if (e.getDisconnectReason() == DisconnectReason.HOST_KEY_NOT_VERIFIABLE) {
                if (InteractiveHostKeyVerifier.lastHostKeyHasChanged != null && pendingLsPath != null){
                    if (InteractiveHostKeyVerifier.lastHostKeyHasChanged == true) {
                        // show "last host key changed" dialog, containing current getChannel input parameter

//                        SSHAlreadyInKnownHostsDialog ad =
//                                new SSHAlreadyInKnownHostsDialog(
//                                        mainActivity,
//                                        authData,
//                                        null, // FIXME need to have old host key here
//                                        InteractiveHostKeyVerifier.currentHostKey,
//                                        this,
//                                        pendingLsPath
//                                );
//                        ad.show();
                    } else {
                        // show "add host key" dialog, containing current getChannel input parameter
//                        SSHNotInKnownHostsDialog nd =
//                                new SSHNotInKnownHostsDialog(
//                                        mainActivity,
//                                        authData,
//                                        InteractiveHostKeyVerifier.currentHostKey,
//                                        this,
//                                        pendingLsPath);
//                        nd.show();
                    }
                }
                 // any way, won't get a list dir response at this request, dismiss listeners in dialogs will do the job by calling main activity methods
            }
            else {
                Log.e(this.getClass().getName(),"transport exception in getChannel: "+e.getMessage());
            }
        }
        catch (IOException e) {
            Log.e(this.getClass().getName(),"getChannel error");
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
    public void createFileOrDirectory(String filePath, FileMode fileOrDirectory) throws IOException {
        // TODO replace IOExceptions with return values
        GenericRemotePath g = null;
        try {
            // parse generic path
            g = new GenericRemotePath(filePath);
        }
        catch (RuntimeException r) {
            throw new IOException("Malformed path");
        }
        // try to get channel, using prefix from last GenericRemotePath object
        SFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) throw new IOException("No channel found");

        if (exists(filePath))
            throw new IOException("File already exists"); // FIXME temporary, remove once changed Fileopshelper interface to boolean return values

        try {
            switch (fileOrDirectory) {
                case FILE:
                    channelSftp.open(g.remotePath, EnumSet.of(OpenMode.CREAT));
//                    channelSftp.put("/dev/null",g.remotePath); // not working with dev null
                    break;
                case DIRECTORY:
                    channelSftp.mkdir(g.remotePath);
                    break;
                default:
                    throw new RuntimeException("Unknown file mode");
            }
        }
        catch (IOException e) { // TODO simplify to boolean return value
            throw new IOException("Unable to create item "+filePath);
        }
    }

    private void recursiveFolderDelete(SFTPClient channelSftp_, String path) throws IOException {
        StatefulSFTPClient channelSftp = new StatefulSFTPClient(channelSftp_.getSFTPEngine());
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
            } else if (!(".".equals(item.getName()) || "..".equals(item.getName()))) { // If it is a subdir.
                try {
                    // removing sub directory.
                    channelSftp.rmdir(path + "/" + item.getName());
                } catch (Exception e) { // (TODO maybe worked on Jsch, to be tested anyway) If subdir is not empty and error occurs.
                    // Do lsFolderRemove on this subdir to enter it and clear its contents.
                    recursiveFolderDelete(channelSftp, path + "/" + item.getName());
                }
            }
        }
        channelSftp.rmdir(path); // delete the parent directory after empty
    }

    @Override
    public void deleteFilesOrDirectories(List<String> files) throws IOException {
        // TODO replace IOExceptions with return values
        GenericRemotePath g = null;
        List<String> remotePaths = new ArrayList<>();

        for (String x : files) {
            try {
                // parse generic path
                g = new GenericRemotePath(x);
                remotePaths.add(g.remotePath);
            }
            catch (RuntimeException r) {
                throw new IOException("Malformed path");
            }
        }
        // try to get channel, using prefix from last GenericRemotePath object
        SFTPClient channelSftp = getChannel(g.authData,null); // assuming all paths in browseradapter selection having authdata in common
        if (channelSftp == null) throw new IOException("No channel found");

        FileAttributes attrs;
        for (String x : remotePaths) {
            try {
                attrs = channelSftp.stat(x);
                if (attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY) recursiveFolderDelete(channelSftp,x);
                else channelSftp.rm(x);
            }
            catch (IOException e) {
                throw new IOException("Unable to delete element "+x);
            }
        }
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListS files, String dstFolder) throws IOException {
        // needs collaboration of fileopshelper handling operations on files in source folder,
        // discriminate based on prefix (like dircommander performs dispatching)
        // (e.g. local, or other channel in SFTPProvider for remote-to-local-to-remote copy)
        // most interesting case: remote-to-remote copy (protocol-level sftp)
    }

    @Override
    public void renameFile(String oldPathname, String newPathname) throws IOException {

    }

    public String getPermString(Set<FilePermission> s) {
        String p = "d"; // TODO add "dir" permission some way

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
    public singleStats_resp statFile(String pathname) throws IOException {
        GenericRemotePath g;
        try {
            // parse generic path
            g = new GenericRemotePath(pathname);
        }
        catch (RuntimeException r) {
            return null; // abuse of notation
        }

        // try to get channel
        SFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return null; // abuse of notation

        FileAttributes attrs = channelSftp.stat(g.remotePath);

        return new singleStats_resp(
                (attrs.getGID()+"").getBytes(), // TODO group string instead of id
                (attrs.getUID()+"").getBytes(), // TODO owner string instead of id
                0L, // creation time not available
                attrs.getAtime()*1000L,
                attrs.getMtime()*1000L,
                getPermString(attrs.getPermissions()).getBytes(),
                attrs.getSize()
        );
    }

    @Override
    public void statFiles(List<String> files) throws IOException {

    }

    @Override
    public folderStats_resp statFolder(String pathname) throws IOException {
        return null;
    }

    @Override
    public boolean exists(String pathname) {
        GenericRemotePath g;
        try {
            // parse generic path
            g = new GenericRemotePath(pathname);
        }
        catch (RuntimeException r) {
            return false; // abuse of notation
        }

        // try to get channel
        SFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return false; // abuse of notation
        try {
            FileAttributes fa = channelSftp.statExistence(pathname);
            return fa != null;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isFile(String pathname) {
        return !isDir(pathname); // not used for now
    }

    @Override
    public boolean isDir(String pathname) {
        GenericRemotePath g;
        try {
            // parse generic path
            g = new GenericRemotePath(pathname);
        }
        catch (RuntimeException r) {
            return false; // abuse of notation
        }

        // try to get channel
        SFTPClient channelSftp = getChannel(g.authData,null);
        if (channelSftp == null) return false; // abuse of notation

        try {
            FileAttributes fa = channelSftp.stat(pathname);
            return fa.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public byte[] hashFile(String pathname, HashRequestCodes hashAlgorithm) throws IOException {
        return new byte[0];
    }

    // directory path is the full address sftp://user@domain:port/directory/path
    @Override
    public DirWithContentUsingBrowserItems listDirectory(String directory) {
        GenericRemotePath g;
        try {
            // parse generic path
            g = new GenericRemotePath(directory);
        }
        catch (RuntimeException r) {
            return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.MALFORMED_PATH_ERROR);
        }

        // try to get channel
        SFTPClient channelSftp = getChannel(g.authData,directory);
        if (channelSftp == null) return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.AUTHENTICATION_ERROR); // TODO connection error to be handled and returned as well

        // list dir
        try {
            List<RemoteResourceInfo> lsContent = channelSftp.ls(g.remotePath);
            List<BrowserItem> l = new ArrayList<>();

            for (RemoteResourceInfo entry : lsContent) {
                FileAttributes fa = entry.getAttributes();
                l.add(new BrowserItem(entry.getName(),
                        fa.getSize(),
                        new Date(fa.getMtime()*1000L),
                        fa.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY,
                        false));
            }

            // successful return, change current helper
            MainActivity.currentHelper = MainActivity.sftpProvider; // or = this
            return new DirWithContentUsingBrowserItems(directory,l);
        }
        catch (IOException e) {
            return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
        }
    }

    @Override
    public boolean findInName(String expr, String filepath) {
        return false;
    }

    @Override
    public Long[] findInContent(String expr, String filepath) {
        return new Long[0];
    }

    @Override
    public byte[] downloadFileToMemory(String srcFilePath) throws IOException {
        // TODO
        return new byte[0];
    }

    @Override
    public void uploadFileFromMemory(String destFilePath, byte[] content) throws IOException {
        // TODO
    }
}

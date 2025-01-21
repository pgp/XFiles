package it.pgp.xfiles.roothelperclient;

import android.content.ContentResolver;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import it.pgp.Native;
import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyListUris;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.FileIOMode;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.enums.SshKeyType;
import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.reqs.ListOfPathPairs_rq;
import it.pgp.xfiles.roothelperclient.reqs.SinglePath_rq;
import it.pgp.xfiles.roothelperclient.reqs.compress_rq;
import it.pgp.xfiles.roothelperclient.reqs.compress_rq_options;
import it.pgp.xfiles.roothelperclient.reqs.copylist_rq;
import it.pgp.xfiles.roothelperclient.reqs.create_rq;
import it.pgp.xfiles.roothelperclient.reqs.del_rq;
import it.pgp.xfiles.roothelperclient.reqs.exists_rq;
import it.pgp.xfiles.roothelperclient.reqs.extract_rq;
import it.pgp.xfiles.roothelperclient.reqs.fileio_rq;
import it.pgp.xfiles.roothelperclient.reqs.hash_rq;
import it.pgp.xfiles.roothelperclient.reqs.link_rq;
import it.pgp.xfiles.roothelperclient.reqs.ls_archive_rq;
import it.pgp.xfiles.roothelperclient.reqs.ls_rq;
import it.pgp.xfiles.roothelperclient.reqs.movelist_rq;
import it.pgp.xfiles.roothelperclient.reqs.multiStats_rq;
import it.pgp.xfiles.roothelperclient.reqs.multi_extract_rq;
import it.pgp.xfiles.roothelperclient.reqs.openssh_ed25519_keygen_rq;
import it.pgp.xfiles.roothelperclient.reqs.openssl_rsa_pem_keygen_rq;
import it.pgp.xfiles.roothelperclient.reqs.setDates_rq;
import it.pgp.xfiles.roothelperclient.reqs.setOwnership_rq;
import it.pgp.xfiles.roothelperclient.reqs.setPermission_rq;
import it.pgp.xfiles.roothelperclient.reqs.singleStats_rq;
import it.pgp.xfiles.roothelperclient.resps.exists_resp;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.roothelperclient.resps.ls_resp;
import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;
import it.pgp.xfiles.roothelperclient.resps.ssh_keygen_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.service.SocketNames;
import it.pgp.xfiles.service.visualization.ProgressIndicator;
import it.pgp.xfiles.utils.ArchiveVMap;
import it.pgp.xfiles.utils.ContentProviderUtils;
import it.pgp.xfiles.utils.FileOperationHelper;
import it.pgp.xfiles.utils.GenericMRU;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.ProgressConflictHandler;
import it.pgp.xfiles.utils.StreamsPair;
import it.pgp.xfiles.utils.XFilesUtils;
import it.pgp.xfiles.utils.dircontent.ArchiveSubDirWithContent;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.LocalDirWithContent;
import it.pgp.xfiles.utils.dircontent.XFilesRemoteDirWithContent;
import it.pgp.xfiles.utils.iterators.VMapSubTreeIterable;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.XREPathContent;

/**
 * Created by pgp on 20/01/17
 */

public class RootHelperClient implements FileOperationHelper {

    private static final long EOF_ind = ProgressConflictHandler.Status.EOF.getStatus(); // end of file
    private static final long EOFs_ind = ProgressConflictHandler.Status.EOFs.getStatus(); // end of files

    // for publishing progress from within a long term task (copy/move/compress/extract/upload/download)
    BaseBackgroundTask task;
//    NotificationManager notifyManager;
//    NotificationCompat.Builder builder;
//    int NOTIF_ID;

    @Override
    public void initProgressSupport(BaseBackgroundTask task/*, NotificationManager notifyManager, NotificationCompat.Builder builder, int NOTIF_ID*/) {
        this.task = task;
//        this.notifyManager = notifyManager;
//        this.builder = builder;
//        this.NOTIF_ID = NOTIF_ID;
    }

    @Override
    public void destroyProgressSupport() {
        task = null;
    }

    ///////////////////////////////////////////////////////////////

    public static final SocketNames defaultaddress = SocketNames.theroothelper;
    public SocketNames address;

    public RootHelperClient() {
        this.address = defaultaddress;
    }

    RootHelperClient(SocketNames address) {
        this.address = address;
    }

    public class RootHelperStreams extends StreamsPair {

        public final LocalSocket ls;

        public RootHelperStreams() throws IOException {
            LocalSocket clientSocket = new LocalSocket();
            LocalSocketAddress socketAddress = new LocalSocketAddress(address.name(), LocalSocketAddress.Namespace.ABSTRACT);
            clientSocket.connect(socketAddress);
            Log.d("roothelperclient","Connected");

            ls = clientSocket;
            o = clientSocket.getOutputStream();
            i = new DataInputStream(clientSocket.getInputStream());
            Log.d("roothelperclient","Streams acquired");
        }

        @Override
        public void close() {
            // Close method on streams won't work, use shutdown methods
            // Web source:
            // https://stackoverflow.com/questions/10984175/android-localsocket-wont-close-when-in-blocked-read

            try {ls.shutdownInput();} catch (Exception ignored) {}
            try {ls.shutdownOutput();} catch (Exception ignored) {}

            try {i.close();} catch (Exception ignored) {}
            try {o.close();} catch (Exception ignored) {}
        }
    }

    public StreamsPair getStreams() throws IOException {
        return new RootHelperStreams();
    }

    public StreamsPair getStreams(BasePathContent bpc, boolean isFastClient) throws IOException {
        if (bpc instanceof LocalPathContent) {
            return new RootHelperStreams();
        }
        else if (bpc instanceof XREPathContent) {
            XREPathContent xrpc = (XREPathContent) bpc;
            RemoteManager rm = MainActivity.rootHelperRemoteClientManager.getClient(xrpc.serverHost,isFastClient);
            if (rm == null) throw new IOException("XRE Session not connected");
            return rm;
        }
        else throw new RuntimeException("Guard block");
    }

    // TODO may be useful in all long-term tasks, change following comment if needed
    public StreamsPair rs; // exposed in order to force closing connection and terminate forked p7zip process on service close

    // returns pid on successful connection, -1 otherwise
    public long checkConnection() {
        try (StreamsPair rs = getStreams()) {
            rs.o.write(ControlCodes.ACTION_GETPID.getValue());
            byte[] resp = new byte[4];
            rs.i.readFully(resp);
            return Misc.castBytesToUnsignedNumber(resp,4);
        }
        catch (IOException e) {
            return -1;
        }
    }

    /***************************************************************************
     * Singleton class for using only one client to server connection
     * from the spawning of roothelper server process till the app exit;
     * any request made to the server gets a response and the streams are
     * not closed, in so preventing possible malicious apps attempts to access
     * roothelper server while running
     ***************************************************************************/
    private static RootHelperStreamsOnce rso;
    private class RootHelperStreamsOnce {
        DataInputStream i;
        OutputStream o;

        RootHelperStreamsOnce() throws IOException {
            LocalSocket clientSocket = new LocalSocket();
            LocalSocketAddress socketAddress = new LocalSocketAddress(address.name(), LocalSocketAddress.Namespace.ABSTRACT);
            clientSocket.connect(socketAddress);
            Log.d("roothelperclient","Connected");

            o = clientSocket.getOutputStream();
            i = new DataInputStream(clientSocket.getInputStream());
            Log.d("roothelperclient","Streams acquired");
        }

        public void close() {
            try {
                i.close();
                o.close();
            }
            catch (IOException ignored) {}
        }
    }

    private RootHelperStreamsOnce getStreamsOnce() {
        try {
            if (rso == null || rso.i == null || rso.o == null) {
                // TODO check that there are not active instances of roothelper (cat /proc/net/unix | grep theroothelper), if so, send exit request
                // TODO rso must be set to null after any IOException in roothelper client
                RootHandler.runRootHelper(address);
                rso = new RootHelperStreamsOnce();
            }
            return rso;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /***************************************************************************
     ***************************************************************************/

    // only with RESPONSE_OK
    public static List<BrowserItem> assembleContentFromLsResps(DataInputStream clientInStream) throws IOException {
        List<BrowserItem> dirContent = new ArrayList<>();
        // read len, if 0 stop reading
        ls_resp resp = ls_resp.readNext(clientInStream);
        while (resp != null) {
            // TODO modify BrowserItem to host all permission instead of only isDirectory
//            BrowserItem b =
//                    new BrowserItem(new String(resp.filename,"UTF-8"),
//                            resp.size,
//                            new Date(resp.date*1000),
//                            new String(resp.permissions, "UTF-8").charAt(0) == 'd');
//            dirContent.add(b);
            dirContent.add(new BrowserItem(resp));
            resp = ls_resp.readNext(clientInStream);
        }
        return dirContent;
    }

    private ArchiveVMap fillArchiveVMap(DataInputStream clientInStream) throws IOException {
        ArchiveVMap v = new ArchiveVMap();
        // read len, if 0 stop reading
        ls_resp resp = ls_resp.readNext(clientInStream);
        int entryCnt = 0; // for extracting selected files, it is necessary to know their position in the archive entries list
        while (resp != null) {
            if(resp.permissions[0] != '!') {
                List<String> inArchivePath = new ArrayList<>(Arrays.asList((new String(resp.filename, StandardCharsets.UTF_8)).split("/")));
                inArchivePath.add(ArchiveVMap.sentinelKeyForNodeProperties);

                Map<String,Object> nodeProps = new HashMap<>();

                nodeProps.put("i",entryCnt);
                nodeProps.put("size",resp.size);
                nodeProps.put("date",new Date(resp.date*1000L));
                nodeProps.put("isDir",new String(resp.permissions, StandardCharsets.UTF_8).charAt(0) == 'd');

                v.set(nodeProps,inArchivePath.toArray()); // put in vMap with properties
            }
            resp = ls_resp.readNext(clientInStream);
            entryCnt++;
        }
        return v;
    }

    // ls interaction return list of BrowserItem (which is only a representation class, not a business logic one)
    // this because every request in the root case has to be passed to roothelper, so it doesn't make sense to create
    // an intermediate DirWithContent object

    public GenericDirWithContent listDirectory(BasePathContent dirPath) {
        StreamsPair rs = null;
        try {
            rs = getStreams(dirPath,true);

            List<BrowserItem> dirContent;
            SinglePath_rq req = new ls_rq(dirPath.dir);

            // send request
            req.write(rs.o);
            Log.d("roothelperclient","Ls request sent");


            // read responses (one item per file in directory)
            // read control byte (ok or error) // TODO embed in constructor? create two response classes (base response -1 file - and full response (accounting length-0 list termination)?

            // TODO response byte to be embedded in response classes (maybe also request byte)
            byte responseByte = rs.i.readByte();
            ResponseCodes c = ResponseCodes.getCode(responseByte);

            switch(c) {
                case RESPONSE_REDIRECT:
                    // read and replace redirect path before directory content
                    dirPath.dir = Misc.receiveStringWithLen(rs.i);
                    // missing break statement is intentional here
                case RESPONSE_OK:
                    dirContent = assembleContentFromLsResps(rs.i);
                    break;
                case RESPONSE_ERROR:
                    // propagate errno within DirWithContent object
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper","Error returned from roothelper server: "+errno);
                    return new LocalDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS); // TODO errno constants in enum
                default:
                    throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
            }

            if (dirPath instanceof LocalPathContent)
                return new LocalDirWithContent(dirPath.dir,dirContent);
            else
                return new XFilesRemoteDirWithContent(
                        ((XREPathContent)dirPath).serverHost,
                        dirPath.dir,
                        dirContent);
        }
        catch (IOException e) {
            try { rs.close(); } catch (Exception ignored) {}
            if (dirPath instanceof LocalPathContent)
                return new LocalDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
            else {
                MainActivity.rootHelperRemoteClientManager.fastClients.remove(((XREPathContent)dirPath).serverHost);
                return new XFilesRemoteDirWithContent(
                        ((XREPathContent)dirPath).serverHost,
                        FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
            }
        }
        finally {
            if (dirPath instanceof LocalPathContent) {
                try { rs.close(); } catch (Exception ignored) {}
            }
        }
    }

    // because there are points when a new RootHelper instance is created locally
    public static final GenericMRU<String,ArchiveVMap> archiveMRU = new GenericMRU<>(10); // up to 10 entries

    // password may be null
    @Override
    public GenericDirWithContent listArchive(BasePathContent archivePath) {
        SingleStatsItem statForModifiedDate;
        Date modifiedFileDate;
        String path;
        String subpath;
        String password;
        ArchiveVMap archiveMap;

        try {
            // retrieve modification time of the archive file
            if (archivePath.providerType == ProviderType.LOCAL) {
                statForModifiedDate = statFile(archivePath);
                path = archivePath.dir;
                subpath = "";
                password = "";
            }
            else if (archivePath.providerType == ProviderType.LOCAL_WITHIN_ARCHIVE) {
                statForModifiedDate = statFile(new LocalPathContent(((ArchivePathContent) archivePath).archivePath));
                path = ((ArchivePathContent) archivePath).archivePath;
                subpath = archivePath.dir;
                password = ((ArchivePathContent) archivePath).password;
            }
            else throw new RuntimeException("Unexpected provider type");
            if (statForModifiedDate == null) {
                return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
            }
            modifiedFileDate = statForModifiedDate.modificationTime;
        }
        catch (IOException e) {
            return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
        }

        // check if the base archive exists in MRU and has not been modified, or not exists
        archiveMap = archiveMRU.getByPath(path,modifiedFileDate);
        if (archiveMap == null) { // file not yet in cache or conflicting modified dates
            // send listArchive request to rootHelper
            // zero-length password not allowed, used by roothelper protocol as indication of no password provided
            ls_archive_rq listArchive_rq = new ls_archive_rq(
                    path.getBytes(),
                    password==null?new byte[0]:password.getBytes()
            );

            try (StreamsPair rs = getStreams()) {
                listArchive_rq.write(rs.o);

                // receive response
                int errno = Misc.receiveBaseResponse(rs.i);
                if (errno==0) archiveMap = fillArchiveVMap(rs.i);
                else if (errno == 0x101010) return new GenericDirWithContent(FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD);
                else return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
            }
            catch (IOException e) {
                return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
            }

            archiveMRU.setLatest(path,archiveMap,modifiedFileDate);
        }
        // at this point, if the file exists, the vmap is non null
        // retrieve subdir-only entries into genericdirwithcontent object

        List<BrowserItem> content = new ArrayList<>();
        List<String> inArchivePath = Arrays.asList(subpath.split("/"));

        if (inArchivePath.size()>0) { // get subdir children
            Map<String,Object> map = (Map) archiveMap.get(inArchivePath.toArray());
            for (Map.Entry entry: map.entrySet()) {
                if (
                        entry != null &&
                                !entry.getKey().equals(ArchiveVMap.sentinelKeyForNodeProperties)) {
                    content.add(
                            new BrowserItem(
                                    (String)(entry.getKey()),
                                    (Map<String,Object>)((Map<String,Object>)entry.getValue()).get(ArchiveVMap.sentinelKeyForNodeProperties)
                            )
                    );
                }
            }
        }
        else { // get children of root
            for (Map.Entry entry: archiveMap.h.entrySet()) {
                if (!entry.getKey().equals(ArchiveVMap.sentinelKeyForNodeProperties)) {
                    content.add(
                            new BrowserItem(
                                    (String)(entry.getKey()),
                                    (Map<String,Object>)((Map<String,Object>)entry.getValue()).get(ArchiveVMap.sentinelKeyForNodeProperties)
                            )
                    );
                }
            }
        }

        return new ArchiveSubDirWithContent(path,subpath,content);
    }

    private int handleCompressProgressAfterConfOK(StreamsPair rs, final long total) throws IOException {
        long last_progress = 0;
        int ret;
        // receive progress (end progress is -1 as uint64)
        for(;;) {
            long progress = Misc.receiveTotalOrProgress(rs.i);
            if (progress == EOF_ind) {
                if (last_progress == total) {
                    // OK
                }
                else {
                    // Warning, last progress before termination value differs from total
                }
                break;
            }
            last_progress = progress;
//            builder.setProgress((int) total, (int) progress,false);
//            notifyManager.notify(NOTIF_ID, builder.build());
            task.publishProgressWrapper(new Pair<>(progress, total));

//            Log.d("setCompleted ","publishProgressWrapper progress:\t"+progress+"\ttotal: "+total);
//            Log.d("setCompleted ","publishProgressWrapper round:\t"+Math.round(progress*100.0/total));
        }

        // receive 1-byte final OK or error response
        ret = Misc.receiveBaseResponse(rs.i);
        if (ret != 0) {
            Log.e("setCompleted ","Received error code after complete: "+ret);
        }
        rs.close();
        return ret;
    }

    private int handleCompressProgressAfterConfOK(StreamsPair rs, final long total, ContentResolver resolver, List<Uri> uris, int nativeUds) throws IOException {
        long last_progress = 0;
        int ret;
        byte[] b_idx = new byte[4];

        long totalFromRh = Misc.receiveTotalOrProgress(rs.i);

        if(totalFromRh != total) {
            Log.w("setCompleted","Expected total "+total+" is different from the one accumulated from RootHelper ("+totalFromRh+")");
        }

        // receive progress (end progress for single file is -1 as uint64, end all is -2 as uint64)
        for(;;) {
            long progress = Misc.receiveTotalOrProgress(rs.i);
            if (progress == EOF_ind) {
                // receive index and send corresponding fd
                Log.d("setCompleted","[RHClient]receiving index after EOF");
                rs.i.readFully(b_idx);
                int index = (int) Misc.castBytesToUnsignedNumber(b_idx,4);
                Log.d("setCompleted","[RHClient]index after EOF is "+index+", now sending fd for that index");
                int fdToSend = resolver.openFileDescriptor(uris.get(index),"r").detachFd(); // will be closed internally by p7zip back-end in rh forked process
                Native.sendDetachedFD(nativeUds,fdToSend);
                Log.d("setCompleted","[RHClient]fd for index "+index+" sent");
            }
            else if (progress == EOFs_ind) {
                Log.d("setCompleted","End of files");
                break;
            }
            else {
                if (progress - last_progress > 1000000) {
                    last_progress = progress;
                    task.publishProgressWrapper(new Pair<>(progress, total));
                }
            }
        }

        // receive 1-byte final OK or error response
        ret = Misc.receiveBaseResponse(rs.i);
        if (ret != 0) {
            Log.e("setCompleted ","Received error code after complete: "+ret);
        }
        rs.close();
        return ret;
    }

    public int compressToArchiveFromFds(CopyListUris contentUris,
                                        BasePathContent destArchive,
                                        @Nullable Integer compressionLevel,
                                        @Nullable Boolean encryptHeaders,
                                        @Nullable Boolean solidMode,
                                        @Nullable String password,
                                        ContentResolver resolver) throws IOException {
        if (destArchive.providerType!=ProviderType.LOCAL)
            throw new RuntimeException("Unexpected path content type"); // abuse of exception

        rs = getStreams();

        // send request byte with flags
        byte customizedRq = ControlCodes.ACTION_COMPRESS.getValue();
        customizedRq ^= (7 << 5); // flags: 111
        rs.o.write(customizedRq);

        int nativeUds = ContentProviderUtils.getNativeDescriptor(((RootHelperStreams)rs).ls);

        long total = 0;

        // compute file stats in JNI and send them all
        List<Uri> parsedUris = new ArrayList<>();
        for (String uri_ : contentUris.contentUris) {
            Uri uri = Uri.parse(uri_);
            parsedUris.add(uri);
            String filename = ContentProviderUtils.getName(resolver,uri);
            int fd = resolver.openFileDescriptor(uri,"r").detachFd();
            long fileSize = Native.sendfstat(nativeUds,fd,filename); // fd will be closed here after fstat
            if (fileSize < 0) throw new IOException("Unable to fstat "+filename);
            total += fileSize;
        }
        rs.o.write(Misc.EOL);

        Misc.sendStringWithLen(rs.o, destArchive.dir); // destArchive

        new compress_rq_options(compressionLevel,encryptHeaders,solidMode).writecompress_rq_options(rs.o); // compress options

        byte[] password_ = (password == null)?new byte[0]:password.getBytes();
        rs.o.write(password_.length); // single byte
        if (password_.length != 0)
            rs.o.write(password_);

        // OK response means archive init has been successful, and actual compression starts now, so start receiving progress
        int ret = Misc.receiveBaseResponse(rs.i);
        if (ret != 0) {
            rs.close();
            Log.e("setCompleted ","Received error code before progress start: "+ret);
            return ret;
        }

        return handleCompressProgressAfterConfOK(rs,total,resolver,parsedUris,nativeUds);
    }

    // Java zip backend
    // JUST IGNORE ALL OPTIONS, DEFAULT OUTPUT FORMAT TO ZIP
//    public int compressToArchiveFromFds(CopyListUris contentUris,
//                                        BasePathContent destArchive,
//                                        @Nullable Integer compressionLevel,
//                                        @Nullable Boolean encryptHeaders,
//                                        @Nullable Boolean solidMode,
//                                        @Nullable String password,
//                                        ContentResolver resolver) throws IOException {
//        if (destArchive.providerType!=ProviderType.LOCAL)
//            throw new RuntimeException("Unexpected path content type"); // abuse of exception
//
//        long total = 0;
//
//        // compute file stats in JNI and send them all
//        List<Uri> parsedUris = new ArrayList<>();
//        for (String uri_ : contentUris.contentUris) {
//            Uri uri = Uri.parse(uri_);
//            parsedUris.add(uri);
//            try(ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri,"r")) {
//                total += pfd.getStatSize();
//            }
//        }
//
//        long progress = 0;
//        long lastShownProgress = 0;
//        // init zip archive
//        try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(destArchive.dir)))) {
//            for (Uri uri : parsedUris) {
//                try(InputStream is = resolver.openInputStream(uri)) {
//                    String filename = ContentProviderUtils.getName(resolver,uri);
//                    ZipEntry ze = new ZipEntry(filename);
//                    zos.putNextEntry(ze);
//                    byte[] bytes = new byte[4096];
//                    for(;;) {
//                        int readBytes = is.read(bytes);
//                        if (readBytes <= 0) break;
//                        zos.write(bytes,0,readBytes);
//                        progress += readBytes;
//                        if (progress - lastShownProgress > 1000000) {
//                            lastShownProgress = progress;
//                            task.publishProgressWrapper((int)Math.round(progress*100.0/total));
//                        }
//                    }
//                    zos.closeEntry();
//                }
//            }
//        }
//        return 0;
//    }

    @Override
    public int compressToArchive(BasePathContent srcDirectory,
                                 BasePathContent destArchive,
                                 @Nullable Integer compressionLevel,
                                 @Nullable Boolean encryptHeaders,
                                 @Nullable Boolean solidMode,
                                 @Nullable String password,
                                 @Nullable List<String> filenames) throws IOException {
        rs = getStreams();

        if (!(srcDirectory.providerType==ProviderType.LOCAL &&
                destArchive.providerType==ProviderType.LOCAL)) {
            throw new RuntimeException("Unexpected path content type"); // abuse of exception
        }

        compress_rq rq = new compress_rq(
                srcDirectory.toString(),
                destArchive.toString(),
                compressionLevel,
                encryptHeaders,
                solidMode,
                password,
                filenames);
        rq.write(rs.o);

        // OK response means archive init has been successful, and actual compression starts now, so start receiving progress
        int ret = Misc.receiveBaseResponse(rs.i);
        if (ret != 0) {
            rs.close();
            Log.e("setCompleted ","Received error code: "+ret);
            return ret;
        }

        // receive total
        long total = Misc.receiveTotalOrProgress(rs.i);
//        Log.d("setCompleted ","Received total size: "+total);

        return handleCompressProgressAfterConfOK(rs,total);
    }

    /*
    extract from archive:
        - extract all: assumes file browser is currently OUTSIDE of an archive, so vmap need not exist;
          error results (among which, the null or wrong password one) are within GenericDirWithContent return value
        - extract some: assumes file browser is currently WITHIN an archive, so vmap MUST exist (throw runtimeexception if it doesn't);
     */
    @Override
    public List<FileOpsErrorCodes> extractFromArchive(List<BasePathContent> srcArchives,
                                                BasePathContent destDirectory,
                                                @Nullable String password,
                                                @Nullable Iterable<String> filenames,
                                                boolean smartDirectoryCreation) throws IOException {

        if (destDirectory != null && destDirectory.providerType != ProviderType.LOCAL) {
            throw new RuntimeException("Forbidden type for destination directory");
        }

        String destDir = destDirectory==null ? "" : destDirectory.dir;

        switch (srcArchives.get(0).providerType) {
            case LOCAL:
                // entryIdxs will be ignored, extract/test all, no need to preload VMap
                return extract(srcArchives, destDir, password,null,smartDirectoryCreation); // extract/test all
            case LOCAL_WITHIN_ARCHIVE:
                break;
            default:
                throw new RuntimeException("Forbidden types for archive and/or directories");
        }

        // since this point, we are LOCAL_WITHIN_ARCHIVE, so srcArchives must have size 1
        if(srcArchives.size() != 1) throw new RuntimeException("Guard block");

        ArchivePathContent srcArchive = (ArchivePathContent) srcArchives.get(0);
        ArchiveVMap avm = archiveMRU.getByPath(srcArchive.archivePath);
        if (avm == null) throw new RuntimeException("VMap should be non-null once in archive!");

        List<Integer> entries = new ArrayList<>();

        // srcArchive is ArchivePathContent
        if (filenames == null || !filenames.iterator().hasNext()) {
            if (srcArchive.dir == null || srcArchive.dir.equals("") || srcArchive.dir.equals("/")) {
                // no selection in root dir of archive, extract/test all
                return extract(srcArchives, // actually srcArchive as only item in the list
                        destDir, password,null,smartDirectoryCreation); // extract all
            }
            else {
                // no selection in subpath of archive
                // iterator over srcArchive (subpath as root), accumulate idxs
                entries.addAll(getEntries(avm,srcArchive.dir));
            }
        }
        else {
            // some selection
            for (String filename : filenames) {
                entries.addAll(getEntries(avm,srcArchive.concat(filename).dir));
            }
        }

        int stripPathLen = (srcArchive.dir==null||srcArchive.dir.equals("/"))?0:srcArchive.dir.length();
        return extract(srcArchives, // actually srcArchive as only item in the list
                destDir,
                password,
                new RelativeExtractEntries(stripPathLen,entries),
                smartDirectoryCreation);
    }

    @Override
    public int setDates(BasePathContent file, @Nullable Date accessDate, @Nullable Date modificationDate) {
        try (StreamsPair rs = getStreams()) {
            if (file.providerType!=ProviderType.LOCAL) return -1;
            setDates_rq rq = new setDates_rq(file.dir,accessDate,modificationDate);
            rq.write(rs.o);
            return Misc.receiveBaseResponse(rs.i);
        }
        catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setPermissions(BasePathContent file, int permMask) {
        try (StreamsPair rs = getStreams()) {
            if (file.providerType!=ProviderType.LOCAL) return -1;
            setPermission_rq rq = new setPermission_rq(file.dir,permMask);
            rq.write(rs.o);
            return Misc.receiveBaseResponse(rs.i);
        }
        catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setOwnership(BasePathContent file, @Nullable Integer ownerId, @Nullable Integer groupId) {
        try (StreamsPair rs = getStreams()) {
            if (file.providerType!=ProviderType.LOCAL) return -1;
            setOwnership_rq rq = new setOwnership_rq(file.dir,ownerId,groupId);
            rq.write(rs.o);
            return Misc.receiveBaseResponse(rs.i);
        }
        catch (Exception e) {
            return -1;
        }
    }

    private List<Integer> getEntries(ArchiveVMap vMap, String relToArchivePathname) throws IOException {
        List<Integer> entries = new ArrayList<>();

        if (relToArchivePathname.equals("") || relToArchivePathname.equals("/"))
            throw new RuntimeException("This condition should be managed earlier than here");

        VMapSubTreeIterable it = new VMapSubTreeIterable(vMap,relToArchivePathname.split("/"));

        for (Map.Entry me : it) {
            if (me.getKey().equals(ArchiveVMap.sentinelKeyForNodeProperties)) {
                Map x = (Map)me.getValue();
                Integer ii = (Integer) x.get("i");
                entries.add(ii);
            }
        }

        return entries;
    }

    private List<FileOpsErrorCodes> extract(List<BasePathContent> archives,
                                      String directory,
                                      @Nullable String password,
                                      @Nullable RelativeExtractEntries entries,
                                      boolean smartDirectoryCreation) throws IOException {
        rs = getStreams();

        if(archives.size() == 1) { // extract/test whole single archive, or extract/test some items from within one single archive
            BasePathContent bpc = archives.get(0);
            new extract_rq(bpc instanceof ArchivePathContent ? ((ArchivePathContent) bpc).archivePath : bpc.dir,
                    directory, password, null, entries, smartDirectoryCreation).write(rs.o);
        }
        else {
            new multi_extract_rq(archives, directory, password, smartDirectoryCreation).write(rs.o);
        }

        List<FileOpsErrorCodes> rets = new ArrayList<>();

        int nArchives = archives.size();
        boolean multiExtract = nArchives > 1;
        for(int i=0;i<nArchives;i++) {
            FileOpsErrorCodes ret;
            int errno = Misc.receiveBaseResponse(rs.i);
            if (errno == 0x101010) ret = FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD; // null or wrong password for encrypted filenames archive
            else if (errno == 0x03) ret = FileOpsErrorCodes.CRC_FAILED; // probably, wrong password for plain filenames archive
            else if (errno == 0) { // start receiving progress here
                // receive total
                long total = Misc.receiveTotalOrProgress(rs.i);
                long last_progress = 0;

                // receive progress (end progress is -1 as uint64)
                for(;;) {
                    long progress = Misc.receiveTotalOrProgress(rs.i);
                    if (progress == EOF_ind) {
                        if (last_progress == total) {
                            // OK
                        }
                        else {
                            // Warning, last progress before termination value differs from total
                        }
                        break;
                    }
                    last_progress = progress;
                    Pair<Long, Long> inner =  new Pair<>(progress, total);
                    if(multiExtract)
                        task.publishProgressWrapper(new Pair<>((long)i,(long)nArchives),inner);
                    else task.publishProgressWrapper(inner);
                }

                // receive 1-byte final OK or error response
                errno = Misc.receiveBaseResponse(rs.i);
                if (errno == 0) ret = null;
                else if (errno == 0x101010) ret = FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD; // null or wrong password for encrypted filenames archive
                else if (errno == 0x03) ret = FileOpsErrorCodes.CRC_FAILED; // probably, wrong password for plain filenames archive
                else ret = FileOpsErrorCodes.TRANSFER_ERROR;
            }
            else ret = FileOpsErrorCodes.TRANSFER_ERROR;
            rets.add(ret);
        }
        rs.close();
        return rets;
    }

    // TODO Remove find methods from interface, already implemented RH only

    @Override
    public String createFileOrDirectory(BasePathContent path, FileMode fileOrDirectory, FileCreationAdvancedOptions... fileOptions) throws IOException {
        SinglePath_rq req = (fileOrDirectory == FileMode.FILE && fileOptions.length > 0 && fileOptions[0] != null) ?
                new create_rq(path.dir, fileOptions[0]):
                new create_rq(path.dir, fileOrDirectory);
        int errno;
        String errMsg;
        StreamsPair rs = getStreams(path,true);
        req.write(rs.o);
        Log.d("roothelperclient","Create request sent");
        errno = Misc.receiveBaseResponse(rs.i);
        if(errno == 0) {
            if(fileOptions.length > 0) { // if a non-empty regular file is being created
                // receive progress
                FileCreationAdvancedOptions fopts = fileOptions[0];
                long progress;
                long total = fopts.size;
                do {
                    progress = Misc.receiveTotalOrProgress(rs.i);
                    task.publishProgressWrapper(new Pair<>(progress, total));
                }
                while(progress != EOF_ind);

                if(fopts.strategy.mode == FileCreationAdvancedOptions.FileCreationMode.RANDOM &&
                        fopts.strategy.outputHashType != null) {
                    return Misc.receiveStringWithLen(rs.i); // output hash, to be shown in dialog
                }
            }
            return null;
        }
        else if(errno == 17)
            errMsg = ALREADY_EXIST;
        else
            errMsg = fileOrDirectory.name().toLowerCase()+" creation error, errno is: "+errno;
        throw new IOException(errMsg);
    }

    @Override
    public void createLink(BasePathContent originPath, BasePathContent linkPath, boolean isHardLink) throws IOException {
        if((originPath.providerType != linkPath.providerType) ||
                ((originPath instanceof XREPathContent) &&
                        (!Objects.equals(((XREPathContent)originPath).serverHost,((XREPathContent)linkPath).serverHost))))
            throw new RuntimeException("Target and link paths must belong to the same filesystem");
        StreamsPair rs = getStreams(originPath,true);
        link_rq rq = new link_rq(originPath.dir,linkPath.dir,isHardLink);
        rq.write(rs.o);
        int errno = Misc.receiveBaseResponse(rs.i);
        if (errno != 0) throw new IOException("link creation error, errno is "+errno);
    }

    public BitSet existsIsFileIsDir(BasePathContent filePath, boolean exists, boolean isFile, boolean isDir) {
        BitSet ret = new BitSet(3);
        if (filePath.providerType==null) return ret;
        switch (filePath.providerType) {
            case LOCAL:
                exists_rq rq = new exists_rq(filePath.dir,exists,isFile,isDir);
                try (StreamsPair rs = getStreams()) {
                    rq.write(rs.o);
                    exists_resp resp = new exists_resp(rs.i);
                    return resp.respFlags;
                }
                catch (IOException ignored) {}
                return ret;
            case LOCAL_WITHIN_ARCHIVE:
                // TODO check existence in VMap
                return ret;
            case XFILES_REMOTE:
                // FIXME add exist request for XRE remote checksum request on single file
                return ret;
            default:
                throw new RuntimeException("Unsupported BasePathContent subtype in roothelperclient exists call");
        }
    }

    public boolean exists(BasePathContent filePath) {
        return existsIsFileIsDir(filePath,true,false,false).get(0);
    }

    public boolean isFile(BasePathContent filePath) {
        return existsIsFileIsDir(filePath,false,true,false).get(1);
    }

    public boolean isDir(BasePathContent filePath) {
        return existsIsFileIsDir(filePath,false,false,true).get(2);
    }

    // TODO to be tested
    // client test case for delete request-response interaction(s)
    @Override
    public void deleteFilesOrDirectories(List<BasePathContent> filePaths) throws IOException {
        // TODO make one connection per set of delete requests
        for (BasePathContent filePath : filePaths) {
            SinglePath_rq req = new del_rq(filePath.dir);
            StreamsPair rs = getStreams();

            // send request
            req.write(rs.o);
            Log.d("roothelperclient","Del request sent");


            // read responses (one item per file in directory)
            // read control byte (ok or error)

            byte responseByte = rs.i.readByte();
            ResponseCodes c = ResponseCodes.getCode(responseByte);

            switch(c) {
                case RESPONSE_OK:
                    Log.d("roothelper","OK returned from roothelper server for delete file: "+filePath.dir);
                    break;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    String msg = "Error returned from roothelper server: "+errno+" for file "+filePath.dir;
                    Log.e("roothelper",msg);
                    throw new IOException(msg);
                default:
                    throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
            }
        }
    }

    @Override
    public boolean renameFile(BasePathContent oldPathname, BasePathContent newPathname) throws IOException {
        // treat as move request
        StreamsPair rs = getStreams();

        // send request
        ListOfPathPairs_rq rq = new movelist_rq(
                Collections.singletonList(oldPathname.dir),
                Collections.singletonList(newPathname.dir)
        );
        rq.write(rs.o);

        // receive 1 EOF and 1 EOFs progress since move sends them, then receive OK/error response
        byte[] b = new byte[8];
        rs.i.read(b); // EOF
        rs.i.read(b); // EOFs

        boolean ret = false;
        if (Misc.receiveBaseResponse(rs.i) == 0) ret = true;

        rs.close();
        return ret;
    }

    @Override
    public SingleStatsItem statFile(BasePathContent pathname) throws IOException {
        switch (pathname.providerType) {
            case LOCAL:
                StreamsPair rs = getStreams();

                singleStats_rq rq = new singleStats_rq(pathname.dir,FileMode.FILE);
                rq.write(rs.o);

                if (Misc.receiveBaseResponse(rs.i) != 0) return null;

                // receive and return response
                singleStats_resp resp = new singleStats_resp(rs.i);
                rs.close();
                return new SingleStatsItem(resp);

            case LOCAL_WITHIN_ARCHIVE:
                ArchiveVMap v = archiveMRU.getByPath(((ArchivePathContent)pathname).archivePath);
                if (v == null)
                    throw new RuntimeException("ArchiveVMap should already be populated within archive");
                Map nodeProps = v.getNodeProps(pathname.dir);
                return new SingleStatsItem(
                        "", // empty group
                        "", // empty owner
                        new Date(0L),
                        new Date(0L),
                        (Date)nodeProps.get("date"),
                        (boolean)nodeProps.get("isDir")?"d---------":"----------",
                        (long)nodeProps.get("size")
                );
            case XFILES_REMOTE:
                XREPathContent xrpc = (XREPathContent) pathname;
                RemoteManager rm = MainActivity.rootHelperRemoteClientManager.getClient(xrpc.serverHost,true);
                if (rm == null) return null;
                // TODO stats_resp
                rq = new singleStats_rq(pathname.dir,FileMode.FILE);
                rq.write(rm.o);

                if (Misc.receiveBaseResponse(rm.i) != 0) return null;

                // receive and return response
                resp = new singleStats_resp(rm.i);
                return new SingleStatsItem(resp);

            default:
                throw new RuntimeException("Roothelper should not be the current helper when exploring SFTP paths");
        }
    }

    @Override
    public folderStats_resp statFiles(List<BasePathContent> files) throws IOException {
        if (files.isEmpty()) throw new IOException("statfiles list empty, cannot determine provider type");
        switch(files.get(0).providerType) {
            case LOCAL:
                StreamsPair rs = getStreams();
                List<String> tmp = new ArrayList<>();
                for (BasePathContent bpc : files) tmp.add(bpc.dir);
                multiStats_rq rq = new multiStats_rq(tmp);
                rq.write(rs.o);

                int errno = Misc.receiveBaseResponse(rs.i);
                if (errno != 0) {
                    Log.e("roothelperclient", "statFiles: Some files could not be stat, error code: " + errno);
                }

                // TODO propagate errno along with response
                // receive and return response
                folderStats_resp response = new folderStats_resp(rs.i);
                rs.close();
                return response;
            case LOCAL_WITHIN_ARCHIVE:
                // stat inner folder of archive (that is, archive is already opened and vmap is in memory)
                ArchiveVMap v = archiveMRU.getByPath(((ArchivePathContent) files.get(0)).archivePath);
                if (v == null)
                    throw new RuntimeException("ArchiveVMap should already be populated within archive");

                long childrenFiles = 0, childrenDirs = 0, totalFiles = 0, totalDirs = 0, totalSize = 0;
                for (BasePathContent pathname : files) {
                    VMapSubTreeIterable it = new VMapSubTreeIterable(v, pathname.dir.split("/"));

                    // FIXME currently, also the current folder node is taken into account when iterating (so, totalFolders is shifted up by 1)
                    for (Map.Entry me : it) {
                        if (me.getKey().equals(ArchiveVMap.sentinelKeyForNodeProperties)) {
                            Map x = (Map) me.getValue();
                            // TODO populate children files and dirs as well
                            boolean isDir = (boolean) x.get("isDir");
                            if (isDir) totalDirs++;
                            else {
                                totalFiles++;
                                totalSize += (long) x.get("size");
                            }
                        }
                    }
                }

                return new folderStats_resp(
                        childrenDirs,
                        childrenFiles,
                        totalDirs,
                        totalFiles,
                        totalSize);

            case XFILES_REMOTE:
                XREPathContent xrpc = (XREPathContent) files.get(0);
                RemoteManager rm = MainActivity.rootHelperRemoteClientManager.getClient(xrpc.serverHost, true);
                if (rm == null) return null;

                tmp = new ArrayList<>();
                for (BasePathContent bpc : files) tmp.add(bpc.dir);
                rq = new multiStats_rq(tmp);
                rq.write(rm.o);

                errno = Misc.receiveBaseResponse(rm.i);
                if (errno != 0) {
                    Log.e("roothelperclient", "Some files could not be stat, error code: " + errno);
                }

                // TODO propagate errno along with response
                // receive and return response
                return new folderStats_resp(rm.i);
            default:
                throw new RuntimeException("Roothelper should not be the current helper when exploring SFTP paths");
        }
    }

    @Override
    public folderStats_resp statFolder(BasePathContent pathname) throws IOException {
        switch(pathname.providerType) {
            case LOCAL:
                StreamsPair rs = getStreams();
                singleStats_rq rq = new singleStats_rq(pathname.dir, FileMode.DIRECTORY);
                rq.write(rs.o);

                int errno = Misc.receiveBaseResponse(rs.i);
                if (errno != 0)
                    Log.e("roothelperclient", "Some files could not be stat, error code: " + errno);

                // TODO propagate errno along with response
                // receive and return response
                folderStats_resp response = new folderStats_resp(rs.i);
                rs.close();
                return response;
            case LOCAL_WITHIN_ARCHIVE:
                // stat inner folder of archive (that is, archive is already opened and vmap is in memory)
                ArchiveVMap v = archiveMRU.getByPath(((ArchivePathContent) pathname).archivePath);
                if (v == null)
                    throw new RuntimeException("ArchiveVMap should already be populated within archive");

                VMapSubTreeIterable it = new VMapSubTreeIterable(v, pathname.dir.split("/"));

                long childrenFiles = 0, childrenDirs = 0, totalFiles = 0, totalDirs = 0, totalSize = 0;

                // FIXME currently, also the current folder node is taken into account when iterating (so, totalFolders is shifted up by 1)
                for (Map.Entry me : it) {
                    if (me.getKey().equals(ArchiveVMap.sentinelKeyForNodeProperties)) {
                        Map x = (Map) me.getValue();
                        // TODO populate children files and dirs as well
                        boolean isDir = (boolean) x.get("isDir");
                        if (isDir) totalDirs++;
                        else {
                            totalFiles++;
                            totalSize += (long) x.get("size");
                        }
                    }
                }

                return new folderStats_resp(
                        childrenDirs,
                        childrenFiles,
                        totalDirs,
                        totalFiles,
                        totalSize);
            case XFILES_REMOTE:
                XREPathContent xrpc = (XREPathContent) pathname;
                RemoteManager rm = MainActivity.rootHelperRemoteClientManager.getClient(xrpc.serverHost, true);
                if (rm == null) return null;

                rq = new singleStats_rq(pathname.dir, FileMode.DIRECTORY);
                rq.write(rm.o);

                errno = Misc.receiveBaseResponse(rm.i);
                if (errno != 0)
                    Log.e("roothelperclient", "Some files could not be stat, error code: " + errno);

                // TODO propagate errno along with response
                // receive and return response
                return new folderStats_resp(rm.i);
            default:
                throw new RuntimeException("Roothelper should not be the current helper when exploring SFTP paths");
        }
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListPathContent files, BasePathContent dstFolder) throws IOException {
        // for x in pathnames (full paths), send to roothelper the pair:
        // (x , dstFolder + "/" + x.getName() ) // stop on first level, roothelper C code performs dir tree move/copy/delete if needed
        // receive one response per pair

        // this variable must be saved into the corresponding CopyMoveTask
        // in order to interrupt the long-term copy operation by close/shutdown streams
//        RootHelperStreams rs = getStreams();
        rs = getStreams();

        // new mode: send list of file pairs in one request, then receive progress
        List<String> srcs = new ArrayList<>();
        List<String> dests = new ArrayList<>();

        for (String pathname : files) {
            srcs.add(pathname);
            dests.add(dstFolder.dir+"/"+(new File(pathname).getName()));
        }

        ListOfPathPairs_rq rq = (files.copyOrMove == CopyMoveMode.COPY) ?
                new copylist_rq(srcs,dests):new movelist_rq(srcs,dests);
        rq.write(rs.o);

        // for copy, receive total number of files for outer progress
        // (all regular files in all subfolders at any level of given items)
        long totalFileCount,totalSize = 0;
        if (files.copyOrMove == CopyMoveMode.COPY) {
            byte[] tot_ = new byte[8];
            rs.i.readFully(tot_);
            totalFileCount = Misc.castBytesToUnsignedNumber(tot_,8);
            rs.i.readFully(tot_);
            totalSize = Misc.castBytesToUnsignedNumber(tot_,8);
        }
        // for move, consider only top-level elements (dir and folders)
        else {
            totalFileCount = files.files.size();
        }

        //////////////////////// BEGIN LEGACY /////////////////////////////
//        long currentFileCount = 0;
//        long currentFileSize = EOF_ind; // placeholder, just to avoid uninitialized error
//
//        boolean hasReceivedSizeForCurrentFile = false;
//
//        // receive progress for single files, increment outer progress bar by 1 on EOF_ind progress
//        for (;;) {
//            long tmp = Misc.receiveTotalOrProgress(rs.i);
//
//            if (tmp == EOF_ind) {
//                hasReceivedSizeForCurrentFile = false;
//                currentFileCount++;
//                task.publishProgressWrapper(
//                        (int)Math.round(currentFileCount*100.0/totalFileCount),
//                        0
//                );
//            }
//            else if (tmp == EOFs_ind) break;
//            else {
//                if (hasReceivedSizeForCurrentFile) {
//                    task.publishProgressWrapper(
//                            (int)Math.round(currentFileCount*100.0/totalFileCount),
//                            (int)Math.round(tmp*100.0/currentFileSize)
//                    );
//                }
//                else {
//                    // here, tmp is current file's size, before starting copying current file
//                    currentFileSize = tmp;
//                    hasReceivedSizeForCurrentFile = true;
//                    task.publishProgressWrapper(
//                            (int)Math.round(currentFileCount*100.0/totalFileCount),
//                            0
//                    );
//                }
//            }
//        }

        //////////////////////// END LEGACY /////////////////////////////

        //////////////////////// BEGIN NEW /////////////////////////////
        new ProgressConflictHandler(rs,task,totalFileCount,totalSize,files.copyOrMove).start();
        //////////////////////// END NEW /////////////////////////////

        rs.close();
    }

    @Override
    public byte[] hashFile(BasePathContent pathname,
                           HashRequestCodes hashAlgorithm,
                           BitSet dirHashOpts) throws IOException {
        if (pathname instanceof XREPathContent)
            if (!ProgressIndicator.acquire(ForegroundServiceType.XRE_HASH)) return null;

        try {
            rs = getStreams(pathname,false);
            SinglePath_rq rq = new hash_rq(
                    pathname.dir,
                    hashAlgorithm,
                    dirHashOpts
            );
            rq.write(rs.o);

            int resp = Misc.receiveBaseResponse(rs.i);
            if (resp == 0) {
                byte[] digest = new byte[hashAlgorithm.getLength()];
                rs.i.readFully(digest);
                return digest;
            }
            return null;
        }
        catch (IOException e) {
            if (pathname instanceof XREPathContent) {
                rs.close();
                MainActivity.rootHelperRemoteClientManager.longTermClients.remove(((XREPathContent)pathname).serverHost);
                return null;
            }
            else throw e;
        }
        finally {
            if (pathname instanceof XREPathContent) {
                ProgressIndicator.release();
            }
            else rs.close();
        }

    }

    public void killServer() throws IOException {
        Log.d("RHClient","killserver invoked!!!!!!!!!!!!!!!");
        try(StreamsPair rs = getStreams()) {
            rs.o.write(ControlCodes.ACTION_EXIT.getValue());
        }
    }

    // kills another RH process (executing some long-term task) via SIGINT,
    // by performing the suitable ACTION_KILL request to the RH server instance
    // connected to this client (which, presumably, is different from the target server to be killed).
    // In theory, since the socket is bidirectional, one could also modify the RH server
    // to create an exit-only-listener thread to wait for request 31 (EXIT) and then issue exit(0)
    // and stop the entire process, but in case of conflict mode enabled, this listener thread
    // should also dispatch non-exit requests (conflict decisions) to the main thread, complicating
    // the server logic.
    /*
     * not used anymore, rh server is multithreaded
     */
    public int killRHProcess(long pid, SocketNames name) {
        Log.d("RHClient","kill invoked on pid "+pid);
        try (StreamsPair rs = getStreams()) {
            rs.o.write(ControlCodes.ACTION_KILL.getValue());
            rs.o.write(Misc.castUnsignedNumberToBytes(pid,4)); // PID
            rs.o.write(Misc.castUnsignedNumberToBytes(2,4)); // SIGNUM (SIGINT = 2)
            return Misc.receiveBaseResponse(rs.i);
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Using RH's internal HTTPS client
    public void downloadHttpsUrl(String url, String destPath, String[] targetFilename, boolean httpsOnly) throws IOException {
        try {
            rs = getStreams();
            try (FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(rs.o)) { // send a single packet instead of multiple ones
                byte req = ControlCodes.ACTION_HTTPS_URL_DOWNLOAD.getValue();
                req ^= ((httpsOnly ? 3 : 1) << 5); // flags: 011 vs 001 (most significant bit unused, httpsOnly variable, download to file true)
                nbf.write(req);
                Misc.sendStringWithLen(nbf,url);
                Misc.sendStringWithLen(nbf,destPath);
                Misc.sendStringWithLen(nbf,targetFilename[0]);
            }

            for(;;) {
                byte resp = rs.i.readByte();
                if (resp != ResponseCodes.RESPONSE_OK.getValue()) {
                    if (resp == ResponseCodes.RESPONSE_REDIRECT.getValue()) {
                        Log.d("RHHttpsClient","End of redirects");
                        break;
                    }
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    throw new IOException("Error returned from roothelper server: " + errno);
                }
                byte[] tlsSessionHash = new byte[32];
                rs.i.readFully(tlsSessionHash);
                Log.d("RHHttpsClient","Client TLS session shared secret hash: "+Misc.toHexString(tlsSessionHash));
            }

            targetFilename[0] = Misc.receiveStringWithLen(rs.i); // Receive possibly updated filename string from rh
            long downloadSize = Misc.receiveTotalOrProgress(rs.i);
            Log.d("RHHttpsClient","Received download size is: "+downloadSize);

            for(;;) {
                long progress = Misc.receiveTotalOrProgress(rs.i);
                if (progress == EOF_ind) break;
                if (downloadSize > 0)
                    task.publishProgressWrapper(new Pair<>(progress, downloadSize));
            }
            Log.d("RHHttpsClient","Download completed");
        }
        finally {
            rs.close();
        }
    }

    public byte[] downloadHttpsUrlInMemory(String url) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(StreamsPair rs = getStreams()) {
            try (FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(rs.o)) { // send a single packet instead of multiple ones
                byte req = ControlCodes.ACTION_HTTPS_URL_DOWNLOAD.getValue();
                req ^= (2 << 5); // flags: 010 -> most significant flag bit: unused; https only: true; download to file: false (i.e. download to memory)
                nbf.write(req);
                Misc.sendStringWithLen(nbf,url);
                Misc.sendStringWithLen(nbf,""); // empty dest path
                Misc.sendStringWithLen(nbf,""); // empty dest filename
            }

            for(;;) {
                byte resp = rs.i.readByte();
                if (resp != ResponseCodes.RESPONSE_OK.getValue()) {
                    if (resp == ResponseCodes.RESPONSE_REDIRECT.getValue()) { // RESPONSE_HTTPS_END_OF_REDIRECTS
                        Log.d("RHHttpsClient","End of redirects");
                        break;
                    }
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    throw new IOException("Error returned from roothelper server: " + errno);
                }
                byte[] tlsSessionHash = new byte[32];
                rs.i.readFully(tlsSessionHash);
                Log.d("RHHttpsClient","Client TLS session shared secret hash: "+Misc.toHexString(tlsSessionHash));
            }

            String unusedFilename = Misc.receiveStringWithLen(rs.i);
            long downloadSize = Misc.receiveTotalOrProgress(rs.i);
            Log.d("RHHttpsClient","Received download size is: "+downloadSize);

            for(;;) {
                byte[] x = new byte[1024];
                int readBytes = rs.i.read(x);
                if(readBytes <= 0) break;
                baos.write(x,0,readBytes);
            }
        }
        Log.d("RHHttpsClient","In-memory download completed");
        return baos.toByteArray();
    }

    // Using RH's internal HTTPS client
    public String uploadHttpsUrl(String domain, String srcPath) throws IOException {
        try {
            rs = getStreams();
            try (FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(rs.o)) { // send a single packet instead of multiple ones
                nbf.write(ControlCodes.ACTION_CLOUD_SERVICES.getValue());
                nbf.write(new byte[]{0x12, 0x00}); // HTTP upload selector string
                Misc.sendStringWithLen(nbf,domain); // upload domain: x0.at or 0x0.st
                Misc.sendStringWithLen(nbf,srcPath);
            }

            if(Misc.receiveBaseResponse(rs.i) != 0) throw new IOException("x0.at connection error");
            byte[] tlsSessionHash = new byte[32];
            rs.i.readFully(tlsSessionHash);
            Log.d("RHHttpsClient","Client TLS session shared secret hash: "+Misc.toHexString(tlsSessionHash));

            long size = Misc.receiveTotalOrProgress(rs.i);
            Log.d("RHHttpsClient","Received upload size is: "+size);

            for(;;) {
                long progress = Misc.receiveTotalOrProgress(rs.i);
                if (progress == EOF_ind) break;
                task.publishProgressWrapper(new Pair<>(progress, size));
            }
            Log.d("RHHttpsClient","Upload completed");

            /////////////////////////////////////////////////////////////////
            // Receive dummy data over local socket (common protocol part)
            byte dummy = rs.i.readByte();
            if(dummy != 0x11) throw new RuntimeException("Expected 0x11");
            Misc.receiveStringWithLen(rs.i); // unused string, but must be received
            /////////////////////////////////////////////////////////////////

            // response body (a.k.a. download link) size
            size = Misc.receiveTotalOrProgress(rs.i);
            if(size > 4096) throw new RuntimeException("Too large content size for generated link: "+size);
            byte[] b = new byte[(int)size];
            rs.i.readFully(b);
            return new String(b, StandardCharsets.UTF_8);
        }
        finally {
            rs.close();
        }
    }

    public ssh_keygen_resp generateSSHKeyPair(SshKeyType type, int keySize) {
        try (StreamsPair rs = getStreams()) {
            openssl_rsa_pem_keygen_rq rq;
            switch(type) {
                case RSA:
                    rq = new openssl_rsa_pem_keygen_rq(keySize);
                    break;
                case ED25519:
                    rq = new openssh_ed25519_keygen_rq();
                    break;
                default:
                    return null;
            }

            rq.write(rs.o);
            if(Misc.receiveBaseResponse(rs.i) != 0) return null;
            return new ssh_keygen_resp(rs.i);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public InputStream getInputStream(String srcPath) throws IOException {
        return new RHInputStream(srcPath);
    }

    public OutputStream getOutputStream(String destPath) throws IOException {
        return new RHOutputStream(destPath);
    }

    // ByteArrayOutputStream to buffer and defer bigger write chunks internally, otherwise IO will be very slow if there are a lot of small writes

    public class RHInputStream extends InputStream {
        private final StreamsPair rs;
        public RHInputStream(String srcPath) throws IOException {
            rs = getStreams();

            // open target filepath
            fileio_rq rq = new fileio_rq(srcPath, FileIOMode.READFROMFILE);
            rq.write(rs.o);

            int ret = Misc.receiveBaseResponse(rs.i);
            if (ret != 0) {
                rs.close();
                throw new IOException("File read error");
            }
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return rs.i.read(b);
        }

        @Override
        public int read() throws IOException {
            return rs.i.read();
        }

        @Override
        public void close() {
            try {rs.close();}
            catch (Exception ignored) {}
        }
    }

    public class RHOutputStream extends OutputStream {
        private final StreamsPair rs;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1048576);
        public RHOutputStream(String destPath) throws IOException {
            rs = getStreams();

            // open target filepath
            fileio_rq rq = new fileio_rq(destPath, FileIOMode.WRITETOFILE);
            rq.write(rs.o);

            int ret = Misc.receiveBaseResponse(rs.i);
            if (ret != 0) {
                rs.close();
                throw new IOException("File creation error");
            }

        }

        private void resetOnOverflow() throws IOException {
            if (baos.size() >= 1048576) {
                rs.o.write(baos.toByteArray());
                baos.reset();
            }
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            resetOnOverflow();
            baos.write(b);
        }

        @Override
        public void write(int i) throws IOException {
            resetOnOverflow();
            baos.write(i);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            resetOnOverflow();
            baos.write(b,off,len);
        }

        @Override
        public void close() throws IOException {
            if (rs!=null) rs.o.write(baos.toByteArray());
            if (rs!=null) rs.close();
            baos.reset();
        }
    }

}

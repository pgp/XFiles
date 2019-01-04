package it.pgp.xfiles.roothelperclient;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.reqs.PairOfPaths_rq;
import it.pgp.xfiles.roothelperclient.reqs.SinglePath_rq;
import it.pgp.xfiles.roothelperclient.reqs.compress_rq;
import it.pgp.xfiles.roothelperclient.reqs.copy_rq;
import it.pgp.xfiles.roothelperclient.reqs.create_rq;
import it.pgp.xfiles.roothelperclient.reqs.del_rq;
import it.pgp.xfiles.roothelperclient.reqs.extract_rq;
import it.pgp.xfiles.roothelperclient.reqs.hash_rq;
import it.pgp.xfiles.roothelperclient.reqs.ls_archive_rq;
import it.pgp.xfiles.roothelperclient.reqs.ls_rq;
import it.pgp.xfiles.roothelperclient.reqs.move_rq;
import it.pgp.xfiles.roothelperclient.reqs.singleStats_rq;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.roothelperclient.resps.ls_resp;
import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.service.SocketNames;
import it.pgp.xfiles.utils.ArchiveVMap;
import it.pgp.xfiles.utils.FileOperationHelperUsingPathContent;
import it.pgp.xfiles.utils.GenericMRU;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.dircontent.ArchiveSubDirWithContent;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.LocalDirWithContent;
import it.pgp.xfiles.utils.iterators.VMapSubTreeIterable;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

@Deprecated
public class RootHelperClientOnce implements FileOperationHelperUsingPathContent {

    public static final SocketNames address = SocketNames.theroothelper;

    /***************************************************************************
     * Singleton class for using only one client to server connection
     * from the spawning of roothelper server process till the app exit;
     * any request made to the server gets a response and the streams are
     * not closed, in so preventing possible malicious apps attempts to access
     * roothelper server while running
     * TODO RootHelper main loop has to be adjusted (that is, not disconnect
     * TODO after a client request has been server, and exiting upon any kind of
     * TODO exception that breaks client connection
     ***************************************************************************/
    private static RootHelperStreams rs;
    private class RootHelperStreams {
        DataInputStream i;
        OutputStream o;

        RootHelperStreams() throws IOException {
            LocalSocket clientSocket = new LocalSocket();
            LocalSocketAddress socketAddress = new LocalSocketAddress(address.name(), LocalSocketAddress.Namespace.ABSTRACT);
            clientSocket.connect(socketAddress);
            Log.e("roothelperclient","Connected");

            o = clientSocket.getOutputStream();
            i = new DataInputStream(clientSocket.getInputStream());
            Log.e("roothelperclient","Streams acquired");
        }

        public void close() {
            try {
                i.close();
                o.close();
            }
            catch (IOException ignored) {}
        }
    }

    private void ensureStreams() {
        try {
            if (rs == null || rs.i == null || rs.o == null) {
                // TODO check that there are not active instances of roothelper (cat /proc/net/unix | grep theroothelper), if so, send exit request
                // TODO rso must be set to null after any IOException in roothelper client
                RootHandler.runRootHelper(address);
                rs = new RootHelperStreams();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            rs = null;
        }
    }
    /***************************************************************************
     ***************************************************************************/

    // only with RESPONSE_OK
    private List<BrowserItem> assembleContentFromLsResps(DataInputStream clientInStream) throws IOException {
        List<BrowserItem> dirContent = new ArrayList<>();
        // read len, if 0 stop reading
        ls_resp resp = new ls_resp(clientInStream);
        while (resp.filename != null) {
            dirContent.add(new BrowserItem(resp));
            resp = new ls_resp(clientInStream);
        }
        return dirContent;
    }

    private ArchiveVMap fillArchiveVMap(DataInputStream clientInStream) throws IOException {
        ArchiveVMap v = new ArchiveVMap();
        // read len, if 0 stop reading
        ls_resp resp = new ls_resp(clientInStream);
        int entryCnt = 0; // for extracting selected files, it is necessary to know their position in the archive entries list
        while (resp.filename != null) {
            // TODO replace BrowserItem with another similar bean without checked box
            List<String> inArchivePath = new ArrayList<>();
            inArchivePath.addAll(Arrays.asList((new String(resp.filename,"UTF-8")).split("/")));
            inArchivePath.add(ArchiveVMap.sentinelKeyForNodeProperties);

            HashMap<String,Object> nodeProps = new HashMap<>();

            nodeProps.put("i",entryCnt);
            nodeProps.put("size",resp.size);
            nodeProps.put("date",new Date(resp.date*1000L));
            nodeProps.put("isDir",new String(resp.permissions, "UTF-8").charAt(0) == 'd');

            v.set(nodeProps,inArchivePath.toArray()); // put in vMap with properties

            resp = new ls_resp(clientInStream);
            entryCnt++;
        }
        return v;
    }

    // ls interaction return list of BrowserItem (which is only a representation class, not a business logic one)
    // this because every request in the root case has to be passed to roothelper, so it doesn't make sense to create
    // an intermediate DirWithContent object

    public GenericDirWithContent listDirectory(BasePathContent dirPath) {
        SinglePath_rq req = new ls_rq(dirPath.dir);
        ensureStreams();
        if (rs == null) return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);

        try {
            List<BrowserItem> dirContent;

            // send request
            req.write(rs.o);
            Log.e("roothelperclient","Ls request sent");


            // read responses (one item per file in directory)
            // read control byte (ok or error) // TODO embed in constructor? create two response classes (base response -1 file - and full response (accounting length-0 list termination)?

            // TODO response byte to be embedded in response classes (maybe also request byte)
            byte responseByte = rs.i.readByte();
            ControlCodes c = ControlCodes.getCode(responseByte);

            switch(c) {
                case RESPONSE_OK:
                    dirContent = assembleContentFromLsResps(rs.i);
                    break;
                case RESPONSE_ERROR:
                    // propagate errno within DirWithContentUsingBrowserItems object
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper","Error returned from roothelper server: "+errno);
                    return new LocalDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS); // TODO errno constants in enum
                default:
                    throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
            }

            for (BrowserItem z : dirContent) {
                Log.d("roothelper","dir item:\t"+z);
            }

            // successful return, change current helper
//            MainActivity.currentHelper = MainActivity.rootHelperClient; // or = this
            MainActivity.currentHelper = null; // DUMMY
            return new LocalDirWithContent(dirPath.dir,dirContent);
        }
        catch (IOException e) {
            return new LocalDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
        }
    }

    // because there are points when a new RootHelper instance is created locally
    private static final GenericMRU<String,ArchiveVMap> archiveMRU = new GenericMRU<>(10); // up to 10 entries

    private Object getArchiveVMapOrListChildren(BasePathContent archivePath, boolean getVMap) {
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
                statForModifiedDate = statFile(new LocalPathContent(((ArchivePathContent)archivePath).archivePath));
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
            // open streams
            ensureStreams();
            if (rs == null) return new GenericDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);

            // zero-length password not allowed, used by roothelper protocol as indication of no password provided
            ls_archive_rq listArchive_rq = new ls_archive_rq(
                    path.getBytes(),
                    password==null?new byte[0]:password.getBytes()
            );

            try {
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

            if(getVMap) return archiveMap;
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

    // password may be null
    @Override
    public GenericDirWithContent listArchive(BasePathContent archivePath) {
        return (GenericDirWithContent) getArchiveVMapOrListChildren(archivePath,false);
    }

    @Override
    public int compressToArchive(BasePathContent srcDirectory,
                                  BasePathContent destArchive,
                                  @Nullable Integer compressionLevel,
                                  @Nullable Boolean encryptHeaders,
                                  @Nullable Boolean solidMode,
                                  @Nullable String password,
                                  @Nullable List<String> filenames) throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

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

        return Misc.receiveBaseResponse(rs.i);
    }

    @Override
    public FileOpsErrorCodes extractFromArchive(BasePathContent srcArchive,
                                                BasePathContent destDirectory,
                                                @Nullable String password,
                                                @Nullable List<String> filenames) throws IOException {

        if (destDirectory.providerType != ProviderType.LOCAL) {
            throw new RuntimeException("Forbidden type for destination directory");
        }

        FileOpsErrorCodes errcode;

        switch (srcArchive.providerType) {
            case LOCAL:
                // entryIdxs will be ignored, extract all, no need to preload VMap
                return extractAll(srcArchive.dir,destDirectory.dir,password);
            case LOCAL_WITHIN_ARCHIVE:
                break;
            default:
                throw new RuntimeException("Forbidden types for archive and/or directories");
        }

        ArchiveVMap avm;
        try {
            avm = (ArchiveVMap) getArchiveVMapOrListChildren(srcArchive,true);
        }
        catch (NullPointerException | ClassCastException r){
            throw new IOException("Unable to refresh archive vmap for extract"); // abuse of exception
        }

        List<Integer> eee = new ArrayList<>();

        // srcArchive is ArchivePathContent
        if (filenames == null || filenames.size()==0) {
            if (srcArchive.dir == null || srcArchive.dir.equals("") || srcArchive.dir.equals("/")) {
                // no selection in root dir of archive, extract all
                return extractAll(((ArchivePathContent)srcArchive).archivePath,destDirectory.dir,password);
            }
            else {
                // no selection in subpath of archive
                // iterator over srcArchive (subpath as root), accumulate idxs
                eee.addAll(getEntries(avm,srcArchive.dir));
            }
        }
        else {
            // some selection
            for (String filename : filenames) {
                eee.addAll(getEntries(
                        avm,
                        srcArchive.concat(filename).dir
                ));
            }
        }
        return extractSome(((ArchivePathContent)srcArchive).archivePath,destDirectory.dir,password,eee);
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

    private List<Integer> getEntries(ArchiveVMap vMap, String relToArchivePathname) throws IOException {
        List<Integer> entries = new ArrayList<>();

        if (relToArchivePathname.equals("") || relToArchivePathname.equals("/"))
            throw new RuntimeException("This condition should be managed earlier than here");

        VMapSubTreeIterable it = new VMapSubTreeIterable(vMap,relToArchivePathname.split("/"));

        for (Map.Entry me : it) {
            try {
                Map x = (Map)((Map)me.getValue()).get(ArchiveVMap.sentinelKeyForNodeProperties);
                Integer ii = (Integer) x.get("i");
                entries.add(ii);
            }
            catch (NullPointerException | ClassCastException ignored) {}
        }

        return entries;
    }

    private FileOpsErrorCodes extractAll(String archive,
                                         String directory,
                                         @Nullable String password) throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        extract_rq rq = new extract_rq(archive,directory,password,null,null);
        rq.write(rs.o);

        int errno = Misc.receiveBaseResponse(rs.i);
        if (errno == 0) return null;
        else if (errno == 0x101010) return FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD;
        else throw new IOException("Unable to extract"); // abuse of exception
    }

    private FileOpsErrorCodes extractSome(String archive,
                                          String directory,
                                          @Nullable String password,
                                          @NonNull List<Integer> entries) throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

//        extract_rq rq = new extract_rq(
//                archive,
//                directory,
//                password,
//                null,
//                entries);
        extract_rq rq = null; // compatibility stub
        rq.write(rs.o);

        int errno = Misc.receiveBaseResponse(rs.i);
        if (errno == 0) return null;
        else if (errno == 0x101010) return FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD;
        else throw new IOException("Unable to extract"); // abuse of exception
    }

    // fileOrDirectory: true for creating file, false for creating directories
    @Override
    public void createFileOrDirectory(BasePathContent filePath, FileMode fileOrDirectory, FileCreationAdvancedOptions... unused) throws IOException {
        SinglePath_rq req = new create_rq(filePath.dir, fileOrDirectory);

        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        // send request
        req.write(rs.o);
        Log.e("roothelperclient","Create request sent");

        // read response
        byte responseByte = rs.i.readByte();
        ControlCodes c = ControlCodes.getCode(responseByte);

        switch(c) {
            case RESPONSE_OK:
                Log.e("roothelper","OK returned from roothelper server for create file: "+filePath.dir);
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                rs.i.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper","Error returned from roothelper server: "+errno+" for file "+filePath.dir);
                throw new IOException("File creation error"); // FIXME temporary, remove once changed Fileopshelper interface to boolean return values
            default:
                throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
        }
    }

    @Override
    public void createLink(BasePathContent originPath, BasePathContent linkPath, boolean isHardLink) throws IOException {
        throw new RuntimeException("Not implemented");
    }

    public boolean exists(BasePathContent filePath) {
        if (filePath.providerType==null) return false;
        switch (filePath.providerType) {
            case LOCAL:
                // TODO send exists request to roothelper
                break;
            case LOCAL_WITHIN_ARCHIVE:
                // TODO check existence in VMap
                break;
            default:
                throw new RuntimeException("Unsupported BasePathContent subtype in roothelperclient exists call");
        }
        return true;
    }

    public boolean isFile(BasePathContent filePath) {
        return true; // TODO
    }

    public boolean isDir(BasePathContent filePath) {
        return true; // TODO
    }

    // TODO to be tested
    // client test case for delete request-response interaction(s)
    @Override
    public void deleteFilesOrDirectories(List<BasePathContent> filePaths) throws IOException {
        // TODO make one connection per set of delete requests
        for (BasePathContent filePath : filePaths) {
            SinglePath_rq req = new del_rq(filePath.dir);
            ensureStreams();
            if (rs == null) throw new IOException("Unable to acquire streams");

            // send request
            req.write(rs.o);
            Log.e("roothelperclient","Del request sent");


            // read responses (one item per file in directory)
            // read control byte (ok or error)

            byte responseByte = rs.i.readByte();
            ControlCodes c = ControlCodes.getCode(responseByte);

            switch(c) {
                case RESPONSE_OK:
                    Log.e("roothelper","OK returned from roothelper server for delete file: "+filePath.dir);
                    break;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper","Error returned from roothelper server: "+errno+" for file "+filePath.dir);
                    break;
                default:
                    throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
            }
        }
    }

    @Override
    public boolean renameFile(BasePathContent oldPathname, BasePathContent newPathname) throws IOException {
        // treat as move request
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        // send request
        PairOfPaths_rq rq = new move_rq(oldPathname.dir,newPathname.dir);
        rq.write(rs.o);

        // receive response
        byte resp = rs.i.readByte();

        ControlCodes c = ControlCodes.getCode(resp);

        if(c != null) {
            switch (c) {
                // FIXME duplicated code in this file
                case RESPONSE_OK:
                    return true;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper", "Error returned from roothelper server: " + errno);
                default:
                    return false;
            }
        }
        else {
            throw new RuntimeException("Illegal response byte from roothelper server");
        }
    }

    // only for local files (not within archives)
    @Override
    public SingleStatsItem statFile(BasePathContent pathname) throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        singleStats_rq rq = new singleStats_rq(pathname.dir,FileMode.FILE);
        rq.write(rs.o);

        // receive base response
        byte responseByte = rs.i.readByte();
        ControlCodes c = ControlCodes.getCode(responseByte);
        switch (c) {
            // FIXME duplicated code in this file
            case RESPONSE_OK:
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                rs.i.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper", "Error returned from roothelper server: " + errno);
                return null;
            default:
                throw new RuntimeException("Illegal response byte from roothelper server");
        }
        // receive and return response
        singleStats_resp resp = new singleStats_resp(rs.i);

        return new SingleStatsItem(resp);
    }

    @Override
    public folderStats_resp statFiles(List<BasePathContent> files) throws IOException {
        return null; // DUMMY
    }

    @Override
    public folderStats_resp statFolder(BasePathContent pathname) throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        singleStats_rq rq = new singleStats_rq(pathname.dir,FileMode.DIRECTORY);
        rq.write(rs.o);

        // receive base response
        byte resp = rs.i.readByte();
        ControlCodes c = ControlCodes.getCode(resp);
        switch (c) {
            // FIXME duplicated code in this file
            case RESPONSE_OK:
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                rs.i.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper", "Error returned from roothelper server: " + errno);
                return null;
            default:
                throw new RuntimeException("Illegal response byte from roothelper server");
        }

        // receive and return response
        return new folderStats_resp(rs.i);
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListPathContent files, BasePathContent dstFolder) throws IOException {
        // for x in pathnames (full paths), send to roothelper the pair:
        // (x , dstFolder + "/" + x.getName() ) // stop on first level, roothelper C code performs dir tree move/copy/delete if needed
        // receive one response per pair

        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        for (String pathname : files) {
            String destinationPathname = dstFolder.dir+"/"+(new File(pathname).getName());
            // send request
            PairOfPaths_rq rq = (files.copyOrMove == CopyMoveMode.COPY) ? new copy_rq(pathname,destinationPathname): new move_rq(pathname,destinationPathname);
            rq.write(rs.o);

            // receive response
            byte resp = rs.i.readByte();
            ControlCodes c = ControlCodes.getCode(resp);
            switch (c) {
                // FIXME duplicated code in this file
                case RESPONSE_OK:
                    continue;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    rs.i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper", "Error returned from roothelper server: " + errno);
                    break;
                default:
                    throw new RuntimeException("Illegal response byte from roothelper server");
            }
        }
    }

    @Override
    public byte[] hashFile(BasePathContent pathname, HashRequestCodes hashAlgorithm) throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");

        SinglePath_rq rq = new hash_rq(pathname.dir,hashAlgorithm);
        rq.write(rs.o);

        // receive response
        byte resp = rs.i.readByte();
        byte[] response;

        ControlCodes c = ControlCodes.getCode(resp);

        // TODO REFACTOR RECEIVE BASE RESPONSE
        switch (c) {
            // FIXME duplicated code in this file
            case RESPONSE_OK:
                byte[] digest = new byte[hashAlgorithm.getLength()];
                rs.i.readFully(digest);
                response = digest;
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                rs.i.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper", "Error returned from roothelper server: " + errno);
                response = null;
                break;
            default:
                Log.e(this.getClass().getName(),"Illegal response byte from roothelper server");
                response = null;
        }
        return response;
    }

    public void killServer() throws IOException {
        ensureStreams();
        if (rs == null) throw new IOException("Unable to acquire streams");
        byte end = ControlCodes.ACTION_EXIT.getValue();
        rs.o.write(end);
        rs.close();
        rs = null;
    }

    @Override
    public void initProgressSupport(BaseBackgroundTask task) {
    }

    @Override
    public void destroyProgressSupport() {
    }
}

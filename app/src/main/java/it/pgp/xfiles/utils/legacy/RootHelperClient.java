package it.pgp.xfiles.utils.legacy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.reqs.PairOfPaths_rq;
import it.pgp.xfiles.roothelperclient.reqs.SinglePath_rq;
import it.pgp.xfiles.roothelperclient.reqs.copy_rq;
import it.pgp.xfiles.roothelperclient.reqs.create_rq;
import it.pgp.xfiles.roothelperclient.reqs.del_rq;
import it.pgp.xfiles.roothelperclient.reqs.hash_rq;
import it.pgp.xfiles.roothelperclient.reqs.ls_rq;
import it.pgp.xfiles.roothelperclient.reqs.move_rq;
import it.pgp.xfiles.roothelperclient.reqs.singleStats_rq;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.roothelperclient.resps.ls_resp;
import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 20/01/17
 */

@Deprecated
public class RootHelperClient implements FileOperationHelper {

    public static final String address = "theroothelper";

    // ls interaction return list of BrowserItem (which is only a representation class, not a business logic one)
    // this because every request in the root case has to be passed to roothelper, so it doesn't make sense to create
    // an intermediate DirWithContent object

    public DirWithContentUsingBrowserItems listDirectory(String dirPath) {
        SinglePath_rq req = new ls_rq(dirPath);
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        try {
            clientSocket.connect(socketAddress);
            Log.e("roothelperclient","Connected");

            OutputStream clientOutStream = clientSocket.getOutputStream();
            DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
            Log.e("roothelperclient","Streams acquired");

            ArrayList<BrowserItem> dirContent = new ArrayList<>();

            // send request
            req.write(clientOutStream);
            Log.e("roothelperclient","Ls request sent");


            // read responses (one item per file in directory)
            // read control byte (ok or error) // TODO embed in constructor? create two response classes (base response -1 file - and full response (accounting length-0 list termination)?

            // TODO response byte to be embedded in response classes (maybe also request byte)
            byte responseByte = clientInStream.readByte();
            ControlCodes c = ControlCodes.getCode(responseByte);

            switch(c) {
                case RESPONSE_OK:
                    // read len, if 0 stop reading
                    ls_resp resp = new ls_resp(clientInStream);
                    while (resp.filename != null) {
                        dirContent.add(new BrowserItem(resp));
                        resp = new ls_resp(clientInStream);
                    }
                    break;
                case RESPONSE_ERROR:
                    // propagate errno within DirWithContentUsingBrowserItems object
                    byte[] errno_ = new byte[4];
                    clientInStream.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper","Error returned from roothelper server: "+errno);
                    return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS); // TODO errno constants in enum
                default:
                    throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
            }

            for (BrowserItem z : dirContent) {
                Log.d("roothelper","dir item:\t"+z);
            }

            // TODO close streams?
            // successful return, change current helper
//            MainActivity.currentHelper = MainActivity.rootHelperClient; // or = this
            MainActivity.currentHelper = null; // DUMMY
            return new DirWithContentUsingBrowserItems(dirPath,dirContent);
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
    public Long[] findInContent(String expr, String filepath) throws IOException {
//        LocalSocket clientSocket = new LocalSocket();
//        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
//        clientSocket.connect(socketAddress);
//        Log.e("roothelperclient","Connected");
//
//        OutputStream clientOutStream = clientSocket.getOutputStream();
//        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
//        Log.e("roothelperclient","Streams acquired");
//
//        PairOfPaths_rq rq = new find_in_content_rq(filepath,expr);
//        rq.write(clientOutStream);
//
//        // receive response
//        byte resp = clientInStream.readByte();
//
//        ControlCodes c = ControlCodes.getCode(resp);
//
//        ArrayList<Long> results = new ArrayList<>();
//
//        // TODO REFACTOR RECEIVE BASE RESPONSE
//        switch (c) {
//            // FIXME duplicated code in this file
//            case RESPONSE_OK:
//                find_resp find_resp = new find_resp(clientInStream);
//                return find_resp.getResults();
//            case RESPONSE_ERROR:
//                byte[] errno_ = new byte[4];
//                clientInStream.readFully(errno_);
//                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
//                Log.e("roothelper", "Error returned from roothelper server: " + errno);
//                return null;
//            default:
//                throw new RuntimeException("Illegal response byte from roothelper server");
//        }
        return null;
    }

    // fileOrDirectory: true for creating file, false for creating directories
    @Override
    public void createFileOrDirectory(String filePath, FileMode fileOrDirectory) throws IOException {
        SinglePath_rq req = new create_rq(filePath, fileOrDirectory);
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");

        // send request
        req.write(clientOutStream);
        Log.e("roothelperclient","Create request sent");

        // read response
        byte responseByte = clientInStream.readByte();
        ControlCodes c = ControlCodes.getCode(responseByte);

        switch(c) {
            case RESPONSE_OK:
                Log.e("roothelper","OK returned from roothelper server for create file: "+filePath);
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                clientInStream.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper","Error returned from roothelper server: "+errno+" for file "+filePath);
                throw new IOException("File creation error"); // FIXME temporary, remove once changed Fileopshelper interface to boolean return values
            default:
                throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
        }
    }

    public boolean exists(String filePath) {
        return true;
    }

    public boolean isFile(String filePath) {
        return true;
    }

    public boolean isDir(String filePath) {
        return true;
    }

    // TODO to be tested
    // client test case for delete request-response interaction(s)
    @Override
    public void deleteFilesOrDirectories(List<String> filePaths) throws IOException {
        // TODO make one connection per set of delete requests
        for (String filePath : filePaths) {
            SinglePath_rq req = new del_rq(filePath);
            LocalSocket clientSocket = new LocalSocket();
            LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
            clientSocket.connect(socketAddress);
            Log.e("roothelperclient","Connected");

            OutputStream clientOutStream = clientSocket.getOutputStream();
            DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
            Log.e("roothelperclient","Streams acquired");

            // send request
            req.write(clientOutStream);
            Log.e("roothelperclient","Del request sent");


            // read responses (one item per file in directory)
            // read control byte (ok or error)

            byte responseByte = clientInStream.readByte();
            ControlCodes c = ControlCodes.getCode(responseByte);

            switch(c) {
                case RESPONSE_OK:
                    Log.e("roothelper","OK returned from roothelper server for delete file: "+filePath);
                    break;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    clientInStream.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper","Error returned from roothelper server: "+errno+" for file "+filePath);
                    break;
                default:
                    throw new RuntimeException("Unexpected response code from roothelper server: "+(int)responseByte);
            }
        }
    }

    @Override
    public void renameFile(String oldPathname, String newPathname) throws IOException {
        // treat as move request
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");


        // send request
        PairOfPaths_rq rq = new move_rq(oldPathname,newPathname);
        rq.write(clientOutStream);

        // receive response
        byte resp = clientInStream.readByte();

        ControlCodes c = ControlCodes.getCode(resp);

        if(c != null) {
            switch (c) {
                // FIXME duplicated code in this file
                case RESPONSE_OK:
                    return;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    clientInStream.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper", "Error returned from roothelper server: " + errno);
                    break;
            }
        }
        else {
            throw new RuntimeException("Illegal response byte from roothelper server");
        }
    }

    @Override
    public singleStats_resp statFile(String pathname) throws IOException {
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");

        singleStats_rq rq = new singleStats_rq(pathname,FileMode.FILE);
        rq.write(clientOutStream);

        // receive base response
        byte resp = clientInStream.readByte();
        ControlCodes c = ControlCodes.getCode(resp);
        switch (c) {
            // FIXME duplicated code in this file
            case RESPONSE_OK:
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                clientInStream.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper", "Error returned from roothelper server: " + errno);
                return null;
            default:
                throw new RuntimeException("Illegal response byte from roothelper server");
        }

        // receive and return response
        return new singleStats_resp(clientInStream);
    }

    @Override
    public void statFiles(List<String> files) throws IOException {
        // TODO
    }

    @Override
    public folderStats_resp statFolder(String pathname) throws IOException {
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");

        singleStats_rq rq = new singleStats_rq(pathname,FileMode.DIRECTORY);
        rq.write(clientOutStream);

        // receive base response
        byte resp = clientInStream.readByte();
        ControlCodes c = ControlCodes.getCode(resp);
        switch (c) {
            // FIXME duplicated code in this file
            case RESPONSE_OK:
                break;
            case RESPONSE_ERROR:
                byte[] errno_ = new byte[4];
                clientInStream.readFully(errno_);
                int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                Log.e("roothelper", "Error returned from roothelper server: " + errno);
                return null;
            default:
                throw new RuntimeException("Illegal response byte from roothelper server");
        }

        // receive and return response
        return new folderStats_resp(clientInStream);
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListS files, String dstFolder) throws IOException {
        // for x in pathnames (full paths), send to roothelper the pair:
        // (x , dstFolder + "/" + x.getName() ) // stop on first level, roothelper C code performs dir tree move/copy/delete if needed
        // receive one response per pair

        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");

        for (String pathname : files) {
            String destinationPathname = dstFolder+"/"+(new File(pathname).getName());
            // send request
            PairOfPaths_rq rq = (files.copyOrMove == CopyMoveMode.COPY) ? new copy_rq(pathname,destinationPathname): new move_rq(pathname,destinationPathname);
            rq.write(clientOutStream);

            // receive response
            byte resp = clientInStream.readByte();
            ControlCodes c = ControlCodes.getCode(resp);
            switch (c) {
                // FIXME duplicated code in this file
                case RESPONSE_OK:
                    continue;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    clientInStream.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper", "Error returned from roothelper server: " + errno);
                    break;
                default:
                    throw new RuntimeException("Illegal response byte from roothelper server");
            }
        }
    }

    @Override
    public byte[] hashFile(String pathname, HashRequestCodes hashAlgorithm) throws IOException {
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");

        SinglePath_rq rq = new hash_rq(pathname,hashAlgorithm);
        rq.write(clientOutStream);

        // receive response
        byte resp = clientInStream.readByte();

        ControlCodes c = ControlCodes.getCode(resp);

        // TODO REFACTOR RECEIVE BASE RESPONSE
            switch (c) {
                // FIXME duplicated code in this file
                case RESPONSE_OK:
                    byte[] digest = new byte[hashAlgorithm.getLength()];
                    clientInStream.readFully(digest);
                    return digest;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    clientInStream.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper", "Error returned from roothelper server: " + errno);
                    return null;
                default:
                    throw new RuntimeException("Illegal response byte from roothelper server");
            }
    }

    public void killServer() throws IOException {
        LocalSocket clientSocket = new LocalSocket();
        LocalSocketAddress socketAddress = new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT);
        clientSocket.connect(socketAddress);
        Log.e("roothelperclient","Connected");

        OutputStream clientOutStream = clientSocket.getOutputStream();
        DataInputStream clientInStream = new DataInputStream(clientSocket.getInputStream());
        Log.e("roothelperclient","Streams acquired");

        byte end = ControlCodes.ACTION_EXIT.getValue();
        clientOutStream.write(end);
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

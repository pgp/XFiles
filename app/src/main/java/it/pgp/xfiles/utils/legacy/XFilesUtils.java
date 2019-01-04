package it.pgp.xfiles.utils.legacy;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.utils.Checksums;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;
import it.pgp.xfiles.utils.XFilesUtilsUsingPathContent;

/**
 * Created by pgp on 27/09/16
 */

// Java-only equivalent to RootHelperClient (in final implementation, these classes should implement the common interface FileOperationHelper)

@Deprecated
public class XFilesUtils implements FileOperationHelper {

    public void deleteFileOrDir(File file) throws IOException {
        if (file.isDirectory()) XFilesUtilsUsingPathContent.deleteDirectory(file);
        else file.delete();
    }

    public void copyFileOrDirectory(File srcFileOrDir, File dstFolder) throws IOException {
        if (srcFileOrDir.isDirectory()) {
            File[] files = srcFileOrDir.listFiles();
            for (File file : files) {
                File src1 = new File(srcFileOrDir, file.getName());
                File dst1 = new File(dstFolder,srcFileOrDir.getName());
                copyFileOrDirectory(src1, dst1);
            }
        } else {
            copyFile(srcFileOrDir, new File(dstFolder,srcFileOrDir.getName()));
        }
    }

    public void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    // to be merged in Fileopshelper interface
    public FileOpsErrorCodes copyFileOrEmptyDir(String sourceFile_, String destFile_) {
        File sourceFile = new File(sourceFile_);
        File destFile = new File(destFile_);

        if (!destFile.getParentFile().exists()) {
            if (!destFile.getParentFile().mkdirs())
                return FileOpsErrorCodes.TRANSFER_ERROR; // mkdirs error
        }

        if (!destFile.exists()) {
            if (sourceFile.isFile()) {
                try {
                    if (!destFile.createNewFile())
                        return FileOpsErrorCodes.TRANSFER_ERROR; // mkfile error
                }
                catch (IOException i) {
                    return FileOpsErrorCodes.TRANSFER_ERROR; // mkfile error
                }
            }
            else {
                if (!destFile.mkdirs())
                    return FileOpsErrorCodes.TRANSFER_ERROR; // mkdirs error
            }
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            return FileOpsErrorCodes.TRANSFER_OK;
        }
        catch (IOException i) {
            return FileOpsErrorCodes.TRANSFER_ERROR; // copy error
        }
        finally {
            try {
                source.close();
                destination.close();
            }
            catch (IOException|NullPointerException ignored) {}
        }
    }

    // copies regular files and empty directories, to be used with DirTreeWalker classes
    public void copyFileOrEmptyDir(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            if (sourceFile.isFile()) destFile.createNewFile();
            else {
                destFile.mkdirs();
                return;
            }
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public void copyFilesToDirectory(CopyMoveListS files, File dstFolder) throws IOException {
        for (String pathname: files)
            copyFileOrDirectory(new File(pathname),dstFolder);
    }

    public void moveFilesToDirectory(CopyMoveListS files, File dstFolder) throws IOException {
        for (String pathname : files) {
            File file = new File(pathname);
            // removed Commons IO, allow only rename-based move
            File destFile = new File(dstFolder,file.getName());
            if (!file.renameTo(destFile)) throw new IOException("Cannot rename filesystem node");
        }
        // TODO need to expand method (like in copy files) in order to account for file conflicts
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListS files, String dstFolder) throws IOException {
        switch (files.copyOrMove) {
            case COPY:
                copyFilesToDirectory(files,new File(dstFolder));
                break;
            case MOVE:
                moveFilesToDirectory(files,new File(dstFolder));
                break;
        }
    }

    @Override
    public void createFileOrDirectory(String filePath, FileMode fileOrDirectory) throws IOException {
        File f = new File(filePath);
        switch (fileOrDirectory) {
            case FILE:
                f.createNewFile();
                break;
            case DIRECTORY:
                f.mkdirs();
                break;
            default:
                throw new RuntimeException("Undefined file mode"); // Unreachable statement
        }
    }

    @Override
    public void deleteFilesOrDirectories(List<String> pathnames) throws IOException {
        for (String pathname : pathnames) {
            File file = new File(pathname);
            if (file.isDirectory()) XFilesUtilsUsingPathContent.deleteDirectory(file);
            else file.delete();
        }
    }

    @Override
    public void renameFile(String oldPathname, String newPathname) {
        File f = new File(oldPathname);
        File g = new File(newPathname);
        f.renameTo(g);
    }

    @Override
    public singleStats_resp statFile(String pathname) throws IOException {
        // it seems that Java implementation of file stats is only in Java NIO Files (not included in Android)
        // reverting to roothelper
        return (new RootHelperClient()).statFile(pathname);
    }

    @Override
    public void statFiles(List<String> files) throws IOException {
    }

    @Override
    public folderStats_resp statFolder(String pathname) throws IOException {
        // TODO
        // here one can use DirTreeWalker to count files and folders
        return (new RootHelperClient()).statFolder(pathname);
    }

    @Override
    public boolean exists(String pathname) {
        File f = new File(pathname);
        return f.exists();
    }

    @Override
    public boolean isFile(String pathname) {
        File f = new File(pathname);
        return f.exists() && f.isFile();
    }

    @Override
    public boolean isDir(String pathname) {
        File f = new File(pathname);
        return f.exists() && f.isDirectory();
    }

    @Override
    public byte[] hashFile(String pathname, HashRequestCodes hashAlgorithm) throws IOException {
        File f = new File(pathname);
        byte[] digest = null;
        try {
            switch (hashAlgorithm) {
                case md5:
                    digest = Checksums.md5(f);
                    break;
                case sha1:
                    digest = Checksums.sha1(f);
                    break;
                // TODO other methods of enum
                default:
                    Log.e(XFilesUtils.class.getName(), "Not implemented");
            }
        }
        catch (NoSuchAlgorithmException n) {
            n.printStackTrace();
        }

        return digest;
    }

    @Override
    public DirWithContentUsingBrowserItems listDirectory(String directory) {
        File[] content = new File(directory).listFiles();
        if (content == null) return new DirWithContentUsingBrowserItems(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS); // TODO specialize error code (enum to be created) in callers from dir commander
        ArrayList<BrowserItem> l = new ArrayList<>();
        for (File f : content) {
            l.add(new BrowserItem(f.getName(),f.length(),new Date(f.lastModified()),f.isDirectory(),false));
        }

        // successful return, change current helper
        MainActivity.currentHelper = MainActivity.xFilesUtils; // or = this
        return new DirWithContentUsingBrowserItems(directory, l);
    }

    @Override
    public boolean findInName(String expr, String filepath) {
        // TODO can be generalized with regex
        File f = new File(filepath);
        return f.exists() && f.getName().contains(expr);
    }

    // find in content should be modified as well for searching in big files, using offset and size
    @Override
    public Long[] findInContent(String expr, String filepath) {
        ArrayList<Long> offsets = new ArrayList<>();
        byte[] currentBuffer = new byte[expr.length()];
        try {
            RandomAccessFile f = new RandomAccessFile(filepath,"rb");
            for (long pos=0;pos<f.length()-expr.length();pos++) { // FIXME < or <= ?
                f.seek(pos);

                // FIXME inefficient!!!
                f.read(currentBuffer,0,expr.length());
                if (Arrays.equals(expr.getBytes(),currentBuffer)) offsets.add(pos);
            }

            return offsets.toArray(new Long[offsets.size()]);
        }
        catch (IOException i) {
            return null;
        }
    }

    @Override
    public byte[] downloadFileToMemory(String srcFilePath) throws IOException {
        // not needed
        throw new RuntimeException("Not implemented, should not be called from this class");
    }

    @Override
    public void uploadFileFromMemory(String destFilePath, byte[] content) throws IOException {
        // not needed
        throw new RuntimeException("Not implemented, should not be called from this class");
    }
}

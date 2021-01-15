package it.pgp.xfiles.utils;

import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import it.pgp.Native;
import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.RootHelperClient;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.LocalDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.XREPathContent;

/**
 * Created by pgp on 27/09/16
 */

// Java-only equivalent to RootHelperClient (in final implementation, these classes should implement the common interface FileOperationHelper)
public class XFilesUtils implements FileOperationHelper {

    // for publishing progress from within a long term task (copy/move/compress/extract/upload/download)
    BaseBackgroundTask task;
    long totalFilesForProgress,currentFilesForProgress;

    private RootHelperClient rhc;

    private void refreshRHClient() throws IOException {
        rhc = MainActivity.getRootHelperClient();
        if(rhc == null) throw new IOException("Unable to connect to RootHelper");
    }

    @Override
    public void initProgressSupport(BaseBackgroundTask task) {
        this.task = task;
    }

    @Override
    public void destroyProgressSupport() {
        task = null;
        totalFilesForProgress = 0;
        currentFilesForProgress = 0;
    }

    public int getTotalFilesCount(File file) {
        File[] files = file.listFiles();
        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) count += getTotalFilesCount(f);
            else count++;
        }
        return count;
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

//            try {Thread.sleep(1000);} catch (InterruptedException ignored) {}

            if(task != null) {
                currentFilesForProgress++;
                task.publishProgressWrapper(new Pair<>(currentFilesForProgress, totalFilesForProgress));
            }
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
        if (!destFile.exists()) destFile.createNewFile();

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
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

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
            return FileOpsErrorCodes.OK;
        }
        catch (IOException i) {
            return FileOpsErrorCodes.TRANSFER_ERROR; // copy error
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

        try (FileChannel source  = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    public void copyFilesToDirectory(CopyMoveListPathContent files, File dstFolder) throws IOException {
        totalFilesForProgress = 0;
        for (String pathname : files)
            totalFilesForProgress += getTotalFilesCount(new File(pathname));

        for (String pathname: files)
            copyFileOrDirectory(new File(pathname),dstFolder);
    }

    public void moveFilesToDirectory(CopyMoveListPathContent files, File dstFolder) throws IOException {
        for (String pathname : files) {
            File file = new File(pathname);
            // removed Commons IO, allow only rename-based move
            File destFile = new File(dstFolder,file.getName());
            if (!file.renameTo(destFile)) throw new IOException("Cannot rename filesystem node");
        }
    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListPathContent files, BasePathContent dstFolder) throws IOException {
        switch (files.copyOrMove) {
            case COPY:
                copyFilesToDirectory(files,new File(dstFolder.dir));
                break;
            case MOVE:
                moveFilesToDirectory(files,new File(dstFolder.dir));
                break;
        }
    }

    @Override
    public void createFileOrDirectory(BasePathContent filePath, FileMode fileOrDirectory, FileCreationAdvancedOptions... unused) throws IOException {
        File f = new File(filePath.dir);
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
    public void createLink(BasePathContent originPath, BasePathContent linkPath, boolean isHardLink) throws IOException {
        refreshRHClient();
        rhc.createLink(originPath,linkPath,isHardLink);
    }

    public static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    @Override
    public void deleteFilesOrDirectories(List<BasePathContent> pathnames) throws IOException {
        for (BasePathContent pathname : pathnames) {
            File file = new File(pathname.dir);
            if (file.isDirectory()) deleteDirectory(file);
            else file.delete();
        }
    }

    @Override
    public boolean renameFile(BasePathContent oldPathname, BasePathContent newPathname) {
        File f = new File(oldPathname.dir);
        File g = new File(newPathname.dir);
        return f.renameTo(g);
    }

    @Override
    public SingleStatsItem statFile(BasePathContent pathname) throws IOException {
        // it seems that Java implementation of file stats is only in Java NIO Files (not included in Android)
        // reverting to roothelper
        refreshRHClient();
        return rhc.statFile(pathname);
    }

    @Override
    public folderStats_resp statFiles(List<BasePathContent> files) throws IOException {
        refreshRHClient();
        return rhc.statFiles(files);
    }

    @Override
    public folderStats_resp statFolder(BasePathContent pathname) throws IOException {
        // here one can use DirTreeWalker to count files and folders
        refreshRHClient();
        return rhc.statFolder(pathname);
    }

    @Override
    public boolean exists(BasePathContent pathname) {
        File f = new File(pathname.dir);
        return f.exists();
    }

    @Override
    public boolean isFile(BasePathContent pathname) {
        File f = new File(pathname.dir);
        return f.exists() && f.isFile();
    }

    @Override
    public boolean isDir(BasePathContent pathname) {
        File f = new File(pathname.dir);
        return f.exists() && f.isDirectory();
    }

    @Override
    public byte[] hashFile(BasePathContent pathname,
                           HashRequestCodes hashAlgorithm,
                           BitSet dirHashOpts) throws IOException {
        refreshRHClient();
        return rhc.hashFile(pathname,hashAlgorithm,dirHashOpts);
//        File f = new File(pathname.dir);
//        byte[] digest = null;
//        try {
//            switch (hashAlgorithm) {
//                case md5:
//                    digest = Checksums.md5(f);
//                    break;
//                case sha1:
//                    digest = Checksums.sha1(f);
//                    break;
//                default:
//                    Log.e(XFilesUtils.class.getName(), "Not implemented");
//            }
//        }
//        catch (NoSuchAlgorithmException n) {
//            n.printStackTrace();
//        }
//
//        return digest;
    }

    @Override
    public GenericDirWithContent listDirectory(BasePathContent directory) {
        if (directory instanceof XREPathContent) {
            try {refreshRHClient();}
            catch(IOException e) {return new GenericDirWithContent(FileOpsErrorCodes.ROOTHELPER_INIT_ERROR);}
            return rhc.listDirectory(directory);
        }
        File[] content = new File(directory.dir).listFiles();
        if (content == null) return new LocalDirWithContent(FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS); // TODO specialize error code (enum to be created) in callers from dir commander
        ArrayList<BrowserItem> l = new ArrayList<>();
        for (File f : content) {
            l.add(new BrowserItem(f.getName(),f.length(),new Date(f.lastModified()),f.isDirectory(), Native.isSymLink(f.getAbsolutePath())>0)); // getCanonicalPath not enough to fully determine symlink attribute (files in symlinked folders), and Files.isSymbolicLink is available only with minAPI >= 26
        }

        return new LocalDirWithContent(directory.dir, l);
    }

    @Override
    public GenericDirWithContent listArchive(BasePathContent archivePath) {
        try {refreshRHClient();}
        catch(IOException e) {return new GenericDirWithContent(FileOpsErrorCodes.ROOTHELPER_INIT_ERROR);}
        return rhc.listArchive(archivePath);
    }

    @Override
    public int compressToArchive(BasePathContent srcDirectory,
                                  BasePathContent destArchive,
                                  @Nullable Integer compressionLevel,
                                  @Nullable Boolean encryptHeaders,
                                  @Nullable Boolean solidMode,
                                  String password,
                                  List<String> filenames) throws IOException {
        RootHelperClient rh = MainActivity.getRootHelperClient();
        if(rh == null) return -1;
        return rh.compressToArchive(
                srcDirectory,
                destArchive,
                compressionLevel,
                encryptHeaders,
                solidMode,
                password,
                filenames);
    }

    @Override
    public List<FileOpsErrorCodes> extractFromArchive(List<BasePathContent> srcArchives, BasePathContent destDirectory, @Nullable String password, @Nullable List<String> filenames, boolean smartDirectoryCreation) throws IOException {
        RootHelperClient rh = MainActivity.getRootHelperClient();
        if(rh == null) return Collections.singletonList(FileOpsErrorCodes.ROOTHELPER_INIT_ERROR);
        return rh.extractFromArchive(srcArchives,destDirectory,password,filenames,smartDirectoryCreation);
    }

    @Override
    public int setDates(BasePathContent file,
                        @Nullable Date accessDate,
                        @Nullable Date modificationDate) {
        if (file.providerType != ProviderType.LOCAL) return -1;
        File f = new File(file.dir);
        if (!f.exists()) return -1;
        if (modificationDate == null) return 0;
        return f.setLastModified(modificationDate.getTime())?0:-1;
    }

    @Override
    public int setPermissions(BasePathContent file, int permMask) {
        // TODO use NIO setPosixFIlePermissions or use roothelper
        return -1;
    }

    @Override
    public int setOwnership(BasePathContent file,
                            @Nullable Integer ownerId,
                            @Nullable Integer groupId) {
        // TODO use roothelper
        return 0;
    }

}

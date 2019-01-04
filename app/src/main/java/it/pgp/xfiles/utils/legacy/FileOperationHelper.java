package it.pgp.xfiles.utils.legacy;

import java.io.IOException;
import java.util.List;

import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.roothelperclient.resps.singleStats_resp;

/**
 * Created by pgp on 26/01/17
 */

@Deprecated
public interface FileOperationHelper {
    // TODO change return value of every method to boolean or int and remove IOException
    void createFileOrDirectory(String filePath, FileMode fileOrDirectory) throws IOException;
    void deleteFilesOrDirectories(List<String> files) throws IOException; // files as full pathnames
    void copyMoveFilesToDirectory(CopyMoveListS files, String dstFolder) throws IOException;
    void renameFile(String oldPathname, String newPathname) throws IOException;
    singleStats_resp statFile(String pathname) throws IOException;
    void statFiles(List<String> files) throws IOException;
    folderStats_resp statFolder(String pathname) throws IOException;

    boolean exists(String pathname);
    boolean isFile(String pathname);
    boolean isDir(String pathname);

    byte[] hashFile(String pathname, HashRequestCodes hashAlgorithm) throws IOException;

    DirWithContentUsingBrowserItems listDirectory(String directory);

    // returns true if the binary, non-directory file contains expr in its name
    boolean findInName(String expr, String filepath);
    // returns an array of occurrence offsets if the binary, non-directory file contains expr in its content, null otherwise
    Long[] findInContent(String expr, String filepath) throws IOException;

    // methods for file exchange between different file ops helpers (e.g. sftp to local root,
    // sftp to another remote protocol (if implemented, e.g. ftp)
    // pipeline (1 download thread, 1 upload thread): file ops helper 1 -> device ram -> file ops helper 2

    // file paths including protocol, e.g. sftp://user@domain.tld:22/remote/path/file.ext and /storage/sdcard/file.ext
    byte[] downloadFileToMemory(String srcFilePath) throws IOException;
    void uploadFileFromMemory(String destFilePath, byte[] content) throws IOException;

    // TODO implement methods for blocks instead of files (device RAM is limited)
//    byte[] downloadFileChunkToMemory(String filePath, long offset, long size) throws IOException;
//    void uploadFileChunkFromMemory(String filePath, byte[] content, long offset, long size) throws IOException;

}

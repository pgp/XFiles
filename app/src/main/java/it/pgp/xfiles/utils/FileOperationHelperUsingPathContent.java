package it.pgp.xfiles.utils;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 26/01/17
 */

public interface FileOperationHelperUsingPathContent {
    // TODO change return value of every method to boolean or int and remove IOException
    void createFileOrDirectory(BasePathContent filePath, FileMode fileOrDirectory, FileCreationAdvancedOptions... fileOptions) throws IOException;
    void createLink(BasePathContent originPath, BasePathContent linkPath, boolean isHardLink) throws IOException;
    void deleteFilesOrDirectories(List<BasePathContent> files) throws IOException; // files as full pathnames
    void copyMoveFilesToDirectory(CopyMoveListPathContent files, BasePathContent dstFolder) throws IOException;
    boolean renameFile(BasePathContent oldPathname, BasePathContent newPathname) throws IOException; // true: rename ok
    SingleStatsItem statFile(BasePathContent pathname) throws IOException;
    folderStats_resp statFiles(List<BasePathContent> files) throws IOException;
    folderStats_resp statFolder(BasePathContent pathname) throws IOException;

    boolean exists(BasePathContent pathname);
    boolean isFile(BasePathContent pathname);
    boolean isDir(BasePathContent pathname);

    byte[] hashFile(BasePathContent pathname,
                    HashRequestCodes hashAlgorithm,
                    BitSet dirHashOpts) throws IOException;

    GenericDirWithContent listDirectory(BasePathContent directory);
    GenericDirWithContent listArchive(BasePathContent archivePath);

    int compressToArchive(BasePathContent srcDirectory,
                           BasePathContent destArchive,
                           @Nullable Integer compressionLevel,
                           @Nullable Boolean encryptHeaders,
                           @Nullable Boolean solidMode,
                           @Nullable String password,
                           @Nullable List<String> filenames) throws IOException;

    FileOpsErrorCodes extractFromArchive(BasePathContent srcArchive, // subDir taken from here
                                         BasePathContent destDirectory,
                                         @Nullable String password,
                                         @Nullable List<String> filenames) throws IOException;

    int setDates(BasePathContent file,
                     @Nullable Date accessDate,
                     @Nullable Date modificationDate);

    int setPermissions(BasePathContent file, int permMask);

    int setOwnership(BasePathContent file,
                     @Nullable Integer ownerId,
                     @Nullable Integer groupId);

    void initProgressSupport(BaseBackgroundTask task);
    void destroyProgressSupport();

}

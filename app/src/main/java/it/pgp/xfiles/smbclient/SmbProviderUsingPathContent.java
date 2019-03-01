package it.pgp.xfiles.smbclient;

import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.io.RobustLocalFileInputStream;
import it.pgp.xfiles.io.RobustLocalFileOutputStream;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.sftpclient.XProgress;
import it.pgp.xfiles.utils.FileOperationHelperUsingPathContent;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.SmbDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.SmbRemotePathContent;
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.Configuration;
import jcifs.config.DelegatingConfiguration;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.context.CIFSContextWrapper;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class SmbProviderUsingPathContent implements FileOperationHelperUsingPathContent {

    BaseBackgroundTask task;
    @Override
    public void initProgressSupport(BaseBackgroundTask task) {
        this.task = task;
    }

    @Override
    public void destroyProgressSupport() {
        task = null;
    }

    private final CIFSContext baseCtx;
    private final Map<String, CIFSContext> smbclients = new ConcurrentHashMap<>();
    private GenericDBHelper dbh;
    private final MainActivity mainActivity;

    static {
        // TODO restructure code using AsyncTask and remove policy loosening
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

    // BEGIN copied from JCIFS source
    private static final class CIFSConfigContextWrapper extends CIFSContextWrapper {
        private final DelegatingConfiguration cfg;

        CIFSConfigContextWrapper ( CIFSContext delegate, DelegatingConfiguration cfg ) {
            super(delegate);
            this.cfg = cfg;
        }

        @Override
        protected CIFSContext wrap ( CIFSContext newContext ) {
            return new CIFSConfigContextWrapper(super.wrap(newContext), this.cfg);
        }

        @Override
        public Configuration getConfig () {
            return this.cfg;
        }
    }


    protected static CIFSContext withConfig ( CIFSContext ctx, final DelegatingConfiguration cfg ) {
        return new CIFSConfigContextWrapper(ctx, cfg);
    }
    // END copied from JCIFS source

    public void closeAllSessions() {
        for (CIFSContext x : smbclients.values())
            try {x.close();} catch (Exception ignored) {}
        smbclients.clear();
    }

    public SmbProviderUsingPathContent(final Context context, final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.dbh = new GenericDBHelper(context);
        try {
            CIFSContext ctx = new BaseContext(new PropertyConfiguration(System.getProperties()));
            baseCtx = withConfig(ctx,new DelegatingConfiguration(ctx.getConfig()) {
                @Override
                public int getSoTimeout () {
                    return 1000;
                }

                @Override
                public int getConnTimeout () {
                    return 1000;
                }

                @Override
                public int getSessionTimeout () {
                    return 1000;
                }
            });
        } catch (CIFSException e) {
            throw new RuntimeException(e);
        }
    }

    public static SmbFile smbfileConcat(SmbFile dir, String filename, boolean... isDirectory) throws MalformedURLException {
        String a = dir.getURL().toString();
        if (a.endsWith("/")) a = a.substring(0,a.length()-1);
        if (filename.startsWith("/")) filename = filename.substring(1);
        return new SmbFile(a+"/"+filename+((isDirectory.length>0 && isDirectory[0])?"/":""),dir.getContext());
    }

    public void downloadSingleFile(SmbFile file, File localPath) throws IOException {
        try (SmbFileInputStream smbfis = new SmbFileInputStream(file);
             RobustLocalFileOutputStream fos = new RobustLocalFileOutputStream(localPath.getAbsolutePath())) {
            byte[] b = new byte[1048576];
            long prg = 0;
            ((XProgress)(task.mr)).incrementOuterProgressThenPublish(file.getContentLength());
            for(;;) {
                int readbytes = smbfis.read(b);
                if (readbytes <= 0) break;
                System.out.println("Read "+readbytes+" bytes");
                fos.write(b,0,readbytes);
                System.out.println("Written "+readbytes+" bytes");
                prg+=readbytes;
                ((XProgress)(task.mr)).publishInnerProgress(prg);
            }
        }
    }

    public void uploadSingleFile(File localFile, SmbFile file) throws IOException {
        try (RobustLocalFileInputStream fis = new RobustLocalFileInputStream(localFile.getAbsolutePath());
             SmbFileOutputStream smbfos = new SmbFileOutputStream(file)) {
            byte[] b = new byte[1048576];
            long prg = 0;
            ((XProgress)(task.mr)).incrementOuterProgressThenPublish(localFile.length());
            for(;;) {
                int readbytes = fis.read(b);
                if (readbytes <= 0) break;
                System.out.println("Read "+readbytes+" bytes");
                smbfos.write(b,0,readbytes);
                System.out.println("Written "+readbytes+" bytes");
                prg+=readbytes;
                ((XProgress)(task.mr)).publishInnerProgress(prg);
            }
        }
    }

    public void uploadFileOrDirectory(String localPath_, SmbFile remotePath) throws IOException {
        File localPath = new File(localPath_);
        if (localPath.isDirectory()) {
            remotePath.mkdirs();
            File[] dirContent = localPath.listFiles();
            if (dirContent != null)
                for (File f : dirContent)
                    uploadFileOrDirectory(f.getAbsolutePath(), smbfileConcat(remotePath,f.getName()));
        }
        else uploadSingleFile(localPath,remotePath);
    }

    public void downloadFileOrDirectory(SmbFile remotePath, String localPath_) throws IOException {
        File localPath = new File(localPath_);
        if(remotePath.isDirectory()) {
            if (!localPath.exists() && !localPath.mkdirs()) {
                System.err.println("Error creating local directory "+localPath_);
                return;
            }
            for (SmbFile fn : remotePath.listFiles())
                downloadFileOrDirectory(fn, new File(localPath,fn.getName()).getAbsolutePath());
        }
        else downloadSingleFile(remotePath,localPath);
    }

    public CIFSContext getChannel(SmbAuthData smbAuthData) {
        // try to get channel, if already existing
        CIFSContext cSMB = smbclients.get(smbAuthData.toString());
        if (cSMB == null) {
            cSMB = baseCtx.withCredentials(
                    new NtlmPasswordAuthentication(
                            baseCtx,
                            smbAuthData.domain,
                            smbAuthData.username,
                            smbAuthData.password));
            smbclients.put(smbAuthData.toString(),cSMB);
        }
        return cSMB;
    }

    @Override
    public void createFileOrDirectory(BasePathContent filePath, FileMode fileOrDirectory, FileCreationAdvancedOptions... fileOptions) throws IOException {

    }

    @Override
    public void createLink(BasePathContent originPath, BasePathContent linkPath, boolean isHardLink) throws IOException {

    }

    @Override
    public void deleteFilesOrDirectories(List<BasePathContent> files) throws IOException {

    }

    @Override
    public void copyMoveFilesToDirectory(CopyMoveListPathContent files, BasePathContent dstFolder) throws IOException {
        if (files.parentDir.providerType == ProviderType.LOCAL && dstFolder.providerType == ProviderType.SMB) { // upload
            CIFSContext cSMB = getChannel(((SmbRemotePathContent)dstFolder).smbAuthData);
            XProgress xp = (XProgress) task.mr;
            xp.clear();

            // count local files via local roothelper or xfilesopshelper and set them in xprogress
            long totalLocalSize = 0;
            for (BrowserItem localItem : files.files) {
                BasePathContent bpc = files.parentDir.concat(localItem.getFilename());
                if (MainActivity.xFilesUtils.isDir(bpc)) {
                    folderStats_resp fsr = MainActivity.xFilesUtils.statFolder(bpc);
                    totalLocalSize+=fsr.totalSize;
                }
                else {
                    totalLocalSize+=MainActivity.xFilesUtils.statFile(bpc).size;
                }
            }

            xp.totalFilesSize = totalLocalSize;
            xp.isDetailedProgress = true;

            try (SmbFile dst = ((SmbRemotePathContent) dstFolder).getSmbFile(cSMB,true)){
                for (BrowserItem localItem : files.files) {
                    String localName = localItem.getFilename();
                    uploadFileOrDirectory(files.parentDir.concat(localName).dir, smbfileConcat(dst,localName, localItem.isDirectory));
                }
            }
        }
        else if (files.parentDir.providerType == ProviderType.SMB && dstFolder.providerType == ProviderType.LOCAL) { // download
            CIFSContext cSMB = getChannel(((SmbRemotePathContent)files.parentDir).smbAuthData);
            ((XProgress)(task.mr)).totalFiles = Long.MAX_VALUE; // FIXME external progress disabled for now
            for (BrowserItem remoteItemName : files.files) { // iterator over filenames only
                // remote dir as local path string
                // ending "/" in order to paste a folder as a child of the destination folder
                try(SmbFile src = ((SmbRemotePathContent)files.parentDir).getSmbFile(cSMB,true)){
                    String remoteName = remoteItemName.getFilename();
                    downloadFileOrDirectory(smbfileConcat(src,remoteName),dstFolder.dir+"/"+remoteName);
                }
            }
        }
        else if (files.parentDir.providerType == ProviderType.SFTP && dstFolder.providerType == ProviderType.SFTP) {
            throw new IOException("To be implemented, if possible, for copy/move on same remote host");
        }
        else throw new IOException("Unsupported remote-to-remote copy");
    }

    @Override
    public boolean renameFile(BasePathContent oldPathname, BasePathContent newPathname) throws IOException {
        return false;
    }

    @Override
    public SingleStatsItem statFile(BasePathContent pathname) throws IOException {
        return null;
    }

    @Override
    public folderStats_resp statFiles(List<BasePathContent> files) throws IOException {
        return null;
    }

    @Override
    public folderStats_resp statFolder(BasePathContent pathname) throws IOException {
        return null;
    }

    @Override
    public boolean exists(BasePathContent pathname) {
        return false;
    }

    @Override
    public boolean isFile(BasePathContent pathname) {
        return false;
    }

    @Override
    public boolean isDir(BasePathContent pathname) {
        return false;
    }

    @Override
    public byte[] hashFile(BasePathContent pathname, HashRequestCodes hashAlgorithm) throws IOException {
        return new byte[0];
    }

    @Override
    public GenericDirWithContent listDirectory(BasePathContent directory) {
        SmbRemotePathContent g = (SmbRemotePathContent) directory;
        CIFSContext cSMB = getChannel(g.smbAuthData);

        try {
            SmbFile dirToList = g.getSmbFile(cSMB,true);
            List<BrowserItem> l = new ArrayList<>();
            for (SmbFile f : dirToList.listFiles()) {
                l.add(new BrowserItem(
                        f.getName(),
                        f.getContentLengthLong(),
                        new Date(f.getLastModified()),
                        f.isDirectory(),
                        false) // TODO get link info
                );
            }

            // successful return, change current helper
            MainActivity.currentHelper = MainActivity.smbProvider; // or = this
            return new SmbDirWithContent(g.smbAuthData,directory.dir,l);
        }
        catch (Exception e) {
            return new SmbDirWithContent(g.smbAuthData,FileOpsErrorCodes.COMMANDER_CANNOT_ACCESS);
        }
    }

    @Override
    public GenericDirWithContent listArchive(BasePathContent archivePath) {
        return null;
    }

    @Override
    public int compressToArchive(BasePathContent srcDirectory, BasePathContent destArchive, @Nullable Integer compressionLevel, @Nullable Boolean encryptHeaders, @Nullable Boolean solidMode, @Nullable String password, @Nullable List<String> filenames) throws IOException {
        return 0;
    }

    @Override
    public FileOpsErrorCodes extractFromArchive(BasePathContent srcArchive, BasePathContent destDirectory, @Nullable String password, @Nullable List<String> filenames) throws IOException {
        return null;
    }

    @Override
    public int setDates(BasePathContent file, @Nullable Date accessDate, @Nullable Date modificationDate) {
        return 0;
    }

    @Override
    public int setPermissions(BasePathContent file, int permMask) {
        return 0;
    }

    @Override
    public int setOwnership(BasePathContent file, @Nullable Integer ownerId, @Nullable Integer groupId) {
        return 0;
    }
}

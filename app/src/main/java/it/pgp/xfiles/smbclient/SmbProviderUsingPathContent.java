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
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.service.BaseBackgroundTask;
import it.pgp.xfiles.utils.FileOperationHelperUsingPathContent;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.SmbDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.SmbRemotePathContent;
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class SmbProviderUsingPathContent implements FileOperationHelperUsingPathContent {

    private final CIFSContext baseCtx;
    private final Map<String, CIFSContext> smbclients = new ConcurrentHashMap<>();
    private GenericDBHelper dbh;
    private final MainActivity mainActivity;

    static {
        // TODO restructure code using AsyncTask and remove policy loosening
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

    public SmbProviderUsingPathContent(final Context context, final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.dbh = new GenericDBHelper(context);
        try {
            this.baseCtx = new BaseContext(new PropertyConfiguration(System.getProperties()));
        } catch (CIFSException e) {
            throw new RuntimeException(e);
        }
    }

    public static SmbFile smbfileConcat(SmbFile dir, String filename) throws MalformedURLException {
        String a = dir.getURL().toString();
        if (a.endsWith("/")) a = a.substring(0,a.length()-1);
        if (filename.startsWith("/")) filename = filename.substring(1);
        return new SmbFile(a+"/"+filename,dir.getContext());
    }

    public static void downloadSingleFile(SmbFile file, File localPath) {
        try (SmbFileInputStream smbfis = new SmbFileInputStream(file);
             FileOutputStream fos = new FileOutputStream(localPath)) {
            byte[] b = new byte[1048576];
            for(;;) {
                int readbytes = smbfis.read(b);
                if (readbytes <= 0) break;
                System.out.println("Read "+readbytes+" bytes");
                fos.write(b,0,readbytes);
                System.out.println("Written "+readbytes+" bytes");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void uploadSingleFile(File localPath, SmbFile file) throws IOException {
        try (FileInputStream fis = new FileInputStream(localPath);
             SmbFileOutputStream smbfos = new SmbFileOutputStream(file)) {
            byte[] b = new byte[1048576];
            for(;;) {
                int readbytes = fis.read(b);
                if (readbytes <= 0) break;
                System.out.println("Read "+readbytes+" bytes");
                smbfos.write(b,0,readbytes);
                System.out.println("Written "+readbytes+" bytes");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void uploadFileOrDirectory(File localPath, SmbFile remotePath) throws IOException {
        if (localPath.isDirectory()) {
            remotePath.mkdirs();
            File[] dirContent = localPath.listFiles();
            if (dirContent != null)
                for (File f : dirContent)
                    uploadFileOrDirectory(f, smbfileConcat(remotePath,f.getName()));
        }
        else uploadSingleFile(localPath,remotePath);
    }

    public static void downloadFileOrDirectory(SmbFile remotePath, File localPath) throws IOException {
        if(remotePath.isDirectory()) {
            if (!localPath.exists() && !localPath.mkdirs()) {
                System.err.println("Error creating local directory "+localPath);
                return;
            }
            for (SmbFile fn : remotePath.listFiles())
                downloadFileOrDirectory(fn, new File(localPath,fn.getName()));
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
            // TODO check if port in address is canonical
            SmbFile dirToList = new SmbFile(
                    "smb://"+g.smbAuthData.host+":"+g.smbAuthData.port+g.dir+"/",cSMB);
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
            MainActivity.currentHelper = this; // TODO add static variable from SmbProvider to MainActivity
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

    @Override
    public void initProgressSupport(BaseBackgroundTask task) {

    }

    @Override
    public void destroyProgressSupport() {

    }
}

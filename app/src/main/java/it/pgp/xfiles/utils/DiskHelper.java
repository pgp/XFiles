package it.pgp.xfiles.utils;

import android.os.Environment;
import android.os.StatFs;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.RamdiskDialog;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.roothelperclient.RootHandler;
import it.pgp.xfiles.roothelperclient.RootHelperClient;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

// Web source:
// https://stackoverflow.com/questions/34375437/get-path-to-the-external-sdcard-in-android

public class DiskHelper {

    public enum MODES {
        MODE_INTERNAL,
        MODE_EXTERNAL,
        MODE_EXTERNAL_SD
    }

    public static final String[] commonInternalStoragePaths = {"/storage/emulated/0", "/sdcard"};

    private StatFs statFs;
    public String path;

    public DiskHelper(MODES mode) {
        try {
            switch(mode) {
                case MODE_INTERNAL:
                    path = Environment.getRootDirectory().getAbsolutePath();
                    statFs = new StatFs(path);
                    statFs.restat(path);
                    break;
                case MODE_EXTERNAL:
                    path = Environment.getExternalStorageDirectory().getAbsolutePath();
                    statFs = new StatFs(path);
                    statFs.restat(path);
                    break;
                case MODE_EXTERNAL_SD:
                    for(String str : getExternalMounts(null)) {
                        path = str;
                        statFs = new StatFs(str);
                        statFs.restat(str);
                        break;
                    }
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getTotalMemory() {
        if(statFs == null) return 0;
        return statFs.getBlockCountLong() * statFs.getBlockSizeLong();
    }

    public long getFreeMemory() {
        if(statFs == null) return 0;
        return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
    }

    public long getBusyMemory() {
        if(statFs == null) return 0;
        long total = getTotalMemory();
        long free = getFreeMemory();
        return total - free;
    }

    public static Set<String> getExternalMounts(MainActivity activity) {
        FileOperationHelper helper = activity.getFileOpsHelper(ProviderType.LOCAL);
        Set<String> out = new HashSet<>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|sdfat|fat32|ext3|ext4|fuseblk).*(rw,|ro,).*";
        StringBuilder sb = new StringBuilder();
        try {
            // root available, granted and enabled -> su -c mount
            // root not available, not granted or not enabled -> mount
            String[] command = (RootHandler.isRootAvailableAndGranted && helper instanceof RootHelperClient) ?
                    new String[]{"su","-c","mount"} : new String[]{"mount"};
            Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
            process.waitFor();
            InputStream is = process.getInputStream();
            byte[] buffer = new byte[1024];
            while(is.read(buffer) != -1) {
                sb.append(new String(buffer));
            }
            is.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        String[] lines = sb.toString().split("\n");
        for(String line : lines) {
            if(!line.toLowerCase().contains("asec") && line.matches(reg)) {
                String[] parts = line.split(" ");
                for(String part : parts)
                    if(part.startsWith("/") && !part.toLowerCase().contains("vold")) out.add(part);
            }
        }

        if(activity == null) return out;
        Set<String> out1 = new HashSet<>();
        // probe mount points found so far
        for(String mnt : out) {
            LocalPathContent lmnt = new LocalPathContent(mnt);
            if(!helper.isDir(lmnt)) {
                // try building fallback paths if they are not accessible
                String mnt1 = "/storage/" + lmnt.getName();
                if(!out1.contains(mnt1) && helper.isDir(new LocalPathContent(mnt1))) out1.add(mnt1);
            }
            else out1.add(mnt); // found mount point is readable without root
        }

        /*
        check common pathnames for internal storage
        motivation: on some devices (e.g. some Huawei phones), getExternalStorageDirectory() returns the external memory card, instead of the phone memory
        - indeed, given the API method name, THIS ONE should be the correct behaviour, but on the overwhelming majority of Android devices,
        that method returns the internal mass storage path instead - so in order to have also a bookmark for internal memory, we check commonly used
        path names as well
        */
        for(String mnt : commonInternalStoragePaths) {
            if(helper.isDir(new LocalPathContent(mnt))) {
                out1.add(mnt);
                break; // assume all the commonly used names represent the same partition, even if more than one is available
            }
        }

        /* if RAM disk is mounted, show it as well */
        if(helper.isDir(new LocalPathContent(RamdiskDialog.mountpath)))
            out1.add(RamdiskDialog.mountpath);
        return out1;
    }

    private static final long MEGABYTE = 1048576;

    public static String humanReadableByteCount(long bytes, boolean si, boolean showInMB) {
        if(showInMB) {
            long ret = bytes / MEGABYTE;
            return ret + " MB";
        }
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}

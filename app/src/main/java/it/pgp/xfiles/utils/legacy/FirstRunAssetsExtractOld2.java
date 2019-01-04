package it.pgp.xfiles.utils.legacy;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Deprecated on 07/01/18
 */

@Deprecated
public class FirstRunAssetsExtractOld2 {

//    static {
//        System.loadLibrary("multihash-lib");
//    }

    private static native String sha512OfFile(String filePath);

    // hashes are equal: true, different: false
    private static boolean checkHashOfLibFile(File filePath, File hash_f) {
        try {
            // load stored hash
            BufferedReader br = new BufferedReader(new FileReader(hash_f));
            String storedHash = br.readLine();
            br.close();

            // compute current hash
            String computedHash = sha512OfFile(filePath.getAbsolutePath());

            Log.e("HASHESHASHES","Expected: "+storedHash+" Actual: "+computedHash);

            return storedHash.toLowerCase().equals(computedHash.toLowerCase());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // duplicated from XFilesUtils
    private static void copyFilePlain(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    // copy and deobfuscate
    private static void copyFile(File sourceFile, File destFile) throws IOException {
        final byte obfuscationPattern = 0x12;
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        int length;
        int i;
        byte[] buffer = new byte[1048576];
        try(FileInputStream fi = new FileInputStream(sourceFile);
            FileOutputStream fo = new FileOutputStream(destFile)) {
            while ((length = fi.read(buffer)) > 0){
                for (i=0;i<length;i++) buffer[i] ^= obfuscationPattern;
                fo.write(buffer, 0, length);
            }
        }
    }

    private static final String rootHelperInstallName = "libr.so";
    private static final String _7zLibInstallName = "lib7z.so";

    public static final String rootHelperName = "r";
    private static final String _7zLibName = "7z.so";

    private static final String rootHelperHashName = "libr_h.so";
    private static final String _7zHashName = "lib7z_h.so";

    private static final String tlsCrtName = "libcrt.so";
    private static final String tlsDestCrtName = "dummycrt.pem";
    private static final String tlsKeyName = "libkey.so";
    private static final String tlsDestKeyName = "dummykey.pem";

    // for r and 7z.so
    public static void copyInstallNamesToRuntimeNames(final Context context) {
        File execDir = context.getFilesDir();

        // jniLibs extraction dir varies with ABI (generally, for 32-bit ABIs is /data/data/<appname>/lib,
        // while in 64-bit ones is /data/app/<appname>-1/lib/<abiname>
        // web source: https://stackoverflow.com/questions/39478884/armv8-library-not-installed-from-apk
        File libDir = new File(context.getApplicationInfo().nativeLibraryDir);

        File r = new File(execDir,rootHelperName);
        File _7z = new File(execDir,_7zLibName);

        File install_r = new File(libDir,rootHelperInstallName);
        File install_7z = new File(libDir,_7zLibInstallName);

        File hash_r = new File(libDir,rootHelperHashName);
        File hash_7z = new File(libDir,_7zHashName);

        File crtFile = new File(libDir,tlsCrtName);
        File keyFile = new File(libDir,tlsKeyName);
        File destCrtFile = new File(execDir,tlsDestCrtName);
        File destKeyFile = new File(execDir,tlsDestKeyName);

        try {
            // BEGIN
            // r
            if (r.exists()) {
                // not working, Gradle (or something else) changes modified timestamps
//                if (r.lastModified() != install_r.lastModified()) {
//                    Log.e(FirstRunAssetsExtract.class.getName(),"r modified,replacing with original...");
//                    r.delete();
//                    copyFile(install_r,r);
//                }
                if (!checkHashOfLibFile(r,hash_r)) {
                    Log.e(FirstRunAssetsExtractOld2.class.getName(),"r has been modified, restoring original one...");
                    r.delete();
                    copyFile(install_r,r);
                    if (!(r.setExecutable(true, true))) {
                        Log.e(FirstRunAssetsExtractOld2.class.getName(), "Unable to set exec permission to r");
                    }
                }

                if (!r.canExecute()) {
                    if (!(r.setExecutable(true, true))) {
                        Log.e(FirstRunAssetsExtractOld2.class.getName(), "Unable to set exec permission to r");
                    }
                }
            }
            else {
                Log.e(FirstRunAssetsExtractOld2.class.getName(),"copying r to files dir...");
                copyFile(install_r,r);
                if (!(r.setExecutable(true, true))) {
                    Log.e(FirstRunAssetsExtractOld2.class.getName(), "Unable to set exec permission to r");
                }
            }

            // 7z.so
            if (_7z.exists()) {
//                if (_7z.lastModified() != install_7z.lastModified()) {
//                    Log.e(FirstRunAssetsExtract.class.getName(),"lib7z modified,replacing with original...");
//                    _7z.delete();
//                    copyFile(install_7z,_7z);
//                }
                if (!checkHashOfLibFile(_7z,hash_7z)) {
                    Log.e(FirstRunAssetsExtractOld2.class.getName(),_7z.getName()+" has been modified, restoring original one...");
                    _7z.delete();
                    copyFile(install_7z,_7z);
                    if (!(r.setExecutable(true, true))) {
                        Log.e(FirstRunAssetsExtractOld2.class.getName(), "Unable to set exec permission to "+_7z.getName());
                    }
                }
            }
            else {
                Log.e(FirstRunAssetsExtractOld2.class.getName(),"copying lib7z to files dir...");
                copyFile(install_7z,_7z);
            }

            // dummy crt & key for TLS server
            if (!destCrtFile.exists()) {
                Log.d(FirstRunAssetsExtractOld2.class.getName(),"Copying tls dummy cert");
                copyFilePlain(crtFile,destCrtFile);
            }
            if (!destKeyFile.exists()) {
                Log.d(FirstRunAssetsExtractOld2.class.getName(),"Copying tls dummy key");
                copyFilePlain(keyFile,destKeyFile);
            }

            // END

            // legacy
//            if (r.exists() && _7z.exists() && r.canExecute()) return;
//            if(!r.exists()) copyFile(install_r,r);
//            if(!_7z.exists()) copyFile(install_7z,_7z);
//
//            if (!(r.setExecutable(true, true) && _7z.setExecutable(true,true))) {
//                Log.e(FirstRunAssetsExtract.class.getName(), "Unable to set executable");
//            }
        }
        catch (IOException i) {
            Log.e(FirstRunAssetsExtractOld2.class.getName(),"Unable to copy assets from lib dir");
        }
    }
}

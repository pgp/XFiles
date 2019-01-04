package it.pgp.xfiles.roothelperclient;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by pgp on 01/02/17
 */

public class FirstRunAssetsExtract {

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

    public static final String rootHelperInstallName = "libr.so";
    private static final String _7zLibInstallName = "lib7z.so";

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

        File crtFile = new File(libDir,tlsCrtName);
        File keyFile = new File(libDir,tlsKeyName);
        File destCrtFile = new File(execDir,tlsDestCrtName);
        File destKeyFile = new File(execDir,tlsDestKeyName);

        try {
            // FIXME to be removed together with this entire method once TLS cert/key generation is done on first XRE usage
            // dummy crt & key for TLS server
            if (!destCrtFile.exists()) {
                Log.d(FirstRunAssetsExtract.class.getName(),"Copying tls dummy cert");
                copyFilePlain(crtFile,destCrtFile);
            }
            if (!destKeyFile.exists()) {
                Log.d(FirstRunAssetsExtract.class.getName(),"Copying tls dummy key");
                copyFilePlain(keyFile,destKeyFile);
            }
        }
        catch (IOException i) {
            Log.e(FirstRunAssetsExtract.class.getName(),"Unable to copy assets from lib dir");
        }
    }
}
